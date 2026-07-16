package com.campuspilot.store;

import com.campuspilot.config.AppConfig;
import com.campuspilot.util.Json;
import com.campuspilot.util.RequestUtil;
import com.campuspilot.util.RequestUtil.UserContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class InMemoryCampusPilotStore {
    private final AppConfig config;
    private final List<Student> students = new ArrayList<>();
    private final List<Course> courses = new ArrayList<>();
    private final List<Behavior> behaviors = new ArrayList<>();
    private final List<WarningOrder> warnings = new ArrayList<>();
    private final List<WorkflowLog> workflowLogs = new ArrayList<>();
    private final List<AuditLog> auditLogs = new ArrayList<>();

    public InMemoryCampusPilotStore(AppConfig config) {
        this.config = config;
        seed();
    }

    public synchronized String healthJson() {
        return Json.object(
                Json.boolField("ok", true),
                Json.field("service", "CampusPilot Java API"),
                Json.field("profile", "enterprise-ready-no-dependency"),
                Json.field("time", RequestUtil.now())
        );
    }

    public synchronized String overviewJson() {
        long high = students.stream().filter(s -> "high".equals(s.riskKey)).count();
        long watch = students.stream().filter(s -> "watch".equals(s.riskKey)).count();
        long normal = students.stream().filter(s -> "normal".equals(s.riskKey)).count();
        long improved = students.stream().filter(s -> "improved".equals(s.riskKey)).count();
        long pending = warnings.stream().filter(w -> "todo".equals(w.statusKey)).count();
        long active = warnings.stream().filter(w -> "active".equals(w.statusKey)).count();
        long done = warnings.stream().filter(w -> "done".equals(w.statusKey)).count();
        double gpa = students.stream().mapToDouble(s -> s.gpa).average().orElse(0);
        double attendance = students.stream().mapToInt(s -> s.attendance).average().orElse(0);
        return Json.object(
                Json.intField("totalStudents", students.size()),
                Json.intField("highRisk", high),
                Json.intField("watchRisk", watch),
                Json.intField("normal", normal),
                Json.intField("improved", improved),
                Json.intField("pendingWarnings", pending),
                Json.intField("activeWarnings", active),
                Json.intField("closedWarnings", done),
                Json.decimalField("averageGpa", gpa),
                Json.decimalField("averageAttendance", attendance)
        );
    }

    public synchronized String riskDistributionJson() {
        return Json.array(List.of(
                Json.object(Json.field("label", "高风险"), Json.field("key", "high"), Json.intField("value", countRisk("high")), Json.field("color", "#e5484d")),
                Json.object(Json.field("label", "需要关注"), Json.field("key", "watch"), Json.intField("value", countRisk("watch")), Json.field("color", "#f5a524")),
                Json.object(Json.field("label", "正常"), Json.field("key", "normal"), Json.intField("value", countRisk("normal")), Json.field("color", "#3b82f6")),
                Json.object(Json.field("label", "改善中"), Json.field("key", "improved"), Json.intField("value", countRisk("improved")), Json.field("color", "#22c55e"))
        ));
    }

    public synchronized String studentsJson(UserContext user) {
        return Json.array(filterStudents(user).stream().map(this::studentJson).toList());
    }

    public synchronized String coursesJson(UserContext user) {
        return Json.array(filterCourses(user).stream().map(this::courseJson).toList());
    }

    public synchronized String behaviorsJson(UserContext user) {
        return Json.array(filterBehaviors(user).stream().map(this::behaviorJson).toList());
    }

    public synchronized String warningsJson(UserContext user) {
        return Json.array(filterWarnings(user).stream()
                .sorted(Comparator.comparing((WarningOrder w) -> w.code).reversed())
                .map(this::warningJson)
                .toList());
    }

    public synchronized String workflowJson() {
        return Json.array(List.of(
                Json.object(Json.field("step", "风险识别"), Json.field("owner", "系统 / Agent"), Json.field("time", "2026-06-04"), Json.field("detail", "根据画像、成绩和行为识别张明远为高风险。")),
                Json.object(Json.field("step", "生成预警单"), Json.field("owner", "风险预警对象"), Json.field("time", "2026-06-04"), Json.field("detail", "形成 RW2026001，预警来源为 Agent 风险分析。")),
                Json.object(Json.field("step", "辅导员确认"), Json.field("owner", "辅导员"), Json.field("time", "2026-06-05"), Json.field("detail", "确认风险，填写辅导员意见并推进导师帮扶。")),
                Json.object(Json.field("step", "导师帮扶"), Json.field("owner", "专业导师"), Json.field("time", "2026-06-05"), Json.field("detail", "制定高等数学与数据结构 4 周补强计划。")),
                Json.object(Json.field("step", "学生反馈"), Json.field("owner", "学生"), Json.field("time", "2026-06-06"), Json.field("detail", "学生提交学习承诺和反馈。")),
                Json.object(Json.field("step", "复评结案"), Json.field("owner", "辅导员 / 学院管理者"), Json.field("time", "2026-06-15"), Json.field("detail", "根据改善情况复评，形成结案记录或继续跟踪。"))
        ));
    }

    public synchronized String workflowLogsJson() {
        return Json.array(workflowLogs.stream().map(this::workflowLogJson).toList());
    }

    public synchronized String riskTrendJson() {
        return Json.array(List.of(
                Json.object(Json.field("week", "第1周"), Json.intField("high", 2), Json.intField("watch", 2), Json.intField("done", 0)),
                Json.object(Json.field("week", "第2周"), Json.intField("high", 2), Json.intField("watch", 1), Json.intField("done", 1)),
                Json.object(Json.field("week", "第3周"), Json.intField("high", 1), Json.intField("watch", 1), Json.intField("done", 1)),
                Json.object(Json.field("week", "当前"), Json.intField("high", countRisk("high")), Json.intField("watch", countRisk("watch")), Json.intField("done", countStatus("done")))
        ));
    }

    public synchronized String effectivenessJson() {
        long total = warnings.size();
        long done = countStatus("done");
        long active = countStatus("active");
        long closedRate = total == 0 ? 0 : Math.round(done * 100.0 / total);
        String highPriority = Json.array(warnings.stream()
                .filter(w -> "high".equals(w.riskKey) && !"done".equals(w.statusKey))
                .map(this::warningJson)
                .toList());
        return Json.object(
                Json.intField("closedRate", closedRate),
                Json.intField("activeCount", active),
                Json.intField("closedCount", done),
                Json.intField("averageCycleDays", 7),
                Json.rawField("highPriority", highPriority)
        );
    }

    public synchronized String integrationStatusJson() {
        return Json.object(
                Json.field("kingdeeBaseUrl", config.kingdeeBaseUrl()),
                Json.field("agentMode", config.agentOpenApiConfigured() ? "金蝶 Agent OpenAPI 自动发现" : "本地 Agent 兜底"),
                Json.field("agentName", config.agentName()),
                Json.field("agentApi", config.kingdeeBaseUrl() + "/v2/gai/*"),
                Json.rawField("objects", Json.array(List.of(
                        Json.object(Json.field("name", "学生画像"), Json.field("code", "cp_student_profile"), Json.field("status", "已建模"), Json.intField("fields", 12)),
                        Json.object(Json.field("name", "课程成绩"), Json.field("code", "cp_course_score"), Json.field("status", "已建模"), Json.intField("fields", 8)),
                        Json.object(Json.field("name", "学习行为"), Json.field("code", "cp_learning_behavior"), Json.field("status", "已建模"), Json.intField("fields", 9)),
                        Json.object(Json.field("name", "风险预警单"), Json.field("code", "cp_risk_warning"), Json.field("status", "已闭环"), Json.intField("fields", 14)),
                        Json.object(Json.field("name", "帮扶反馈"), Json.field("code", "cp_support_feedback"), Json.field("status", "已闭环"), Json.intField("fields", 7))
                ))),
                Json.rawField("apis", Json.stringArray(List.of("POST /api/campuspilot/agent/chat", "POST /api/campuspilot/warnings/suggest", "POST /api/campuspilot/warnings/{code}/confirm", "POST /api/campuspilot/warnings/{code}/mentor-plan", "POST /api/campuspilot/warnings/{code}/feedback", "POST /api/campuspilot/warnings/{code}/close"))),
                Json.rawField("thirdPartyApp", Json.object(Json.field("appId", "campuspilot_isv"), Json.field("auth", "AccessToken / API 授权 / IP 白名单"), Json.field("status", "待配置真实密钥")))
        );
    }

    public synchronized String lowcodeBlueprintJson() {
        return Json.object(
                Json.rawField("objects", Json.array(List.of(
                        Json.object(Json.field("name", "学生画像"), Json.field("code", "cp_student_profile"), Json.field("type", "领域模型 + 表单"), Json.field("purpose", "承载学生基础信息、目标、学业表现、行为指标和风险标签。")),
                        Json.object(Json.field("name", "课程成绩"), Json.field("code", "cp_course_score"), Json.field("type", "业务对象 + 列表报表"), Json.field("purpose", "为风险解释和导师帮扶提供课程证据。")),
                        Json.object(Json.field("name", "学习行为"), Json.field("code", "cp_learning_behavior"), Json.field("type", "采集表单 + 异常规则"), Json.field("purpose", "记录出勤、作业、平台活跃和课堂互动。")),
                        Json.object(Json.field("name", "风险预警单"), Json.field("code", "cp_risk_warning"), Json.field("type", "流程表单"), Json.field("purpose", "承载风险识别、确认、帮扶、反馈和结案。")),
                        Json.object(Json.field("name", "风险处理日志"), Json.field("code", "cp_warning_log"), Json.field("type", "流程日志对象"), Json.field("purpose", "保留每次状态变更和处理说明，解决单状态字段无法追溯的问题。"))
                ))),
                Json.rawField("platformCapabilities", Json.array(List.of(
                        Json.object(Json.field("name", "表单及插件开发"), Json.field("status", "已设计"), Json.field("detail", "预警单确认、导师计划、学生反馈和结案动作对应表单插件事件。")),
                        Json.object(Json.field("name", "流程服务"), Json.field("status", "已设计"), Json.field("detail", "风险识别 -> 预警生成 -> 确认 -> 帮扶 -> 反馈 -> 复评结案。")),
                        Json.object(Json.field("name", "报表"), Json.field("status", "已实现前端看板"), Json.field("detail", "风险分布、状态分布、趋势、专业对比和帮扶成效。")),
                        Json.object(Json.field("name", "集成开发"), Json.field("status", "已实现"), Json.field("detail", "后端通过 OpenAPI 自动鉴权、发现助手、创建会话并接收回调。"))
                )))
        );
    }

    public synchronized String agentWorkflowJson() {
        return Json.object(
                Json.field("agentName", "CampusPilot 启航智伴学业成长助手"),
                Json.field("modelRoute", "通过苍穹平台统一接入 DeepSeek / 豆包 / 通义千问等模型"),
                Json.rawField("rag", Json.array(List.of(
                        Json.object(Json.field("source", "学生画像知识库"), Json.field("content", "学生基础信息、成长目标、课程短板和风险规则。")),
                        Json.object(Json.field("source", "帮扶政策知识库"), Json.field("content", "教育强国、拔尖创新人才培养、心理健康和学业帮扶政策摘要。")),
                        Json.object(Json.field("source", "历史预警案例库"), Json.field("content", "预警单、处理轨迹、导师措施、学生反馈和结案结果。"))
                ))),
                Json.rawField("tools", Json.array(List.of(
                        Json.object(Json.field("name", "query_student_profile"), Json.field("method", "GET /api/campuspilot/students"), Json.field("purpose", "查询学生画像并作为回答主证据。")),
                        Json.object(Json.field("name", "query_warning_order"), Json.field("method", "GET /api/campuspilot/warnings"), Json.field("purpose", "查询已有预警单，避免重复建单。")),
                        Json.object(Json.field("name", "create_warning_order"), Json.field("method", "POST /api/campuspilot/warnings/suggest"), Json.field("purpose", "生成结构化预警建议并进入待确认。")),
                        Json.object(Json.field("name", "update_warning_status"), Json.field("method", "POST /api/campuspilot/warnings/{code}/{action}"), Json.field("purpose", "推进确认、导师计划、学生反馈和结案。"))
                ))),
                Json.rawField("workflow", Json.array(List.of(
                        Json.object(Json.field("step", "理解问题"), Json.field("detail", "识别学生、角色、风险类型和期望输出。")),
                        Json.object(Json.field("step", "检索知识库"), Json.field("detail", "RAG 拉取规则、政策和历史案例。")),
                        Json.object(Json.field("step", "调用工具"), Json.field("detail", "查询画像、课程、行为、预警单等实时数据。")),
                        Json.object(Json.field("step", "多步推理"), Json.field("detail", "综合 GPA、挂科、出勤、作业、目标和历史处理判断风险。")),
                        Json.object(Json.field("step", "结构化输出"), Json.field("detail", "输出风险等级、依据、建议处理人、复评时间和预警单字段。"))
                )))
        );
    }

    public synchronized String reportCenterJson() {
        return Json.array(List.of(
                Json.object(Json.field("name", "学业风险驾驶舱"), Json.field("metric", "风险等级分布、预警状态、结案率"), Json.field("decision", "帮助学院管理者判断当前风险治理压力。")),
                Json.object(Json.field("name", "学生画像报告"), Json.field("metric", "GPA、挂科、出勤、作业、科创参与度"), Json.field("decision", "帮助辅导员和导师定位学生主要短板。")),
                Json.object(Json.field("name", "帮扶成效报表"), Json.field("metric", "处理周期、结案数、改善案例"), Json.field("decision", "验证帮扶措施是否真正产生改善。")),
                Json.object(Json.field("name", "Agent 建议审计报表"), Json.field("metric", "建议来源、工具调用、人工确认结果"), Json.field("decision", "避免 AI 黑箱，便于答辩说明可控性。"))
        ));
    }

    public synchronized String cloudNativeJson() {
        return Json.array(List.of(
                Json.object(Json.field("layer", "前端"), Json.field("unit", "campuspilot-kwc build 静态资源"), Json.field("deploy", "可由 Java 静态服务、Nginx 或对象存储托管。")),
                Json.object(Json.field("layer", "业务服务"), Json.field("unit", "campuspilot-server Java 21 HTTP 服务"), Json.field("deploy", "无框架依赖，适合容器化和云主机部署。")),
                Json.object(Json.field("layer", "数据层"), Json.field("unit", "PostgreSQL schema.sql / seed.sql"), Json.field("deploy", "当前内存数据可平滑迁移到 PostgreSQL。")),
                Json.object(Json.field("layer", "Agent 集成"), Json.field("unit", "KINGDEE_BASE_URL + OpenAPI 凭据"), Json.field("deploy", "后端自动获取 accessToken 并调用 /v2/gai 接口。")),
                Json.object(Json.field("layer", "可观测"), Json.field("unit", "health / audit-logs"), Json.field("deploy", "提供健康检查、操作审计和 Agent 模式响应头。"))
        ));
    }

    public synchronized String multimodalJson() {
        return Json.array(List.of(
                Json.object(Json.field("input", "学生上传学习计划 PDF / 文档"), Json.field("model", "多模态/文档理解模型"), Json.field("output", "提取目标、困难和阶段计划，写入成长画像。"), Json.field("status", "接口预留")),
                Json.object(Json.field("input", "课堂签到或活动图片"), Json.field("model", "视觉理解模型"), Json.field("output", "辅助识别出勤异常和活动参与证据。"), Json.field("status", "演示设计")),
                Json.object(Json.field("input", "学生语音反馈"), Json.field("model", "语音识别 + 情感分析"), Json.field("output", "生成帮扶反馈摘要并提示辅导员复核。"), Json.field("status", "演示设计")),
                Json.object(Json.field("input", "成绩单截图"), Json.field("model", "OCR + 结构化抽取"), Json.field("output", "提取课程成绩，触发风险规则或补强建议。"), Json.field("status", "后续增强"))
        ));
    }

    public synchronized String agentInsightJson() {
        return Json.object(
                Json.field("title", "优先处理张明远高风险学业预警"),
                Json.field("summary", "建议辅导员先确认 RW2026001，导师同步制定高等数学与数据结构 4 周补强计划，并在下次复评中重点观察出勤率和作业完成率。"),
                Json.rawField("tags", Json.stringArray(List.of("高风险", "导师帮扶", "4 周复评", "可生成预警建议")))
        );
    }

    public synchronized String rolesJson() {
        return Json.array(List.of(
                Json.object(Json.field("role", "学生"), Json.field("duty", "查看自己的成长画像、查看 AI 学习建议、提交帮扶反馈。"), Json.field("route", "#students"), Json.field("actor", "张明远")),
                Json.object(Json.field("role", "辅导员"), Json.field("duty", "查看风险学生列表、确认风险预警单、填写辅导员意见、跟踪处理状态。"), Json.field("route", "#warnings"), Json.field("actor", "王老师")),
                Json.object(Json.field("role", "导师"), Json.field("duty", "查看学生课程短板、制定帮扶措施、推荐课程和项目。"), Json.field("route", "#courses"), Json.field("actor", "陈导师")),
                Json.object(Json.field("role", "学院管理者"), Json.field("duty", "查看驾驶舱、风险分布、预警处理情况和帮扶成效。"), Json.field("route", "#dashboard"), Json.field("actor", "学院管理者"))
        ));
    }

    public synchronized String auditLogsJson(UserContext user) {
        return Json.array(filterAudit(user).stream().map(this::auditJson).toList());
    }

    public synchronized String loginJson(String body) {
        String role = RequestUtil.value(body, "role", "辅导员");
        String account = RequestUtil.value(body, "account", RequestUtil.value(body, "email", "demo@campus.edu"));
        String name = switch (role) {
            case "学生" -> "张明远";
            case "导师" -> "陈导师";
            case "学院管理者" -> "学院管理者";
            default -> "王老师";
        };
        addAudit(name, "登录智慧校园平台");
        return Json.object(Json.boolField("ok", true), Json.rawField("user", userJson(name, role, account)));
    }

    public synchronized String registerJson(String body) {
        String role = RequestUtil.value(body, "role", "学生");
        String name = RequestUtil.value(body, "name", "校园用户");
        String email = RequestUtil.value(body, "email", "demo@campus.edu");
        addAudit(name, "注册智慧校园平台账号");
        return Json.object(Json.boolField("ok", true), Json.rawField("user", userJson(name, role, email)));
    }

    public synchronized String createWarningSuggestionJson() {
        List<String> activeStudentNos = warnings.stream().filter(w -> !"done".equals(w.statusKey)).map(w -> w.studentNo).toList();
        Student candidate = students.stream().filter(s -> !activeStudentNos.contains(s.no)).findFirst().orElse(students.getFirst());
        String code = nextWarningCode();
        String riskKey = "normal".equals(candidate.riskKey) ? "watch" : candidate.riskKey;
        String level = "normal".equals(candidate.riskKey) ? "需要关注" : candidate.riskLevel;
        WarningOrder warning = new WarningOrder(code, candidate.name + level + "智能预警", candidate.no, candidate.name,
                level, riskKey, Math.max(candidate.riskScore, 55), "金蝶 Agent API 建议", "待确认", "todo",
                "辅导员", "2026-06-18", "待辅导员核实画像、成绩和行为证据。", "待导师制定课程补强或成长规划。", "未反馈。");
        warnings.addFirst(warning);
        addAudit("CampusPilot Agent", "创建预警单 " + warning.code + "：" + warning.student);
        addWorkflow(warning.code, "CampusPilot Agent", "生成预警单", "根据画像、成绩、行为和风险规则生成结构化预警单。");
        return Json.object(Json.boolField("ok", true), Json.boolField("created", true), Json.rawField("warning", warningJson(warning)));
    }

    public synchronized String updateWarningJson(String code, String action, String body, UserContext user) {
        WarningOrder warning = findWarning(code);
        if (warning == null) {
            return "";
        }
        switch (action) {
            case "confirm" -> {
                warning.status = "帮扶中";
                warning.statusKey = "active";
                warning.owner = "辅导员 / 专业导师";
                warning.counselorNote = RequestUtil.value(body, "counselorNote", "已确认风险，进入帮扶阶段。");
                addAudit(user.name(), "确认预警单 " + code);
                addWorkflow(code, user.name(), "辅导员确认", warning.counselorNote);
            }
            case "mentor-plan" -> {
                warning.status = "帮扶中";
                warning.statusKey = "active";
                warning.owner = "专业导师";
                warning.mentorPlan = RequestUtil.value(body, "mentorPlan", "制定课程补强计划。");
                addAudit(user.name(), "更新导师帮扶计划 " + code);
                addWorkflow(code, user.name(), "导师帮扶", warning.mentorPlan);
            }
            case "feedback" -> {
                warning.studentFeedback = RequestUtil.value(body, "studentFeedback", "学生已提交反馈。");
                addAudit(user.name(), "提交学生反馈 " + code);
                addWorkflow(code, user.name(), "学生反馈", warning.studentFeedback);
            }
            case "close" -> {
                warning.status = "已结案";
                warning.statusKey = "done";
                warning.owner = "学院管理者";
                String closeNote = RequestUtil.value(body, "closeNote", "复评通过，阶段结案。");
                warning.counselorNote = closeNote;
                addAudit(user.name(), "复评结案 " + code);
                addWorkflow(code, user.name(), "复评结案", closeNote);
            }
            default -> {
                return "";
            }
        }
        return Json.object(Json.boolField("ok", true), Json.rawField("warning", warningJson(warning)));
    }

    public String agentContextJson() {
        return Json.object(
                Json.rawField("overview", overviewJson()),
                Json.rawField("riskDistribution", riskDistributionJson()),
                Json.rawField("agentWorkflow", agentWorkflowJson()),
                Json.rawField("lowcodeBlueprint", lowcodeBlueprintJson()),
                Json.intField("warningCount", warnings.size())
        );
    }

    public synchronized void addAudit(String actor, String action) {
        auditLogs.addFirst(new AuditLog(RequestUtil.now(), actor == null || actor.isBlank() ? "系统" : actor, action));
    }

    public boolean canPerform(String role, String action) {
        return switch (role == null ? "" : role) {
            case "学生" -> List.of("viewOwnProfile", "viewCourses", "viewAgentAdvice", "submitFeedback").contains(action);
            case "辅导员" -> List.of("viewRiskStudents", "confirmWarning", "closeWarning", "createWarningSuggestion", "viewBehaviors", "viewWarnings").contains(action);
            case "导师" -> List.of("viewCourseShortfalls", "saveMentorPlan", "viewWarnings", "viewWorkflow").contains(action);
            case "学院管理者" -> List.of("viewDashboard", "viewWarnings", "viewAudit", "saveSettings", "closeWarning").contains(action);
            default -> false;
        };
    }

    private void addWorkflow(String code, String actor, String step, String detail) {
        workflowLogs.addFirst(new WorkflowLog(code, RequestUtil.now(), actor, step, detail));
    }

    private WarningOrder findWarning(String code) {
        return warnings.stream().filter(w -> w.code.equals(code)).findFirst().orElse(null);
    }

    private String nextWarningCode() {
        int max = warnings.stream()
                .filter(w -> w.code.startsWith("RW"))
                .map(w -> w.code.substring(2))
                .mapToInt(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (Exception ignored) {
                        return 2026000;
                    }
                })
                .max()
                .orElse(2026000);
        return "RW" + (max + 1);
    }

    private long countRisk(String key) {
        return students.stream().filter(s -> key.equals(s.riskKey)).count();
    }

    private long countStatus(String key) {
        return warnings.stream().filter(w -> key.equals(w.statusKey)).count();
    }

    private List<Student> filterStudents(UserContext user) {
        if ("学生".equals(user.role())) {
            return students.stream().filter(s -> s.name.equals(user.name()) || "STU2026001".equals(s.no)).toList();
        }
        if ("辅导员".equals(user.role())) {
            return students.stream().filter(s -> List.of("high", "watch", "improved").contains(s.riskKey)).toList();
        }
        if ("导师".equals(user.role())) {
            return students.stream().filter(s -> "high".equals(s.riskKey) || s.status.contains("帮扶") || s.status.contains("规划")).toList();
        }
        return students;
    }

    private List<Course> filterCourses(UserContext user) {
        if ("学生".equals(user.role())) {
            return courses.stream().filter(c -> c.student.equals(user.name()) || "张明远".equals(c.student)).toList();
        }
        if ("导师".equals(user.role())) {
            return courses.stream().filter(c -> c.score < 75 || c.type.contains("核心")).toList();
        }
        return courses;
    }

    private List<Behavior> filterBehaviors(UserContext user) {
        if ("学生".equals(user.role())) {
            return behaviors.stream().filter(b -> b.student.equals(user.name()) || "张明远".equals(b.student)).toList();
        }
        if ("辅导员".equals(user.role())) {
            return behaviors.stream().filter(b -> b.score < 80 || b.trend.contains("下降")).toList();
        }
        return behaviors;
    }

    private List<WarningOrder> filterWarnings(UserContext user) {
        if ("学生".equals(user.role())) {
            return warnings.stream().filter(w -> w.student.equals(user.name()) || "STU2026001".equals(w.studentNo)).toList();
        }
        if ("导师".equals(user.role())) {
            return warnings.stream().filter(w -> "active".equals(w.statusKey) || "high".equals(w.riskKey)).toList();
        }
        if ("辅导员".equals(user.role())) {
            return warnings.stream().filter(w -> !"done".equals(w.statusKey) || !"normal".equals(w.riskKey)).toList();
        }
        return warnings;
    }

    private List<AuditLog> filterAudit(UserContext user) {
        if ("学生".equals(user.role())) {
            return auditLogs.stream().filter(a -> a.actor.equals(user.name()) || a.actor.equals("CampusPilot Agent")).toList();
        }
        if ("辅导员".equals(user.role()) || "导师".equals(user.role())) {
            return auditLogs.stream().limit(12).toList();
        }
        return auditLogs;
    }

    private String userJson(String name, String role, String account) {
        return Json.object(Json.field("name", name), Json.field("role", role), Json.field("account", account));
    }

    private String studentJson(Student s) {
        return Json.object(
                Json.field("name", s.name), Json.field("no", s.no), Json.field("college", s.college), Json.field("major", s.major),
                Json.field("grade", s.grade), Json.field("clazz", s.clazz), Json.field("careerGoal", s.careerGoal),
                Json.field("interests", s.interests), Json.decimalField("gpa", s.gpa), Json.intField("attendance", s.attendance),
                Json.intField("failedCourses", s.failedCourses), Json.intField("courseScore", s.courseScore),
                Json.intField("behaviorScore", s.behaviorScore), Json.intField("innovationScore", s.innovationScore),
                Json.field("riskLevel", s.riskLevel), Json.field("riskKey", s.riskKey), Json.intField("riskScore", s.riskScore),
                Json.field("profile", s.profile), Json.field("suggestion", s.suggestion), Json.field("status", s.status),
                Json.field("updatedAt", s.updatedAt)
        );
    }

    private String courseJson(Course c) {
        return Json.object(Json.field("student", c.student), Json.field("course", c.course), Json.intField("score", c.score),
                Json.field("type", c.type), Json.field("status", c.status), Json.field("suggestion", c.suggestion));
    }

    private String behaviorJson(Behavior b) {
        return Json.object(Json.field("student", b.student), Json.field("item", b.item), Json.intField("score", b.score),
                Json.field("trend", b.trend), Json.field("evidence", b.evidence), Json.field("updatedAt", b.updatedAt));
    }

    private String warningJson(WarningOrder w) {
        return Json.object(Json.field("code", w.code), Json.field("title", w.title), Json.field("studentNo", w.studentNo),
                Json.field("student", w.student), Json.field("level", w.level), Json.field("riskKey", w.riskKey),
                Json.intField("score", w.score), Json.field("source", w.source), Json.field("status", w.status),
                Json.field("statusKey", w.statusKey), Json.field("owner", w.owner), Json.field("deadline", w.deadline),
                Json.field("counselorNote", w.counselorNote), Json.field("mentorPlan", w.mentorPlan),
                Json.field("studentFeedback", w.studentFeedback));
    }

    private String workflowLogJson(WorkflowLog log) {
        return Json.object(Json.field("code", log.code), Json.field("time", log.time), Json.field("actor", log.actor),
                Json.field("step", log.step), Json.field("detail", log.detail));
    }

    private String auditJson(AuditLog log) {
        return Json.object(Json.field("time", log.time), Json.field("actor", log.actor), Json.field("action", log.action));
    }

    private void seed() {
        students.add(new Student("张明远", "STU2026001", "信息工程学院", "人工智能", "2023级", "AI 2301", "AI 算法工程师", "机器学习、算法竞赛", 2.31, 71, 2, 76, 62, 38, "高风险", "high", 86, "高等数学和数据结构薄弱，出勤率与作业完成率偏低。", "建议辅导员确认预警，导师制定 4 周课程补强计划。", "帮扶中", "2026-06-05"));
        students.add(new Student("李佳怡", "STU2026002", "信息工程学院", "数据科学", "2023级", "DS 2302", "数据分析师", "数据可视化、商业分析", 3.08, 88, 0, 78, 82, 55, "需要关注", "watch", 62, "无挂科，但近期出勤率下降，学习平台活跃度偏低。", "建议辅导员进行学习习惯提醒，并跟踪未来两周出勤。", "待确认", "2026-06-05"));
        students.add(new Student("陈思琪", "STU2026003", "计算机学院", "软件工程", "2023级", "SE 2301", "待明确", "前端开发、产品设计", 3.21, 91, 0, 91, 89, 42, "正常", "normal", 28, "学业表现稳定，但职业目标不够明确。", "建议导师帮助其明确成长方向，推荐参与课程项目。", "成长规划中", "2026-06-04"));
        students.add(new Student("周泽宇", "STU2026004", "网络空间安全学院", "网络安全", "2023级", "NS 2301", "安全工程师", "攻防演练、网络协议", 3.62, 96, 0, 96, 94, 81, "正常", "normal", 16, "学业和行为表现良好，科创参与度高。", "建议继续参加安全竞赛和项目实践。", "正常跟踪", "2026-06-03"));
        students.add(new Student("王嘉宁", "STU2026005", "信息工程学院", "计算机科学与技术", "2023级", "CS 2303", "后端工程师", "数据库、分布式系统", 2.92, 84, 0, 89, 87, 63, "改善中", "improved", 41, "曾有行为风险，近期出勤和作业完成率明显改善。", "建议保持复评，沉淀为帮扶改善案例。", "已结案", "2026-06-02"));

        courses.add(new Course("张明远", "高等数学", 58, "核心基础", "不及格", "每周 2 次辅导与错题复盘"));
        courses.add(new Course("张明远", "数据结构", 64, "专业核心", "低分", "结合在线题库完成链表、树、图专题训练"));
        courses.add(new Course("李佳怡", "统计建模", 72, "专业核心", "需提升", "补充案例训练并跟踪课堂参与"));
        courses.add(new Course("陈思琪", "软件工程实践", 89, "实践课程", "良好", "建议担任项目产品负责人"));
        courses.add(new Course("周泽宇", "网络攻防", 94, "专业方向", "优秀", "推荐参加校级安全竞赛"));
        courses.add(new Course("王嘉宁", "数据库原理", 82, "专业核心", "改善", "继续完成分布式数据库小项目"));

        behaviors.add(new Behavior("张明远", "课堂出勤", 71, "下降", "近两周缺勤 3 次", "2026-06-05"));
        behaviors.add(new Behavior("张明远", "作业完成", 62, "下降", "数据结构作业逾期 2 次", "2026-06-05"));
        behaviors.add(new Behavior("李佳怡", "学习平台活跃", 68, "下降", "近 7 天在线学习时长减少", "2026-06-05"));
        behaviors.add(new Behavior("陈思琪", "项目协作", 91, "稳定", "课程项目提交及时", "2026-06-04"));
        behaviors.add(new Behavior("周泽宇", "科创参与", 94, "上升", "安全竞赛训练营活跃", "2026-06-03"));
        behaviors.add(new Behavior("王嘉宁", "作业完成", 87, "改善", "连续两周按时提交", "2026-06-02"));

        warnings.add(new WarningOrder("RW2026001", "张明远高风险学业预警", "STU2026001", "张明远", "高风险", "high", 86, "Agent 风险分析", "帮扶中", "active", "辅导员 / 专业导师", "2026-06-10", "已确认存在学业高风险，需进入重点帮扶。", "制定高等数学与数据结构 4 周补强计划。", "愿意参加每周辅导并提交学习记录。"));
        warnings.add(new WarningOrder("RW2026002", "李佳怡学习行为关注预警", "STU2026002", "李佳怡", "需要关注", "watch", 62, "学习行为监测", "待确认", "todo", "辅导员", "2026-06-12", "待辅导员核实近期请假与课程参与情况。", "暂未进入导师帮扶。", "未反馈。"));
        warnings.add(new WarningOrder("RW2026003", "王嘉宁阶段帮扶改善记录", "STU2026005", "王嘉宁", "改善中", "improved", 41, "辅导员复评", "已结案", "done", "学院管理者", "2026-06-15", "连续两周出勤与作业完成率改善。", "继续跟踪数据库课程项目。", "已完成阶段任务。"));

        workflowLogs.add(new WorkflowLog("RW2026001", "2026-06-04 09:12", "CampusPilot Agent", "风险识别", "根据画像、课程成绩和学习行为识别为高风险。"));
        workflowLogs.add(new WorkflowLog("RW2026001", "2026-06-05 10:20", "王老师", "辅导员确认", "确认风险，进入导师帮扶阶段。"));
        workflowLogs.add(new WorkflowLog("RW2026001", "2026-06-05 16:40", "陈导师", "导师帮扶", "制定 4 周课程补强计划。"));
        workflowLogs.add(new WorkflowLog("RW2026002", "2026-06-04 11:35", "CampusPilot Agent", "风险识别", "学习行为下降，生成关注预警。"));
        workflowLogs.add(new WorkflowLog("RW2026003", "2026-06-02 18:10", "王老师", "复评结案", "出勤和作业完成率改善，沉淀为帮扶案例。"));

        auditLogs.add(new AuditLog("2026-06-06 18:47", "CampusPilot Agent", "生成高风险帮扶建议"));
        auditLogs.add(new AuditLog("2026-06-06 18:55", "陈导师", "更新导师帮扶措施"));
    }

    private record Student(String name, String no, String college, String major, String grade, String clazz,
                           String careerGoal, String interests, double gpa, int attendance, int failedCourses,
                           int courseScore, int behaviorScore, int innovationScore, String riskLevel,
                           String riskKey, int riskScore, String profile, String suggestion, String status,
                           String updatedAt) {
    }

    private record Course(String student, String course, int score, String type, String status, String suggestion) {
    }

    private record Behavior(String student, String item, int score, String trend, String evidence, String updatedAt) {
    }

    private static final class WarningOrder {
        private final String code;
        private final String title;
        private final String studentNo;
        private final String student;
        private final String level;
        private final String riskKey;
        private final int score;
        private final String source;
        private String status;
        private String statusKey;
        private String owner;
        private final String deadline;
        private String counselorNote;
        private String mentorPlan;
        private String studentFeedback;

        private WarningOrder(String code, String title, String studentNo, String student, String level, String riskKey,
                             int score, String source, String status, String statusKey, String owner, String deadline,
                             String counselorNote, String mentorPlan, String studentFeedback) {
            this.code = code;
            this.title = title;
            this.studentNo = studentNo;
            this.student = student;
            this.level = level;
            this.riskKey = riskKey;
            this.score = score;
            this.source = source;
            this.status = status;
            this.statusKey = statusKey;
            this.owner = owner;
            this.deadline = deadline;
            this.counselorNote = counselorNote;
            this.mentorPlan = mentorPlan;
            this.studentFeedback = studentFeedback;
        }
    }

    private record WorkflowLog(String code, String time, String actor, String step, String detail) {
    }

    private record AuditLog(String time, String actor, String action) {
    }
}
