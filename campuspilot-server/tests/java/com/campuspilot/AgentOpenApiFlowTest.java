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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

final class AgentOpenApiFlowTest {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String ASSISTANT_NAME = "CampusPilot 启航智伴学业成长助手";

    static void run() throws Exception {
        HttpServer upstream = TestSupport.server();
        HttpServer api = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ExecutorService apiExecutor = Executors.newCachedThreadPool();
        AtomicInteger tokenCalls = new AtomicInteger();
        AtomicInteger assistantCalls = new AtomicInteger();
        AtomicInteger sessionCalls = new AtomicInteger();
        AtomicInteger chatCalls = new AtomicInteger();
        try {
            String apiBase = TestSupport.baseUrl(api);
            AppConfig config = TestSupport.config(TestSupport.baseUrl(upstream), "", true,
                    "", "", "api-secret", "http://127.0.0.1:8881",
                    apiBase, "", "callback-test", 3000);
            InMemoryCampusPilotStore store = new InMemoryCampusPilotStore(config);
            KingdeeDataClient dataClient = new KingdeeDataClient(config, store);
            AgentClient agentClient = new AgentClient(config, store, dataClient);
            ApiHandler handler = new ApiHandler(config, store, agentClient, dataClient);

            upstream.createContext("/kapi/oauth2/getToken", exchange -> {
                tokenCalls.incrementAndGet();
                TestSupport.require("POST".equals(exchange.getRequestMethod()),
                        "OpenAPI accessToken must be acquired with POST");
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                TestSupport.require(body.contains(Json.field("client_id", "client"))
                                && body.contains(Json.field("client_secret", "secret")),
                        "token request must use server-side client credentials");
                TestSupport.json(exchange, 200, Json.object(
                        Json.rawField("data", Json.object(
                                Json.field("access_token", "dynamic-token"), Json.intField("expires_in", 7200000)
                        )), Json.boolField("status", true)
                ));
            });
            upstream.createContext("/v2/gai/assistants", exchange -> {
                assistantCalls.incrementAndGet();
                requireOpenApiPost(exchange.getRequestMethod(), exchange.getRequestHeaders().getFirst("accessToken"));
                TestSupport.json(exchange, 200, Json.object(
                        Json.field("errCode", "0"),
                        Json.rawField("data", Json.array(java.util.List.of(Json.object(
                                Json.field("id", "assistant-001"), Json.field("name", ASSISTANT_NAME)
                        ))))
                ));
            });
            upstream.createContext("/v2/gai/newsession", exchange -> {
                sessionCalls.incrementAndGet();
                requireOpenApiPost(exchange.getRequestMethod(), exchange.getRequestHeaders().getFirst("accessToken"));
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                TestSupport.require(body.contains(Json.field("assistantId", "assistant-001")),
                        "newsession must use the assistant discovered by name");
                TestSupport.require(body.contains("/api/campuspilot/agent/callback?token=callback-test"),
                        "newsession must provide the backend callback URL");
                TestSupport.json(exchange, 200, Json.object(
                        Json.field("errCode", "0"),
                        Json.rawField("data", Json.object(Json.field("sessionId", "session-001")))
                ));
            });
            upstream.createContext("/v2/gai/chat", exchange -> {
                chatCalls.incrementAndGet();
                requireOpenApiPost(exchange.getRequestMethod(), exchange.getRequestHeaders().getFirst("accessToken"));
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String traceId = com.campuspilot.kingdee.KingdeeClient.jsonString(body, "chatTraceId");
                TestSupport.require(body.contains(Json.field("sessionId", "session-001")),
                        "chat must reuse the created session");
                TestSupport.json(exchange, 200, Json.object(
                        Json.field("errCode", "0"),
                        Json.rawField("data", Json.object(Json.field("traceId", traceId)))
                ));
                Thread.startVirtualThread(() -> sendCallback(apiBase, traceId));
            });

            api.setExecutor(apiExecutor);
            api.createContext("/api/campuspilot", handler);
            api.start();

            HttpResponse<String> first = chat(apiBase, "请分析张明远的学业风险");
            TestSupport.require(first.statusCode() == 200, "Agent chat must return HTTP 200");
            TestSupport.require(first.body().contains("已根据画像生成学业建议"),
                    "Agent callback answer must be returned to the caller");
            TestSupport.require(first.body().contains(Json.boolField("demo", false)),
                    "successful Agent OpenAPI response must not be marked as demo");

            HttpResponse<String> second = chat(apiBase, "再给出课程补强建议");
            TestSupport.require(second.statusCode() == 200 && second.body().contains("已根据画像生成学业建议"),
                    "the cached Agent session must support another chat");
            HttpResponse<String> anotherUser = chat(apiBase, "查看我的学业建议", "Another-User");
            TestSupport.require(anotherUser.statusCode() == 200,
                    "another authenticated user must receive an independent Agent session");
            TestSupport.require(assistantCalls.get() == 1, "assistant list should be discovered once and cached");
            TestSupport.require(sessionCalls.get() == 2, "Agent sessions should be cached separately per user");
            TestSupport.require(chatCalls.get() == 3, "each question should call /v2/gai/chat");
            TestSupport.require(tokenCalls.get() == 1, "accessToken should be acquired once and cached");
        } finally {
            api.stop(0);
            upstream.stop(0);
            apiExecutor.shutdownNow();
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

    private static void sendCallback(String apiBase, String traceId) {
        String body = Json.object(
                Json.field("chatTraceId", traceId),
                Json.rawField("actionList", Json.array(java.util.List.of(
                        Json.object(Json.field("type", "chat"), Json.rawField("data", Json.object(
                                Json.field("message", "已根据画像生成学业建议"), Json.boolField("stream", true)
                        ))),
                        Json.object(Json.field("type", "waitingDone"), Json.rawField("data", Json.object(
                                Json.field("id", "session-001")
                        )))
                )))
        );
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase + "/api/campuspilot/agent/callback?token=callback-test"))
                    .header("Content-Type", "application/json")
                    .header("access_token", "callback-test")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            TestSupport.require(response.statusCode() == 200, "Agent callback endpoint must accept the configured token");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
