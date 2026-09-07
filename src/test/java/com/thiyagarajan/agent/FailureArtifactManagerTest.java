package com.thiyagarajan.agent.runtime;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FailureArtifactManagerTest {
    @Test
    void createsSafeFailureMetadata() throws Exception {
        Path temp = Files.createTempDirectory("agent-artifacts-");
        var config = new com.thiyagarajan.agent.config.Config(
                "http://localhost", "test", true, 1000, 0, 1, temp.toString(), "screenshots");
        var manager = new FailureArtifactManager(config);
        Path file = manager.createFailureMetadata("Login / Checkout", 0, "assertText", "boom");
        assertTrue(Files.exists(file));
        assertTrue(file.getFileName().toString().matches("[a-zA-Z0-9._-]+"));
        assertTrue(Files.readString(file).contains("Login / Checkout"));
    }

    @Test
    void preservesBackwardCompatibleStepConstructorAndArtifacts() {
        var step = new ExecutionResult.StepResult("click", false, "failed", 10, List.of("reports/screenshots/failure.png"));
        assertEquals(1, step.artifacts.size());
        assertEquals("reports/screenshots/failure.png", step.artifacts.get(0));
    }
}
