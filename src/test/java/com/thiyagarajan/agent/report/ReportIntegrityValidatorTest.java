package com.thiyagarajan.agent.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportIntegrityValidatorTest {
    @TempDir
    Path tempDir;

    @Test
    void acceptsCompleteStandardReportPackage() throws Exception {
        Files.writeString(tempDir.resolve("report.html"), "AI Testing Agent report.csv report.pdf", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("report.csv"), "\ufeffScenario No,Result\nSUMMARY,1", StandardCharsets.UTF_8);
        Files.write(tempDir.resolve("report.pdf"), "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII));

        assertDoesNotThrow(() -> ReportIntegrityValidator.validate(tempDir));
    }

    @Test
    void rejectsMissingReportFile() throws Exception {
        Files.writeString(tempDir.resolve("report.html"), "AI Testing Agent report.csv report.pdf", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("report.csv"), "\ufeffScenario No,Result\nSUMMARY,1", StandardCharsets.UTF_8);

        assertThrows(Exception.class, () -> ReportIntegrityValidator.validate(tempDir));
    }

    @Test
    void rejectsUnexpectedRegularFile() throws Exception {
        Files.writeString(tempDir.resolve("report.html"), "AI Testing Agent report.csv report.pdf", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("report.csv"), "\ufeffScenario No,Result\nSUMMARY,1", StandardCharsets.UTF_8);
        Files.write(tempDir.resolve("report.pdf"), "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII));
        Files.writeString(tempDir.resolve("execution.json"), "{}", StandardCharsets.UTF_8);

        assertThrows(Exception.class, () -> ReportIntegrityValidator.validate(tempDir));
    }

    @Test
    void rejectsInvalidPdfSignature() throws Exception {
        Files.writeString(tempDir.resolve("report.html"), "AI Testing Agent report.csv report.pdf", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("report.csv"), "\ufeffScenario No,Result\nSUMMARY,1", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("report.pdf"), "not a pdf", StandardCharsets.UTF_8);

        assertThrows(Exception.class, () -> ReportIntegrityValidator.validate(tempDir));
    }
}
