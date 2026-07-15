package com.campuspilot.service;

import com.campuspilot.kingdee.KingdeeClient;
import com.campuspilot.kingdee.KingdeeFieldMappings;
import com.campuspilot.util.Json;

import java.util.Map;

public final class StudentProfileService {
    public static final String ENDPOINT = "/ierp/kapi/v2/code/code_campus_pilot/code_cp_student_profile/cp_student_profile_query";
    private final KingdeeClient client;

    public StudentProfileService(KingdeeClient client) { this.client = client; }

    public String profileJson(String studentId) {
        Map<String, String> row = KingdeeServiceSupport.findStudent(KingdeeServiceSupport.rows(client, ENDPOINT), studentId);
        return Json.object(
                Json.field("studentId", KingdeeServiceSupport.first(row, "code_studentnumber", "code_studentno", "number", "id")),
                Json.field("name", KingdeeServiceSupport.first(row, "code_studentname", "code_student_name", "name")),
                Json.rawField("academic", Json.object(
                        Json.field("gpa", row.getOrDefault("code_gpa", "")),
                        Json.field("creditRate", row.getOrDefault("code_creditrate", "")),
                        Json.field("attendanceRate", row.getOrDefault("code_attendancerate", "")),
                        Json.field("assignmentRate", row.getOrDefault("code_assignmentrate", ""))
                )),
                Json.rawField("risk", Json.object(
                        Json.field("level", row.getOrDefault("code_risklevel", "")),
                        Json.field("score", row.getOrDefault("code_riskscore", "")),
                        Json.field("reason", row.getOrDefault("code_riskreason", "")),
                        Json.field("suggestion", row.getOrDefault("code_aisuggestion", ""))
                )),
                Json.rawField("details", KingdeeFieldMappings.toJson(row))
        );
    }
}