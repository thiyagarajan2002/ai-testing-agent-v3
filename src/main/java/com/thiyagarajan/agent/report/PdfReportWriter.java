package com.thiyagarajan.agent.report;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.thiyagarajan.agent.runtime.ExecutionResult;

import java.nio.file.Files;
import java.nio.file.Path;

/** Generates a real PDF execution report using iText. */
public final class PdfReportWriter {
    public void write(ExecutionResult result, Path file) throws Exception {
        if (result == null) throw new IllegalArgumentException("Execution result cannot be null");
        if (file == null) throw new IllegalArgumentException("PDF path cannot be null");
        Files.createDirectories(file.toAbsolutePath().getParent());
        try (PdfWriter writer = new PdfWriter(file.toString());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            document.add(new Paragraph("AI Testing Agent Execution Report").setBold().setFontSize(18));
            document.add(new Paragraph("Test: " + text(result.testName)));
            document.add(new Paragraph("Status: " + (result.passed ? "PASS" : "FAIL")));
            document.add(new Paragraph("Steps: " + result.steps.size()));
            document.add(new Paragraph("Passed: " + result.steps.stream().filter(s -> s.passed).count()
                    + " | Failed: " + result.steps.stream().filter(s -> !s.passed).count()));
            document.add(new Paragraph("Failure Analysis: " + text(result.failureAnalysis)));

            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 4})).useAllAvailableWidth();
            table.addHeaderCell("Action");
            table.addHeaderCell("Status");
            table.addHeaderCell("Duration (ms)");
            table.addHeaderCell("Details");
            for (ExecutionResult.StepResult step : result.steps) {
                table.addCell(text(step.action));
                table.addCell(step.passed ? "PASS" : "FAIL");
                table.addCell(String.valueOf(step.durationMs));
                table.addCell(text(step.details));
            }
            document.add(table);
        }
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
