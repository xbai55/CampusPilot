package com.campuspilot;

import com.campuspilot.kingdee.KingdeeClient;
import com.campuspilot.service.GrowthPlanService;
import com.sun.net.httpserver.HttpServer;

final class GrowthPlanServiceTest {
    static void run() throws Exception {
        HttpServer server = TestSupport.server();
        try {
            server.createContext(GrowthPlanService.ENDPOINT, exchange ->
                    TestSupport.json(exchange, 200, "{\"data\":{\"rows\":[]},\"status\":true}"));
            GrowthPlanService service = new GrowthPlanService(
                    new KingdeeClient(TestSupport.config(TestSupport.baseUrl(server), "static", false)));
            TestSupport.require("{}".equals(service.growthPlanJson("S404")), "empty rows should produce an empty object");
        } finally {
            server.stop(0);
        }
    }
}