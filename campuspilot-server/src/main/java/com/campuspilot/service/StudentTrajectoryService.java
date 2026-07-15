package com.campuspilot.service;

import com.campuspilot.kingdee.KingdeeClient;

import java.util.List;
import java.util.Map;

public final class StudentTrajectoryService {
    public static final String ENDPOINT = "/ierp/kapi/v2/code/code_campus_pilot/code_studenttrajectory/student_trajectory_query";
    private final KingdeeClient client;

    public StudentTrajectoryService(KingdeeClient client) { this.client = client; }

    public String trajectoryJson(String studentId) {
        return KingdeeServiceSupport.mappedArray(rows(studentId));
    }

    List<Map<String, String>> rows(String studentId) {
        return KingdeeServiceSupport.rows(client, ENDPOINT).stream()
                .filter(row -> KingdeeServiceSupport.matchesStudent(row, studentId)).toList();
    }
}