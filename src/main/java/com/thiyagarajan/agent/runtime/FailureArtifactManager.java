package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.config.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Creates filesystem-safe failure artifacts while masking sensitive evidence. */
public final class FailureArtifactManager {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSSXXX");
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Path root;

    public FailureArtifactManager() { this(Config.load()); }

    public FailureArtifactManager(Config config) {
        if (config == null) throw new IllegalArgumentException("Config cannot be null");
        root = Path.of(config.reportsDir(), config.screenshotsDir());
    }

    public Path createFailureMetadata(String testName, int stepIndex, String action, String details) throws Exception {
        Files.createDirectories(root);
        Path file = root.resolve(safe(testName, "test") + "-step-" + (stepIndex + 1) + "-" + timestamp() + ".json");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("generatedAt", OffsetDateTime.now().toString());
        data.put("testName", SecurityRedactor.redactText(testName == null ? "" : testName));
        data.put("step", stepIndex + 1);
        data.put("action", SecurityRedactor.redactText(action == null ? "" : action));
        data.put("details", SecurityRedactor.redactText(details == null ? "" : details));
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), data);
        return file;
    }

    public Path createApiFailureArtifact(String testName, int stepIndex, String action, String url,
                                         String requestBody, String details, String responseBody) throws Exception {
        Files.createDirectories(root);
        Path file = root.resolve(safe(testName, "test") + "-step-" + (stepIndex + 1) + "-" + timestamp() + "-api.txt");
        String content = "AI Testing Agent API Failure Artifact\n"
                + "Generated: " + OffsetDateTime.now() + "\n"
                + "Test: " + safeText(testName) + "\n"
                + "Step: " + (stepIndex + 1) + "\n"
                + "Action: " + safeText(action) + "\n"
                + "URL: " + safeText(url) + "\n"
                + "Request Body: " + safeText(requestBody) + "\n"
                + "Details: " + safeText(details) + "\n"
                + "Response Body: " + safeText(responseBody) + "\n";
        Files.writeString(file, content);
        return file;
    }

    public static String safe(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String cleaned = value.replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.isBlank() ? fallback : cleaned.substring(0, Math.min(cleaned.length(), 80));
    }

    private String timestamp() { return TS.format(OffsetDateTime.now()).replace(":", ""); }

    private String safeText(String value) {
        return SecurityRedactor.redactText(value == null ? "" : value).replace("\r", " ").replace("\n", "\\n");
    }
}
