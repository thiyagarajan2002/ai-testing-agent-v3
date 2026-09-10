package com.thiyagarajan.agent;

import com.thiyagarajan.agent.runtime.QualityGateManager;
import com.thiyagarajan.agent.runtime.SuiteExecutionResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QualityGateManagerTest {
    private SuiteExecutionResult suite(int total, int passed, int failed, long duration) {
        SuiteExecutionResult s = new SuiteExecutionResult();
        s.totalTests = total; s.passedTests = passed; s.failedTests = failed; s.durationMs = duration;
        return s;
    }

    @Test void passesWhenAllGatesMeetPolicy() {
        var result = QualityGateManager.evaluate(suite(10, 10, 0, 500), new QualityGateManager.Policy(100, 0, 1000, 0), 0);
        assertTrue(result.passed());
        assertEquals(100.0, result.passRate());
    }

    @Test void failsForPassRateFailuresDurationAndRegression() {
        var result = QualityGateManager.evaluate(suite(10, 7, 3, 2000), new QualityGateManager.Policy(80, 1, 1000, 0), 2);
        assertFalse(result.passed());
        assertEquals(4, result.failureCount());
    }

    @Test void validatesPolicy() {
        assertThrows(IllegalArgumentException.class, () -> new QualityGateManager.Policy(101, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new QualityGateManager.Policy(90, -1, 0, 0));
    }
}
