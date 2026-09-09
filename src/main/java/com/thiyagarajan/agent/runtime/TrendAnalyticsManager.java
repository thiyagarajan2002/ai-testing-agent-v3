package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds pass-rate, duration, regression and cumulative flaky-test trends from execution history. */
public final class TrendAnalyticsManager {
    private static final double FLAKY_THRESHOLD = 0.50d;
    private final Path root;
    private final ObjectMapper mapper;

    public TrendAnalyticsManager(Path root) throws Exception { this(root, new ObjectMapper().findAndRegisterModules()); }
    public TrendAnalyticsManager(Path root, ObjectMapper mapper) throws Exception {
        if (root == null) throw new IllegalArgumentException("History directory cannot be null");
        if (mapper == null) throw new IllegalArgumentException("ObjectMapper cannot be null");
        this.root = root.toAbsolutePath().normalize(); this.mapper = mapper; Files.createDirectories(this.root);
    }

    public TrendReport analyze(int runLimit) throws Exception {
        if (runLimit < 1) throw new IllegalArgumentException("runLimit must be at least 1");
        List<RunHistoryManager.RunSummary> all = loadIndex();
        int from = Math.max(0, all.size() - runLimit);
        List<RunHistoryManager.RunSummary> runs = new ArrayList<>(all.subList(from, all.size()));
        Map<String, StatusHistory> histories = new LinkedHashMap<>();
        List<TrendPoint> points = new ArrayList<>();
        for (RunHistoryManager.RunSummary run : runs) {
            SuiteExecutionResult suite = readSuite(run.runId);
            if (suite != null && suite.tests != null) for (SuiteExecutionResult.TestExecution test : suite.tests) {
                String key = key(test); StatusHistory h = histories.computeIfAbsent(key, k -> new StatusHistory());
                h.executions++; boolean pass = "PASS".equalsIgnoreCase(test.status);
                if (pass) h.passed++; else h.failed++;
                if (h.previous != null && h.previous != pass) h.changes++; h.previous = pass;
            }
            int flaky = (int) histories.values().stream().filter(h -> h.executions >= 2 && h.passed > 0 && h.failed > 0
                    && h.changes / (double) (h.executions - 1) >= FLAKY_THRESHOLD).count();
            points.add(new TrendPoint(run.runId, run.completedAt, run.passRate, run.durationMs, run.regressions, flaky, run.totalTests));
        }
        TrendReport report = new TrendReport(); report.generatedAt = java.time.Instant.now().toString(); report.runLimit = runLimit;
        report.runsAnalyzed = points.size(); report.points = points;
        if (!points.isEmpty()) { TrendPoint first = points.get(0), last = points.get(points.size() - 1);
            report.passRateDelta = last.passRate - first.passRate; report.durationDeltaMs = last.durationMs - first.durationMs;
            report.regressionTotal = points.stream().mapToInt(p -> p.regressions).sum(); report.peakDurationMs = points.stream().mapToLong(p -> p.durationMs).max().orElse(0); }
        return report;
    }

    public TrendReport writeReports(int runLimit) throws Exception {
        TrendReport report = analyze(runLimit);
        mapper.writerWithDefaultPrettyPrinter().writeValue(root.resolve("trends.json").toFile(), report);
        writeCsv(report); writeHtml(report); return report;
    }
    private List<RunHistoryManager.RunSummary> loadIndex() throws Exception {
        Path file = root.resolve("index.json"); if (!Files.isRegularFile(file)) return new ArrayList<>();
        List<RunHistoryManager.RunSummary> values = mapper.readValue(file.toFile(), new TypeReference<List<RunHistoryManager.RunSummary>>() {});
        return values == null ? new ArrayList<>() : values;
    }
    private SuiteExecutionResult readSuite(String runId) throws Exception {
        if (runId == null || runId.isBlank()) return null; Path file = root.resolve(runId).resolve("suite-execution.json").normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) return null; return mapper.readValue(file.toFile(), SuiteExecutionResult.class);
    }
    private String key(SuiteExecutionResult.TestExecution t) { return (t == null || t.planFile == null ? "" : t.planFile.trim().replace('\\', '/')) + "::" + (t == null || t.testName == null ? "" : t.testName.trim()); }
    private void writeCsv(TrendReport r) throws Exception {
        StringBuilder out = new StringBuilder("run_id,completed_at,pass_rate,duration_ms,regressions,flaky_tests,total_tests\n");
        for (TrendPoint p : r.points) out.append(csv(p.runId)).append(',').append(csv(p.completedAt)).append(',').append(String.format(Locale.ROOT, "%.2f", p.passRate)).append(',').append(p.durationMs).append(',').append(p.regressions).append(',').append(p.flakyTests).append(',').append(p.totalTests).append('\n');
        Files.writeString(root.resolve("trends.csv"), out.toString());
    }
    private void writeHtml(TrendReport r) throws Exception {
        StringBuilder rows = new StringBuilder();
        for (TrendPoint p : r.points) rows.append("<tr><td><a href=\"").append(esc(p.runId)).append("/comparison.html\">").append(esc(p.runId)).append("</a></td><td>").append(esc(p.completedAt)).append("</td><td>").append(String.format(Locale.ROOT, "%.2f%%", p.passRate)).append("</td><td>").append(p.durationMs).append("</td><td>").append(p.regressions).append("</td><td>").append(p.flakyTests).append("</td><td>").append(p.totalTests).append("</td></tr>");
        String html = "<!doctype html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Execution Trends</title><style>body{font-family:Arial,sans-serif;margin:24px;background:#f7f7f7;color:#222}.grid{display:flex;gap:12px;flex-wrap:wrap}.card{background:#fff;border:1px solid #ddd;border-radius:8px;padding:14px;min-width:170px}.value{font-size:24px;font-weight:bold}section{background:#fff;border:1px solid #ddd;border-radius:8px;padding:16px;margin-top:16px}table{border-collapse:collapse;width:100%}th,td{border:1px solid #ccc;padding:8px;text-align:left}th{background:#eee}</style></head><body><h1>AI Testing Agent Execution Trends</h1><p>Generated: " + esc(r.generatedAt) + " | Runs analyzed: " + r.runsAnalyzed + "</p><div class=\"grid\"><div class=\"card\">Pass-rate delta<div class=\"value\">" + String.format(Locale.ROOT, "%.2f pp", r.passRateDelta) + "</div></div><div class=\"card\">Duration delta<div class=\"value\">" + r.durationDeltaMs + " ms</div></div><div class=\"card\">Regressions<div class=\"value\">" + r.regressionTotal + "</div></div><div class=\"card\">Peak duration<div class=\"value\">" + r.peakDurationMs + " ms</div></div></div><section><h2>Run trend</h2><table><tr><th>Run</th><th>Completed</th><th>Pass rate</th><th>Duration (ms)</th><th>Regressions</th><th>Flaky</th><th>Tests</th></tr>" + rows + "</table></section><p><a href=\"index.html\">Execution history</a> | <a href=\"analytics.html\">Flaky/slow analytics</a> | <a href=\"trends.json\">trends.json</a> | <a href=\"trends.csv\">trends.csv</a></p></body></html>";
        Files.writeString(root.resolve("trends.html"), html);
    }
    private String csv(String v) { return "\"" + text(v).replace("\"", "\"\"").replace("\n", " ") + "\""; }
    private String esc(String v) { return text(v).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private String text(String v) { return v == null ? "" : SecurityRedactor.redactText(v); }
    private static final class StatusHistory { int executions; int passed; int failed; int changes; Boolean previous; }
    public static class TrendReport { public String generatedAt; public int runLimit; public int runsAnalyzed; public double passRateDelta; public long durationDeltaMs; public int regressionTotal; public long peakDurationMs; public List<TrendPoint> points = new ArrayList<>(); }
    public record TrendPoint(String runId, String completedAt, double passRate, long durationMs, int regressions, int flakyTests, int totalTests) { }
}
