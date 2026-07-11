package com.campuspilot.util;

import java.util.Collection;
import java.util.stream.Collectors;

public final class Json {
    private Json() {
    }

    public static String object(String... fields) {
        return "{" + String.join(",", fields) + "}";
    }

    public static String field(String key, String value) {
        return quote(key) + ":" + quote(value);
    }

    public static String rawField(String key, String rawJson) {
        return quote(key) + ":" + rawJson;
    }

    public static String intField(String key, long value) {
        return quote(key) + ":" + value;
    }

    public static String decimalField(String key, double value) {
        return quote(key) + ":" + String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    public static String boolField(String key, boolean value) {
        return quote(key) + ":" + value;
    }

    public static String array(Collection<String> items) {
        return "[" + String.join(",", items) + "]";
    }

    public static String stringArray(Collection<String> items) {
        return "[" + items.stream().map(Json::quote).collect(Collectors.joining(",")) + "]";
    }

    public static String quote(String value) {
        return "\"" + escape(value == null ? "" : value) + "\"";
    }

    public static String escape(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
