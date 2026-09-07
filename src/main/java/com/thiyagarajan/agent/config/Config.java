package com.thiyagarajan.agent.config;

import com.thiyagarajan.agent.runtime.AgentExecutionException;

public record Config(
        String ollamaUrl,
        String ollamaModel,
        boolean headless,
        int defaultTimeoutMs,
        int retries,
        int parallelism,
        String reportsDir,
        String screenshotsDir) {

    public Config {
        if (ollamaUrl == null || ollamaUrl.isBlank())
            throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "OLLAMA_URL cannot be blank");
        if (ollamaModel == null || ollamaModel.isBlank())
            throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "OLLAMA_MODEL cannot be blank");
        if (defaultTimeoutMs < 1)
            throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "DEFAULT_TIMEOUT_MS must be at least 1");
        if (retries < 0)
            throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "RETRIES cannot be negative");
        if (parallelism < 1)
            throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "PARALLELISM must be at least 1");
        if (reportsDir == null || reportsDir.isBlank())
            throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "REPORTS_DIR cannot be blank");
        if (screenshotsDir == null || screenshotsDir.isBlank())
            throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "SCREENSHOTS_DIR cannot be blank");
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int intEnv(String key, int fallback) {
        String value = env(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new AgentExecutionException(
                    AgentExecutionException.Category.CONFIGURATION,
                    key + " must be an integer, but was: " + value,
                    e);
        }
    }

    public static Config load() {
        return new Config(
                env("OLLAMA_URL", "http://localhost:11434"),
                env("OLLAMA_MODEL", "llama3.2"),
                Boolean.parseBoolean(env("HEADLESS", "true")),
                intEnv("DEFAULT_TIMEOUT_MS", 30000),
                intEnv("RETRIES", 0),
                intEnv("PARALLELISM", 4),
                env("REPORTS_DIR", "reports"),
                env("SCREENSHOTS_DIR", "screenshots")
        );
    }
}
