package com.thiyagarajan.agent.config;

public record Config(
        String ollamaUrl,
        String ollamaModel,
        boolean headless,
        int defaultTimeoutMs,
        String reportsDir,
        String screenshotsDir) {

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int intEnv(String key, int fallback) {
        try { return Integer.parseInt(env(key, String.valueOf(fallback))); }
        catch (NumberFormatException e) { return fallback; }
    }

    public static Config load() {
        return new Config(
                env("OLLAMA_URL", "http://localhost:11434"),
                env("OLLAMA_MODEL", "llama3.2"),
                Boolean.parseBoolean(env("HEADLESS", "true")),
                Math.max(1, intEnv("DEFAULT_TIMEOUT_MS", 30000)),
                env("REPORTS_DIR", "reports"),
                env("SCREENSHOTS_DIR", "screenshots")
        );
    }
}
