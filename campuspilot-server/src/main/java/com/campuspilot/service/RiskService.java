package com.campuspilot.service;

import com.campuspilot.kingdee.KingdeeClient;
import com.campuspilot.kingdee.KingdeeFieldMappings;

import java.util.Map;

public final class RiskService {
    public static final String ENDPOINT = "/ierp/kapi/v2/code/code_campus_pilot/code_cp_risk_warning/cp_risk_warning_query";
    private final KingdeeClient client;

    public RiskService(KingdeeClient client) { this.client = client; }

    public String riskJson(String studentId) {
        Map<String, String> row = KingdeeServiceSupport.findStudent(KingdeeServiceSupport.rows(client, ENDPOINT), studentId);
        return KingdeeFieldMappings.toJson(row);
    }
}