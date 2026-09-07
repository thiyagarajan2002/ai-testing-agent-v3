package com.thiyagarajan.agent.report;

import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.runtime.ExecutionResult;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class ReportManager {
    private final Path dir;
    private final Path reportRoot;
    private final ExecutionLogWriter logWriter = new ExecutionLogWriter();
    private final PdfReportWriter pdfWriter = new PdfReportWriter();

    public ReportManager() throws Exception {
        this(Path.of(Config.load().reportsDir()));
    }

    public ReportManager(Path directory) throws Exception {
        if (directory == null) throw new IllegalArgumentException("Report directory cannot be null");
        dir = directory.toAbsolutePath().normalize();
        reportRoot = Path.of(Config.load().reportsDir()).toAbsolutePath().normalize();
        Files.createDirectories(dir);
    }

    public void writeAll(ExecutionResult result) throws Exception {
        if (result == null) throw new IllegalArgumentException("Execution result cannot be null");
        writeHtml(result);
        writeCsv(result);
        pdfWriter.write(result, dir.resolve("report.pdf"));
        logWriter.write(result, dir);
    }

    private void writeHtml(ExecutionResult r) throws Exception {
        long passed = r.steps.stream().filter(s -> s.passed).count();
        long failed = r.steps.stream().filter(s -> !s.passed).count();
        long duration = r.steps.stream().mapToLong(s -> s.durationMs).sum();
        String rows = r.steps.stream().map(s -> {
            String artifacts = s.artifacts == null || s.artifacts.isEmpty() ? "" : "<br><b>Artifacts:</b> " + s.artifacts.stream()
                    .map(a -> "<a href=\"" + esc(artifactLink(a)) + "\">" + esc(a) + "</a>").collect(Collectors.joining(", "));
            return "<tr><td>" + esc(s.action) + "</td><td>" + (s.passed ? "PASS" : "FAIL") + "</td><td>" + s.durationMs
                    + "</td><td>" + esc(s.details) + artifacts + "</td></tr>";
        }).collect(Collectors.joining());
        String html = """
        <!doctype html><html><head><meta charset="UTF-8"><title>AI Testing Agent Dashboard</title>
        <style>body{font-family:Arial;margin:30px;background:#f7f7f7;color:#222}.cards{display:flex;gap:12px;flex-wrap:wrap}.card{background:white;border:1px solid #ddd;border-radius:8px;padding:16px;min-width:140px}.value{font-size:24px;font-weight:bold}table{border-collapse:collapse;width:100%%;background:white}th,td{border:1px solid #ccc;padding:8px;text-align:left;vertical-align:top}th{background:#eee}a{word-break:break-all}pre{white-space:pre-wrap;background:white;padding:12px;border:1px solid #ddd}</style>
        </head><body><h1>AI Testing Agent Execution Dashboard</h1>
        <p><b>Test:</b> %s</p><p><b>Status:</b> %s</p>
        <div class="cards"><div class="card">Steps<div class="value">%d</div></div><div class="card">Passed<div class="value">%d</div></div><div class="card">Failed<div class="value">%d</div></div><div class="card">Duration<div class="value">%d ms</div></div></div>
        <h2>Step Results</h2><table><tr><th>Action</th><th>Status</th><th>Duration(ms)</th><th>Details / Input &amp; Output / Artifacts</th></tr>%s</table>
        <h2>Failure Analysis</h2><pre>%s</pre>
        <p><a href="execution.json">execution.json</a> | <a href="execution.log">execution.log</a> | <a href="report.csv">report.csv</a> | <a href="report.pdf">report.pdf</a></p>
        </body></html>
        """.formatted(esc(r.testName), r.passed ? "PASS" : "FAIL", r.steps.size(), passed, failed, duration, rows, esc(r.failureAnalysis));
        Files.writeString(dir.resolve("report.html"), html);
    }

    private void writeCsv(ExecutionResult r) throws Exception {
        try (BufferedWriter w = Files.newBufferedWriter(dir.resolve("report.csv"))) {
            w.write("test,action,status,duration_ms,details,artifacts\n");
            for (var s : r.steps) w.write(csv(r.testName) + "," + csv(s.action) + "," + (s.passed ? "PASS" : "FAIL") + "," + s.durationMs
                    + "," + csv(s.details) + "," + csv(s.artifacts == null ? "" : String.join(" | ", s.artifacts)) + "\n");
        }
    }

    private String artifactLink(String artifact) {
        if (artifact == null || artifact.isBlank()) return "";
        try {
            Path target = Path.of(artifact).toAbsolutePath().normalize();
            if (target.startsWith(dir)) return dir.relativize(target).toString().replace('\\', '/');
            if (target.startsWith(reportRoot)) return dir.relativize(target).toString().replace('\\', '/');
        } catch (Exception ignored) { }
        return artifact.replace('\\', '/');
    }

    private String csv(String s) { return s == null ? "" : "\"" + s.replace("\"", "\"\"").replace("\n", " ") + "\""; }
    private String esc(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
}
