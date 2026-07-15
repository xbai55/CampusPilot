package com.campuspilot.service;

import com.campuspilot.kingdee.KingdeeClient;
import com.campuspilot.kingdee.KingdeeFieldMappings;
import com.campuspilot.util.Json;

import java.util.List;
import java.util.Map;

final class KingdeeServiceSupport {
    private KingdeeServiceSupport() {}

    static List<Map<String, String>> rows(KingdeeClient client, String endpoint) {
        return client.configured() ? client.getRows(endpoint) : List.of();
    }

    static Map<String, String> findStudent(List<Map<String, String>> rows, String studentId) {
        return rows.stream().filter(row -> matchesStudent(row, studentId)).findFirst().orElse(Map.of());
    }

    static boolean matchesStudent(Map<String, String> row, String studentId) {
        if (studentId == null || studentId.isBlank()) return true;
        return studentId.equals(first(row, "code_studentnumber", "code_studentno", "number", "id"))
                || studentId.equals(first(row, "code_studentname", "code_student_name", "name"));
    }

    static String mappedArray(List<Map<String, String>> rows) {
        return Json.array(rows.stream().map(KingdeeFieldMappings::toJson).toList());
    }

    static String first(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(key);
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }
}