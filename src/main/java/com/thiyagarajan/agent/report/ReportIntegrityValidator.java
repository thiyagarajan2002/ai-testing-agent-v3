package com.thiyagarajan.agent.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/** Validates the deterministic three-file execution report package. */
public final class ReportIntegrityValidator {
    private static final Set<String> REQUIRED_FILES = Set.of("report.html", "report.csv", "report.pdf");
    private static final String CSV_HEADER_PREFIX = "\ufeffScenario No";

    private ReportIntegrityValidator() {
    }

    public static void validate(Path reportDirectory) throws IOException {
        if (reportDirectory == null) throw new IllegalArgumentException("Report directory cannot be null");
        Path dir = reportDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(dir)) throw new IOException("Report directory does not exist: " + dir);

        try (var stream = Files.list(dir)) {
            Set<String> files = stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());
            if (!files.equals(REQUIRED_FILES)) {
                throw new IOException("Invalid report package. Expected exactly " + REQUIRED_FILES + " but found " + files);
            }
        }

        Path html = dir.resolve("report.html");
        Path csv = dir.resolve("report.csv");
        Path pdf = dir.resolve("report.pdf");
        requireNonEmpty(html, "HTML report");
        requireNonEmpty(csv, "CSV report");
        requireNonEmpty(pdf, "PDF report");

        String htmlText = Files.readString(html, StandardCharsets.UTF_8);
        if (!htmlText.contains("AI Testing Agent") || !htmlText.contains("report.csv") || !htmlText.contains("report.pdf")) {
            throw new IOException("HTML report is missing standard package markers");
        }

        String csvText = Files.readString(csv, StandardCharsets.UTF_8);
        if (!csvText.startsWith(CSV_HEADER_PREFIX)) {
            throw new IOException("CSV report must use UTF-8 BOM and the standard Scenario No header");
        }
        if (!csvText.contains("SUMMARY")) throw new IOException("CSV report is missing its summary section");

        byte[] pdfBytes = Files.readAllBytes(pdf);
        if (pdfBytes.length < 5 || pdfBytes[0] != '%' || pdfBytes[1] != 'P' || pdfBytes[2] != 'D'
                || pdfBytes[3] != 'F' || pdfBytes[4] != '-') {
            throw new IOException("PDF report does not have a valid PDF signature");
        }
    }

    private static void requireNonEmpty(Path file, String label) throws IOException {
        if (!Files.isRegularFile(file) || Files.size(file) == 0) throw new IOException(label + " is missing or empty: " + file);
    }
}
