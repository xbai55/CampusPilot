package com.campuspilot.service;

import com.campuspilot.kingdee.KingdeeClient;

import java.util.List;
import java.util.Map;

/** Queries Kingdee notification/reminder records for one student. */
public final class NotificationService {
    public static final String ENDPOINT = "/ierp/kapi/v2/code/code_campus_pilot/code_notificationrecord/notificationrecord";
    private final KingdeeClient client;

    public NotificationService(KingdeeClient client) {
        this.client = client;
    }

    public String notificationsJson(String studentId) {
        List<Map<String, String>> rows = KingdeeServiceSupport.rows(client, ENDPOINT).stream()
                .filter(row -> KingdeeServiceSupport.matchesStudent(row, studentId))
                .toList();
        return KingdeeServiceSupport.mappedArray(rows);
    }
}
