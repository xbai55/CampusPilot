package com.campuspilot.kingdee;

import com.campuspilot.config.AppConfig;
import com.campuspilot.util.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unified server-side client for Kingdee AI Cangqiong KAPI.
 *
 * <p>V7.0.8 removed refreshToken, so an expiring or rejected token is refreshed
 * by acquiring a new token from /kapi/oauth2/getToken.</p>
 */
public final class KingdeeClient {
    private static final Logger LOG = Logger.getLogger(KingdeeClient.class.getName());
    private static final long EXPIRY_SKEW_MILLIS = 60_000L;

    private final AppConfig config;
    private final HttpClient httpClient;
    private volatile String cachedToken;
    private volatile long tokenExpiresAt;

    public KingdeeClient(AppConfig config) {
        this(config, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.kingdeeTimeoutMs()))
                .build());
    }

    KingdeeClient(AppConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
        this.cachedToken = config.kingdeeAccessToken();
        this.tokenExpiresAt = config.kingdeeAccessToken().isBlank() ? 0L : Long.MAX_VALUE;
    }

    public boolean configured() {
        return !config.kingdeeBaseUrl().isBlank()
                && (!config.kingdeeAccessToken().isBlank() || config.hasKingdeeCredentials());
    }

    public List<Map<String, String>> getRows(String endpoint) {
        if (!configured()) throw new KingdeeException("Kingdee KAPI is not configured");
        HttpResponse<String> response = sendGet(endpoint, token(false));
        if (isAuthFailure(response) && config.hasKingdeeCredentials()) {
            invalidateToken();
            response = sendGet(endpoint, token(true));
        }
        requireSuccess(response);
        return parseRows(response.body());
    }

    public String postOpenApiJson(String endpoint, String jsonBody) {
        if (!configured()) throw new KingdeeException("Kingdee OpenAPI is not configured");
        HttpResponse<String> response = sendPost(endpoint, jsonBody, token(false));
        if (isAuthFailure(response) && config.hasKingdeeCredentials()) {
            invalidateToken();
            response = sendPost(endpoint, jsonBody, token(true));
        }
        requireSuccess(response);
        return response.body() == null ? "" : response.body();
    }

    private HttpResponse<String> sendGet(String endpoint, String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(resolveBusinessUri(endpoint))
                    .timeout(Duration.ofMillis(config.kingdeeTimeoutMs()))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("accessToken", token)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .GET()
                    .build();
            LOG.fine(() -> "Kingdee GET " + request.uri().getPath());
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Kingdee KAPI network error for " + endpoint, ex);
            throw new KingdeeException("Kingdee KAPI network error: " + safeMessage(ex), ex);
        }
    }

    private HttpResponse<String> sendPost(String endpoint, String jsonBody, String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(resolveOpenApiUri(endpoint))
                    .timeout(Duration.ofMillis(config.kingdeeTimeoutMs()))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("accessToken", token)
                    .header("access_token", token)
                    .header("Authorization", "Bearer " + token)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody == null ? "{}" : jsonBody, StandardCharsets.UTF_8))
                    .build();
            LOG.fine(() -> "Kingdee OpenAPI POST " + request.uri().getPath());
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Kingdee OpenAPI network error for " + endpoint, ex);
            throw new KingdeeException("Kingdee OpenAPI network error: " + safeMessage(ex), ex);
        }
    }

    private synchronized String token(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && cachedToken != null && !cachedToken.isBlank() && now + EXPIRY_SKEW_MILLIS < tokenExpiresAt) {
            return cachedToken;
        }
        if (!config.hasKingdeeCredentials()) {
            if (cachedToken != null && !cachedToken.isBlank()) return cachedToken;
            throw new KingdeeException("Kingdee access token or client credentials are required");
        }
        TokenResult result = requestToken();
        cachedToken = result.value();
        tokenExpiresAt = now + Math.max(result.expiresInMillis(), EXPIRY_SKEW_MILLIS);
        return cachedToken;
    }

    private TokenResult requestToken() {
        String body = Json.object(
                Json.field("client_id", config.kingdeeClientId()),
                Json.field("client_secret", config.kingdeeClientSecret()),
                Json.field("username", config.kingdeeUsername()),
                Json.field("accountId", config.kingdeeAccountId()),
                Json.field("language", config.kingdeeLanguage()),
                Json.field("nonce", UUID.randomUUID().toString()),
                Json.field("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
        );
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(resolvePlatformUri("/kapi/oauth2/getToken"))
                    .timeout(Duration.ofMillis(config.kingdeeTimeoutMs()))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            requireSuccess(response);
            String token = stringValue(response.body(), "access_token");
            long expiresIn = longValue(response.body(), "expires_in", 7_200_000L);
            if (token.isBlank()) throw new KingdeeException("Token response is missing data.access_token");
            LOG.info("Kingdee access token acquired; expires in " + expiresIn + " ms");
            return new TokenResult(token, expiresIn);
        } catch (KingdeeException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Kingdee token request failed", ex);
            throw new KingdeeException("Kingdee token request failed: " + safeMessage(ex), ex);
        }
    }

    private void requireSuccess(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new KingdeeException("Kingdee HTTP " + response.statusCode());
        }
        String body = response.body() == null ? "" : response.body();
        String status = rawValue(body, "status");
        if ("false".equalsIgnoreCase(status)) {
            throw new KingdeeException("Kingdee " + stringValue(body, "errorCode") + ": " + stringValue(body, "message"));
        }
        String errorCode = stringValue(body, "errorCode");
        if (errorCode.isBlank()) errorCode = stringValue(body, "errCode");
        if (!errorCode.isBlank() && !"0".equals(errorCode)) {
            throw new KingdeeException("Kingdee " + errorCode + ": " + stringValue(body, "message"));
        }
    }

    private boolean isAuthFailure(HttpResponse<String> response) {
        if (response.statusCode() == 401 || response.statusCode() == 403) return true;
        String body = response.body() == null ? "" : response.body();
        String code = stringValue(body, "errorCode");
        if (code.isBlank()) code = stringValue(body, "errCode");
        String message = stringValue(body, "message").toLowerCase();
        return ("401".equals(code) || "612".equals(code))
                || message.contains("token") && (message.contains("expired") || message.contains("失效") || message.contains("过期"));
    }

    private synchronized void invalidateToken() {
        cachedToken = "";
        tokenExpiresAt = 0L;
    }

    private URI resolveBusinessUri(String endpoint) {
        String path = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        if (!path.startsWith("/ierp/")) path = "/ierp" + path;
        String base = config.kingdeeBaseUrl();
        if (base.endsWith("/ierp")) path = path.substring("/ierp".length());
        return URI.create(base + path);
    }

    private URI resolvePlatformUri(String endpoint) {
        String base = config.kingdeeBaseUrl();
        if (!base.endsWith("/ierp")) base = base + "/ierp";
        return URI.create(base + (endpoint.startsWith("/") ? endpoint : "/" + endpoint));
    }

    private URI resolveOpenApiUri(String endpoint) {
        String base = config.kingdeeBaseUrl();
        String path = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        if (!base.endsWith("/ierp")) base = base + "/ierp";
        if (!path.startsWith("/kapi/")) path = "/kapi" + path;
        return URI.create(base + path);
    }

    public static List<Map<String, String>> parseRows(String json) {
        int rowsKey = json == null ? -1 : json.indexOf("\"rows\"");
        int start = rowsKey < 0 ? -1 : json.indexOf('[', rowsKey);
        if (start < 0) throw new KingdeeException("Kingdee response is missing data.rows");
        int end = matching(json, start, '[', ']');
        List<Map<String, String>> rows = new ArrayList<>();
        int cursor = start + 1;
        while (cursor < end) {
            int objectStart = json.indexOf('{', cursor);
            if (objectStart < 0 || objectStart >= end) break;
            int objectEnd = matching(json, objectStart, '{', '}');
            rows.add(parseFlatObject(json.substring(objectStart + 1, objectEnd)));
            cursor = objectEnd + 1;
        }
        return rows;
    }

    public static List<Map<String, String>> parseObjectArray(String json, String key) {
        int keyAt = json == null ? -1 : json.indexOf(Json.quote(key));
        int start = keyAt < 0 ? -1 : json.indexOf('[', keyAt);
        if (start < 0) throw new KingdeeException("Kingdee response is missing " + key + " array");
        int end = matching(json, start, '[', ']');
        List<Map<String, String>> rows = new ArrayList<>();
        int cursor = start + 1;
        while (cursor < end) {
            int objectStart = json.indexOf('{', cursor);
            if (objectStart < 0 || objectStart >= end) break;
            int objectEnd = matching(json, objectStart, '{', '}');
            rows.add(parseFlatObject(json.substring(objectStart + 1, objectEnd)));
            cursor = objectEnd + 1;
        }
        return rows;
    }

    private static Map<String, String> parseFlatObject(String body) {
        Map<String, String> values = new LinkedHashMap<>();
        int i = 0;
        while (i < body.length()) {
            i = skip(body, i, " ,\r\n\t");
            if (i >= body.length() || body.charAt(i) != '"') break;
            Parsed key = quoted(body, i);
            i = skip(body, key.next(), " \r\n\t");
            if (i >= body.length() || body.charAt(i) != ':') break;
            i = skip(body, i + 1, " \r\n\t");
            Parsed value;
            if (i < body.length() && body.charAt(i) == '"') value = quoted(body, i);
            else {
                int next = i;
                while (next < body.length() && body.charAt(next) != ',') next++;
                String raw = body.substring(i, next).trim();
                value = new Parsed("null".equals(raw) ? "" : raw, next);
            }
            values.put(key.value(), value.value());
            i = value.next();
        }
        return values;
    }

    static String stringValue(String json, String key) {
        String value = rawValue(json, key);
        return value == null ? "" : value;
    }

    public static String jsonString(String json, String key) {
        return stringValue(json, key);
    }

    private static long longValue(String json, String key, long fallback) {
        try {
            return Long.parseLong(stringValue(json, key));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String rawValue(String json, String key) {
        if (json == null) return "";
        int at = json.indexOf("\"" + key + "\"");
        if (at < 0) return "";
        int colon = json.indexOf(':', at);
        if (colon < 0) return "";
        int valueAt = skip(json, colon + 1, " \r\n\t");
        if (valueAt < json.length() && json.charAt(valueAt) == '"') return quoted(json, valueAt).value();
        int end = valueAt;
        while (end < json.length() && ",}]\r\n".indexOf(json.charAt(end)) < 0) end++;
        return json.substring(valueAt, end).trim();
    }

    private static Parsed quoted(String text, int quoteAt) {
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = quoteAt + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                value.append(switch (c) { case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t'; default -> c; });
                escaped = false;
            } else if (c == '\\') escaped = true;
            else if (c == '"') return new Parsed(value.toString(), i + 1);
            else value.append(c);
        }
        throw new KingdeeException("Unclosed JSON string");
    }

    private static int matching(String text, int start, char open, char close) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') inString = false;
            } else if (c == '"') inString = true;
            else if (c == open) depth++;
            else if (c == close && --depth == 0) return i;
        }
        throw new KingdeeException("Unclosed JSON structure");
    }

    private static int skip(String text, int at, String chars) {
        int i = at;
        while (i < text.length() && chars.indexOf(text.charAt(i)) >= 0) i++;
        return i;
    }

    private static String safeMessage(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private record Parsed(String value, int next) {}
    private record TokenResult(String value, long expiresInMillis) {}

    public static final class KingdeeException extends RuntimeException {
        public KingdeeException(String message) { super(message); }
        public KingdeeException(String message, Throwable cause) { super(message, cause); }
    }
}
