package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Persists suite execution history and compares each run with the previous recorded run. */
public final class RunHistoryManager {
    private static final DateTimeFormatter RUN_ID = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS", Locale.ROOT).withZone(ZoneOffset.UTC);
    private final Path root;
    private final ObjectMapper mapper;

    public RunHistoryManager(Path root) throws Exception {
        this(root, new ObjectMapper().findAndRegisterModules());
    }

    public RunHistoryManager(Path root, ObjectMapper mapper) throws Exception {
        if (root == null) throw new IllegalArgumentException("History directory cannot be null");
        if (mapper == null) throw new IllegalArgumentException("ObjectMapper cannot be null");
        this.root = root.toAbsolutePath().normalize();
        this.mapper = mapper;
        Files.createDirectories(this.root);
    }

    public RecordedRun recordSuite(SuiteExecutionResult suite) throws Exception {
        if (suite == null) throw new IllegalArgumentException("Suite result cannot be null");
        List<RunSummary> index = loadIndex();
        RunSummary previousSummary = index.isEmpty() ? null : index.get(index.size() - 1);
        SuiteExecutionResult previous = previousSummary == null ? null : readSuite(previousSummary.runId);

        String runId = uniqueRunId();
        Path runDir = root.resolve(runId);
        Files.createDirectories(runDir);
        mapper.writerWithDefaultPrettyPrinter().writeValue(runDir.resolve("suite-execution.json").toFile(), suite);

        RunComparisonResult comparison = compare(previousSummary == null ? null : previousSummary.runId, previous, runId, suite);
        mapper.writerWithDefaultPrettyPrinter().writeValue(runDir.resolve("comparison.json").toFile(), comparison);
        writeComparisonCsv(runDir.resolve("comparison.csv"), comparison);
        writeComparisonHtml(runDir.resolve("comparison.html"), comparison);

        RunSummary current = new RunSummary();
        current.runId = runId;
        current.suiteName = suite.suiteName;
        current.status = suite.status;
        current.completedAt = suite.completedAt;
        current.totalTests = suite.totalTests;
        current.passedTests = suite.passedTests;
        current.failedTests = suite.failedTests;
        current.passRate = rate(suite.passedTests, suite.totalTests);
        current.durationMs = suite.durationMs;
        current.regressions = comparison.regressions;
        current.fixed = comparison.fixed;
        index.add(current);
        mapper.writerWithDefaultPrettyPrinter().writeValue(root.resolve("index.json").toFile(), index);
        writeIndexHtml(index);
        return new RecordedRun(runId, runDir, comparison);
    }

    public RunComparisonResult compare(String previousRunId, SuiteExecutionResult previous,
                                       String currentRunId, SuiteExecutionResult current) {
        if (current == null) throw new IllegalArgumentException("Current suite result cannot be null");
        RunComparisonResult out = new RunComparisonResult();
        out.previousRunId = previousRunId;
        out.currentRunId = currentRunId;
        out.currentTotalTests = current.totalTests;
        out.currentPassedTests = current.passedTests;
        out.currentFailedTests = current.failedTests;
        out.currentPassRate = rate(current.passedTests, current.totalTests);
        out.currentDurationMs = current.durationMs;
        if (previous == null) {
            for (var test : current.tests) {
                out.changes.add(change(null, test, "NEW_TEST"));
                out.newTests++;
            }
            return out;
        }

        out.previousTotalTests = previous.totalTests;
        out.previousPassedTests = previous.passedTests;
        out.previousFailedTests = previous.failedTests;
        out.previousPassRate = rate(previous.passedTests, previous.totalTests);
        out.previousDurationMs = previous.durationMs;
        out.passRateDelta = out.currentPassRate - out.previousPassRate;
        out.durationDeltaMs = out.currentDurationMs - out.previousDurationMs;

        Map<String, SuiteExecutionResult.TestExecution> oldTests = mapByKey(previous.tests);
        Map<String, SuiteExecutionResult.TestExecution> newTests = mapByKey(current.tests);
        for (var entry : newTests.entrySet()) {
            var now = entry.getValue();
            var old = oldTests.remove(entry.getKey());
            String category;
            if (old == null) { category = "NEW_TEST"; out.newTests++; }
            else if (isPass(old) && !isPass(now)) { category = "REGRESSION"; out.regressions++; }
            else if (!isPass(old) && isPass(now)) { category = "FIXED"; out.fixed++; }
            else if (isPass(now)) { category = "PASSED_UNCHANGED"; out.unchangedPassed++; }
            else { category = "FAILED_UNCHANGED"; out.unchangedFailed++; }
            out.changes.add(change(old, now, category));
        }
        for (var removed : oldTests.values()) {
            out.removedTests++;
            out.changes.add(new RunComparisonResult.TestChange(key(removed), removed.planFile, removed.testName,
                    removed.status, null, "REMOVED_TEST", removed.durationMs, 0));
        }
        out.changes.sort(Comparator.comparing(c -> c.key == null ? "" : c.key));
        return out;
    }

    private List<RunSummary> loadIndex() throws Exception {
        Path file = root.resolve("index.json");
        if (!Files.isRegularFile(file)) return new ArrayList<>();
        List<RunSummary> values = mapper.readValue(file.toFile(), new TypeReference<List<RunSummary>>() {});
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private SuiteExecutionResult readSuite(String runId) throws Exception {
        Path file = root.resolve(runId).resolve("suite-execution.json").normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) return null;
        return mapper.readValue(file.toFile(), SuiteExecutionResult.class);
    }

    private Map<String, SuiteExecutionResult.TestExecution> mapByKey(List<SuiteExecutionResult.TestExecution> tests) {
        Map<String, SuiteExecutionResult.TestExecution> out = new LinkedHashMap<>();
        if (tests == null) return out;
        for (var test : tests) {
            String key = key(test);
            if (out.putIfAbsent(key, test) != null) throw new IllegalArgumentException("Duplicate test identity in suite history: " + key);
        }
        return out;
    }

    private String key(SuiteExecutionResult.TestExecution test) {
        String plan = test == null || test.planFile == null ? "" : test.planFile.trim().replace('\\', '/');
        String name = test == null || test.testName == null ? "" : test.testName.trim();
        return plan + "::" + name;
    }

    private RunComparisonResult.TestChange change(SuiteExecutionResult.TestExecution old, SuiteExecutionResult.TestExecution now, String category) {
        return new RunComparisonResult.TestChange(key(now), now.planFile, now.testName, old == null ? null : old.status,
                now.status, category, old == null ? 0 : old.durationMs, now.durationMs);
    }

    private boolean isPass(SuiteExecutionResult.TestExecution test) { return test != null && "PASS".equalsIgnoreCase(test.status); }
    private double rate(int passed, int total) { return total <= 0 ? 0d : Math.round((passed * 10000d) / total) / 100d; }

    private String uniqueRunId() {
        String id = RUN_ID.format(Instant.now());
        if (!Files.exists(root.resolve(id))) return id;
        return id + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void writeComparisonCsv(Path file, RunComparisonResult comparison) throws Exception {
        StringBuilder out = new StringBuilder("category,plan,test,previous_status,current_status,previous_duration_ms,current_duration_ms,duration_delta_ms\n");
        for (var change : comparison.changes) {
            out.append(csv(change.category)).append(',').append(csv(change.planFile)).append(',').append(csv(change.testName)).append(',')
                    .append(csv(change.previousStatus)).append(',').append(csv(change.currentStatus)).append(',')
                    .append(change.previousDurationMs).append(',').append(change.currentDurationMs).append(',').append(change.durationDeltaMs).append('\n');
        }
        Files.writeString(file, out.toString());
    }

    private void writeComparisonHtml(Path file, RunComparisonResult c) throws Exception {
        StringBuilder rows = new StringBuilder();
        for (var change : c.changes) rows.append("<tr><td>").append(esc(change.category)).append("</td><td>").append(esc(change.planFile))
                .append("</td><td>").append(esc(change.testName)).append("</td><td>").append(esc(change.previousStatus)).append("</td><td>")
                .append(esc(change.currentStatus)).append("</td><td>").append(change.durationDeltaMs).append("</td></tr>");
        String html = "<!doctype html><html><head><meta charset=\"UTF-8\"><title>Run Comparison</title>"
                + "<style>body{font-family:Arial;margin:30px}table{border-collapse:collapse;width:100%}th,td{border:1px solid #ccc;padding:8px}th{background:#eee}</style></head><body>"
                + "<h1>Run Comparison</h1><p>Previous: " + esc(c.previousRunId) + " | Current: " + esc(c.currentRunId) + "</p>"
                + "<p>Pass rate: " + c.previousPassRate + "% → " + c.currentPassRate + "% (delta " + c.passRateDelta + ")</p>"
                + "<p>Regressions: " + c.regressions + " | Fixed: " + c.fixed + " | New: " + c.newTests + " | Removed: " + c.removedTests + "</p>"
                + "<table><tr><th>Category</th><th>Plan</th><th>Test</th><th>Previous</th><th>Current</th><th>Duration Δ(ms)</th></tr>" + rows + "</table>"
                + "<p><a href=\"comparison.json\">comparison.json</a> | <a href=\"comparison.csv\">comparison.csv</a> | <a href=\"suite-execution.json\">suite-execution.json</a></p></body></html>";
        Files.writeString(file, html);
    }

    private void writeIndexHtml(List<RunSummary> index) throws Exception {
        StringBuilder rows = new StringBuilder();
        for (int i = index.size() - 1; i >= 0; i--) {
            RunSummary r = index.get(i);
            rows.append("<tr><td><a href=\"").append(esc(r.runId)).append("/comparison.html\">").append(esc(r.runId)).append("</a></td><td>")
                    .append(esc(r.suiteName)).append("</td><td>").append(esc(r.status)).append("</td><td>").append(r.passRate)
                    .append("%</td><td>").append(r.durationMs).append("</td><td>").append(r.regressions).append("</td><td>").append(r.fixed).append("</td></tr>");
        }
        String html = "<!doctype html><html><head><meta charset=\"UTF-8\"><title>Execution History</title>"
                + "<style>body{font-family:Arial;margin:30px}table{border-collapse:collapse;width:100%}th,td{border:1px solid #ccc;padding:8px}th{background:#eee}</style></head><body>"
                + "<h1>AI Testing Agent Execution History</h1><table><tr><th>Run</th><th>Suite</th><th>Status</th><th>Pass rate</th><th>Duration(ms)</th><th>Regressions</th><th>Fixed</th></tr>"
                + rows + "</table></body></html>";
        Files.writeString(root.resolve("index.html"), html);
    }

    private String csv(String value) { return "\"" + text(value).replace("\"", "\"\"").replace("\n", " ") + "\""; }
    private String esc(String value) { return text(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private String text(String value) { return value == null ? "" : SecurityRedactor.redactText(value); }

    public record RecordedRun(String runId, Path directory, RunComparisonResult comparison) { }

    public static class RunSummary {
        public String runId;
        public String suiteName;
        public String status;
        public String completedAt;
        public int totalTests;
        public int passedTests;
        public int failedTests;
        public double passRate;
        public long durationMs;
        public int regressions;
        public int fixed;
    }
}
