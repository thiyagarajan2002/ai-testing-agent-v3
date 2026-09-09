package com.thiyagarajan.agent;

import com.thiyagarajan.agent.ai.FailureIntelligence;
import com.thiyagarajan.agent.runtime.AdaptiveRetryPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveRetryPolicyTest {
    @Test
    void usesExponentialBackoffWithCap() {
        assertEquals(250, AdaptiveRetryPolicy.delayMs(1));
        assertEquals(500, AdaptiveRetryPolicy.delayMs(2));
        assertEquals(1000, AdaptiveRetryPolicy.delayMs(3));
        assertEquals(4000, AdaptiveRetryPolicy.delayMs(10));
    }

    @Test
    void onlyTransientRecommendationsCanRetry() {
        var retry = new FailureIntelligence.Analysis(
                FailureIntelligence.Category.NETWORK,
                FailureIntelligence.RetryRecommendation.RETRY_WITH_BACKOFF,
                "network");
        var assertion = new FailureIntelligence.Analysis(
                FailureIntelligence.Category.ASSERTION,
                FailureIntelligence.RetryRecommendation.DO_NOT_RETRY,
                "assertion");
        assertTrue(AdaptiveRetryPolicy.shouldRetry(retry, 1, 2));
        assertTrue(AdaptiveRetryPolicy.shouldRetry(retry, 2, 2));
        assertFalse(AdaptiveRetryPolicy.shouldRetry(retry, 3, 2));
        assertFalse(AdaptiveRetryPolicy.shouldRetry(assertion, 1, 2));
    }
}
