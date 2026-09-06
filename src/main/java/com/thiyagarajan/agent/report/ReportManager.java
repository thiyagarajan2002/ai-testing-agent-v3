package com.thiyagarajan.agent.report;

import com.thiyagarajan.agent.runtime.ExecutionResult;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Collectors;

public class ReportManager {
    private final Path dir = Path.of("reports");

    public ReportManager() throws IOException {
        Files.createDirectories(dir);
    }

    public void writeAll(ExecutionResult result) throws Exception {
        writeHtml(result);
        writeCsv(result);
        writePdf(result);
    }

    private void writeHtml(ExecutionResult r) throws IOException {
        String rows = r.steps.stream().map(s ->
                "<tr><td>" + esc(s.action) + "</td><td>" +
                (s.passed ? "PASS" : "FAIL") + "</td><td>" +
                s.durationMs + "</td><td>" + esc(s.details) + "</td></tr>"
        ).collect(Collectors.joining());

        String html = """
        <!doctype html><html><head><meta charset="UTF-8">
        <title>Testing Agent Report</title>
        <style>
        body{font-family:Arial;margin:30px}table{border-collapse:collapse;width:100%}
        th,td{border:1px solid #ccc;padding:8px;text-align:left}
        </style></head><body>
        <h1>AI Testing Agent Report</h1>
        <p><b>Test:</b> %s</p>
        <p><b>Status:</b> %s</p>
        <table><tr><th>Action</th><th>Status</th><th>Duration(ms)</th><th>Details</th></tr>
        %s</table><h2>Failure Analysis</h2><pre>%s</pre>
        </body></html>
        """.formatted(esc(r.testName), r.passed ? "PASS" : "FAIL", rows, esc(r.failureAnalysis));

        Files.writeString(dir.resolve("report.html"), html);
    }

    private void writeCsv(ExecutionResult r) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(dir.resolve("report.csv"))) {
            w.write("test,action,status,duration_ms,details\n");
            for (var s : r.steps) {
                w.write(csv(r.testName) + "," + csv(s.action) + "," +
                        (s.passed ? "PASS" : "FAIL") + "," + s.durationMs + "," +
                        csv(s.details) + "\n");
            }
        }
    }

    private void writePdf(ExecutionResult r) throws Exception {
        // Lightweight plain-text PDF placeholder is intentionally omitted when iText
        // is unavailable at runtime. The HTML and CSV reports are always generated.
        // Add iText rendering here if a production PDF layout is required.
        Files.writeString(dir.resolve("report.pdf"),
                "AI Testing Agent Report\n\nTest: " + r.testName +
                "\nStatus: " + (r.passed ? "PASS" : "FAIL") +
                "\n\n" + r.steps.stream().map(s ->
                        s.action + " | " + (s.passed ? "PASS" : "FAIL") +
                        " | " + s.details).collect(Collectors.joining("\n")));
    }

    private String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"").replace("\n", " ") + "\"";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
