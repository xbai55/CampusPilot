package com.campuspilot.config;

import java.nio.file.Path;
import java.util.Arrays;

public record AppConfig(
        String host,
        int port,
        Path staticRoot,
        String agentApiUrl,
        String agentApiKey,
        int agentTimeoutMs,
        String kingdeeBaseUrl,
        String kingdeeAccessToken,
        String kingdeeClientId,
        String kingdeeClientSecret,
        String kingdeeUsername,
        String kingdeeAccountId,
        String kingdeeLanguage,
        int kingdeeTimeoutMs,
        int workerThreads,
        String corsAllowedOrigins,
        String apiBearerToken,
        String workflowApiUrl,
        String workflowApiKey,
        int workflowTimeoutMs
) {
    public static AppConfig load(String[] args) {
        int cliPort = args.length > 0 ? parseInt(args[0], -1) : -1;
        String host = env("CAMPUSPILOT_HOST", "0.0.0.0");
        int port = cliPort > 0 ? cliPort : parseInt(env("CAMPUSPILOT_PORT", "8787"), 8787);
        Path staticRoot = Path.of(env("CAMPUSPILOT_STATIC_ROOT", "../campuspilot-home")).toAbsolutePath().normalize();
        String agentApiUrl = env("CAMPUSPILOT_AGENT_API_URL", "");
        String agentApiKey = env("CAMPUSPILOT_AGENT_API_KEY", "");
        return new AppConfig(
                host,
                port,
                staticRoot,
                agentApiUrl,
                agentApiKey,
                parseInt(env("CAMPUSPILOT_AGENT_TIMEOUT_MS", "8000"), 8000),
                stripTrailingSlash(envAlias("KINGDEE_BASE_URL", "CAMPUSPILOT_KINGDEE_BASE_URL", "http://127.0.0.1:8881")),
                envAlias("KINGDEE_ACCESS_TOKEN", "CAMPUSPILOT_KINGDEE_ACCESS_TOKEN", ""),
                envAlias("KINGDEE_CLIENT_ID", "CAMPUSPILOT_KINGDEE_CLIENT_ID", ""),
                envAlias("KINGDEE_CLIENT_SECRET", "CAMPUSPILOT_KINGDEE_CLIENT_SECRET", ""),
                envAlias("KINGDEE_USERNAME", "CAMPUSPILOT_KINGDEE_USERNAME", ""),
                envAlias("KINGDEE_ACCOUNT_ID", "CAMPUSPILOT_KINGDEE_ACCOUNT_ID", ""),
                envAlias("KINGDEE_LANGUAGE", "CAMPUSPILOT_KINGDEE_LANGUAGE", "zh_CN"),
                parseInt(envAlias("KINGDEE_TIMEOUT", "CAMPUSPILOT_KINGDEE_TIMEOUT_MS", "8000"), 8000),
                parseInt(env("CAMPUSPILOT_WORKER_THREADS", "12"), 12),
                env("CAMPUSPILOT_CORS_ALLOWED_ORIGINS", "http://127.0.0.1:8881"),
                env("CAMPUSPILOT_API_BEARER_TOKEN", ""),
                env("CAMPUSPILOT_WORKFLOW_API_URL", agentApiUrl),
                env("CAMPUSPILOT_WORKFLOW_API_KEY", agentApiKey),
                parseInt(env("CAMPUSPILOT_WORKFLOW_TIMEOUT_MS", "12000"), 12000)
        );
    }

    public String hostForLog() {
        return "0.0.0.0".equals(host) ? "127.0.0.1" : host;
    }

    public boolean hasKingdeeCredentials() {
        return !kingdeeClientId.isBlank()
                && !kingdeeClientSecret.isBlank()
                && !kingdeeUsername.isBlank()
                && !kingdeeAccountId.isBlank();
    }

    public boolean workflowConfigured() {
        return workflowApiUrl != null && !workflowApiUrl.isBlank();
    }

    public boolean requiresBearerToken() {
        return apiBearerToken != null && !apiBearerToken.isBlank();
    }

    public boolean originAllowed(String origin) {
        if (origin == null || origin.isBlank()) return true;
        return Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .anyMatch(allowed -> "*".equals(allowed) || allowed.equals(origin));
    }

    public String responseOrigin(String requestOrigin) {
        if (requestOrigin != null && !requestOrigin.isBlank() && originAllowed(requestOrigin)) return requestOrigin;
        return Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse("http://127.0.0.1:8881");
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String envAlias(String preferredKey, String legacyKey, String fallback) {
        String preferred = System.getenv(preferredKey);
        if (preferred != null && !preferred.isBlank()) return preferred.trim();
        return env(legacyKey, fallback);
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String stripTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
