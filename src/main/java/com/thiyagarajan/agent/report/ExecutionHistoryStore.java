package com.thiyagarajan.agent.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.runtime.ExecutionResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Persists compact, queryable execution history independently from the report package. */
public final class ExecutionHistoryStore {
    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final ObjectMapper mapper;
    private final Path historyDirectory;

    public ExecutionHistoryStore(Path reportsDirectory) throws IOException {
        if (reportsDirectory == null) throw new IllegalArgumentException("Reports directory cannot be null");
        this.mapper = new ObjectMapper().findAndRegisterModules();
        this.historyDirectory = reportsDirectory.toAbsolutePath().normalize().resolve("history");
        Files.createDirectories(historyDirectory);
    }

    public Path save(ExecutionResult result) throws IOException {
        Objects.requireNonNull(result, "Execution result cannot be null");
        String runId = result.runId == null || result.runId.isBlank() ? "legacy-" + System.currentTimeMillis() : result.runId;
        Path target = historyDirectory.resolve(safe(runId) + ".json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), HistoryEntry.from(result));
        return target;
    }

    public List<HistoryEntry> list() throws IOException {
        try (Stream<Path> files = Files.list(historyDirectory)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .map(this::readUnchecked).toList();
        }
    }

    public HistoryEntry find(String runId) throws IOException {
        if (runId == null || runId.isBlank()) return null;
        Path file = historyDirectory.resolve(safe(runId) + ".json");
        if (!Files.isRegularFile(file)) return null;
        return mapper.readValue(Files.readString(file, StandardCharsets.UTF_8), HistoryEntry.class);
    }

    public Path directory() { return historyDirectory; }

    private HistoryEntry readUnchecked(Path path) {
        try { return mapper.readValue(Files.readString(path, StandardCharsets.UTF_8), HistoryEntry.class); }
        catch (IOException e) { throw new IllegalStateException("Cannot read execution history: " + path, e); }
    }

    private static String safe(String value) {
        String safe = value.replaceAll("[^a-zA-Z0-9._-]+", "_");
        return safe.isBlank() ? "run" : safe.substring(0, Math.min(120, safe.length()));
    }

    public static final class HistoryEntry {
        public String runId;
        public String testId;
        public String testName;
        public boolean passed;
        public int totalSteps;
        public int passedSteps;
        public int failedSteps;
        public long durationMs;
        public String recordedAt;

        public HistoryEntry() { }

        static HistoryEntry from(ExecutionResult result) {
            HistoryEntry entry = new HistoryEntry();
            entry.runId = result.runId;
            entry.testId = result.testId;
            entry.testName = result.testName;
            entry.passed = result.passed;
            entry.totalSteps = result.steps == null ? 0 : result.steps.size();
            entry.passedSteps = result.steps == null ? 0 : (int) result.steps.stream().filter(s -> s != null && s.passed).count();
            entry.failedSteps = entry.totalSteps - entry.passedSteps;
            entry.durationMs = result.steps == null ? 0 : result.steps.stream().filter(Objects::nonNull).mapToLong(s -> Math.max(0, s.durationMs)).sum();
            entry.recordedAt = OffsetDateTime.now().format(TS);
            return entry;
        }
    }
}
