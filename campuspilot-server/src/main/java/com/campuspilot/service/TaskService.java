package com.campuspilot.service;

import com.campuspilot.kingdee.KingdeeClient;
import com.campuspilot.util.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds role tasks from Kingdee warning states and notification records. */
public final class TaskService {
    private final KingdeeClient client;

    public TaskService(KingdeeClient client) {
        this.client = client;
    }

    public String tasksJson(String role) {
        if (!client.configured()) {
            return Json.object(
                    Json.boolField("ok", false), Json.boolField("demo", true),
                    Json.field("dataSource", "unavailable"), Json.field("role", role),
                    Json.field("message", "未配置金蝶 KAPI，未伪造角色待办。"), Json.rawField("tasks", "[]")
            );
        }
        List<String> tasks = new ArrayList<>();
        for (Map<String, String> row : KingdeeServiceSupport.rows(client, NotificationService.ENDPOINT)) {
            String targetRole = row.getOrDefault("code_receiverrole", "");
            if (matchesRole(role, targetRole)) tasks.add(notificationTask(row, targetRole));
        }
        for (Map<String, String> row : KingdeeServiceSupport.rows(client, RiskService.ENDPOINT)) {
            String targetRole = warningRole(row);
            if (matchesRole(role, targetRole)) tasks.add(warningTask(row, targetRole));
        }
        return Json.object(
                Json.boolField("ok", true), Json.boolField("demo", false),
                Json.field("dataSource", "kingdee-api"), Json.field("role", role),
                Json.rawField("tasks", Json.array(tasks))
        );
    }

    private static String notificationTask(Map<String, String> row, String role) {
        return Json.object(
                Json.field("id", KingdeeServiceSupport.first(row, "code_notinumber", "number", "id")),
                Json.field("type", "notification"), Json.field("role", role),
                Json.field("student", row.getOrDefault("code_studentname", "")),
                Json.field("title", row.getOrDefault("code_content", "")),
                Json.field("status", row.getOrDefault("code_processstatus", "")),
                Json.field("dueAt", row.getOrDefault("code_remindertime", "")),
                Json.field("source", "notificationrecord")
        );
    }

    private static String warningTask(Map<String, String> row, String role) {
        return Json.object(
                Json.field("id", KingdeeServiceSupport.first(row, "number", "id")),
                Json.field("type", "risk-warning"), Json.field("role", role),
                Json.field("student", row.getOrDefault("code_student_name", "")),
                Json.field("title", KingdeeServiceSupport.first(row, "name", "code_risk_reason")),
                Json.field("status", row.getOrDefault("code_status", "")),
                Json.field("riskLevel", row.getOrDefault("code_risk_level", "")),
                Json.field("source", "cp_risk_warning")
        );
    }

    private static String warningRole(Map<String, String> row) {
        String status = row.getOrDefault("code_status", "");
        if (status.contains("待") || status.contains("草稿")) return "辅导员";
        if (row.getOrDefault("code_tutor", "").isBlank()) return "导师";
        if (row.getOrDefault("code_std_feedback", "").isBlank()) return "学生";
        return "学院管理者";
    }

    private static boolean matchesRole(String requested, String actual) {
        if (requested == null || requested.isBlank()) return true;
        String normalized = switch (requested) {
            case "student" -> "学生";
            case "counselor" -> "辅导员";
            case "mentor" -> "导师";
            case "manager" -> "学院管理者";
            default -> requested;
        };
        return actual == null || actual.isBlank() || actual.contains(normalized) || normalized.contains(actual);
    }
}
