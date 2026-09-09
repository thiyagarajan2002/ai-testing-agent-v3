package com.thiyagarajan.agent;

import com.thiyagarajan.agent.runtime.SelfHealingEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SelfHealingEngineConfidenceTest {
    @Test
    void filtersCandidatesBelowConfiguredConfidence() {
        var candidates = SelfHealingEngine.candidates("#login");
        assertEquals(3, candidates.size());
        assertEquals(0.96, candidates.get(0).confidence());
        assertEquals(0.90, candidates.get(1).confidence());
    }

    @Test
    void rejectsInvalidConfidenceThreshold() {
        assertThrows(IllegalArgumentException.class, () -> SelfHealingEngine.heal(null, "#login", 1.1));
        assertThrows(IllegalArgumentException.class, () -> SelfHealingEngine.heal(null, "#login", -0.1));
    }
}
