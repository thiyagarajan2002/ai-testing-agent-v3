package com.thiyagarajan.agent.io;

import com.thiyagarajan.agent.runtime.SecurityRedactor;

import java.io.ByteArrayOutputStream;
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

/** Creates durable, redacted terminal logs while preserving live terminal output. */
public final class RunLogManager implements AutoCloseable {
    private final Path logFile;
    private final PrintStream originalOut;
    private final PrintStream originalErr;
    private final PrintStream filePrint;
    private final RedactingLineOutputStream fileOutput;
    private final PrintStream teeOut;
    private final PrintStream teeErr;
    private boolean closed;

    private RunLogManager(Path logFile, PrintStream originalOut, PrintStream originalErr) throws IOException {
        this.logFile = logFile;
        this.originalOut = originalOut;
        this.originalErr = originalErr;
        Files.createDirectories(logFile.getParent());
        this.filePrint = new PrintStream(
                Files.newOutputStream(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                true, StandardCharsets.UTF_8);
        this.fileOutput = new RedactingLineOutputStream(filePrint);
        this.teeOut = new TeePrintStream(originalOut, fileOutput);
        this.teeErr = new TeePrintStream(originalErr, fileOutput);
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

    public synchronized void log(String message) {
        if (closed) return;
        teeOut.println("[" + Instant.now() + "] " + (message == null ? "" : message));
    }

    public Path logFile() { return logFile; }

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        try {
            teeOut.println("[" + Instant.now() + "] === AI Testing Agent run finished ===");
            teeOut.flush();
            teeErr.flush();
            fileOutput.flush();
        } finally {
            closed = true;
            System.setOut(originalOut);
            System.setErr(originalErr);
            filePrint.close();
        }
    }

    private static String safe(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String safe = value.replaceAll("[^a-zA-Z0-9._-]+", "_");
        return safe.isBlank() ? fallback : safe.substring(0, Math.min(80, safe.length()));
    }

    private static final class TeePrintStream extends PrintStream {
        TeePrintStream(PrintStream console, OutputStream file) throws IOException {
            super(new OutputStream() {
                @Override public synchronized void write(int b) throws IOException {
                    console.write(b);
                    file.write(b);
                }
                @Override public synchronized void write(byte[] b, int off, int len) throws IOException {
                    console.write(b, off, len);
                    file.write(b, off, len);
                }
                @Override public synchronized void flush() throws IOException {
                    console.flush();
                    file.flush();
                }
            }, true, StandardCharsets.UTF_8);
        }

        @Override public void close() { flush(); }
    }

    /** Buffers UTF-8 terminal output by line so secrets are redacted without corrupting Unicode. */
    private static final class RedactingLineOutputStream extends OutputStream {
        private final OutputStream target;
        private final ByteArrayOutputStream line = new ByteArrayOutputStream();

        RedactingLineOutputStream(OutputStream target) {
            this.target = target;
        }

        @Override
        public synchronized void write(int b) throws IOException {
            line.write(b);
            if (b == '\n') flushLine();
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            line.write(b, off, len);
            int end = off + len;
            for (int i = off; i < end; i++) {
                if (b[i] == '\n') {
                    flushLine();
                    if (i + 1 < end) line.write(b, i + 1, end - i - 1);
                    return;
                }
            }
        }

        @Override
        public synchronized void flush() throws IOException {
            if (line.size() > 0) flushLine();
            target.flush();
        }

        private void flushLine() throws IOException {
            String redacted = SecurityRedactor.redactText(line.toString(StandardCharsets.UTF_8));
            target.write(redacted.getBytes(StandardCharsets.UTF_8));
            line.reset();
        }
    }
}
