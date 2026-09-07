package com.thiyagarajan.agent.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.thiyagarajan.agent.runtime.ExecutionResult;
import com.thiyagarajan.agent.runtime.SuiteExecutionResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/** Writes aggregated suite reports and one report directory per test. */
public final class SuiteReportManager {
    private final Path root;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public SuiteReportManager(Path root) throws Exception {
        if (root == null) throw new IllegalArgumentException("Suite report directory cannot be null");
        this.root = root.toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    public void writeAll(SuiteExecutionResult suite) throws Exception {
        if (suite == null) throw new IllegalArgumentException("Suite result cannot be null");
        writeJson(suite);
        writeCsv(suite);
        writeHtml(suite);
        writePdf(suite);
        writeIndividualReports(suite);
    }

    private void writeJson(SuiteExecutionResult suite) throws Exception {
        mapper.writerWithDefaultPrettyPrinter().writeValue(root.resolve("suite-execution.json").toFile(), suite);
    }

    private void writeCsv(SuiteExecutionResult suite) throws Exception {
        StringBuilder out = new StringBuilder("index,plan,test,status,duration_ms,steps,passed_steps,failed_steps\n");
        for (var test : suite.tests) {
            ExecutionResult r = test.result;
            long passed = r.steps.stream().filter(s -> s.passed).count();
            long failed = r.steps.stream().filter(s -> !s.passed).count();
            out.append(test.index + 1).append(',').append(csv(test.planFile)).append(',').append(csv(test.testName)).append(',')
                    .append(test.status).append(',').append(test.durationMs).append(',').append(r.steps.size()).append(',')
                    .append(passed).append(',').append(failed).append('\n');
        }
        Files.writeString(root.resolve("suite-report.csv"), out.toString());
    }

    private void writeHtml(SuiteExecutionResult suite) throws Exception {
        String rows = suite.tests.stream().map(t -> "<tr><td>" + (t.index + 1) + "</td><td>" + esc(t.planFile)
                + "</td><td>" + esc(t.testName) + "</td><td>" + t.status + "</td><td>" + t.durationMs
                + "</td><td><a href=\"tests/" + (t.index + 1) + "/report.html\">Open report</a></td></tr>").collect(Collectors.joining());
        String html = """
                <!doctype html><html><head><meta charset="UTF-8"><title>Suite Report</title>
                <style>body{font-family:Arial;margin:30px;background:#f7f7f7;color:#222}.cards{display:flex;gap:12px;flex-wrap:wrap}.card{background:white;border:1px solid #ddd;border-radius:8px;padding:16px;min-width:140px}.value{font-size:24px;font-weight:bold}table{border-collapse:collapse;width:100%%;background:white}th,td{border:1px solid #ccc;padding:8px;text-align:left}th{background:#eee}a{word-break:break-all}</style>
                </head><body><h1>AI Testing Agent Suite Report</h1><p><b>Suite:</b> %s</p><p><b>Status:</b> %s</p>
                <div class="cards"><div class="card">Tests<div class="value">%d</div></div><div class="card">Passed<div class="value">%d</div></div><div class="card">Failed<div class="value">%d</div></div><div class="card">Duration<div class="value">%d ms</div></div></div>
                <h2>Tests</h2><table><tr><th>#</th><th>Plan</th><th>Test</th><th>Status</th><th>Duration(ms)</th><th>Report</th></tr>%s</table>
                <p><a href="suite-execution.json">suite-execution.json</a> | <a href="suite-report.csv">suite-report.csv</a> | <a href="suite-report.pdf">suite-report.pdf</a></p></body></html>
                """.formatted(esc(suite.suiteName), suite.status, suite.totalTests, suite.passedTests, suite.failedTests, suite.durationMs, rows);
        Files.writeString(root.resolve("suite-report.html"), html);
    }

    private void writePdf(SuiteExecutionResult suite) throws Exception {
        Path file = root.resolve("suite-report.pdf");
        try (PdfWriter writer = new PdfWriter(file.toString());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            document.add(new Paragraph("AI Testing Agent Suite Report").setBold().setFontSize(18));
            document.add(new Paragraph("Suite: " + text(suite.suiteName)));
            document.add(new Paragraph("Status: " + suite.status));
            document.add(new Paragraph("Tests: " + suite.totalTests + " | Passed: " + suite.passedTests + " | Failed: " + suite.failedTests));
            document.add(new Paragraph("Duration: " + suite.durationMs + " ms"));
            Table table = new Table(UnitValue.createPercentArray(new float[]{0.5f, 2.5f, 2.5f, 1, 1.2f})).useAllAvailableWidth();
            table.addHeaderCell("#"); table.addHeaderCell("Plan"); table.addHeaderCell("Test"); table.addHeaderCell("Status"); table.addHeaderCell("Duration");
            for (var test : suite.tests) {
                table.addCell(String.valueOf(test.index + 1)); table.addCell(text(test.planFile)); table.addCell(text(test.testName));
                table.addCell(test.status); table.addCell(test.durationMs + " ms");
            }
            document.add(table);
        }
    }

    private void writeIndividualReports(SuiteExecutionResult suite) throws Exception {
        Path tests = root.resolve("tests");
        for (var test : suite.tests) {
            Path dir = tests.resolve(String.valueOf(test.index + 1));
            new ReportManager(dir).writeAll(test.result);
        }
    }

    private String csv(String value) { return "\"" + text(value).replace("\"", "\"\"").replace("\n", " ") + "\""; }
    private String esc(String value) { return text(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private String text(String value) { return value == null ? "" : value; }
}
