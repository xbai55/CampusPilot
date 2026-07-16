package com.campuspilot.service;

import com.campuspilot.config.AppConfig;
import com.campuspilot.kingdee.KingdeeClient;
import com.campuspilot.store.InMemoryCampusPilotStore;
import com.campuspilot.util.Json;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Implements the documented Kingdee Agent OpenAPI assistant/session/chat flow. */
public final class AgentClient {
    private static final String ASSISTANTS = "/v2/gai/assistants";
    private static final String NEW_SESSION = "/v2/gai/newsession";
    private static final String CHAT = "/v2/gai/chat";

    private final AppConfig config;
    private final InMemoryCampusPilotStore store;
    private final KingdeeDataClient kingdeeDataClient;
    private final KingdeeClient openApiClient;
    private final String callbackToken;
    private final Map<String, CompletableFuture<String>> pending = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> callbackText = new ConcurrentHashMap<>();
    private final Map<String, String> completed = new ConcurrentHashMap<>();
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
                store.addAudit("系统", "金蝶 Agent OpenAPI 调用失败，已启用本地兜底：" + lastError);
            }
        }
        store.addAudit(roleOrAgent(role), "使用本地 Agent 兜底建议：" + shortText(normalized));
        return localFallback(normalized);
    }

    private String remoteChat(String question, String userName, String role,
                              String sessionKey, boolean retryExpiredSession) throws Exception {
        String assistantId = assistantId();
        String sessionId = sessionId(assistantId, sessionKey);
        String chatTraceId = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();
        pending.put(chatTraceId, future);
        String body = Json.object(
                Json.field("sessionId", sessionId),
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
            store.addAudit(roleOrAgent(role), "调用金蝶 Agent OpenAPI：" + shortText(question));
            return Json.object(
                    Json.field("answer", answer),
                    Json.rawField("chips", Json.stringArray(List.of("金蝶 Agent OpenAPI", "自动助手发现", "会话回调"))),
                    Json.field("source", "Kingdee Agent OpenAPI"),
                    Json.field("assistantId", assistantId), Json.field("sessionId", sessionId),
                    Json.field("chatTraceId", chatTraceId),
                    Json.field("requestTraceId", platformTraceId),
                    Json.field("dataSource", "kingdee-agent"), Json.boolField("demo", false)
            );
        } catch (TimeoutException ex) {
            pending.remove(chatTraceId);
            lastSuccessAt = System.currentTimeMillis();
            lastError = "等待 Agent 回调超时";
            return Json.object(
                    Json.field("answer", "Agent 已接收请求，回答仍在处理中。"),
                    Json.rawField("chips", Json.stringArray(List.of("异步处理中", "稍后查询", "Agent OpenAPI"))),
                    Json.field("source", "Kingdee Agent OpenAPI"),
                    Json.field("assistantId", assistantId), Json.field("sessionId", sessionId),
                    Json.field("chatTraceId", chatTraceId), Json.boolField("pending", true),
                    Json.field("dataSource", "kingdee-agent"), Json.boolField("demo", false)
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
        String traceId = KingdeeClient.jsonString(body, "chatTraceId");
        if (traceId.isBlank()) traceId = KingdeeClient.jsonString(body, "traceId");
        String message = KingdeeClient.jsonString(body, "message");
        String error = KingdeeClient.jsonString(body, "desc");
        if (!message.isBlank()) callbackText.computeIfAbsent(traceId, ignored -> new StringBuilder()).append(message);
        boolean done = body.contains(Json.field("type", "streamDone"))
                || body.contains(Json.field("type", "waitingDone"))
                || body.contains(Json.field("type", "error"));
        if (done && !traceId.isBlank()) {
            String answer = callbackText.getOrDefault(traceId, new StringBuilder()).toString();
            if (answer.isBlank() && completed.containsKey(traceId)) answer = completed.get(traceId);
            if (answer.isBlank()) answer = error.isBlank() ? "Agent 已完成处理。" : error;
            completed.put(traceId, answer);
            CompletableFuture<String> future = pending.remove(traceId);
            if (future != null) future.complete(answer);
            callbackText.remove(traceId);
        }
        return Json.object(Json.boolField("ok", true), Json.field("errorCode", "0"), Json.field("message", ""));
    }

    public String resultJson(String traceId) {
        String answer = completed.get(traceId);
        return Json.object(
                Json.boolField("ok", answer != null),
                Json.boolField("pending", answer == null),
                Json.field("chatTraceId", traceId),
                Json.field("answer", answer == null ? "" : answer)
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
        String answer;
        List<String> chips;
        if (question.contains("预警") || question.contains("风险")) {
            answer = "建议先核对画像、成绩和行为证据，再由辅导员确认预警、导师制定补强计划。";
            chips = List.of("生成预警草稿", "导师帮扶", "复评结案");
        } else if (question.contains("课程") || question.contains("成绩")) {
            answer = "可以结合课程成绩、出勤和作业完成率生成课程补强任务。";
            chips = List.of("课程短板", "补强计划", "成长画像");
        } else {
            answer = "可以从学生画像、课程成绩、学习行为和风险预警数据形成识别、确认、帮扶、反馈、结案闭环。";
            chips = List.of("画像", "闭环", "驾驶舱");
        }
        return Json.object(
                Json.field("answer", answer), Json.rawField("chips", Json.stringArray(chips)),
                Json.field("source", "CampusPilot Java Local Fallback"),
                Json.field("dataSource", "local-demo"), Json.boolField("demo", true)
        );
    }

    private static String roleOrAgent(String role) {
        return role == null || role.isBlank() ? "CampusPilot Agent" : role;
    }

    private static String shortText(String value) {
        if (value == null || value.isBlank()) return "空请求";
        return value.length() > 160 ? value.substring(0, 160) : value;
    }
}
