package com.thiyagarajan.agent.io;

import com.thiyagarajan.agent.runtime.SecurityRedactor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Durable per-test API request/response log. Sensitive values are redacted before persistence. */
public final class ApiRunLogger implements AutoCloseable {
    private final Path file;

    private ApiRunLogger(Path file) throws IOException {
        this.file = file;
        Files.createDirectories(file.getParent());
        Files.writeString(file, "=== API execution started ===\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static ApiRunLogger start(String reportsDir, String testName) throws IOException {
        String safe = safe(testName, "api-test");
        String ts = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS", Locale.ROOT)
                .withZone(java.time.ZoneId.systemDefault()).format(Instant.now());
        return new ApiRunLogger(Path.of(reportsDir, "api", "logs", safe + "-" + ts + ".log").toAbsolutePath().normalize());
    }

    public synchronized void log(String event, String details) {
        String safeDetails = SecurityRedactor.redactText(details == null ? "" : details);
        try {
            Files.writeString(file, "[" + Instant.now() + "] " + event + " | " + safeDetails + "\n",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Logging must never make a test fail.
        }
    }

    public Path file() { return file; }

    @Override
    public void close() { log("FINISHED", "API execution completed"); }

    private static String safe(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String s = value.replaceAll("[^a-zA-Z0-9._-]+", "_");
        return s.isBlank() ? fallback : s.substring(0, Math.min(80, s.length()));
    }
}
