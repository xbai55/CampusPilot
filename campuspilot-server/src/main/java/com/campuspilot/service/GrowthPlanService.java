package com.campuspilot.service;

import com.campuspilot.kingdee.KingdeeClient;
import com.campuspilot.kingdee.KingdeeFieldMappings;

import java.util.Map;

public final class GrowthPlanService {
    public static final String ENDPOINT = "/ierp/kapi/v2/code/code_campus_pilot/code_growthplan/growthplan";
    private final KingdeeClient client;

    public GrowthPlanService(KingdeeClient client) { this.client = client; }

    public String growthPlanJson(String studentId) {
        Map<String, String> row = KingdeeServiceSupport.findStudent(KingdeeServiceSupport.rows(client, ENDPOINT), studentId);
        return KingdeeFieldMappings.toJson(row);
    }
}