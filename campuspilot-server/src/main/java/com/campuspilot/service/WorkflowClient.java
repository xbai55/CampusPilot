package com.campuspilot.service;

import com.campuspilot.config.AppConfig;
import com.campuspilot.store.InMemoryCampusPilotStore;
import com.campuspilot.util.Json;
import com.campuspilot.util.RequestUtil.UserContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Server-side gateway for Kingdee Agent/task-flow operations. */
public final class WorkflowClient {
    private final AppConfig config;
    private final InMemoryCampusPilotStore store;
    private final HttpClient httpClient;
    private volatile long lastSuccessAt;
    private volatile String lastError = "";

    public WorkflowClient(AppConfig config, InMemoryCampusPilotStore store) {
        this.config = config;
        this.store = store;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.workflowTimeoutMs()))
                .build();
    }

    public WorkflowResponse execute(String operation, String inputJson, UserContext user) {
        if (!config.workflowConfigured()) {
            return new WorkflowResponse(503, Json.object(
                    Json.boolField("ok", false),
                    Json.boolField("demo", true),
                    Json.field("dataSource", "local-demo"),
                    Json.field("operation", operation),
                    Json.field("message", "未配置 CAMPUSPILOT_WORKFLOW_API_URL，未执行平台写入或任务流操作。")
            ));
        }
        String payload = Json.object(
                Json.field("operation", operation),
                Json.field("requestId", UUID.randomUUID().toString()),
                Json.rawField("actor", Json.object(
                        Json.field("name", user == null ? "" : user.name()),
                        Json.field("role", user == null ? "" : user.role())
                )),
                Json.rawField("input", validJson(inputJson))
        );
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(config.workflowApiUrl()))
                    .timeout(Duration.ofMillis(config.workflowTimeoutMs()))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
            if (!config.workflowApiKey().isBlank()) {
                builder.header("Authorization", "Bearer " + config.workflowApiKey());
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String remoteBody = response.body() == null ? "" : response.body().trim();
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                lastSuccessAt = System.currentTimeMillis();
                lastError = "";
                store.addAudit(user == null ? "系统" : user.role(), "调用金蝶任务流：" + operation);
                return new WorkflowResponse(response.statusCode(), Json.object(
                        Json.boolField("ok", true),
                        Json.boolField("demo", false),
                        Json.field("dataSource", "kingdee-workflow"),
                        Json.field("operation", operation),
                        Json.rawField("result", validJson(remoteBody))
                ));
            }
            lastError = "Workflow HTTP " + response.statusCode();
            return upstreamFailure(operation, lastError, response.statusCode());
        } catch (Exception ex) {
            lastError = shortText(ex.getMessage());
            return upstreamFailure(operation, lastError, 502);
        }
    }

    public String statusJson() {
        String state = !config.workflowConfigured() ? "unconfigured"
                : lastSuccessAt > 0 ? "connected"
                : lastError.isBlank() ? "configured-unverified" : "failed";
        return Json.object(
                Json.boolField("configured", config.workflowConfigured()),
                Json.field("state", state),
                Json.field("url", config.workflowConfigured() ? config.workflowApiUrl() : "未配置"),
                Json.field("lastSuccessAt", lastSuccessAt == 0 ? "" : Instant.ofEpochMilli(lastSuccessAt).toString()),
                Json.field("lastError", lastError)
        );
    }

    private static WorkflowResponse upstreamFailure(String operation, String message, int upstreamStatus) {
        return new WorkflowResponse(502, Json.object(
                Json.boolField("ok", false),
                Json.boolField("demo", false),
                Json.field("dataSource", "kingdee-workflow"),
                Json.field("operation", operation),
                Json.intField("upstreamStatus", upstreamStatus),
                Json.field("message", message)
        ));
    }

    private static String validJson(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.startsWith("{") || normalized.startsWith("[") ? normalized : "{}";
    }

    private static String shortText(String value) {
        if (value == null || value.isBlank()) return "未知错误";
        return value.length() > 160 ? value.substring(0, 160) : value;
    }

    public record WorkflowResponse(int statusCode, String json) {}
}
