package com.campuspilot.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonParser {
    private final String text;
    private int at;

    private JsonParser(String text) {
        this.text = text == null ? "" : text;
    }

    public static Object parse(String text) {
        JsonParser parser = new JsonParser(text);
        Object value = parser.value();
        parser.ws();
        if (parser.at != parser.text.length()) throw new IllegalArgumentException("Unexpected JSON content");
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(String text) {
        Object value = parse(text);
        if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
        throw new IllegalArgumentException("JSON root is not an object");
    }

    private Object value() {
        ws();
        if (at >= text.length()) throw new IllegalArgumentException("Unexpected end of JSON");
        char c = text.charAt(at);
        if (c == '"') return string();
        if (c == '{') return object();
        if (c == '[') return array();
        if (text.startsWith("true", at)) {
            at += 4;
            return Boolean.TRUE;
        }
        if (text.startsWith("false", at)) {
            at += 5;
            return Boolean.FALSE;
        }
        if (text.startsWith("null", at)) {
            at += 4;
            return null;
        }
        return number();
    }

    private Map<String, Object> object() {
        at++;
        Map<String, Object> map = new LinkedHashMap<>();
        ws();
        if (take('}')) return map;
        while (true) {
            ws();
            String key = string();
            ws();
            expect(':');
            map.put(key, value());
            ws();
            if (take('}')) return map;
            expect(',');
        }
    }

    private List<Object> array() {
        at++;
        List<Object> list = new ArrayList<>();
        ws();
        if (take(']')) return list;
        while (true) {
            list.add(value());
            ws();
            if (take(']')) return list;
            expect(',');
        }
    }

    private String string() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (at < text.length()) {
            char c = text.charAt(at++);
            if (c == '"') return sb.toString();
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (at >= text.length()) throw new IllegalArgumentException("Unclosed escape");
            char escaped = text.charAt(at++);
            switch (escaped) {
                case '"', '\\', '/' -> sb.append(escaped);
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    if (at + 4 > text.length()) throw new IllegalArgumentException("Invalid unicode escape");
                    sb.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                    at += 4;
                }
                default -> throw new IllegalArgumentException("Invalid escape");
            }
        }
        throw new IllegalArgumentException("Unclosed JSON string");
    }

    private Number number() {
        int start = at;
        if (peek('-')) at++;
        while (at < text.length() && Character.isDigit(text.charAt(at))) at++;
        if (peek('.')) {
            at++;
            while (at < text.length() && Character.isDigit(text.charAt(at))) at++;
        }
        if (peek('e') || peek('E')) {
            at++;
            if (peek('+') || peek('-')) at++;
            while (at < text.length() && Character.isDigit(text.charAt(at))) at++;
        }
        String raw = text.substring(start, at);
        if (raw.isBlank() || "-".equals(raw)) throw new IllegalArgumentException("Invalid JSON number");
        return raw.contains(".") || raw.contains("e") || raw.contains("E")
                ? Double.parseDouble(raw) : Long.parseLong(raw);
    }

    private void ws() {
        while (at < text.length() && Character.isWhitespace(text.charAt(at))) at++;
    }

    private boolean take(char c) {
        if (peek(c)) {
            at++;
            return true;
        }
        return false;
    }

    private boolean peek(char c) {
        return at < text.length() && text.charAt(at) == c;
    }

    private void expect(char c) {
        if (!take(c)) throw new IllegalArgumentException("Expected '" + c + "'");
    }
}
