package com.campuspilot;

import com.campuspilot.config.AppConfig;
import com.campuspilot.http.ApiHandler;
import com.campuspilot.service.AgentClient;
import com.campuspilot.service.KingdeeDataClient;
import com.campuspilot.service.WorkflowClient;
import com.campuspilot.store.InMemoryCampusPilotStore;
import com.campuspilot.util.Json;
import com.campuspilot.util.RequestUtil.UserContext;
import com.sun.net.httpserver.HttpServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class BackendRequirementsTest {
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    static void run() throws Exception {
        allRequiredRoutesAndCors();
        unconfiguredWorkflowIsExplicit();
        failedKapiIsNotReportedConnected();
    }

    private static void allRequiredRoutesAndCors() throws Exception {
        HttpServer upstream = TestSupport.server();
        HttpServer api = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger workflowCalls = new AtomicInteger();
        try {
            upstream.createContext("/ierp/kapi/v2/code/code_campus_pilot", exchange ->
                    TestSupport.json(exchange, 200, rowsResponse()));
            upstream.createContext("/workflow", exchange -> {
                workflowCalls.incrementAndGet();
                TestSupport.require("Bearer workflow-secret".equals(exchange.getRequestHeaders().getFirst("Authorization")),
                        "workflow key must stay server-side and be forwarded as bearer token");
                TestSupport.json(exchange, 200, Json.object(Json.boolField("accepted", true)));
            });
            String upstreamBase = TestSupport.baseUrl(upstream);
            AppConfig config = TestSupport.config(upstreamBase, "static-token", false, "",
                    upstreamBase + "/workflow", "api-secret", "http://127.0.0.1:8881");
            InMemoryCampusPilotStore store = new InMemoryCampusPilotStore(config);
            KingdeeDataClient dataClient = new KingdeeDataClient(config, store);
            AgentClient agentClient = new AgentClient(config, store, dataClient);
            ApiHandler handler = new ApiHandler(config, store, agentClient, dataClient);
            api.createContext("/api/campuspilot", handler);
            api.createContext("/api/student", handler);
            api.start();
            String base = TestSupport.baseUrl(api);

            HttpResponse<String> integration = request("GET", base + "/api/campuspilot/integration-status", "", true);
            requireStatus(integration, 200);
            TestSupport.require(integration.body().contains("kingdee-api"),
                    "integration status must require successful KAPI probes");
            TestSupport.require(integration.body().contains("connected"),
                    "core objects should be marked connected after successful probes");

            requireStatus(request("GET", base + "/api/campuspilot/students/S001/trajectory", "", true), 200);
            requireStatus(request("GET", base + "/api/campuspilot/students/S001/profile-analysis", "", true), 200);
            requireStatus(request("GET", base + "/api/campuspilot/students/S001/opportunities", "", true), 200);
            requireStatus(request("GET", base + "/api/campuspilot/tasks?role=counselor", "", true), 200);
            requireStatus(request("POST", base + "/api/campuspilot/plans/generate", Json.object(Json.field("studentId", "S001")), true), 200);
            requireStatus(request("POST", base + "/api/campuspilot/risk/batch-scan", Json.object(Json.field("className", "AI2301")), true), 200);
            requireStatus(request("PATCH", base + "/api/campuspilot/tasks/T001", Json.object(Json.field("status", "done")), true), 200);
            TestSupport.require(workflowCalls.get() == 3, "three workflow operations should be proxied");

            HttpResponse<String> options = preflight(base + "/api/campuspilot/tasks/T001");
            requireStatus(options, 204);
            TestSupport.require("http://127.0.0.1:8881".equals(options.headers().firstValue("Access-Control-Allow-Origin").orElse("")),
                    "CORS origin must match the configured Kingdee origin");
            TestSupport.require(options.headers().firstValue("Access-Control-Allow-Headers").orElse("").contains("Authorization"),
                    "CORS must allow Authorization");
            TestSupport.require(options.headers().firstValue("Access-Control-Allow-Methods").orElse("").contains("PATCH"),
                    "CORS must allow PATCH");
            requireStatus(request("GET", base + "/api/campuspilot/tasks", "", false), 401);
        } finally {
            api.stop(0);
            upstream.stop(0);
        }
    }

    private static void unconfiguredWorkflowIsExplicit() {
        AppConfig config = TestSupport.config("http://127.0.0.1:1", "", false);
        InMemoryCampusPilotStore store = new InMemoryCampusPilotStore(config);
        WorkflowClient.WorkflowResponse response = new WorkflowClient(config, store)
                .execute("plans.generate", "{}", new UserContext("测试", "辅导员"));
        TestSupport.require(response.statusCode() == 503, "unconfigured workflow must return 503");
        TestSupport.require(response.json().contains("local-demo"), "workflow fallback must be explicitly marked demo");
    }

    private static void failedKapiIsNotReportedConnected() {
        AppConfig config = TestSupport.config("http://127.0.0.1:1", "invalid-token", false);
        InMemoryCampusPilotStore store = new InMemoryCampusPilotStore(config);
        KingdeeDataClient dataClient = new KingdeeDataClient(config, store);
        dataClient.studentsJson(new UserContext("Audit", "学院管理者"));
        TestSupport.require(!"kingdee-api".equals(dataClient.dataMode()),
                "a configured but unreachable KAPI must not be reported as connected");
    }

    private static HttpResponse<String> request(String method, String url, String body, boolean authorized) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Origin", "http://127.0.0.1:8881")
                .header("Content-Type", "application/json")
                .method(method, body.isBlank() ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (authorized) {
            builder.header("X-CampusPilot-User", "CampusPilot-Audit")
                    .header("X-CampusPilot-Role-Key", "manager")
                    .header("Authorization", "Bearer api-secret");
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> preflight(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Origin", "http://127.0.0.1:8881")
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", "Authorization, Content-Type, X-CampusPilot-User, X-CampusPilot-Role-Key")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void requireStatus(HttpResponse<String> response, int expected) {
        TestSupport.require(response.statusCode() == expected,
                "expected HTTP " + expected + " but got " + response.statusCode() + ": " + response.body());
    }

    private static String rowsResponse() {
        String row = Json.object(
                Json.field("id", "1"), Json.field("number", "S001"), Json.field("name", "张明远"),
                Json.field("code_studentnumber", "S001"), Json.field("code_studentno", "S001"),
                Json.field("code_studentname", "张明远"), Json.field("code_student_name", "张明远"),
                Json.field("code_gpa", "2.31"), Json.field("code_failedcount", "2"),
                Json.field("code_attendancerate", "71"), Json.field("code_assignmentrate", "68"),
                Json.field("code_score", "55"), Json.field("code_risk_level", "高风险"),
                Json.field("code_risk_score", "86"), Json.field("code_status", "待确认"),
                Json.field("code_receiverrole", "辅导员"), Json.field("code_content", "处理学业预警"),
                Json.field("code_notinumber", "N001"), Json.field("code_studentopportunity", "算法竞赛")
        );
        return Json.object(
                Json.rawField("data", Json.object(Json.rawField("rows", Json.array(List.of(row))))),
                Json.boolField("status", true), Json.field("errorCode", ""), Json.field("message", "")
        );
    }
}
