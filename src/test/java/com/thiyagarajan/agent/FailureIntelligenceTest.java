package com.thiyagarajan.agent;

import com.thiyagarajan.agent.ai.FailureIntelligence;
import com.thiyagarajan.agent.runtime.ExecutionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FailureIntelligenceTest {
    private ExecutionResult failed(String details) {
        ExecutionResult result = new ExecutionResult();
        result.passed = false;
        result.steps.add(new ExecutionResult.StepResult("test", false, details, 10));
        return result;
    }

    @Test
    void classifiesTimeoutAsBackoffRetry() {
        var analysis = FailureIntelligence.analyze(failed("Timeout waiting for locator"));
        assertEquals(FailureIntelligence.Category.TIMEOUT, analysis.category());
        assertEquals(FailureIntelligence.RetryRecommendation.RETRY_WITH_BACKOFF, analysis.recommendation());
    }

    @Test
    void doesNotRetryAuthenticationFailure() {
        var analysis = FailureIntelligence.analyze(failed("401 Unauthorized"));
        assertEquals(FailureIntelligence.Category.AUTHENTICATION, analysis.category());
        assertEquals(FailureIntelligence.RetryRecommendation.DO_NOT_RETRY, analysis.recommendation());
    }

    @Test
    void recommendsLocatorHealing() {
        var analysis = FailureIntelligence.analyze(failed("Locator #login not visible"));
        assertEquals(FailureIntelligence.Category.LOCATOR, analysis.category());
        assertEquals(FailureIntelligence.RetryRecommendation.HEAL_LOCATOR, analysis.recommendation());
    }
}
