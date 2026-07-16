package com.campuspilot.service;

import com.campuspilot.config.AppConfig;
import com.campuspilot.store.InMemoryCampusPilotStore;
import com.campuspilot.util.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class AgentClient {
    private final AppConfig config;
    private final InMemoryCampusPilotStore store;
    private final KingdeeDataClient kingdeeDataClient;
    private final HttpClient httpClient;
    private volatile long lastSuccessAt;
    private volatile String lastError = "";

    public AgentClient(AppConfig config, InMemoryCampusPilotStore store, KingdeeDataClient kingdeeDataClient) {
        this.config = config;
        this.store = store;
        this.kingdeeDataClient = kingdeeDataClient;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.agentTimeoutMs()))
                .build();
    }

    public String chat(String question, String role) {
        String normalized = question == null ? "" : question.trim();
        if (!config.agentApiUrl().isBlank()) {
            String remote = callRemoteAgent(normalized, role);
            if (!remote.isBlank()) {
                store.addAudit(roleOrAgent(role), "调用金蝶 Agent API：" + shortText(normalized));
                return remote;
            }
        }
        store.addAudit(roleOrAgent(role), "使用本地 Agent 兜底建议：" + shortText(normalized));
        return localFallback(normalized, role);
    }

    private String callRemoteAgent(String question, String role) {
        try {
            String body = Json.object(
                    Json.field("question", question),
                    Json.field("role", role == null ? "" : role),
                    Json.rawField("campusContext", kingdeeDataClient.agentContextJson())
            );
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(config.agentApiUrl()))
                    .timeout(Duration.ofMillis(config.agentTimeoutMs()))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            if (!config.agentApiKey().isBlank()) {
                builder.header("Authorization", "Bearer " + config.agentApiKey());
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                lastSuccessAt = System.currentTimeMillis();
                lastError = "";
                String responseBody = response.body() == null ? "" : response.body().trim();
                if (responseBody.startsWith("{") && responseBody.endsWith("}")) {
                    return responseBody;
                }
                return Json.object(
                        Json.field("answer", responseBody),
                        Json.rawField("chips", Json.stringArray(List.of("金蝶 Agent API", "远程返回", "业务问答"))),
                        Json.field("source", "Kingdee Agent API")
                );
            }
            lastError = "Agent HTTP " + response.statusCode();
        } catch (Exception ex) {
            lastError = shortText(ex.getMessage());
            store.addAudit("系统", "金蝶 Agent API 调用失败，已启用本地兜底：" + shortText(ex.getMessage()));
        }
        return "";
    }

    public String statusJson() {
        String state = config.agentApiUrl().isBlank() ? "unconfigured"
                : lastSuccessAt > 0 ? "connected"
                : lastError.isBlank() ? "configured-unverified" : "failed";
        return Json.object(
                Json.boolField("configured", !config.agentApiUrl().isBlank()),
                Json.field("state", state),
                Json.field("url", config.agentApiUrl().isBlank() ? "未配置" : config.agentApiUrl()),
                Json.field("lastSuccessAt", lastSuccessAt == 0 ? "" : Instant.ofEpochMilli(lastSuccessAt).toString()),
                Json.field("lastError", lastError)
        );
    }

    private String localFallback(String question, String role) {
        String answer;
        List<String> chips;
        if (question.contains("预警") || question.contains("风险")) {
            answer = "建议优先处理张明远的高风险学业预警：辅导员先确认画像、成绩和行为证据，导师同步制定 4 周课程补强计划，学生每周提交一次学习反馈。";
            chips = List.of("生成预警单", "导师帮扶", "复评结案");
        } else if (question.contains("课程") || question.contains("成绩")) {
            answer = "当前课程短板集中在高等数学、数据结构和统计建模。可以按学生风险等级生成补强任务，并把导师建议写入预警处理记录。";
            chips = List.of("课程短板", "补强计划", "成长画像");
        } else if (question.contains("平台") || question.contains("金蝶")) {
            answer = "本地后端已经预留金蝶 Agent API 代理入口。部署到云主机后设置 CAMPUSPILOT_AGENT_API_URL 和 CAMPUSPILOT_AGENT_API_KEY 即可切换到真实 Agent。";
            chips = List.of("Agent API", "云部署", "环境变量");
        } else {
            answer = "可以从学生画像、课程成绩、学习行为、风险预警单和处理日志五类数据出发，形成“识别-确认-帮扶-反馈-结案”的闭环。";
            chips = List.of("画像", "闭环", "驾驶舱");
        }
        return Json.object(
                Json.field("answer", answer),
                Json.rawField("chips", Json.stringArray(chips)),
                Json.field("source", config.agentApiUrl().isBlank() ? "CampusPilot Java Local Fallback" : "CampusPilot Java Agent Proxy Fallback"),
                Json.field("dataSource", "local-demo"),
                Json.boolField("demo", true)
        );
    }

    private static String roleOrAgent(String role) {
        return role == null || role.isBlank() ? "CampusPilot Agent" : role;
    }

    private static String shortText(String value) {
        if (value == null || value.isBlank()) {
            return "空请求";
        }
        return value.length() > 24 ? value.substring(0, 24) : value;
    }
}
