package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds historical execution analytics and identifies tests that alternate between pass and fail. */
public final class HistoryAnalyticsManager {
    public static final int DEFAULT_RUN_LIMIT = 20;
    public static final double DEFAULT_FLAKY_THRESHOLD = 0.50d;

    private final Path root;
    private final ObjectMapper mapper;

    public HistoryAnalyticsManager(Path root) throws Exception {
        this(root, new ObjectMapper().findAndRegisterModules());
    }

    public HistoryAnalyticsManager(Path root, ObjectMapper mapper) throws Exception {
        if (root == null) throw new IllegalArgumentException("History directory cannot be null");
        if (mapper == null) throw new IllegalArgumentException("ObjectMapper cannot be null");
        this.root = root.toAbsolutePath().normalize();
        this.mapper = mapper;
        Files.createDirectories(this.root);
    }

    public AnalyticsReport analyze() throws Exception {
        return analyze(DEFAULT_RUN_LIMIT, DEFAULT_FLAKY_THRESHOLD);
    }

    public AnalyticsReport analyze(int runLimit, double flakyThreshold) throws Exception {
        if (runLimit < 1) throw new IllegalArgumentException("runLimit must be at least 1");
        if (flakyThreshold < 0 || flakyThreshold > 1) throw new IllegalArgumentException("flakyThreshold must be between 0 and 1");

        List<RunHistoryManager.RunSummary> allRuns = loadIndex();
        int from = Math.max(0, allRuns.size() - runLimit);
        List<RunHistoryManager.RunSummary> runs = new ArrayList<>(allRuns.subList(from, allRuns.size()));
        Map<String, TestStats> stats = new LinkedHashMap<>();
        for (RunHistoryManager.RunSummary run : runs) {
            SuiteExecutionResult suite = readSuite(run.runId);
            if (suite == null || suite.tests == null) continue;
            for (SuiteExecutionResult.TestExecution test : suite.tests) {
                String key = key(test);
                TestStats stat = stats.computeIfAbsent(key, k -> new TestStats(k, test.planFile, test.testName));
                stat.executions++;
                if (isPass(test)) stat.passed++;
                else stat.failed++;
                if (stat.lastStatus != null && !stat.lastStatus.equalsIgnoreCase(test.status)) stat.statusChanges++;
                stat.lastStatus = test.status;
                stat.totalDurationMs += Math.max(0, test.durationMs);
            }
        }

        List<TestStats> flaky = stats.values().stream()
                .filter(s -> s.executions >= 2 && s.passed > 0 && s.failed > 0 && s.flakinessRate() >= flakyThreshold)
                .sorted(Comparator.comparingDouble(TestStats::flakinessRate).reversed().thenComparing(s -> s.key))
                .toList();
        List<TestStats> slowest = stats.values().stream()
                .filter(s -> s.executions > 0)
                .sorted(Comparator.comparingDouble(TestStats::averageDurationMs).reversed().thenComparing(s -> s.key))
                .limit(10)
                .toList();

        AnalyticsReport report = new AnalyticsReport();
        report.runLimit = runLimit;
        report.flakyThreshold = flakyThreshold;
        report.runsAnalyzed = runs.size();
        report.totalTestsObserved = stats.size();
        report.flakyTests = flaky;
        report.slowestTests = slowest;
        report.runs = runs;
        report.generatedAt = java.time.Instant.now().toString();
        return report;
    }

    public AnalyticsReport writeReports() throws Exception {
        return writeReports(DEFAULT_RUN_LIMIT, DEFAULT_FLAKY_THRESHOLD);
    }

    public AnalyticsReport writeReports(int runLimit, double flakyThreshold) throws Exception {
        AnalyticsReport report = analyze(runLimit, flakyThreshold);
        mapper.writerWithDefaultPrettyPrinter().writeValue(root.resolve("analytics.json").toFile(), report);
        writeCsv(report);
        writeHtml(report);
        return report;
    }

    private List<RunHistoryManager.RunSummary> loadIndex() throws Exception {
        Path file = root.resolve("index.json");
        if (!Files.isRegularFile(file)) return new ArrayList<>();
        List<RunHistoryManager.RunSummary> values = mapper.readValue(file.toFile(), new TypeReference<List<RunHistoryManager.RunSummary>>() {});
        return values == null ? new ArrayList<>() : values;
    }

    private SuiteExecutionResult readSuite(String runId) throws Exception {
        if (runId == null || runId.isBlank()) return null;
        Path file = root.resolve(runId).resolve("suite-execution.json").normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) return null;
        return mapper.readValue(file.toFile(), SuiteExecutionResult.class);
    }

    private String key(SuiteExecutionResult.TestExecution test) {
        String plan = test == null || test.planFile == null ? "" : test.planFile.trim().replace('\\', '/');
        String name = test == null || test.testName == null ? "" : test.testName.trim();
        return plan + "::" + name;
    }

    private boolean isPass(SuiteExecutionResult.TestExecution test) {
        return test != null && "PASS".equalsIgnoreCase(test.status);
    }

    private void writeCsv(AnalyticsReport report) throws Exception {
        StringBuilder out = new StringBuilder("category,plan,test,executions,passed,failed,status_changes,flakiness_rate,average_duration_ms\n");
        for (TestStats stat : report.flakyTests) appendCsv(out, "FLAKY", stat);
        for (TestStats stat : report.slowestTests) appendCsv(out, "SLOWEST", stat);
        Files.writeString(root.resolve("analytics.csv"), out.toString());
    }

    private void appendCsv(StringBuilder out, String category, TestStats s) {
        out.append(csv(category)).append(',').append(csv(s.planFile)).append(',').append(csv(s.testName)).append(',')
                .append(s.executions).append(',').append(s.passed).append(',').append(s.failed).append(',').append(s.statusChanges).append(',')
                .append(String.format(Locale.ROOT, "%.2f", s.flakinessRate() * 100)).append(',').append(String.format(Locale.ROOT, "%.2f", s.averageDurationMs())).append('\n');
    }

    private void writeHtml(AnalyticsReport report) throws Exception {
        StringBuilder flakyRows = new StringBuilder();
        for (TestStats s : report.flakyTests) flakyRows.append(row(s, "FLAKY"));
        StringBuilder slowRows = new StringBuilder();
        for (TestStats s : report.slowestTests) slowRows.append(row(s, "SLOWEST"));
        String html = "<!doctype html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Execution Analytics</title>"
                + "<style>body{font-family:Arial,sans-serif;margin:24px;background:#f7f7f7;color:#222}.grid{display:flex;gap:12px;flex-wrap:wrap}.card{background:#fff;border:1px solid #ddd;border-radius:8px;padding:14px;min-width:160px}.value{font-size:24px;font-weight:bold}section{background:#fff;border:1px solid #ddd;border-radius:8px;padding:16px;margin-top:16px}table{border-collapse:collapse;width:100%;overflow:hidden}th,td{border:1px solid #ccc;padding:8px;text-align:left}th{background:#eee}.muted{color:#666}</style></head><body>"
                + "<h1>AI Testing Agent Historical Analytics</h1><p class=\"muted\">Generated: " + esc(report.generatedAt) + " | Runs analyzed: " + report.runsAnalyzed + "</p>"
                + "<div class=\"grid\"><div class=\"card\">Runs<div class=\"value\">" + report.runsAnalyzed + "</div></div>"
                + "<div class=\"card\">Tests observed<div class=\"value\">" + report.totalTestsObserved + "</div></div>"
                + "<div class=\"card\">Flaky tests<div class=\"value\">" + report.flakyTests.size() + "</div></div></div>"
                + "<section><h2>Flaky Tests</h2>" + table(flakyRows) + "</section>"
                + "<section><h2>Slowest Tests</h2>" + table(slowRows) + "</section>"
                + "<p><a href=\"index.html\">Execution history</a> | <a href=\"analytics.json\">analytics.json</a> | <a href=\"analytics.csv\">analytics.csv</a></p></body></html>";
        Files.writeString(root.resolve("analytics.html"), html);
    }

    private String table(StringBuilder rows) {
        return "<table><tr><th>Category</th><th>Plan</th><th>Test</th><th>Runs</th><th>Pass</th><th>Fail</th><th>Changes</th><th>Flakiness</th><th>Avg duration</th></tr>" + rows + "</table>";
    }

    private String row(TestStats s, String category) {
        return "<tr><td>" + esc(category) + "</td><td>" + esc(s.planFile) + "</td><td>" + esc(s.testName) + "</td><td>" + s.executions
                + "</td><td>" + s.passed + "</td><td>" + s.failed + "</td><td>" + s.statusChanges + "</td><td>"
                + String.format(Locale.ROOT, "%.2f%%", s.flakinessRate() * 100) + "</td><td>" + String.format(Locale.ROOT, "%.2f ms", s.averageDurationMs()) + "</td></tr>";
    }

    private String csv(String value) { return "\"" + text(value).replace("\"", "\"\"").replace("\n", " ") + "\""; }
    private String esc(String value) { return text(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private String text(String value) { return value == null ? "" : SecurityRedactor.redactText(value); }

    public static class AnalyticsReport {
        public String generatedAt;
        public int runLimit;
        public double flakyThreshold;
        public int runsAnalyzed;
        public int totalTestsObserved;
        public List<RunHistoryManager.RunSummary> runs = new ArrayList<>();
        public List<TestStats> flakyTests = new ArrayList<>();
        public List<TestStats> slowestTests = new ArrayList<>();
    }

    public static class TestStats {
        public String key;
        public String planFile;
        public String testName;
        public int executions;
        public int passed;
        public int failed;
        public int statusChanges;
        public String lastStatus;
        public long totalDurationMs;

        public TestStats() { }
        TestStats(String key, String planFile, String testName) { this.key = key; this.planFile = planFile; this.testName = testName; }
        public double flakinessRate() { return executions <= 1 ? 0d : statusChanges / (double) (executions - 1); }
        public double averageDurationMs() { return executions == 0 ? 0d : totalDurationMs / (double) executions; }
    }
}
