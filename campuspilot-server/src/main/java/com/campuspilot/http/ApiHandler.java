package com.campuspilot.http;

import com.campuspilot.config.AppConfig;
import com.campuspilot.kingdee.KingdeeClient.KingdeeException;
import com.campuspilot.service.AgentClient;
import com.campuspilot.service.CourseAbilityService;
import com.campuspilot.service.GrowthPlanService;
import com.campuspilot.service.KingdeeDataClient;
import com.campuspilot.service.LearningService;
import com.campuspilot.service.NotificationService;
import com.campuspilot.service.OpportunityService;
import com.campuspilot.service.ProfileAnalysisService;
import com.campuspilot.service.RiskService;
import com.campuspilot.service.StudyCheckinService;
import com.campuspilot.service.StudentProfileService;
import com.campuspilot.service.StudentTrajectoryService;
import com.campuspilot.service.TaskService;
import com.campuspilot.service.WorkflowClient;
import com.campuspilot.service.WorkflowClient.WorkflowResponse;
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
    private static final Pattern STUDENT_EXTENSION = Pattern.compile("^/api/campuspilot/students/([^/]+)/(trajectory|profile-analysis|opportunities)$");
    private static final Pattern TASK_ACTION = Pattern.compile("^/api/campuspilot/tasks/([^/]+)$");
    private static final Pattern AGENT_RESULT = Pattern.compile("^/api/campuspilot/agent/results/([^/]+)$");

    private final AppConfig config;
    private final InMemoryCampusPilotStore store;
    private final AgentClient agentClient;
    private final KingdeeDataClient kingdeeDataClient;
    private final StudentProfileService studentProfileService;
    private final GrowthPlanService growthPlanService;
    private final LearningService learningService;
    private final RiskService riskService;
    private final OpportunityService opportunityService;
    private final NotificationService notificationService;
    private final StudyCheckinService studyCheckinService;
    private final CourseAbilityService courseAbilityService;
    private final StudentTrajectoryService trajectoryService;
    private final ProfileAnalysisService profileAnalysisService;
    private final TaskService taskService;
    private final WorkflowClient workflowClient;

    public ApiHandler(AppConfig config, InMemoryCampusPilotStore store, AgentClient agentClient, KingdeeDataClient kingdeeDataClient) {
        this.config = config;
        this.store = store;
        this.agentClient = agentClient;
        this.kingdeeDataClient = kingdeeDataClient;
        this.trajectoryService = new StudentTrajectoryService(kingdeeDataClient.kingdeeClient());
        this.studentProfileService = new StudentProfileService(kingdeeDataClient.kingdeeClient());
        this.growthPlanService = new GrowthPlanService(kingdeeDataClient.kingdeeClient());
        this.learningService = new LearningService(kingdeeDataClient.kingdeeClient(), trajectoryService);
        this.riskService = new RiskService(kingdeeDataClient.kingdeeClient());
        this.opportunityService = new OpportunityService(kingdeeDataClient.kingdeeClient());
        this.notificationService = new NotificationService(kingdeeDataClient.kingdeeClient());
        this.studyCheckinService = new StudyCheckinService(kingdeeDataClient.kingdeeClient());
        this.courseAbilityService = new CourseAbilityService(kingdeeDataClient.kingdeeClient());
        this.profileAnalysisService = new ProfileAnalysisService(kingdeeDataClient.kingdeeClient());
        this.taskService = new TaskService(kingdeeDataClient.kingdeeClient());
        this.workflowClient = new WorkflowClient(config, store);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod().toUpperCase();
            String body = RequestUtil.readBody(exchange);
            UserContext user = RequestUtil.user(exchange, body);

            String origin = exchange.getRequestHeaders().getFirst("Origin");
            if (!config.originAllowed(origin)) {
                sendJson(exchange, 403, Json.object(Json.boolField("ok", false), Json.field("message", "origin not allowed")));
                return;
            }

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
            if ("PATCH".equals(method)) {
                handlePatch(exchange, path, body, user);
                return;
            }
            sendJson(exchange, 405, Json.object(Json.boolField("ok", false), Json.field("message", "method not allowed")));
        } catch (KingdeeException ex) {
            sendJson(exchange, 502, Json.object(
                    Json.boolField("ok", false), Json.boolField("demo", false),
                    Json.field("dataSource", "kingdee-api"), Json.field("message", ex.getMessage())
            ));
        } catch (Exception ex) {
            sendJson(exchange, 500, Json.object(Json.boolField("ok", false), Json.field("message", ex.getMessage())));
        }
    }

    private void handleGet(HttpExchange exchange, String path, UserContext user) throws IOException {
        if ("/api/campuspilot/health".equals(path)) {
            sendJson(exchange, 200, healthJson());
            return;
        }
        if (!authorize(exchange, user)) return;
        Matcher agentResult = AGENT_RESULT.matcher(path);
        if (agentResult.matches()) {
            sendJson(exchange, 200, agentClient.resultJson(java.net.URLDecoder.decode(agentResult.group(1), StandardCharsets.UTF_8)));
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
        if ((studentId = studentId(path, "/api/student/notifications/")) != null) {
            sendJson(exchange, 200, notificationService.notificationsJson(studentId));
            return;
        }
        if ((studentId = studentId(path, "/api/student/study-checkins/")) != null) {
            sendJson(exchange, 200, studyCheckinService.checkinsJson(studentId));
            return;
        }
        if ("/api/student/course-ability-mappings".equals(path)) {
            sendJson(exchange, 200, courseAbilityService.mappingsJson());
            return;
        }
        Matcher extensionMatcher = STUDENT_EXTENSION.matcher(path);
        if (extensionMatcher.matches()) {
            String id = java.net.URLDecoder.decode(extensionMatcher.group(1), StandardCharsets.UTF_8);
            String json = switch (extensionMatcher.group(2)) {
                case "trajectory" -> extensionEnvelope(id, "trajectory", Json.object(
                        Json.rawField("records", trajectoryService.trajectoryJson(id)),
                        Json.rawField("studyCheckins", studyCheckinService.checkinsJson(id))
                ));
                case "profile-analysis" -> profileAnalysisService.analysisJson(id);
                case "opportunities" -> extensionEnvelope(id, "opportunities", opportunityService.opportunitiesJson(id));
                default -> throw new IllegalStateException("unsupported student extension");
            };
            sendJson(exchange, 200, json);
            return;
        }
        if ("/api/campuspilot/tasks".equals(path)) {
            String role = RequestUtil.queryValue(exchange, "role", user.role());
            sendJson(exchange, 200, taskService.tasksJson(role));
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
            case "/api/campuspilot/integration-status" -> sendJson(exchange, 200,
                    kingdeeDataClient.integrationJson(agentClient.statusJson(), workflowClient.statusJson()));
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
        if ("/api/campuspilot/agent/callback".equals(path)) {
            String token = exchange.getRequestHeaders().getFirst("access_token");
            if (token == null || token.isBlank()) token = RequestUtil.queryValue(exchange, "token", "");
            String response = agentClient.callbackJson(body, token);
            sendJson(exchange, response.contains("invalid callback token") ? 401 : 200, response);
            return;
        }
        if ("/api/campuspilot/auth/login".equals(path)) {
            sendJson(exchange, 200, store.loginJson(body));
            return;
        }
        if ("/api/campuspilot/auth/register".equals(path)) {
            sendJson(exchange, 200, store.registerJson(body));
            return;
        }
        if (!authorize(exchange, user)) return;
        if ("/api/campuspilot/agent/chat".equals(path)) {
            String question = RequestUtil.value(body, "question", "");
            sendJson(exchange, 200, agentClient.chat(question, user.name(), user.role()));
            return;
        }
        if ("/api/campuspilot/plans/generate".equals(path)) {
            sendWorkflow(exchange, workflowClient.execute("plans.generate", body, user));
            return;
        }
        if ("/api/campuspilot/risk/batch-scan".equals(path)) {
            if (!java.util.List.of("辅导员", "学院管理者").contains(user.role())) {
                sendJson(exchange, 403, forbidden());
                return;
            }
            sendWorkflow(exchange, workflowClient.execute("risk.batch-scan", body, user));
            return;
        }
        if ("/api/campuspilot/warnings/suggest".equals(path)) {
            if (!store.canPerform(user.role(), "createWarningSuggestion")) {
                sendJson(exchange, 403, forbidden());
                return;
            }
            sendWorkflow(exchange, workflowClient.execute("warning.create-draft", body, user));
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
            String input = Json.object(
                    Json.field("warningId", actionMatcher.group(1)),
                    Json.field("action", action),
                    Json.rawField("payload", body == null || body.isBlank() ? "{}" : body)
            );
            sendWorkflow(exchange, workflowClient.execute("warning." + action, input, user));
            return;
        }
        sendJson(exchange, 404, Json.object(Json.boolField("ok", false), Json.field("message", "not found")));
    }

    private void handlePatch(HttpExchange exchange, String path, String body, UserContext user) throws IOException {
        if (!authorize(exchange, user)) return;
        Matcher taskMatcher = TASK_ACTION.matcher(path);
        if (!taskMatcher.matches()) {
            sendJson(exchange, 404, Json.object(Json.boolField("ok", false), Json.field("message", "not found")));
            return;
        }
        String input = Json.object(
                Json.field("taskId", java.net.URLDecoder.decode(taskMatcher.group(1), StandardCharsets.UTF_8)),
                Json.rawField("payload", body == null || body.isBlank() ? "{}" : body)
        );
        sendWorkflow(exchange, workflowClient.execute("tasks.update", input, user));
    }

    private boolean authorize(HttpExchange exchange, UserContext user) throws IOException {
        if (!user.authenticated()) {
            sendJson(exchange, 401, unauthorized());
            return false;
        }
        if (config.requiresBearerToken()) {
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            if (!("Bearer " + config.apiBearerToken()).equals(authorization)) {
                sendJson(exchange, 401, Json.object(Json.boolField("ok", false), Json.field("message", "invalid bearer token")));
                return false;
            }
        }
        return true;
    }

    private String healthJson() {
        return Json.object(
                Json.boolField("ok", true),
                Json.field("service", "CampusPilot Java API"),
                Json.field("time", RequestUtil.now()),
                Json.rawField("configuration", Json.object(
                        Json.boolField("kingdeeConfigured", kingdeeDataClient.configured()),
                        Json.boolField("agentConfigured", config.agentOpenApiConfigured()),
                        Json.boolField("workflowConfigured", config.workflowConfigured()),
                        Json.boolField("bearerProtectionEnabled", config.requiresBearerToken()),
                        Json.field("corsAllowedOrigins", config.corsAllowedOrigins())
                ))
        );
    }

    private String extensionEnvelope(String studentId, String key, String dataJson) {
        boolean configured = kingdeeDataClient.configured();
        return Json.object(
                Json.boolField("ok", configured),
                Json.boolField("demo", !configured),
                Json.field("dataSource", configured ? "kingdee-api" : "unavailable"),
                Json.field("studentId", studentId),
                Json.rawField(key, dataJson),
                Json.field("message", configured ? "" : "未配置金蝶 KAPI，未伪造扩展接口结果。")
        );
    }

    private void sendWorkflow(HttpExchange exchange, WorkflowResponse response) throws IOException {
        sendJson(exchange, response.statusCode(), response.json());
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
        String requestOrigin = exchange.getRequestHeaders().getFirst("Origin");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", config.responseOrigin(requestOrigin));
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-CampusPilot-User, X-CampusPilot-Role-Key");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PATCH,OPTIONS");
        exchange.getResponseHeaders().set("Vary", "Origin");
        exchange.getResponseHeaders().set("X-CampusPilot-Agent-Mode", config.agentOpenApiConfigured() ? "openapi-auto" : "local-fallback");
        exchange.getResponseHeaders().set("X-CampusPilot-Data-Mode", kingdeeDataClient.dataMode());
        if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }
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
