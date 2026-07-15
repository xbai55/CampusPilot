package com.campuspilot.service;

import com.campuspilot.config.AppConfig;
import com.campuspilot.kingdee.KingdeeClient;
import com.campuspilot.kingdee.KingdeeFieldMappings;
import com.campuspilot.store.InMemoryCampusPilotStore;
import com.campuspilot.util.Json;
import com.campuspilot.util.RequestUtil.UserContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side adapter for CampusPilot business-object APIs published by Kingdee AI Cangqiong. */
public final class KingdeeDataClient {
    private static final String PROFILE = "/kapi/v2/code/code_campus_pilot/code_cp_student_profile/cp_student_profile_query";
    private static final String COURSE = "/kapi/v2/code/code_campus_pilot/code_cp_course_score/cp_course_score_query";
    private static final String BEHAVIOR = "/kapi/v2/code/code_campus_pilot/code_cp_learning_behavior/cp_learning_behavior_query";
    private static final String WARNING = "/kapi/v2/code/code_campus_pilot/code_cp_risk_warning/cp_risk_warning_query";
    private static final String GROWTH_PLAN = "/kapi/v2/code/code_campus_pilot/code_growthplan/growthplan";
    private static final String TRAJECTORY = "/kapi/v2/code/code_campus_pilot/code_studenttrajectory/student_trajectory_query";
    private static final String MULTI_BEHAVIOR = "/kapi/v2/code/code_campus_pilot/code_stumultibehaviorrec/stumultibehaviorrec";
    private static final String OPPORTUNITY = "/kapi/v2/code/code_campus_pilot/code_studentopprec/studentopprec";
    private static final String OPPORTUNITY_LIBRARY = "/kapi/v2/code/code_campus_pilot/code_growthopportunity/growthopportunity";
    private static final String CLASSROOM = "/kapi/v2/code/code_campus_pilot/code_classlearningrec/classlearningrec";
    private static final String NOTIFICATION = "/kapi/v2/code/code_campus_pilot/code_notificationrecord/notificationrecord";
    private static final String STUDY_CHECKIN = "/kapi/v2/code/code_campus_pilot/code_studycheckinrec/studycheckinrec";
    private static final String COURSE_ABILITY = "/kapi/v2/code/code_campus_pilot/code_courseabilitymap/courseabilitymap";
    private static final long CACHE_MILLIS = 30_000L;

    private final AppConfig config;
    private final InMemoryCampusPilotStore store;
    private final KingdeeClient client;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private volatile String lastError = "";
    private volatile long lastSuccessAt;

    public KingdeeDataClient(AppConfig config, InMemoryCampusPilotStore store) {
        this.config = config;
        this.store = store;
        this.client = new KingdeeClient(config);
    }

    public boolean configured() {
        return client.configured();
    }

    public String dataMode() {
        return configured() ? "kingdee-api" : "local-fallback";
    }

    public String studentsJson(UserContext user) {
        List<Map<String, String>> rows = configured() ? queryRows(PROFILE) : null;
        if (rows == null) return store.studentsJson(user);
        return Json.array(rows.stream().filter(row -> allowStudent(row, user)).map(this::studentJson).toList());
    }

    public String coursesJson(UserContext user) {
        List<Map<String, String>> rows = configured() ? queryRows(COURSE) : null;
        if (rows == null) return store.coursesJson(user);
        return Json.array(rows.stream().filter(row -> allowNamedStudent(row.get("code_student_name"), user)).map(this::courseJson).toList());
    }

    public String behaviorsJson(UserContext user) {
        List<Map<String, String>> rows = configured() ? queryRows(BEHAVIOR) : null;
        if (rows == null) return store.behaviorsJson(user);
        Map<String, String> names = profileNames();
        List<String> items = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String no = row.getOrDefault("code_studentno", "");
            String student = names.getOrDefault(no, no);
            if (!allowNamedStudent(student, user)) continue;
            String evidence = row.getOrDefault("code_behavior_risk_reason", "");
            String updatedAt = first(row, "code_last_login_time", "modifytime");
            items.add(behaviorJson(student, "课堂出勤", row.get("code_attendance_rate"), evidence, updatedAt));
            items.add(behaviorJson(student, "作业完成", row.get("code_assignment_completio"), evidence, updatedAt));
            items.add(behaviorJson(student, "学习平台活跃", row.get("code_learning_activity"), evidence, updatedAt));
            items.add(behaviorJson(student, "课堂互动", row.get("code_interaction_count"), evidence, updatedAt));
        }
        return Json.array(items);
    }

    public String warningsJson(UserContext user) {
        List<Map<String, String>> rows = configured() ? queryRows(WARNING) : null;
        if (rows == null) return store.warningsJson(user);
        return Json.array(rows.stream().filter(row -> allowNamedStudent(row.get("code_student_name"), user)).map(this::warningJson).toList());
    }

    public String agentContextJson() {
        if (!configured()) return store.agentContextJson();
        UserContext system = new UserContext("CampusPilot Agent", "学院管理者");
        return Json.object(
                Json.rawField("students", studentsJson(system)),
                Json.rawField("courses", coursesJson(system)),
                Json.rawField("behaviors", behaviorsJson(system)),
                Json.rawField("warnings", warningsJson(system)),
                Json.rawField("growthPlans", mappedRowsJson(GROWTH_PLAN)),
                Json.rawField("trajectories", mappedRowsJson(TRAJECTORY)),
                Json.rawField("multiDimensionalBehaviors", mappedRowsJson(MULTI_BEHAVIOR)),
                Json.rawField("opportunityRecommendations", mappedRowsJson(OPPORTUNITY)),
                Json.rawField("opportunityLibrary", mappedRowsJson(OPPORTUNITY_LIBRARY)),
                Json.rawField("classroomLearning", mappedRowsJson(CLASSROOM)),
                Json.rawField("notifications", mappedRowsJson(NOTIFICATION)),
                Json.rawField("studyCheckins", mappedRowsJson(STUDY_CHECKIN)),
                Json.rawField("courseAbilityMappings", mappedRowsJson(COURSE_ABILITY)),
                Json.field("dataSource", "Kingdee AI Cangqiong business objects")
        );
    }

    public String integrationJson() {
        String connection = !configured() ? "未配置 accessToken，使用本地演示数据"
                : lastSuccessAt > 0 ? "已连接金蝶业务对象 API"
                : lastError.isBlank() ? "已配置，等待首次请求" : "调用失败，已自动回退本地数据";
        return Json.object(
                Json.field("kingdeeBaseUrl", config.kingdeeBaseUrl()),
                Json.field("dataMode", dataMode()),
                Json.field("connection", connection),
                Json.field("lastError", lastError),
                Json.field("agentMode", config.agentApiUrl().isBlank() ? "本地 Agent 兜底" : "金蝶 Agent API 远程代理"),
                Json.field("agentApiUrl", config.agentApiUrl().isBlank() ? "未配置" : config.agentApiUrl()),
                Json.rawField("thirdPartyApp", Json.object(
                        Json.field("appId", "campuspilot_isv"),
                        Json.field("auth", "accessToken / API 授权 / IP 白名单"),
                        Json.field("status", configured() ? "业务对象 API 已配置" : "待配置 accessToken")
                )),
                Json.rawField("objects", Json.array(List.of(
                        objectStatus("学生画像", "cp_student_profile", 28, PROFILE),
                        objectStatus("课程成绩", "cp_course_score", 20, COURSE),
                        objectStatus("学习行为", "cp_learning_behavior", 18, BEHAVIOR),
                        objectStatus("风险预警单", "cp_risk_warning", 27, WARNING),
                        objectStatus("成长计划", "growthplan", 27, GROWTH_PLAN),
                        objectStatus("成长轨迹", "studenttrajectory", 27, TRAJECTORY),
                        objectStatus("多维行为", "stumultibehaviorrec", 26, MULTI_BEHAVIOR),
                        objectStatus("机会推荐", "studentopprec", 25, OPPORTUNITY),
                        objectStatus("成长机会库", "growthopportunity", 28, OPPORTUNITY_LIBRARY),
                        objectStatus("课堂学情", "classlearningrec", 24, CLASSROOM),
                        objectStatus("通知提醒", "notificationrecord", 23, NOTIFICATION),
                        objectStatus("学习打卡", "studycheckinrec", 24, STUDY_CHECKIN),
                        objectStatus("课程能力映射", "courseabilitymap", 21, COURSE_ABILITY)
                ))),
                Json.rawField("apis", Json.stringArray(List.of(
                        "GET /api/campuspilot/students", "GET /api/campuspilot/courses",
                        "GET /api/campuspilot/behaviors", "GET /api/campuspilot/warnings",
                        "GET /api/student/profile/{student_id}", "GET /api/student/growth-plan/{student_id}",
                        "GET /api/student/learning/{student_id}", "GET /api/student/risk-warning/{student_id}",
                        "GET /api/student/opportunities/{student_id}", "GET /api/student/notifications/{student_id}",
                        "GET /api/student/study-checkins/{student_id}", "GET /api/student/course-ability-mappings",
                        "POST /api/campuspilot/agent/chat"
                )))
        );
    }

    private String mappedRowsJson(String path) {
        List<Map<String, String>> rows = queryRows(path);
        if (rows == null) return "[]";
        return Json.array(rows.stream().map(KingdeeFieldMappings::toJson).toList());
    }

    private Map<String, String> profileNames() {
        List<Map<String, String>> rows = queryRows(PROFILE);
        Map<String, String> names = new LinkedHashMap<>();
        if (rows != null) for (Map<String, String> row : rows) names.put(first(row, "number", "code_studentno"), row.getOrDefault("name", ""));
        return names;
    }

    public KingdeeClient kingdeeClient() {
        return client;
    }

    private List<Map<String, String>> queryRows(String path) {
        CacheEntry cached = cache.get(path);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.createdAt < CACHE_MILLIS) return cached.rows;
        try {
            List<Map<String, String>> rows = client.getRows(path);
            cache.put(path, new CacheEntry(now, rows));
            lastSuccessAt = now;
            lastError = "";
            return rows;
        } catch (Exception ex) {
            lastError = shortText(ex.getMessage());
            return cached == null ? null : cached.rows;
        }
    }
    // Mapping helpers are kept explicit so Kingdee field codes remain auditable.
    private String studentJson(Map<String, String> row) {
        String level = first(row, "code_risklevel", "code_currentstatus");
        return Json.object(
                Json.field("name", row.getOrDefault("name", "")), Json.field("no", first(row, "number", "code_studentno")),
                Json.field("college", row.getOrDefault("code_college", "")), Json.field("major", row.getOrDefault("code_major", "")),
                Json.field("grade", row.getOrDefault("code_grade", "")), Json.field("clazz", row.getOrDefault("code_classname", "")),
                Json.field("careerGoal", row.getOrDefault("code_careergoal", "")), Json.field("interests", row.getOrDefault("code_interestarea", "")),
                Json.decimalField("gpa", number(row.get("code_gpa"))), Json.intField("attendance", score(row.get("code_attendancerate"))),
                Json.intField("failedCourses", integer(row.get("code_failedcount"))), Json.intField("courseScore", score(row.get("code_creditrate"))),
                Json.intField("behaviorScore", averageScore(row.get("code_attendancerate"), row.get("code_assignmentrate"))),
                Json.intField("innovationScore", score(row.get("code_innovationscore"))), Json.field("riskLevel", level),
                Json.field("riskKey", riskKey(level)), Json.intField("riskScore", score(row.get("code_riskscore"))),
                Json.field("profile", row.getOrDefault("code_riskreason", "")), Json.field("suggestion", row.getOrDefault("code_aisuggestion", "")),
                Json.field("status", row.getOrDefault("code_currentstatus", "")), Json.field("updatedAt", first(row, "code_lastevaltime", "modifytime"))
        );
    }

    private String courseJson(Map<String, String> row) {
        boolean failed = truthy(row.get("code_is_failed")) || number(row.get("code_score")) < 60;
        return Json.object(
                Json.field("student", row.getOrDefault("code_student_name", "")),
                Json.field("course", first(row, "code_course_name", "name")),
                Json.intField("score", score(row.get("code_score"))),
                Json.field("type", row.getOrDefault("code_course_type", "")),
                Json.field("status", failed ? "不及格" : "正常"),
                Json.field("suggestion", row.getOrDefault("code_course_direction", ""))
        );
    }

    private String behaviorJson(String student, String item, String value, String evidence, String updatedAt) {
        int normalizedScore = score(value);
        return Json.object(
                Json.field("student", student), Json.field("item", item), Json.intField("score", normalizedScore),
                Json.field("trend", !evidence.isBlank() || normalizedScore < 80 ? "需关注" : "稳定"),
                Json.field("evidence", evidence), Json.field("updatedAt", updatedAt)
        );
    }

    private String warningJson(Map<String, String> row) {
        String level = row.getOrDefault("code_risk_level", "");
        String status = row.getOrDefault("code_status", "");
        return Json.object(
                Json.field("code", first(row, "number", "id")), Json.field("title", first(row, "name", "code_risk_reason")),
                Json.field("studentNo", row.getOrDefault("code_studentno", "")), Json.field("student", row.getOrDefault("code_student_name", "")),
                Json.field("level", level), Json.field("riskKey", riskKey(level)), Json.intField("score", score(row.get("code_risk_score"))),
                Json.field("source", row.getOrDefault("code_warning_source", "")), Json.field("status", status),
                Json.field("statusKey", statusKey(status)), Json.field("owner", first(row, "code_counselor", "code_tutor")),
                Json.field("deadline", row.getOrDefault("code_confirm_time", "")), Json.field("counselorNote", row.getOrDefault("code_counselor", "")),
                Json.field("mentorPlan", first(row, "code_tutor", "code_ai_suggestion")),
                Json.field("studentFeedback", row.getOrDefault("code_std_feedback", ""))
        );
    }

    private boolean allowStudent(Map<String, String> row, UserContext user) {
        if ("学生".equals(user.role())) return user.name().equals(row.getOrDefault("name", ""));
        String key = riskKey(row.get("code_risklevel"));
        if ("辅导员".equals(user.role())) return List.of("high", "watch", "improved").contains(key);
        if ("导师".equals(user.role())) return "high".equals(key) || row.getOrDefault("code_currentstatus", "").contains("帮扶");
        return true;
    }

    private static boolean allowNamedStudent(String student, UserContext user) {
        return !"学生".equals(user.role()) || user.name().equals(student);
    }

    private String objectStatus(String name, String code, int fields, String path) {
        return Json.object(Json.field("name", name), Json.field("code", code),
                Json.field("status", configured() ? "API 已配置" : "本地兜底"), Json.intField("fields", fields), Json.field("path", path));
    }

    private static String riskKey(String level) {
        String value = level == null ? "" : level.toLowerCase(Locale.ROOT);
        if (value.contains("高") || value.contains("high")) return "high";
        if (value.contains("关注") || value.contains("watch") || value.contains("中")) return "watch";
        if (value.contains("改善") || value.contains("improved")) return "improved";
        return "normal";
    }

    private static String statusKey(String status) {
        String value = status == null ? "" : status.toLowerCase(Locale.ROOT);
        if (value.contains("结案") || value.contains("关闭") || value.contains("完成") || value.contains("done")) return "done";
        if (value.contains("待") || value.contains("todo")) return "todo";
        return "active";
    }

    private static boolean truthy(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return List.of("1", "true", "yes", "是", "y").contains(normalized);
    }

    private static int averageScore(String first, String second) {
        return (score(first) + score(second)) / 2;
    }

    private static int score(String value) {
        return Math.max(0, Math.min(100, (int) Math.round(number(value))));
    }

    private static int integer(String value) {
        return (int) Math.round(number(value));
    }

    private static double number(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Double.parseDouble(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String first(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(key);
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    private static String shortText(String value) {
        if (value == null) return "未知错误";
        return value.length() > 120 ? value.substring(0, 120) : value;
    }

    private record CacheEntry(long createdAt, List<Map<String, String>> rows) {}
}
