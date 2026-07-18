package com.campuspilot.service;

import com.campuspilot.config.AppConfig;
import com.campuspilot.kingdee.KingdeeClient;
import com.campuspilot.store.InMemoryCampusPilotStore;
import com.campuspilot.util.Json;
import com.campuspilot.util.JsonParser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Implements the Kingdee Agent assistants/session/chat callback OpenAPI flow. */
public final class AgentClient {
    private static final String ASSISTANTS = "/v2/gai/assistants";
    private static final String NEW_SESSION = "/v2/gai/newsession";
    private static final String CHAT = "/v2/gai/chat";
    private static final String NO_AGENT_TEXT = "Agent only returned completion events and did not return answer text.";

    private final AppConfig config;
    private final InMemoryCampusPilotStore store;
    private final KingdeeDataClient kingdeeDataClient;
    private final KingdeeClient openApiClient;
    private final String callbackToken;
    private final Map<String, CompletableFuture<String>> pending = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> callbackText = new ConcurrentHashMap<>();
    private final Map<String, String> completed = new ConcurrentHashMap<>();
    private final Map<String, String> callbackBodies = new ConcurrentHashMap<>();
    private final Map<String, List<String>> callbackEvents = new ConcurrentHashMap<>();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private volatile String cachedAssistantId = "";
    private volatile long lastSuccessAt;
    private volatile String lastError = "";

    public AgentClient(AppConfig config, InMemoryCampusPilotStore store, KingdeeDataClient kingdeeDataClient) {
        this.config = config;
        this.store = store;
        this.kingdeeDataClient = kingdeeDataClient;
        this.openApiClient = kingdeeDataClient.kingdeeClient();
        this.callbackToken = config.agentCallbackToken().isBlank()
                ? UUID.randomUUID().toString() : config.agentCallbackToken();
    }

    public String chat(String question, String userName, String role) {
        String normalized = question == null ? "" : question.trim();
        String sessionKey = (userName == null ? "" : userName) + "|" + (role == null ? "" : role);
        if (config.agentOpenApiConfigured()) {
            try {
                return remoteChat(normalized, userName, role, sessionKey, true);
            } catch (Exception ex) {
                lastError = shortText(ex.getMessage());
                store.addAudit("system", "Kingdee Agent OpenAPI call failed, using local fallback: " + lastError);
            }
        }
        store.addAudit(roleOrAgent(role), "Using local Agent fallback: " + shortText(normalized));
        return localFallback(normalized);
    }

    private String remoteChat(String question, String userName, String role,
                              String sessionKey, boolean retryExpiredSession) throws Exception {
        String assistantId = assistantId();
        String sessionId = sessionId(assistantId, sessionKey);
        String chatTraceId = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();
        pending.put(chatTraceId, future);
        String callbackUrl = config.publicBaseUrl() + "/api/campuspilot/agent/callback?token="
                + URLEncoder.encode(callbackToken, StandardCharsets.UTF_8);
        String body = Json.object(
                Json.field("sessionId", sessionId),
                Json.field("callbackUrl", callbackUrl),
                Json.field("chatTraceId", chatTraceId),
                Json.rawField("message", Json.object(
                        Json.rawField("inputParams", Json.object(
                                Json.field("role", role == null ? "" : role),
                                Json.field("userName", userName == null ? "" : userName),
                                Json.field("campusContext", kingdeeDataClient.agentContextJson())
                        )),
                        Json.field("query", question)
                ))
        );
        try {
            String accepted = openApiClient.postOpenApiJson(CHAT, body);
            String platformTraceId = KingdeeClient.jsonString(accepted, "traceId");
            String answer = future.get(config.agentResponseWaitMs(), TimeUnit.MILLISECONDS);
            lastSuccessAt = System.currentTimeMillis();
            lastError = "";
            store.addAudit(roleOrAgent(role), "Called Kingdee Agent OpenAPI: " + shortText(question));
            return Json.object(
                    Json.field("answer", answer),
                    Json.rawField("chips", Json.stringArray(List.of("Kingdee Agent OpenAPI", "callback", "session"))),
                    Json.field("source", "Kingdee Agent OpenAPI"),
                    Json.field("assistantId", assistantId),
                    Json.field("sessionId", sessionId),
                    Json.field("chatTraceId", chatTraceId),
                    Json.field("requestTraceId", platformTraceId),
                    Json.field("dataSource", "kingdee-agent"),
                    Json.boolField("demo", false)
            );
        } catch (TimeoutException ex) {
            pending.remove(chatTraceId);
            lastSuccessAt = System.currentTimeMillis();
            lastError = "waiting for Agent callback timed out";
            return Json.object(
                    Json.field("answer", "Agent has accepted the request and is still processing."),
                    Json.rawField("chips", Json.stringArray(List.of("pending", "callback", "Agent OpenAPI"))),
                    Json.field("source", "Kingdee Agent OpenAPI"),
                    Json.field("assistantId", assistantId),
                    Json.field("sessionId", sessionId),
                    Json.field("chatTraceId", chatTraceId),
                    Json.boolField("pending", true),
                    Json.field("dataSource", "kingdee-agent"),
                    Json.boolField("demo", false)
            );
        } catch (Exception ex) {
            pending.remove(chatTraceId);
            if (retryExpiredSession && shortText(ex.getMessage()).contains("ai.gai.900001")) {
                sessions.remove(sessionKey);
                return remoteChat(question, userName, role, sessionKey, false);
            }
            throw ex;
        }
    }

    public String callbackJson(String body, String receivedToken) {
        if (!callbackToken.equals(receivedToken)) {
            return Json.object(Json.boolField("ok", false), Json.field("message", "invalid callback token"));
        }
        CallbackEvent event;
        try {
            event = parseCallback(body);
        } catch (Exception ex) {
            return Json.object(Json.boolField("ok", false), Json.field("errorCode", "400"),
                    Json.field("message", "invalid callback json: " + shortText(ex.getMessage())));
        }
        String traceId = event.traceId();
        if (!traceId.isBlank()) {
            String rawBody = body == null ? "" : body;
            callbackBodies.put(traceId, rawBody);
            callbackEvents.computeIfAbsent(traceId, ignored -> new CopyOnWriteArrayList<>()).add(rawBody);
        }
        if (!event.text().isBlank()) {
            callbackText.computeIfAbsent(traceId, ignored -> new StringBuilder()).append(event.text());
        }
        if (event.done() && !traceId.isBlank()) {
            String answer = callbackText.getOrDefault(traceId, new StringBuilder()).toString();
            if (answer.isBlank() && completed.containsKey(traceId)) answer = completed.get(traceId);
            if (answer.isBlank()) answer = event.error().isBlank() ? NO_AGENT_TEXT : "Agent returned error: " + event.error();
            complete(traceId, answer);
        }
        return Json.object(Json.boolField("ok", true), Json.field("errorCode", "0"), Json.field("message", ""));
    }

    public String resultJson(String traceId) {
        String answer = completed.get(traceId);
        return Json.object(
                Json.boolField("ok", answer != null),
                Json.boolField("pending", answer == null),
                Json.field("chatTraceId", traceId),
                Json.field("answer", answer == null ? "" : answer),
                Json.field("callbackBody", callbackBodies.getOrDefault(traceId, "")),
                Json.rawField("callbackEvents", Json.stringArray(callbackEvents.getOrDefault(traceId, List.of())))
        );
    }

    public String statusJson() {
        String state = !config.agentOpenApiConfigured() ? "unconfigured"
                : lastSuccessAt > 0 ? "connected"
                : lastError.isBlank() ? "configured-unverified" : "failed";
        return Json.object(
                Json.boolField("configured", config.agentOpenApiConfigured()),
                Json.field("state", state),
                Json.field("assistantName", config.agentName()),
                Json.field("assistantId", cachedAssistantId.isBlank() ? config.agentAssistantId() : cachedAssistantId),
                Json.intField("activeSessionCount", sessions.size()),
                Json.field("lastSuccessAt", lastSuccessAt == 0 ? "" : Instant.ofEpochMilli(lastSuccessAt).toString()),
                Json.field("lastError", lastError)
        );
    }

    private synchronized String assistantId() {
        if (!cachedAssistantId.isBlank()) return cachedAssistantId;
        if (!config.agentAssistantId().isBlank()) {
            cachedAssistantId = config.agentAssistantId();
            return cachedAssistantId;
        }
        String response = openApiClient.postOpenApiJson(ASSISTANTS, "{}");
        for (Map<String, String> assistant : KingdeeClient.parseObjectArray(response, "data")) {
            if (config.agentName().equals(assistant.getOrDefault("name", ""))) {
                cachedAssistantId = assistant.getOrDefault("id", "");
                break;
            }
        }
        if (cachedAssistantId.isBlank()) {
            throw new KingdeeClient.KingdeeException("Agent assistant not found: " + config.agentName());
        }
        return cachedAssistantId;
    }

    private synchronized String sessionId(String assistantId, String sessionKey) {
        String cachedSessionId = sessions.getOrDefault(sessionKey, "");
        if (!cachedSessionId.isBlank()) return cachedSessionId;
        String callbackUrl = config.publicBaseUrl() + "/api/campuspilot/agent/callback?token="
                + URLEncoder.encode(callbackToken, StandardCharsets.UTF_8);
        String response = openApiClient.postOpenApiJson(NEW_SESSION, Json.object(
                Json.field("assistantId", assistantId), Json.field("callbackUrl", callbackUrl)
        ));
        cachedSessionId = KingdeeClient.jsonString(response, "sessionId");
        if (cachedSessionId.isBlank()) throw new KingdeeClient.KingdeeException("Agent session response is missing sessionId");
        sessions.put(sessionKey, cachedSessionId);
        return cachedSessionId;
    }

    private String localFallback(String question) {
        String answer = "可以从学生画像、课程成绩、学习行为和风险预警数据形成识别、确认、帮扶、反馈、结案闭环。";
        return Json.object(
                Json.field("answer", answer),
                Json.rawField("chips", Json.stringArray(List.of("画像", "闭环", "驾驶舱"))),
                Json.field("source", "CampusPilot Java Local Fallback"),
                Json.field("dataSource", "local-demo"),
                Json.boolField("demo", true)
        );
    }

    private static String roleOrAgent(String role) {
        return role == null || role.isBlank() ? "CampusPilot Agent" : role;
    }

    private static String shortText(String value) {
        if (value == null || value.isBlank()) return "empty request";
        return value.length() > 160 ? value.substring(0, 160) : value;
    }

    private void complete(String traceId, String answer) {
        completed.put(traceId, answer);
        CompletableFuture<String> future = pending.remove(traceId);
        if (future != null) future.complete(answer);
        callbackText.remove(traceId);
    }

    @SuppressWarnings("unchecked")
    private static CallbackEvent parseCallback(String body) {
        Map<String, Object> root = JsonParser.object(body);
        Object messageValue = root.get("message");
        Map<String, Object> message = messageValue instanceof Map<?, ?> map ? (Map<String, Object>) map : root;
        String traceId = string(message.get("chatTraceId"));
        if (traceId.isBlank()) traceId = string(root.get("chatTraceId"));
        if (traceId.isBlank()) traceId = string(root.get("traceId"));
        StringBuilder text = new StringBuilder();
        String error = "";
        boolean done = false;
        Object actionList = message.get("actionList");
        if (actionList instanceof List<?> actions) {
            for (Object actionValue : actions) {
                if (!(actionValue instanceof Map<?, ?> action)) continue;
                String type = string(action.get("type"));
                Object dataValue = action.get("data");
                Map<String, Object> data = dataValue instanceof Map<?, ?> dataMap
                        ? (Map<String, Object>) dataMap : Map.of();
                if (traceId.isBlank()) traceId = string(data.get("chatTraceId"));
                if ("chat".equals(type)) {
                    text.append(string(data.get("message")));
                } else if ("multiMsg".equals(type)) {
                    appendMultiMsgText(text, data.get("msgList"));
                } else if ("error".equals(type)) {
                    done = true;
                    error = firstNonBlank(string(data.get("desc")), string(data.get("message")), string(data.get("code")));
                } else if ("streamDone".equals(type) || "waitingDone".equals(type)) {
                    done = true;
                }
            }
        }
        return new CallbackEvent(traceId, text.toString(), done, error);
    }

    @SuppressWarnings("unchecked")
    private static void appendMultiMsgText(StringBuilder text, Object msgListValue) {
        if (!(msgListValue instanceof List<?> msgList)) return;
        for (Object itemValue : msgList) {
            if (!(itemValue instanceof Map<?, ?> item)) continue;
            if ("text".equals(string(item.get("type")))) {
                if (!text.isEmpty()) text.append('\n');
                text.append(string(((Map<String, Object>) item).get("value")));
            }
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record CallbackEvent(String traceId, String text, boolean done, String error) {}
}
