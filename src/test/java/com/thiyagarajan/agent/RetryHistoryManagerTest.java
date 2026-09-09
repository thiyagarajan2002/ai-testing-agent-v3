package com.thiyagarajan.agent;

import com.thiyagarajan.agent.ai.FailureIntelligence;
import com.thiyagarajan.agent.runtime.RetryHistoryManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class RetryHistoryManagerTest {
    @Test
    void persistsRetryDecisionAndOutcome() throws Exception {
        var root = Files.createTempDirectory("retry-history-");
        var manager = new RetryHistoryManager(root);
        var analysis = new FailureIntelligence.Analysis(FailureIntelligence.Category.TIMEOUT,
                FailureIntelligence.RetryRecommendation.RETRY_WITH_BACKOFF, "transient timing failure");
        manager.record("run-1", "login", "click", 1, analysis, true);
        var entries = manager.load();
        assertEquals(1, entries.size());
        assertEquals("TIMEOUT", entries.get(0).category);
        assertEquals("RETRY_WITH_BACKOFF", entries.get(0).recommendation);
        assertTrue(entries.get(0).recovered);
    }
}
