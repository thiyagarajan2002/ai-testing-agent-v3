package com.thiyagarajan.agent.io;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RunLogManagerTest {
    @Test
    void preservesTerminalOutputInRunLog() throws Exception {
        Path reports = Files.createTempDirectory("run-log-");
        try (RunLogManager ignored = RunLogManager.start(reports.toString(), "api", "plan")) {
            System.out.println("run-log-test-output");
        }
        Path log = Files.walk(reports).filter(Files::isRegularFile).findFirst().orElseThrow();
        String content = Files.readString(log);
        assertTrue(content.contains("AI Testing Agent run started"));
        assertTrue(content.contains("run-log-test-output"));
        assertTrue(content.contains("AI Testing Agent run finished"));
    }
}
