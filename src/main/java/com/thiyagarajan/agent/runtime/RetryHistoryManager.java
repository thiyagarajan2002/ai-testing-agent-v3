package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.ai.FailureIntelligence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Persists bounded retry decisions and outcomes for auditability. */
public final class RetryHistoryManager {
    private final Path file;
    private final ObjectMapper mapper;

    public RetryHistoryManager(Path reportsDir) throws Exception { this(reportsDir, new ObjectMapper().findAndRegisterModules()); }
    public RetryHistoryManager(Path reportsDir, ObjectMapper mapper) throws Exception {
        if (reportsDir == null) throw new IllegalArgumentException("Reports directory cannot be null");
        if (mapper == null) throw new IllegalArgumentException("ObjectMapper cannot be null");
        this.file = reportsDir.toAbsolutePath().normalize().resolve("retries").resolve("history.json");
        this.mapper = mapper;
        Files.createDirectories(file.getParent());
    }

    public synchronized void record(String runId, String testName, String action, int retryNumber,
                                    FailureIntelligence.Analysis analysis, boolean recovered) throws Exception {
        if (analysis == null) return;
        List<Entry> entries = load();
        entries.add(new Entry(Instant.now().toString(), safe(runId), safe(testName), safe(action), retryNumber,
                analysis.category().name(), analysis.recommendation().name(), safe(analysis.rationale()), recovered));
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
        public String timestamp, runId, testName, action, category, recommendation, rationale;
        public int retryNumber;
        public boolean recovered;
        public Entry() { }
        public Entry(String timestamp, String runId, String testName, String action, int retryNumber,
                     String category, String recommendation, String rationale, boolean recovered) {
            this.timestamp = timestamp; this.runId = runId; this.testName = testName; this.action = action;
            this.retryNumber = retryNumber; this.category = category; this.recommendation = recommendation;
            this.rationale = rationale; this.recovered = recovered;
        }
    }
}
