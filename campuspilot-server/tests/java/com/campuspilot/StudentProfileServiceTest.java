package com.campuspilot;

import com.campuspilot.kingdee.KingdeeClient;
import com.campuspilot.service.StudentProfileService;
import com.sun.net.httpserver.HttpServer;

final class StudentProfileServiceTest {
    static void run() throws Exception {
        HttpServer server = TestSupport.server();
        try {
            server.createContext(StudentProfileService.ENDPOINT, exchange -> TestSupport.json(exchange, 200,
                    "{\"data\":{\"rows\":[{\"number\":\"S001\",\"name\":\"张三\","
                            + "\"code_gpa\":\"3.72\",\"code_creditrate\":\"91.5\","
                            + "\"code_risklevel\":\"低\",\"code_riskscore\":\"18\","
                            + "\"code_aisuggestion\":\"保持节奏\"}]},\"status\":true}"));
            StudentProfileService service = new StudentProfileService(
                    new KingdeeClient(TestSupport.config(TestSupport.baseUrl(server), "static", false)));
            String json = service.profileJson("S001");
            TestSupport.require(json.contains("\"studentId\":\"S001\""), "student id mapping");
            TestSupport.require(json.contains("\"gpa\":\"3.72\""), "gpa mapping");
            TestSupport.require(json.contains("\"suggestion\":\"保持节奏\""), "suggestion mapping");
            TestSupport.require(!json.contains("code_gpa"), "Kingdee field codes must not leak");
        } finally {
            server.stop(0);
        }
    }
}