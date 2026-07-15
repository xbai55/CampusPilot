package com.campuspilot;

import com.campuspilot.kingdee.KingdeeClient;
import com.sun.net.httpserver.HttpServer;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

final class KingdeeClientTest {
    static void run() throws Exception {
        tokenAcquisitionAndRetry();
        networkException();
    }

    private static void tokenAcquisitionAndRetry() throws Exception {
        HttpServer server = TestSupport.server();
        AtomicInteger tokens = new AtomicInteger();
        try {
            server.createContext("/kapi/oauth2/getToken", exchange -> {
                int n = tokens.incrementAndGet();
                String body = "{\"data\":{\"access_token\":\"token-" + n
                        + "\",\"expires_in\":\"7200000\"},\"errorCode\":\"0\",\"message\":\"\",\"status\":true}";
                TestSupport.json(exchange, 200, body);
            });
            server.createContext("/ierp/kapi/test", exchange -> {
                String token = exchange.getRequestHeaders().getFirst("accessToken");
                if ("token-1".equals(token)) {
                    TestSupport.json(exchange, 401, "{\"status\":false,\"errorCode\":\"401\",\"message\":\"token expired\"}");
                } else {
                    TestSupport.json(exchange, 200, "{\"data\":{\"rows\":[{\"code_studentno\":\"S001\"}]},\"status\":true}");
                }
            });
            KingdeeClient client = new KingdeeClient(TestSupport.config(TestSupport.baseUrl(server), "", true));
            Map<String, String> row = client.getRows("/ierp/kapi/test").get(0);
            TestSupport.require("S001".equals(row.get("code_studentno")), "rows should be parsed");
            TestSupport.require(tokens.get() == 2, "invalid token should trigger one reacquisition");
        } finally {
            server.stop(0);
        }
    }

    private static void networkException() {
        KingdeeClient client = new KingdeeClient(TestSupport.config("http://127.0.0.1:1", "static", false));
        boolean failed = false;
        try {
            client.getRows("/ierp/kapi/test");
        } catch (KingdeeClient.KingdeeException expected) {
            failed = expected.getMessage().contains("network error");
        }
        TestSupport.require(failed, "network errors should be wrapped as KingdeeException");
    }
}