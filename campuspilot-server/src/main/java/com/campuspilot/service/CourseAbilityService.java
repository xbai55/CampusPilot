package com.campuspilot.service;

import com.campuspilot.kingdee.KingdeeClient;

/** Queries the global Kingdee course-to-ability mapping table. */
public final class CourseAbilityService {
    public static final String ENDPOINT = "/ierp/kapi/v2/code/code_campus_pilot/code_courseabilitymap/courseabilitymap";
    private final KingdeeClient client;

    public CourseAbilityService(KingdeeClient client) {
        this.client = client;
    }

    public String mappingsJson() {
        return KingdeeServiceSupport.mappedArray(KingdeeServiceSupport.rows(client, ENDPOINT));
    }
}
