package com.thiyagarajan.agent.report;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.thiyagarajan.agent.runtime.ExecutionResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Generates the printable PDF counterpart of the HTML/CSV execution report. */
public final class PdfReportWriter {

    public void write(ExecutionResult result, Path file) throws Exception {
        write(result, file, "");
    }

    public void write(ExecutionResult result, Path file, String generatedAt) throws Exception {
        if (result == null) throw new IllegalArgumentException("Execution result cannot be null");
        if (file == null) throw new IllegalArgumentException("PDF path cannot be null");
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);

        List<ExecutionResult.StepResult> steps = result.steps == null ? List.of() : result.steps;
        long passed = steps.stream().filter(s -> s.passed).count();
        long failed = steps.size() - passed;
        long totalDuration = steps.stream().mapToLong(s -> Math.max(0, s.durationMs)).sum();
        long minDuration = steps.stream().mapToLong(s -> Math.max(0, s.durationMs)).min().orElse(0);
        long maxDuration = steps.stream().mapToLong(s -> Math.max(0, s.durationMs)).max().orElse(0);
        double averageDuration = steps.isEmpty() ? 0 : (double) totalDuration / steps.size();
        double passRate = steps.isEmpty() ? (result.passed ? 100.0 : 0.0) : (passed * 100.0) / steps.size();

        try (PdfWriter writer = new PdfWriter(file.toString());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.add(new Paragraph("AI Testing Agent Execution Report")
                    .setFontSize(20).setBold().setTextAlignment(TextAlignment.CENTER));
            if (generatedAt != null && !generatedAt.isBlank()) {
                document.add(new Paragraph("Generated: " + generatedAt)
                        .setFontSize(9).setTextAlignment(TextAlignment.CENTER));
            }

            document.add(new Paragraph("Execution Summary").setFontSize(14).setBold());
            Table summary = new Table(UnitValue.createPercentArray(new float[]{2, 3})).useAllAvailableWidth();
            addSummaryRow(summary, "Test", text(result.testName));
            addSummaryRow(summary, "Overall Status", result.passed ? "PASS" : "FAIL");
            addSummaryRow(summary, "Total Steps", String.valueOf(steps.size()));
            addSummaryRow(summary, "Passed / Failed", passed + " / " + failed);
            addSummaryRow(summary, "Pass Rate", String.format(Locale.ROOT, "%.2f%%", passRate));
            addSummaryRow(summary, "Total Duration", totalDuration + " ms");
            addSummaryRow(summary, "Average Duration", String.format(Locale.ROOT, "%.2f ms", averageDuration));
            addSummaryRow(summary, "Min / Max Duration", minDuration + " ms / " + maxDuration + " ms");
            document.add(summary);

            document.add(new Paragraph("Step Results").setFontSize(14).setBold().setMarginTop(14));
            Table table = new Table(UnitValue.createPercentArray(new float[]{2.2f, 1, 1.2f, 4.6f})).useAllAvailableWidth();
            addHeader(table, "Action");
            addHeader(table, "Status");
            addHeader(table, "Duration (ms)");
            addHeader(table, "Details / Input / Output / Artifacts");

            for (ExecutionResult.StepResult step : steps) {
                table.addCell(new Cell().add(new Paragraph(text(step.action))));
                Paragraph status = new Paragraph(step.passed ? "PASS" : "FAIL").setBold();
                status.setFontColor(step.passed ? ColorConstants.GREEN : ColorConstants.RED);
                table.addCell(new Cell().add(status));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(step.durationMs))));

                StringBuilder detail = new StringBuilder(text(step.details));
                if (step.artifacts != null && !step.artifacts.isEmpty()) {
                    detail.append("\n\nArtifacts:");
                    for (String artifact : step.artifacts) {
                        detail.append("\n- ").append(text(artifact));
                    }
                }
                table.addCell(new Cell().add(new Paragraph(detail.toString())));
            }
            document.add(table);

            document.add(new Paragraph("Failure Analysis").setFontSize(14).setBold().setMarginTop(14));
            String failure = text(result.failureAnalysis);
            document.add(new Paragraph(failure.isBlank() ? "No failure analysis recorded." : failure));

            document.add(new Paragraph("Report Formats").setFontSize(14).setBold().setMarginTop(14));
            document.add(new Paragraph("This execution is published consistently as report.html, report.csv and report.pdf. "
                    + "Runtime logs and UI screenshots remain separate execution evidence and may be referenced as step artifacts."));
        }
    }

    private void addSummaryRow(Table table, String key, String value) {
        table.addCell(new Cell().add(new Paragraph(key).setBold()));
        table.addCell(new Cell().add(new Paragraph(value)));
    }

    private void addHeader(Table table, String value) {
        table.addHeaderCell(new Cell().add(new Paragraph(value).setBold()));
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
