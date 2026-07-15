package com.campuspilot;

import com.campuspilot.config.AppConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

final class TestSupport {
    private TestSupport() {}

    static AppConfig config(String baseUrl, String staticToken, boolean credentials) {
        return new AppConfig("127.0.0.1", 0, Path.of("."), "", "", 1000,
                baseUrl, staticToken,
                credentials ? "client" : "", credentials ? "secret" : "",
                credentials ? "user" : "", credentials ? "account" : "",
                "zh_CN", 1000, 1);
    }

    static HttpServer server() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        return server;
    }

    static String baseUrl(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    static void json(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}