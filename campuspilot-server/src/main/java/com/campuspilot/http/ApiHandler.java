package com.campuspilot.http;

import com.campuspilot.config.AppConfig;
import com.campuspilot.service.AgentClient;
import com.campuspilot.service.GrowthPlanService;
import com.campuspilot.service.KingdeeDataClient;
import com.campuspilot.service.LearningService;
import com.campuspilot.service.OpportunityService;
import com.campuspilot.service.RiskService;
import com.campuspilot.service.StudentProfileService;
import com.campuspilot.service.StudentTrajectoryService;
import com.campuspilot.store.InMemoryCampusPilotStore;
import com.campuspilot.util.Json;
import com.campuspilot.util.RequestUtil;
import com.campuspilot.util.RequestUtil.UserContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ApiHandler implements HttpHandler {
    private static final Pattern WARNING_ACTION = Pattern.compile("^/api/campuspilot/warnings/([^/]+)/(confirm|mentor-plan|feedback|close)$");

    private final AppConfig config;
    private final InMemoryCampusPilotStore store;
    private final AgentClient agentClient;
    private final KingdeeDataClient kingdeeDataClient;
    private final StudentProfileService studentProfileService;
    private final GrowthPlanService growthPlanService;
    private final LearningService learningService;
    private final RiskService riskService;
    private final OpportunityService opportunityService;

    public ApiHandler(AppConfig config, InMemoryCampusPilotStore store, AgentClient agentClient, KingdeeDataClient kingdeeDataClient) {
        this.config = config;
        this.store = store;
        this.agentClient = agentClient;
        this.kingdeeDataClient = kingdeeDataClient;
        StudentTrajectoryService trajectoryService = new StudentTrajectoryService(kingdeeDataClient.kingdeeClient());
        this.studentProfileService = new StudentProfileService(kingdeeDataClient.kingdeeClient());
        this.growthPlanService = new GrowthPlanService(kingdeeDataClient.kingdeeClient());
        this.learningService = new LearningService(kingdeeDataClient.kingdeeClient(), trajectoryService);
        this.riskService = new RiskService(kingdeeDataClient.kingdeeClient());
        this.opportunityService = new OpportunityService(kingdeeDataClient.kingdeeClient());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod().toUpperCase();
            String body = RequestUtil.readBody(exchange);
            UserContext user = RequestUtil.user(exchange, body);

            if ("OPTIONS".equals(method)) {
                sendJson(exchange, 204, "");
                return;
            }
            if ("GET".equals(method)) {
                handleGet(exchange, path, user);
                return;
            }
            if ("POST".equals(method)) {
                handlePost(exchange, path, body, user);
                return;
            }
            sendJson(exchange, 405, Json.object(Json.boolField("ok", false), Json.field("message", "method not allowed")));
        } catch (Exception ex) {
            sendJson(exchange, 500, Json.object(Json.boolField("ok", false), Json.field("message", ex.getMessage())));
        }
    }

    private void handleGet(HttpExchange exchange, String path, UserContext user) throws IOException {
        if ("/api/campuspilot/health".equals(path)) {
            sendJson(exchange, 200, store.healthJson());
            return;
        }
        if (!user.authenticated()) {
            sendJson(exchange, 401, unauthorized());
            return;
        }
        String studentId;
        if ((studentId = studentId(path, "/api/student/profile/")) != null) {
            sendJson(exchange, 200, studentProfileService.profileJson(studentId));
            return;
        }
        if ((studentId = studentId(path, "/api/student/growth-plan/")) != null) {
            sendJson(exchange, 200, growthPlanService.growthPlanJson(studentId));
            return;
        }
        if ((studentId = studentId(path, "/api/student/learning/")) != null) {
            sendJson(exchange, 200, learningService.learningJson(studentId));
            return;
        }
        if ((studentId = studentId(path, "/api/student/risk-warning/")) != null) {
            sendJson(exchange, 200, riskService.riskJson(studentId));
            return;
        }
        if ((studentId = studentId(path, "/api/student/opportunities/")) != null) {
            sendJson(exchange, 200, opportunityService.opportunitiesJson(studentId));
            return;
        }
        switch (path) {
            case "/api/campuspilot/overview" -> sendJson(exchange, 200, store.overviewJson());
            case "/api/campuspilot/risk-distribution" -> sendJson(exchange, 200, store.riskDistributionJson());
            case "/api/campuspilot/students" -> sendJson(exchange, 200, kingdeeDataClient.studentsJson(user));
            case "/api/campuspilot/courses" -> sendJson(exchange, 200, kingdeeDataClient.coursesJson(user));
            case "/api/campuspilot/behaviors" -> sendJson(exchange, 200, kingdeeDataClient.behaviorsJson(user));
            case "/api/campuspilot/warnings" -> sendJson(exchange, 200, kingdeeDataClient.warningsJson(user));
            case "/api/campuspilot/workflow" -> sendJson(exchange, 200, store.workflowJson());
            case "/api/campuspilot/workflow-logs" -> sendJson(exchange, 200, store.workflowLogsJson());
            case "/api/campuspilot/risk-trend" -> sendJson(exchange, 200, store.riskTrendJson());
            case "/api/campuspilot/effectiveness" -> sendJson(exchange, 200, store.effectivenessJson());
            case "/api/campuspilot/integration-status" -> sendJson(exchange, 200, kingdeeDataClient.integrationJson());
            case "/api/campuspilot/lowcode-blueprint" -> sendJson(exchange, 200, store.lowcodeBlueprintJson());
            case "/api/campuspilot/agent-workflow" -> sendJson(exchange, 200, store.agentWorkflowJson());
            case "/api/campuspilot/report-center" -> sendJson(exchange, 200, store.reportCenterJson());
            case "/api/campuspilot/cloud-native" -> sendJson(exchange, 200, store.cloudNativeJson());
            case "/api/campuspilot/multimodal" -> sendJson(exchange, 200, store.multimodalJson());
            case "/api/campuspilot/agent-insight" -> sendJson(exchange, 200, store.agentInsightJson());
            case "/api/campuspilot/roles" -> sendJson(exchange, 200, store.rolesJson());
            case "/api/campuspilot/audit-logs" -> sendJson(exchange, 200, store.auditLogsJson(user));
            default -> sendJson(exchange, 404, Json.object(Json.boolField("ok", false), Json.field("message", "not found")));
        }
    }

    private void handlePost(HttpExchange exchange, String path, String body, UserContext user) throws IOException {
        if ("/api/campuspilot/auth/login".equals(path)) {
            sendJson(exchange, 200, store.loginJson(body));
            return;
        }
        if ("/api/campuspilot/auth/register".equals(path)) {
            sendJson(exchange, 200, store.registerJson(body));
            return;
        }
        if (!user.authenticated()) {
            sendJson(exchange, 401, unauthorized());
            return;
        }
        if ("/api/campuspilot/agent/chat".equals(path)) {
            String question = RequestUtil.value(body, "question", "");
            sendJson(exchange, 200, agentClient.chat(question, user.role()));
            return;
        }
        if ("/api/campuspilot/warnings/suggest".equals(path)) {
            if (!store.canPerform(user.role(), "createWarningSuggestion")) {
                sendJson(exchange, 403, forbidden());
                return;
            }
            sendJson(exchange, 200, store.createWarningSuggestionJson());
            return;
        }
        Matcher actionMatcher = WARNING_ACTION.matcher(path);
        if (actionMatcher.matches()) {
            String action = actionMatcher.group(2);
            String permission = switch (action) {
                case "confirm" -> "confirmWarning";
                case "mentor-plan" -> "saveMentorPlan";
                case "feedback" -> "submitFeedback";
                case "close" -> "closeWarning";
                default -> "";
            };
            if (!store.canPerform(user.role(), permission)) {
                sendJson(exchange, 403, forbidden());
                return;
            }
            String response = store.updateWarningJson(actionMatcher.group(1), action, body, user);
            if (response.isBlank()) {
                sendJson(exchange, 404, Json.object(Json.boolField("ok", false), Json.field("message", "warning not found")));
            } else {
                sendJson(exchange, 200, response);
            }
            return;
        }
        sendJson(exchange, 404, Json.object(Json.boolField("ok", false), Json.field("message", "not found")));
    }

    private static String studentId(String path, String prefix) {
        if (!path.startsWith(prefix)) return null;
        String value = path.substring(prefix.length());
        if (value.isBlank() || value.contains("/")) return null;
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json == null ? new byte[0] : json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-CampusPilot-User, X-CampusPilot-Role-Key");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        exchange.getResponseHeaders().set("X-CampusPilot-Agent-Mode", config.agentApiUrl().isBlank() ? "local-fallback" : "remote-proxy");
        exchange.getResponseHeaders().set("X-CampusPilot-Data-Mode", kingdeeDataClient.dataMode());
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static String unauthorized() {
        return Json.object(Json.boolField("ok", false), Json.field("message", "请先登录后再访问业务接口。"));
    }

    private static String forbidden() {
        return Json.object(Json.boolField("ok", false), Json.field("message", "当前角色没有执行该操作的权限。"));
    }
}
