package com.campuspilot;

import com.campuspilot.kingdee.KingdeeClient;
import com.campuspilot.service.CourseAbilityService;
import com.campuspilot.service.NotificationService;
import com.campuspilot.service.StudyCheckinService;
import com.sun.net.httpserver.HttpServer;

final class MissingKapiServicesTest {
    static void run() throws Exception {
        HttpServer server = TestSupport.server();
        try {
            server.createContext(NotificationService.ENDPOINT, exchange -> TestSupport.json(exchange, 200,
                    "{\"data\":{\"rows\":[{\"code_studentname\":\"S001\","
                            + "\"code_remindertype\":\"风险提醒\",\"code_processstatus\":\"待处理\"}]},\"status\":true}"));
            server.createContext(StudyCheckinService.ENDPOINT, exchange -> TestSupport.json(exchange, 200,
                    "{\"data\":{\"rows\":[{\"code_studentnumber\":\"S001\","
                            + "\"code_checkindate\":\"2026-07-16\",\"code_studycontent\":\"复习高数\"}]},\"status\":true}"));
            server.createContext(CourseAbilityService.ENDPOINT, exchange -> TestSupport.json(exchange, 200,
                    "{\"data\":{\"rows\":[{\"code_coursename\":\"高等数学\","
                            + "\"code_abilitydimension\":\"逻辑推理\",\"code_importance\":\"高\"}]},\"status\":true}"));

            KingdeeClient client = new KingdeeClient(TestSupport.config(TestSupport.baseUrl(server), "static", false));
            String notifications = new NotificationService(client).notificationsJson("S001");
            String checkins = new StudyCheckinService(client).checkinsJson("S001");
            String abilities = new CourseAbilityService(client).mappingsJson();

            TestSupport.require(notifications.contains("\"reminderType\":\"风险提醒\""), "notification mapping");
            TestSupport.require(checkins.contains("\"studyContent\":\"复习高数\""), "study check-in mapping");
            TestSupport.require(abilities.contains("\"abilityDimension\":\"逻辑推理\""), "course ability mapping");
            TestSupport.require(!notifications.contains("code_") && !checkins.contains("code_") && !abilities.contains("code_"),
                    "Kingdee field codes must not leak from the three completed APIs");
        } finally {
            server.stop(0);
        }
    }
}