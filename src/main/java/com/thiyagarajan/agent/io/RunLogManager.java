package com.thiyagarajan.agent.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Creates durable terminal logs while preserving live terminal output. */
public final class RunLogManager implements AutoCloseable {
    private final Path logFile;
    private final PrintStream originalOut;
    private final PrintStream originalErr;
    private final PrintStream filePrint;
    private final PrintStream teeOut;
    private final PrintStream teeErr;

    private RunLogManager(Path logFile, PrintStream originalOut, PrintStream originalErr) throws IOException {
        this.logFile = logFile;
        this.originalOut = originalOut;
        this.originalErr = originalErr;
        Files.createDirectories(logFile.getParent());
        this.filePrint = new PrintStream(
                Files.newOutputStream(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                true, StandardCharsets.UTF_8);
        this.teeOut = new TeePrintStream(originalOut, filePrint);
        this.teeErr = new TeePrintStream(originalErr, filePrint);
        System.setOut(teeOut);
        System.setErr(teeErr);
        log("=== AI Testing Agent run started ===");
    }

    public static RunLogManager start(String reportsDir, String area, String command) throws IOException {
        String safeArea = safe(area, "terminal");
        String safeCommand = safe(command, "run");
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS", Locale.ROOT)
                .withZone(java.time.ZoneId.systemDefault()).format(Instant.now());
        Path file = Path.of(reportsDir, safeArea, "logs",
                "terminal-" + timestamp + "-" + safeCommand + ".log").toAbsolutePath().normalize();
        return new RunLogManager(file, System.out, System.err);
    }

    public void log(String message) {
        String line = "[" + Instant.now() + "] " + (message == null ? "" : message);
        teeOut.println(line);
    }

    public Path logFile() { return logFile; }

    @Override
    public void close() {
        log("=== AI Testing Agent run finished ===");
        teeOut.flush();
        teeErr.flush();
        System.setOut(originalOut);
        System.setErr(originalErr);
        filePrint.close();
    }

    private static String safe(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String safe = value.replaceAll("[^a-zA-Z0-9._-]+", "_");
        return safe.isBlank() ? fallback : safe.substring(0, Math.min(80, safe.length()));
    }

    private static final class TeePrintStream extends PrintStream {
        TeePrintStream(PrintStream console, PrintStream file) throws IOException {
            super(new OutputStream() {
                @Override public void write(int b) { console.write(b); file.write(b); }
                @Override public void write(byte[] b, int off, int len) { console.write(b, off, len); file.write(b, off, len); }
                @Override public void flush() { console.flush(); file.flush(); }
            }, true, StandardCharsets.UTF_8);
        }

        @Override public void close() { flush(); }
    }
}
