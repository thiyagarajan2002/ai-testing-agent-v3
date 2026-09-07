package com.thiyagarajan.agent.report;

import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.runtime.ExecutionResult;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class ReportManager {
    private final Path dir;
    private final ExecutionLogWriter logWriter = new ExecutionLogWriter();

    public ReportManager() throws Exception { this(Path.of(Config.load().reportsDir())); }
    public ReportManager(Path directory) throws Exception {
        if (directory == null) throw new IllegalArgumentException("Report directory cannot be null");
        dir = directory; Files.createDirectories(dir);
    }

    public void writeAll(ExecutionResult result) throws Exception {
        if (result == null) throw new IllegalArgumentException("Execution result cannot be null");
        writeHtml(result); writeCsv(result); writePdf(result); logWriter.write(result, dir);
    }

    private void writeHtml(ExecutionResult r) throws Exception {
        String rows = r.steps.stream().map(s -> {
            String artifacts = s.artifacts == null || s.artifacts.isEmpty() ? "" : "<br><b>Artifacts:</b> " + s.artifacts.stream()
                    .map(a -> "<a href=\"" + esc(a) + "\">" + esc(a) + "</a>").collect(Collectors.joining(", "));
            return "<tr><td>" + esc(s.action) + "</td><td>" + (s.passed ? "PASS" : "FAIL") + "</td><td>" + s.durationMs
                    + "</td><td>" + esc(s.details) + artifacts + "</td></tr>";
        }).collect(Collectors.joining());
        String html = """
        <!doctype html><html><head><meta charset="UTF-8"><title>AI Testing Agent Report</title>
        <style>body{font-family:Arial;margin:30px;background:#fafafa}table{border-collapse:collapse;width:100%%}th,td{border:1px solid #ccc;padding:8px;text-align:left;vertical-align:top}th{background:#eee}a{word-break:break-all}</style>
        </head><body><h1>AI Testing Agent Report</h1>
        <p><b>Test:</b> %s</p><p><b>Status:</b> %s</p>
        <p><b>Steps:</b> %d &nbsp; <b>Passed:</b> %d &nbsp; <b>Failed:</b> %d</p>
        <table><tr><th>Action</th><th>Status</th><th>Duration(ms)</th><th>Details / Input &amp; Output / Artifacts</th></tr>%s</table>
        <h2>Failure Analysis</h2><pre>%s</pre></body></html>
        """.formatted(esc(r.testName), r.passed ? "PASS" : "FAIL", r.steps.size(), r.steps.stream().filter(s -> s.passed).count(),
                r.steps.stream().filter(s -> !s.passed).count(), rows, esc(r.failureAnalysis));
        Files.writeString(dir.resolve("report.html"), html);
    }

    private void writeCsv(ExecutionResult r) throws Exception {
        try (BufferedWriter w = Files.newBufferedWriter(dir.resolve("report.csv"))) {
            w.write("test,action,status,duration_ms,details,artifacts\n");
            for (var s : r.steps) w.write(csv(r.testName) + "," + csv(s.action) + "," + (s.passed ? "PASS" : "FAIL") + "," + s.durationMs
                    + "," + csv(s.details) + "," + csv(s.artifacts == null ? "" : String.join(" | ", s.artifacts)) + "\n");
        }
    }

    private void writePdf(ExecutionResult r) throws Exception {
        Files.writeString(dir.resolve("report.pdf"), "AI Testing Agent Report\n\nTest: " + r.testName + "\nStatus: " + (r.passed ? "PASS" : "FAIL") + "\n\n"
                + r.steps.stream().map(s -> s.action + " | " + (s.passed ? "PASS" : "FAIL") + " | " + s.durationMs + "ms | " + s.details
                        + (s.artifacts == null || s.artifacts.isEmpty() ? "" : " | artifacts=" + String.join(";", s.artifacts))).collect(Collectors.joining("\n")));
    }
    private String csv(String s) { return s == null ? "" : "\"" + s.replace("\"", "\"\"").replace("\n", " ") + "\""; }
    private String esc(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
}
