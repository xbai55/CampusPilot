package com.campuspilot.util;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RequestUtil {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private RequestUtil() {
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    public static String value(String body, String key, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            return fallback;
        }
        return unescape(matcher.group(1));
    }

    public static UserContext user(HttpExchange exchange, String body) {
        Headers headers = exchange.getRequestHeaders();
        String name = first(headers, "X-CampusPilot-User", "");
        if (!name.isBlank()) {
            name = URLDecoder.decode(name, StandardCharsets.UTF_8);
        }
        String roleKey = first(headers, "X-CampusPilot-Role-Key", "");
        String role = roleFromKey(roleKey);
        if (role.isBlank()) {
            role = value(body, "role", "");
        }
        if (name.isBlank()) {
            name = value(body, "name", "");
        }
        return new UserContext(name, role);
    }

    public static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    public static String roleFromKey(String key) {
        return switch (key == null ? "" : key) {
            case "student" -> "学生";
            case "counselor" -> "辅导员";
            case "mentor" -> "导师";
            case "manager" -> "学院管理者";
            default -> "";
        };
    }

    private static String first(Headers headers, String key, String fallback) {
        String value = headers.getFirst(key);
        return value == null ? fallback : value;
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    public record UserContext(String name, String role) {
        public boolean authenticated() {
            return name != null && !name.isBlank() && role != null && !role.isBlank();
        }
    }
}
