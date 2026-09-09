package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Persists successful locator-healing decisions for audit and later analysis. */
public final class HealingHistoryManager {
    private final Path file;
    private final ObjectMapper mapper;

    public HealingHistoryManager(Path reportsDir) throws Exception {
        this(reportsDir, new ObjectMapper().findAndRegisterModules());
    }

    public HealingHistoryManager(Path reportsDir, ObjectMapper mapper) throws Exception {
        if (reportsDir == null) throw new IllegalArgumentException("Reports directory cannot be null");
        if (mapper == null) throw new IllegalArgumentException("ObjectMapper cannot be null");
        this.file = reportsDir.toAbsolutePath().normalize().resolve("healing").resolve("history.json");
        this.mapper = mapper;
        Files.createDirectories(file.getParent());
    }

    public synchronized void record(String runId, String testName, String action,
                                    SelfHealingEngine.HealingResult healing) throws Exception {
        if (healing == null || !healing.healed()) return;
        List<Entry> entries = load();
        entries.add(new Entry(Instant.now().toString(), safe(runId), safe(testName), safe(action),
                safe(healing.originalLocator()), safe(healing.healedLocator()), healing.confidence(), safe(healing.reason())));
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), entries);
    }

    public List<Entry> load() throws Exception { 
        if (!Files.isRegularFile(file)) return new ArrayList<>();
        List<Entry> values = mapper.readValue(file.toFile(), new TypeReference<List<Entry>>() {});
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    public Path file() { return file; }
    private String safe(String value) { return SecurityRedactor.redactText(value == null ? "" : value); }

    public static class Entry {
        public String timestamp;
        public String runId;
        public String testName;
        public String action;
        public String originalLocator;
        public String healedLocator;
        public double confidence;
        public String reason;
        public Entry() { }
        public Entry(String timestamp, String runId, String testName, String action, String originalLocator,
                     String healedLocator, double confidence, String reason) {
            this.timestamp = timestamp; this.runId = runId; this.testName = testName; this.action = action;
            this.originalLocator = originalLocator; this.healedLocator = healedLocator;
            this.confidence = confidence; this.reason = reason;
        }
    }
}
