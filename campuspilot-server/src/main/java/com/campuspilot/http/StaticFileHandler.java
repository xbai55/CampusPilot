package com.campuspilot.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class StaticFileHandler implements HttpHandler {
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            ".html", "text/html; charset=utf-8",
            ".js", "application/javascript; charset=utf-8",
            ".css", "text/css; charset=utf-8",
            ".png", "image/png",
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".svg", "image/svg+xml; charset=utf-8"
    );

    private final Path staticRoot;

    public StaticFileHandler(Path staticRoot) {
        this.staticRoot = staticRoot;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "method not allowed".getBytes());
            return;
        }
        String uriPath = exchange.getRequestURI().getPath();
        String relative = "/".equals(uriPath) ? "index.html" : uriPath.substring(1);
        Path target = staticRoot.resolve(relative).normalize();
        if (!target.startsWith(staticRoot) || Files.isDirectory(target) || !Files.exists(target)) {
            target = staticRoot.resolve("index.html").normalize();
        }
        if (!target.startsWith(staticRoot) || !Files.exists(target)) {
            send(exchange, 404, "text/plain; charset=utf-8", "not found".getBytes());
            return;
        }
        byte[] body = Files.readAllBytes(target);
        send(exchange, 200, contentType(target), body);
    }

    private static String contentType(Path target) {
        String name = target.getFileName().toString().toLowerCase();
        for (Map.Entry<String, String> entry : CONTENT_TYPES.entrySet()) {
            if (name.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "application/octet-stream";
    }

    private static void send(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
