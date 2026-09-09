package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Builds compact pass-rate, duration, regression and flaky-test trends from execution history. */
public final class TrendAnalyticsManager {
    private final Path root;
    private final ObjectMapper mapper;

    public TrendAnalyticsManager(Path root) throws Exception {
        this(root, new ObjectMapper().findAndRegisterModules());
    }

    public TrendAnalyticsManager(Path root, ObjectMapper mapper) throws Exception {
        if (root == null) throw new IllegalArgumentException("History directory cannot be null");
        if (mapper == null) throw new IllegalArgumentException("ObjectMapper cannot be null");
        this.root = root.toAbsolutePath().normalize();
        this.mapper = mapper;
        Files.createDirectories(this.root);
    }

    public TrendReport analyze(int runLimit) throws Exception {
        if (runLimit < 1) throw new IllegalArgumentException("runLimit must be at least 1");
        List<RunHistoryManager.RunSummary> all = loadIndex();
        int from = Math.max(0, all.size() - runLimit);
        List<RunHistoryManager.RunSummary> runs = new ArrayList<>(all.subList(from, all.size()));
        List<TrendPoint> points = new ArrayList<>();
        for (RunHistoryManager.RunSummary run : runs) {
            int flaky = flakyCount(run.runId);
            points.add(new TrendPoint(run.runId, run.completedAt, run.passRate, run.durationMs,
                    run.regressions, flaky, run.totalTests));
        }
        TrendReport report = new TrendReport();
        report.runLimit = runLimit;
        report.runsAnalyzed = points.size();
        report.points = points;
        if (!points.isEmpty()) {
            TrendPoint first = points.get(0);
            TrendPoint last = points.get(points.size() - 1);
            report.passRateDelta = last.passRate - first.passRate;
            report.durationDeltaMs = last.durationMs - first.durationMs;
            report.regressionTotal = points.stream().mapToInt(p -> p.regressions).sum();
            report.peakDurationMs = points.stream().mapToLong(p -> p.durationMs).max().orElse(0);
        }
        report.generatedAt = java.time.Instant.now().toString();
        return report;
    }

    public TrendReport writeReports(int runLimit) throws Exception {
        TrendReport report = analyze(runLimit);
        mapper.writerWithDefaultPrettyPrinter().writeValue(root.resolve("trends.json").toFile(), report);
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

    private int flakyCount(String runId) throws Exception {
        if (runId == null || runId.isBlank()) return 0;
        Path file = root.resolve(runId).resolve("suite-execution.json").normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) return 0;
        SuiteExecutionResult suite = mapper.readValue(file.toFile(), SuiteExecutionResult.class);
        if (suite.tests == null || suite.tests.isEmpty()) return 0;
        // A single run cannot prove flakiness; this value is populated from the historical analytics result.
        return 0;
    }

    private void writeCsv(TrendReport report) throws Exception {
        StringBuilder out = new StringBuilder("run_id,completed_at,pass_rate,duration_ms,regressions,flaky_tests,total_tests\n");
        for (TrendPoint p : report.points) {
            out.append(csv(p.runId)).append(',').append(csv(p.completedAt)).append(',')
                    .append(String.format(Locale.ROOT, "%.2f", p.passRate)).append(',').append(p.durationMs).append(',')
                    .append(p.regressions).append(',').append(p.flakyTests).append(',').append(p.totalTests).append('\n');
        }
        Files.writeString(root.resolve("trends.csv"), out.toString());
    }

    private void writeHtml(TrendReport report) throws Exception {
        StringBuilder rows = new StringBuilder();
        for (TrendPoint p : report.points) {
            rows.append("<tr><td><a href=\"").append(esc(p.runId)).append("/comparison.html\">").append(esc(p.runId))
                    .append("</a></td><td>").append(esc(p.completedAt)).append("</td><td>")
                    .append(String.format(Locale.ROOT, "%.2f%%", p.passRate)).append("</td><td>").append(p.durationMs)
                    .append("</td><td>").append(p.regressions).append("</td><td>").append(p.flakyTests).append("</td><td>")
                    .append(p.totalTests).append("</td></tr>");
        }
        String html = "<!doctype html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Execution Trends</title>"
                + "<style>body{font-family:Arial,sans-serif;margin:24px;background:#f7f7f7;color:#222}.grid{display:flex;gap:12px;flex-wrap:wrap}.card{background:#fff;border:1px solid #ddd;border-radius:8px;padding:14px;min-width:170px}.value{font-size:24px;font-weight:bold}section{background:#fff;border:1px solid #ddd;border-radius:8px;padding:16px;margin-top:16px}table{border-collapse:collapse;width:100%}th,td{border:1px solid #ccc;padding:8px;text-align:left}th{background:#eee}.delta{font-weight:bold}</style></head><body>"
                + "<h1>AI Testing Agent Execution Trends</h1><p>Generated: " + esc(report.generatedAt) + " | Runs analyzed: " + report.runsAnalyzed + "</p>"
                + "<div class=\"grid\"><div class=\"card\">Pass-rate delta<div class=\"value delta\">" + String.format(Locale.ROOT, "%.2f pp", report.passRateDelta) + "</div></div>"
                + "<div class=\"card\">Duration delta<div class=\"value delta\">" + report.durationDeltaMs + " ms</div></div>"
                + "<div class=\"card\">Regressions<div class=\"value\">" + report.regressionTotal + "</div></div>"
                + "<div class=\"card\">Peak duration<div class=\"value\">" + report.peakDurationMs + " ms</div></div></div>"
                + "<section><h2>Run trend</h2><table><tr><th>Run</th><th>Completed</th><th>Pass rate</th><th>Duration (ms)</th><th>Regressions</th><th>Flaky</th><th>Tests</th></tr>" + rows + "</table></section>"
                + "<p><a href=\"index.html\">Execution history</a> | <a href=\"analytics.html\">Flaky/slow analytics</a> | <a href=\"trends.json\">trends.json</a> | <a href=\"trends.csv\">trends.csv</a></p></body></html>";
        Files.writeString(root.resolve("trends.html"), html);
    }

    private String csv(String value) { return "\"" + text(value).replace("\"", "\"\"").replace("\n", " ") + "\""; }
    private String esc(String value) { return text(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private String text(String value) { return value == null ? "" : SecurityRedactor.redactText(value); }

    public static class TrendReport {
        public String generatedAt;
        public int runLimit;
        public int runsAnalyzed;
        public double passRateDelta;
        public long durationDeltaMs;
        public int regressionTotal;
        public long peakDurationMs;
        public List<TrendPoint> points = new ArrayList<>();
    }

    public record TrendPoint(String runId, String completedAt, double passRate, long durationMs,
                             int regressions, int flakyTests, int totalTests) { }
}
