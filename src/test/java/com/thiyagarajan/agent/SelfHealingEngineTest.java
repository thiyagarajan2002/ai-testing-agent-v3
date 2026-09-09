package com.thiyagarajan.agent;

import com.thiyagarajan.agent.runtime.SelfHealingEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SelfHealingEngineTest {
    @Test
    void generatesConservativeAlternativesForIdLocator() {
        var candidates = SelfHealingEngine.candidates("#login");
        assertEquals(3, candidates.size());
        assertEquals("[data-testid=\"login\"]", candidates.get(0).locator());
        assertEquals(0.96, candidates.get(0).confidence());
        assertEquals("[name=\"login\"]", candidates.get(1).locator());
        assertEquals("[aria-label=\"login\"]", candidates.get(2).locator());
    }

    @Test
    void doesNotGenerateBroadXpathOrTextSelectors() {
        assertTrue(SelfHealingEngine.candidates(".missing-button").isEmpty());
    }
}
