package com.thiyagarajan.agent;

import com.thiyagarajan.agent.runtime.HealingHistoryManager;
import com.thiyagarajan.agent.runtime.SelfHealingEngine;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HealingHistoryManagerTest {
    @Test
    void persistsSuccessfulHealingWithRedactionBoundary() throws Exception {
        Path root = Files.createTempDirectory("healing-history-");
        HealingHistoryManager manager = new HealingHistoryManager(root);
        var healing = new SelfHealingEngine.HealingResult("#secret", "[data-testid=\"secret\"]", 0.96, "safe candidate", java.util.List.of());

        manager.record("run-1", "login", "click", healing);

        var entries = manager.load();
        assertEquals(1, entries.size());
        assertEquals("run-1", entries.get(0).runId);
        assertEquals("login", entries.get(0).testName);
        assertEquals(0.96, entries.get(0).confidence);
        assertTrue(Files.isRegularFile(manager.file()));
    }

    @Test
    void ignoresUnhealedResult() throws Exception {
        Path root = Files.createTempDirectory("healing-history-empty-");
        HealingHistoryManager manager = new HealingHistoryManager(root);
        manager.record("run-1", "login", "click", new SelfHealingEngine.HealingResult("#missing", null, 0.0, "none", java.util.List.of()));
        assertTrue(manager.load().isEmpty());
    }
}
