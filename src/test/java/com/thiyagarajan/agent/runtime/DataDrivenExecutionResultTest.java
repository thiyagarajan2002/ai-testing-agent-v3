package com.thiyagarajan.agent.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataDrivenExecutionResultTest {
    @Test
    void reportsPerformanceMetrics() {
        DataDrivenExecutionResult result = new DataDrivenExecutionResult();
        result.totalIterations = 4;
        result.passedIterations = 3;
        result.failedIterations = 1;
        result.executionMode = "PARALLEL";
        result.parallelism = 4;
        result.estimatedSequentialDurationMs = 800;
        result.durationMs = 250;
        result.estimatedSpeedup = 3.2;

        assertEquals(200.0, result.averageIterationDurationMs());
        assertEquals(3.2, result.estimatedSpeedup);
        assertTrue(!result.passed());
    }

    @Test
    void passRequiresAtLeastOneIterationAndNoFailures() {
        DataDrivenExecutionResult empty = new DataDrivenExecutionResult();
        assertTrue(!empty.passed());

        DataDrivenExecutionResult success = new DataDrivenExecutionResult();
        success.totalIterations = 2;
        success.passedIterations = 2;
        assertTrue(success.passed());
    }
}
