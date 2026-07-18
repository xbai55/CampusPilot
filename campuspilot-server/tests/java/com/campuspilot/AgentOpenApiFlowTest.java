package com.campuspilot;

import com.campuspilot.config.AppConfig;
import com.campuspilot.http.ApiHandler;
import com.campuspilot.service.AgentClient;
import com.campuspilot.service.KingdeeDataClient;
import com.campuspilot.store.InMemoryCampusPilotStore;
import com.campuspilot.util.Json;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

final class AgentOpenApiFlowTest {
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    static void run() throws Exception {
        fullOpenApiFlowUsesOfficialCallbackShape();
        callbackParserCases();
    }

    private static void fullOpenApiFlowUsesOfficialCallbackShape() throws Exception {
        Harness harness = new Harness();
        try {
            AtomicInteger tokenCalls = new AtomicInteger();
            AtomicInteger sessionCalls = new AtomicInteger();
            AtomicInteger chatCalls = new AtomicInteger();
            harness.upstream.createContext("/ierp/kapi/oauth2/getToken", exchange -> {
                tokenCalls.incrementAndGet();
                TestSupport.require("POST".equals(exchange.getRequestMethod()), "token must use POST");
                TestSupport.json(exchange, 200, Json.object(
                        Json.rawField("data", Json.object(
                                Json.field("access_token", "dynamic-token"), Json.intField("expires_in", 7200000)
                        )), Json.boolField("status", true)
                ));
            });
            harness.upstream.createContext("/ierp/kapi/v2/gai/newsession", exchange -> {
                sessionCalls.incrementAndGet();
                requireOpenApiPost(exchange.getRequestMethod(), exchange.getRequestHeaders().getFirst("accessToken"));
                TestSupport.json(exchange, 200, Json.object(
                        Json.field("errCode", "0"),
                        Json.rawField("data", Json.object(Json.field("sessionId", "session-001")))
                ));
            });
            harness.upstream.createContext("/ierp/kapi/v2/gai/chat", exchange -> {
                chatCalls.incrementAndGet();
                requireOpenApiPost(exchange.getRequestMethod(), exchange.getRequestHeaders().getFirst("accessToken"));
                TestSupport.require("dynamic-token".equals(exchange.getRequestHeaders().getFirst("access_token")),
                        "chat request must include access_token header");
                TestSupport.require("Bearer dynamic-token".equals(exchange.getRequestHeaders().getFirst("Authorization")),
                        "chat request must include Authorization bearer header");
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String traceId = com.campuspilot.kingdee.KingdeeClient.jsonString(body, "chatTraceId");
                TestSupport.json(exchange, 200, Json.object(
                        Json.field("errCode", "0"),
                        Json.rawField("data", Json.object(Json.field("traceId", traceId)))
                ));
                Thread.startVirtualThread(() -> postCallback(harness.apiBase(), traceId,
                        officialCallback("session-001", traceId, List.of(
                                chat("OpenAPI mock answer"),
                                action("waitingDone", Json.object(Json.field("id", "session-001")))
                        )), "callback-test"));
            });
            harness.start();

            HttpResponse<String> first = chat(harness.apiBase(), "risk analysis");
            TestSupport.require(first.statusCode() == 200, "Agent chat must return HTTP 200");
            TestSupport.require(first.body().contains("OpenAPI mock answer"),
                    "official callback answer must be returned to the caller");
            TestSupport.require(first.body().contains(Json.boolField("demo", false)),
                    "successful Agent OpenAPI response must not be marked as demo");

            HttpResponse<String> second = chat(harness.apiBase(), "course advice");
            TestSupport.require(second.statusCode() == 200 && second.body().contains("OpenAPI mock answer"),
                    "the cached Agent session must support another chat");
            HttpResponse<String> anotherUser = chat(harness.apiBase(), "profile advice", "Another-User");
            TestSupport.require(anotherUser.statusCode() == 200,
                    "another authenticated user must receive an independent Agent session");
            TestSupport.require(sessionCalls.get() == 2, "Agent sessions should be cached separately per user");
            TestSupport.require(chatCalls.get() == 3, "each question should call /ierp/kapi/v2/gai/chat");
            TestSupport.require(tokenCalls.get() == 1, "accessToken should be acquired once and cached");
        } finally {
            harness.close();
        }
    }

    private static void callbackParserCases() throws Exception {
        Harness harness = new Harness();
        try {
            harness.start();
            String splitTrace = "trace-split";
            postCallback(harness.apiBase(), splitTrace, officialCallback("s1", splitTrace, List.of(chat("Hello "))), "callback-test");
            postCallback(harness.apiBase(), splitTrace, officialCallback("s1", splitTrace, List.of(chat("world"))), "callback-test");
            postCallback(harness.apiBase(), splitTrace, officialCallback("s1", splitTrace,
                    List.of(action("waitingDone", Json.object(Json.field("id", "s1"))))), "callback-test");
            TestSupport.require(result(harness.apiBase(), splitTrace).contains("Hello world"),
                    "multiple chat callbacks must be concatenated before waitingDone completes");
            postCallback(harness.apiBase(), splitTrace, officialCallback("s1", splitTrace,
                    List.of(action("waitingDone", Json.object(Json.field("id", "s1"))))), "callback-test");
            String splitResult = result(harness.apiBase(), splitTrace);
            TestSupport.require(splitResult.contains("Hello world"),
                    "later completion-only callbacks must not overwrite an existing answer");
            TestSupport.require(splitResult.contains(Json.quote(officialCallback("s1", splitTrace, List.of(chat("Hello "))))),
                    "result endpoint must include all callback events for the trace");

            String doneOnlyTrace = "trace-done-only";
            postCallback(harness.apiBase(), doneOnlyTrace, officialCallback("s2", doneOnlyTrace,
                    List.of(action("waitingDone", Json.object(Json.field("id", "s2"))))), "callback-test");
            TestSupport.require(result(harness.apiBase(), doneOnlyTrace)
                            .contains("Agent only returned completion events and did not return answer text."),
                    "completion-only callbacks must return a diagnostic instead of a fake answer");

            String errorTrace = "trace-error";
            postCallback(harness.apiBase(), errorTrace, officialCallback("s3", errorTrace,
                    List.of(action("error", Json.object(Json.field("desc", "agent failed"))))), "callback-test");
            TestSupport.require(result(harness.apiBase(), errorTrace).contains("Agent returned error: agent failed"),
                    "error callbacks must expose the error description");

            HttpResponse<String> invalid = postCallback(harness.apiBase(), "bad-token",
                    officialCallback("s4", "bad-token", List.of(chat("ignored"))), "wrong-token");
            TestSupport.require(invalid.statusCode() == 401 && invalid.body().contains("invalid callback token"),
                    "invalid callback token must be rejected");

            String nestedTrace = "trace-nested-only";
            String nestedOnly = Json.object(
                    Json.field("sessionId", "s5"),
                    Json.rawField("message", Json.object(
                            Json.field("chatTraceId", nestedTrace),
                            Json.rawField("actionList", Json.array(List.of(
                                    chat("nested trace text"),
                                    action("waitingDone", Json.object(Json.field("id", "s5")))
                            )))
                    ))
            );
            postCallback(harness.apiBase(), nestedTrace, nestedOnly, "callback-test");
            TestSupport.require(result(harness.apiBase(), nestedTrace).contains("nested trace text"),
                    "chatTraceId inside message object must be supported");

            TestSupport.require(result(harness.apiBase(), splitTrace).contains(Json.field("callbackBody", "")) == false,
                    "result endpoint must include the stored callback body");
        } finally {
            harness.close();
        }
    }

    private static void requireOpenApiPost(String method, String accessToken) {
        TestSupport.require("POST".equals(method), "Agent OpenAPI endpoints must use POST");
        TestSupport.require("dynamic-token".equals(accessToken),
                "Agent OpenAPI must use the server-side accessToken header");
    }

    private static HttpResponse<String> chat(String apiBase, String question) throws Exception {
        return chat(apiBase, question, "CampusPilot-Test");
    }

    private static HttpResponse<String> chat(String apiBase, String question, String userName) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase + "/api/campuspilot/agent/chat"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer api-secret")
                .header("X-CampusPilot-User", userName)
                .header("X-CampusPilot-Role-Key", "counselor")
                .POST(HttpRequest.BodyPublishers.ofString(Json.object(Json.field("question", question)), StandardCharsets.UTF_8))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> postCallback(String apiBase, String traceId, String body, String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase + "/api/campuspilot/agent/callback?token=" + token))
                    .header("Content-Type", "application/json")
                    .header("access_token", token)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new RuntimeException("callback failed for " + traceId, ex);
        }
    }

    private static String result(String apiBase, String traceId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase + "/api/campuspilot/agent/results/" + traceId))
                .header("Authorization", "Bearer api-secret")
                .header("X-CampusPilot-User", "CampusPilot-Test")
                .header("X-CampusPilot-Role-Key", "counselor")
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        TestSupport.require(response.statusCode() == 200, "result endpoint must return HTTP 200");
        return response.body();
    }

    private static String officialCallback(String sessionId, String traceId, List<String> actions) {
        return Json.object(
                Json.field("sessionId", sessionId),
                Json.rawField("message", Json.object(
                        Json.rawField("actionList", Json.array(actions)),
                        Json.field("time", "1784386554914"),
                        Json.field("chatSessionId", sessionId),
                        Json.field("chatTraceId", traceId)
                ))
        );
    }

    private static String chat(String message) {
        return action("chat", Json.object(Json.field("id", "session-001"),
                Json.field("message", message), Json.boolField("stream", true)));
    }

    private static String action(String type, String data) {
        return Json.object(Json.field("type", type), Json.rawField("data", data));
    }

    private static final class Harness {
        private final HttpServer upstream;
        private final HttpServer api;
        private final ExecutorService apiExecutor = Executors.newCachedThreadPool();
        private final AtomicReference<String> apiBase = new AtomicReference<>();

        private Harness() throws Exception {
            upstream = TestSupport.server();
            api = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            apiBase.set(TestSupport.baseUrl(api));
            AppConfig config = TestSupport.config(TestSupport.baseUrl(upstream), "", true,
                    "", "", "api-secret", "http://127.0.0.1:8881",
                    apiBase.get(), "2522880941110602754", "callback-test", 3000);
            InMemoryCampusPilotStore store = new InMemoryCampusPilotStore(config);
            KingdeeDataClient dataClient = new KingdeeDataClient(config, store);
            AgentClient agentClient = new AgentClient(config, store, dataClient);
            api.setExecutor(apiExecutor);
            api.createContext("/api/campuspilot", new ApiHandler(config, store, agentClient, dataClient));
        }

        private String apiBase() {
            return apiBase.get();
        }

        private void start() {
            api.start();
        }

        private void close() {
            api.stop(0);
            upstream.stop(0);
            apiExecutor.shutdownNow();
        }
    }
}
