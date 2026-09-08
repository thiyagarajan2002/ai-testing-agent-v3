package com.thiyagarajan.agent.report;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
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

/** Generates a professional, color-coded PDF counterpart of the HTML/CSV report. */
public final class PdfReportWriter {
    private static final PdfFont REGULAR_FONT;
    private static final PdfFont BOLD_FONT;

    private static final Color NAVY = new DeviceRgb(31, 41, 55);
    private static final Color BLUE = new DeviceRgb(37, 99, 235);
    private static final Color BLUE_LIGHT = new DeviceRgb(239, 246, 255);
    private static final Color GREEN = new DeviceRgb(22, 163, 74);
    private static final Color GREEN_LIGHT = new DeviceRgb(240, 253, 244);
    private static final Color RED = new DeviceRgb(220, 38, 38);
    private static final Color RED_LIGHT = new DeviceRgb(254, 242, 242);
    private static final Color AMBER = new DeviceRgb(217, 119, 6);
    private static final Color AMBER_LIGHT = new DeviceRgb(255, 251, 235);
    private static final Color SLATE = new DeviceRgb(100, 116, 139);
    private static final Color BORDER = new DeviceRgb(226, 232, 240);
    private static final Color SURFACE = new DeviceRgb(248, 250, 252);

    static {
        try {
            REGULAR_FONT = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            BOLD_FONT = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

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
            document.setMargins(30, 30, 32, 30);

            Table banner = new Table(UnitValue.createPercentArray(new float[]{4, 1})).useAllAvailableWidth();
            banner.setBackgroundColor(NAVY);
            banner.setBorder(Border.NO_BORDER);
            Cell title = new Cell().setBorder(Border.NO_BORDER).setPadding(16)
                    .add(bold("AI TESTING AGENT").setFontColor(ColorConstants.WHITE).setFontSize(9))
                    .add(new Paragraph("Execution Report").setFont(REGULAR_FONT).setFontColor(ColorConstants.WHITE).setFontSize(20));
            Cell outcome = new Cell().setBorder(Border.NO_BORDER).setPadding(16).setTextAlignment(TextAlignment.RIGHT)
                    .add(bold(result.passed ? "PASSED" : "FAILED").setFontColor(ColorConstants.WHITE).setFontSize(14));
            banner.addCell(title).addCell(outcome);
            document.add(banner);

            document.add(new Paragraph(text(result.testName)).setFont(BOLD_FONT).setFontSize(15).setFontColor(NAVY).setMarginTop(16).setMarginBottom(2));
            if (generatedAt != null && !generatedAt.isBlank()) {
                document.add(new Paragraph("Generated " + generatedAt).setFont(REGULAR_FONT).setFontSize(8).setFontColor(SLATE));
            }

            document.add(sectionTitle("Execution Summary"));
            Table summary = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1, 1})).useAllAvailableWidth();
            addKpi(summary, "TOTAL", String.valueOf(steps.size()), BLUE, BLUE_LIGHT);
            addKpi(summary, "PASSED", String.valueOf(passed), GREEN, GREEN_LIGHT);
            addKpi(summary, "FAILED", String.valueOf(failed), RED, RED_LIGHT);
            addKpi(summary, "PASS RATE", String.format(Locale.ROOT, "%.1f%%", passRate), result.passed ? GREEN : RED,
                    result.passed ? GREEN_LIGHT : RED_LIGHT);
            addKpi(summary, "DURATION", totalDuration + " ms", BLUE, BLUE_LIGHT);
            addKpi(summary, "AVERAGE", String.format(Locale.ROOT, "%.1f ms", averageDuration), AMBER, AMBER_LIGHT);
            document.add(summary);

            document.add(sectionTitle("Performance Profile"));
            Table performance = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1})).useAllAvailableWidth();
            addMetric(performance, "Fastest Step", minDuration + " ms", GREEN_LIGHT, GREEN);
            addMetric(performance, "Slowest Step", maxDuration + " ms", AMBER_LIGHT, AMBER);
            addMetric(performance, "Execution Status", result.passed ? "PASS" : "FAIL", result.passed ? GREEN_LIGHT : RED_LIGHT,
                    result.passed ? GREEN : RED);
            document.add(performance);

            document.add(sectionTitle("Step Results"));
            Table table = new Table(UnitValue.createPercentArray(new float[]{.45f, 2.1f, .85f, 1.0f, 4.6f})).useAllAvailableWidth();
            addHeader(table, "#");
            addHeader(table, "Action");
            addHeader(table, "Status");
            addHeader(table, "Duration");
            addHeader(table, "Details / Input / Output / Artifacts");

            for (int i = 0; i < steps.size(); i++) {
                ExecutionResult.StepResult step = steps.get(i);
                table.addCell(cell(String.valueOf(i + 1), SURFACE, TextAlignment.CENTER));
                table.addCell(cell(text(step.action), ColorConstants.WHITE, TextAlignment.LEFT));
                Color statusColor = step.passed ? GREEN : RED;
                Color statusBg = step.passed ? GREEN_LIGHT : RED_LIGHT;
                table.addCell(cell(step.passed ? "PASS" : "FAIL", statusBg, TextAlignment.CENTER)
                        .setFontColor(statusColor));
                Color durationColor = step.durationMs >= 1000 ? AMBER : NAVY;
                table.addCell(cell(step.durationMs + " ms", ColorConstants.WHITE, TextAlignment.RIGHT)
                        .setFontColor(durationColor));

                StringBuilder detail = new StringBuilder(text(step.details));
                if (step.artifacts != null && !step.artifacts.isEmpty()) {
                    detail.append("\n\nArtifacts:");
                    for (String artifact : step.artifacts) detail.append("\n- ").append(text(artifact));
                }
                table.addCell(cell(detail.toString(), ColorConstants.WHITE, TextAlignment.LEFT));
            }
            document.add(table);

            document.add(sectionTitle("Failure Analysis"));
            String failure = text(result.failureAnalysis);
            boolean hasFailure = !failure.isBlank();
            Cell failureCell = new Cell().setPadding(12)
                    .setBorder(new SolidBorder(hasFailure ? RED : GREEN, 1.5f))
                    .setBackgroundColor(hasFailure ? RED_LIGHT : GREEN_LIGHT)
                    .add(bold(hasFailure ? failure : "No failure analysis recorded.")
                            .setFontColor(hasFailure ? RED : GREEN).setFontSize(9));
            Table failureTable = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
            failureTable.addCell(failureCell);
            document.add(failureTable);

            document.add(sectionTitle("Report Package"));
            document.add(new Paragraph("Standard report formats generated from the same execution result: HTML dashboard, CSV data export and this PDF. Runtime logs and UI screenshots remain separate execution evidence and may be referenced by step artifacts.")
                    .setFont(REGULAR_FONT).setFontSize(8.5f).setFontColor(SLATE));
        }
    }

    private Paragraph sectionTitle(String value) {
        return bold(value).setFontSize(13).setFontColor(NAVY).setMarginTop(15).setMarginBottom(8);
    }

    private void addKpi(Table table, String label, String value, Color accent, Color background) {
        Cell cell = new Cell().setPadding(9).setBackgroundColor(background).setBorder(new SolidBorder(BORDER, 0.6f));
        cell.add(bold(label).setFontSize(7).setFontColor(SLATE));
        cell.add(bold(value).setFontSize(13).setFontColor(accent).setMarginTop(3));
        table.addCell(cell);
    }

    private void addMetric(Table table, String label, String value, Color background, Color accent) {
        Cell cell = new Cell().setPadding(10).setBackgroundColor(background).setBorder(new SolidBorder(BORDER, 0.6f));
        cell.add(bold(label).setFontSize(8).setFontColor(SLATE));
        cell.add(bold(value).setFontSize(11).setFontColor(accent).setMarginTop(3));
        table.addCell(cell);
    }

    private void addHeader(Table table, String value) {
        table.addHeaderCell(new Cell().setBackgroundColor(NAVY).setPadding(7)
                .add(bold(value).setFontSize(7.5f).setFontColor(ColorConstants.WHITE)));
    }

    private Cell cell(String value, Color background, TextAlignment alignment) {
        return new Cell().setBackgroundColor(background).setBorder(new SolidBorder(BORDER, 0.5f)).setPadding(6)
                .setTextAlignment(alignment).add(new Paragraph(value).setFont(REGULAR_FONT).setFontSize(8));
    }

    private Paragraph bold(String value) {
        return new Paragraph(value == null ? "" : value).setFont(BOLD_FONT);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
