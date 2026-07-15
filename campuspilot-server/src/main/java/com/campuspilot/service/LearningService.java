package com.campuspilot.service;

import com.campuspilot.kingdee.KingdeeClient;
import com.campuspilot.util.Json;

import java.util.List;
import java.util.Map;

public final class LearningService {
    public static final String BEHAVIOR = "/ierp/kapi/v2/code/code_campus_pilot/code_cp_learning_behavior/cp_learning_behavior_query";
    public static final String MULTI_BEHAVIOR = "/ierp/kapi/v2/code/code_campus_pilot/code_stumultibehaviorrec/stumultibehaviorrec";
    public static final String CLASSROOM = "/ierp/kapi/v2/code/code_campus_pilot/code_classlearningrec/classlearningrec";
    private final KingdeeClient client;
    private final StudentTrajectoryService trajectoryService;

    public LearningService(KingdeeClient client, StudentTrajectoryService trajectoryService) {
        this.client = client;
        this.trajectoryService = trajectoryService;
    }

    public String learningJson(String studentId) {
        return Json.object(
                Json.rawField("behavior", KingdeeServiceSupport.mappedArray(filtered(BEHAVIOR, studentId))),
                Json.rawField("trajectory", KingdeeServiceSupport.mappedArray(trajectoryService.rows(studentId))),
                Json.rawField("multiDimensionalBehavior", KingdeeServiceSupport.mappedArray(filtered(MULTI_BEHAVIOR, studentId))),
                Json.rawField("classroomLearning", KingdeeServiceSupport.mappedArray(filtered(CLASSROOM, studentId)))
        );
    }

    private List<Map<String, String>> filtered(String endpoint, String studentId) {
        return KingdeeServiceSupport.rows(client, endpoint).stream()
                .filter(row -> KingdeeServiceSupport.matchesStudent(row, studentId)).toList();
    }
}