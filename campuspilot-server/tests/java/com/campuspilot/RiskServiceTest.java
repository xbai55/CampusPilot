package com.campuspilot;

import com.campuspilot.kingdee.KingdeeClient;
import com.campuspilot.service.RiskService;
import com.sun.net.httpserver.HttpServer;

final class RiskServiceTest {
    static void run() throws Exception {
        HttpServer server = TestSupport.server();
        try {
            server.createContext(RiskService.ENDPOINT, exchange -> TestSupport.json(exchange, 200,
                    "{\"data\":{\"rows\":[{\"code_studentno\":\"S002\","
                            + "\"code_student_name\":\"李四\",\"code_risk_level\":\"高\","
                            + "\"code_risk_score\":\"86\",\"code_ai_suggestion\":\"安排辅导\"}]},\"status\":true}"));
            RiskService service = new RiskService(
                    new KingdeeClient(TestSupport.config(TestSupport.baseUrl(server), "static", false)));
            String json = service.riskJson("S002");
            TestSupport.require(json.contains("\"riskLevel\":\"高\""), "risk level mapping");
            TestSupport.require(json.contains("\"riskScore\":\"86\""), "risk score mapping");
            TestSupport.require(!json.contains("code_risk_level"), "Kingdee field codes must not leak");
        } finally {
            server.stop(0);
        }
    }
}