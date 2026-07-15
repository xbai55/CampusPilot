package com.campuspilot.service;

import com.campuspilot.kingdee.KingdeeClient;
import com.campuspilot.util.Json;

import java.util.List;
import java.util.Map;

public final class OpportunityService {
    public static final String RECOMMENDATIONS = "/ierp/kapi/v2/code/code_campus_pilot/code_studentopprec/studentopprec";
    public static final String LIBRARY = "/ierp/kapi/v2/code/code_campus_pilot/code_growthopportunity/growthopportunity";
    private final KingdeeClient client;

    public OpportunityService(KingdeeClient client) { this.client = client; }

    public String opportunitiesJson(String studentId) {
        List<Map<String, String>> recommendations = KingdeeServiceSupport.rows(client, RECOMMENDATIONS).stream()
                .filter(row -> KingdeeServiceSupport.matchesStudent(row, studentId)).toList();
        return Json.object(
                Json.rawField("recommendations", KingdeeServiceSupport.mappedArray(recommendations)),
                Json.rawField("library", KingdeeServiceSupport.mappedArray(KingdeeServiceSupport.rows(client, LIBRARY)))
        );
    }
}