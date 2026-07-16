package com.campuspilot.service;

import com.campuspilot.kingdee.KingdeeClient;
import com.campuspilot.kingdee.KingdeeFieldMappings;
import com.campuspilot.util.Json;

import java.util.List;
import java.util.Map;

/** Aggregates profile, course and learning behavior into an auditable profile analysis. */
public final class ProfileAnalysisService {
    private final KingdeeClient client;

    public ProfileAnalysisService(KingdeeClient client) {
        this.client = client;
    }

    public String analysisJson(String studentId) {
        if (!client.configured()) return unavailable(studentId);
        Map<String, String> profile = KingdeeServiceSupport.findStudent(
                KingdeeServiceSupport.rows(client, StudentProfileService.ENDPOINT), studentId);
        List<Map<String, String>> courses = filtered(KingdeeDataClient.COURSE_ENDPOINT, studentId);
        List<Map<String, String>> behaviors = filtered(LearningService.BEHAVIOR, studentId);
        int failedCourses = (int) courses.stream().filter(ProfileAnalysisService::failed).count();
        int attendance = score(profile.get("code_attendancerate"));
        int assignment = score(profile.get("code_assignmentrate"));
        return Json.object(
                Json.boolField("ok", true),
                Json.boolField("demo", false),
                Json.field("dataSource", "kingdee-api"),
                Json.field("studentId", studentId),
                Json.rawField("profile", KingdeeFieldMappings.toJson(profile)),
                Json.rawField("abilityDimensions", Json.object(
                        Json.field("academic", profile.getOrDefault("code_gpa", "")),
                        Json.intField("learningEngagement", (attendance + assignment) / 2),
                        Json.intField("failedCourses", failedCourses),
                        Json.intField("innovation", score(profile.get("code_innovationscore")))
                )),
                Json.rawField("tags", Json.stringArray(List.of(
                        profile.getOrDefault("code_profiletags", ""),
                        profile.getOrDefault("code_riskreason", "")
                ).stream().filter(value -> !value.isBlank()).toList())),
                Json.rawField("courses", KingdeeServiceSupport.mappedArray(courses)),
                Json.rawField("behaviors", KingdeeServiceSupport.mappedArray(behaviors))
        );
    }

    private List<Map<String, String>> filtered(String endpoint, String studentId) {
        return KingdeeServiceSupport.rows(client, endpoint).stream()
                .filter(row -> KingdeeServiceSupport.matchesStudent(row, studentId)).toList();
    }

    private static boolean failed(Map<String, String> row) {
        String flag = row.getOrDefault("code_is_failed", "").toLowerCase();
        return List.of("1", "true", "yes", "是").contains(flag) || decimal(row.get("code_score")) < 60;
    }

    private static int score(String value) {
        return Math.max(0, Math.min(100, (int) Math.round(decimal(value))));
    }

    private static double decimal(String value) {
        try { return value == null || value.isBlank() ? 0 : Double.parseDouble(value); }
        catch (Exception ignored) { return 0; }
    }

    private static String unavailable(String studentId) {
        return Json.object(
                Json.boolField("ok", false), Json.boolField("demo", true),
                Json.field("dataSource", "unavailable"), Json.field("studentId", studentId),
                Json.field("message", "未配置金蝶 KAPI，未伪造画像分析结果。"),
                Json.rawField("profile", "{}"), Json.rawField("abilityDimensions", "{}"),
                Json.rawField("tags", "[]"), Json.rawField("courses", "[]"), Json.rawField("behaviors", "[]")
        );
    }
}
