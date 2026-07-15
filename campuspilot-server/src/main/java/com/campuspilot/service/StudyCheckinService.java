package com.campuspilot.service;

import com.campuspilot.kingdee.KingdeeClient;

import java.util.List;
import java.util.Map;

/** Queries Kingdee study check-in records for one student. */
public final class StudyCheckinService {
    public static final String ENDPOINT = "/ierp/kapi/v2/code/code_campus_pilot/code_studycheckinrec/studycheckinrec";
    private final KingdeeClient client;

    public StudyCheckinService(KingdeeClient client) {
        this.client = client;
    }

    public String checkinsJson(String studentId) {
        List<Map<String, String>> rows = KingdeeServiceSupport.rows(client, ENDPOINT).stream()
                .filter(row -> KingdeeServiceSupport.matchesStudent(row, studentId))
                .toList();
        return KingdeeServiceSupport.mappedArray(rows);
    }
}
