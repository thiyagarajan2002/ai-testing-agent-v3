package com.thiyagarajan.agent;

import com.thiyagarajan.agent.runtime.ExecutionResult;
import com.thiyagarajan.agent.runtime.RunContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RunContextTest {
    @Test
    void createsUniqueRunIdsAndCorrelatesResults() {
        try (RunContext context = RunContext.start()) {
            assertTrue(context.runId().matches("run-[a-f0-9]{12}"));
            ExecutionResult result = new ExecutionResult();
            result.testName = "correlation-test";
            ExecutionResult.StepResult step = new ExecutionResult.StepResult("GET", true, "OK", 10);
            result.steps.add(step);
            result.ensureIdentity();

            assertEquals(context.runId(), result.runId);
            assertEquals(context.runId() + "-test-1", result.testId);
            assertTrue(step.stepId.startsWith("step-"));
        }
        assertEquals("", RunContext.currentRunId());
    }

    @Test
    void persistedIdentityFieldsRemainJacksonFriendly() {
        ExecutionResult result = new ExecutionResult();
        result.runId = "run-123456789abc";
        result.testId = "run-123456789abc-test-1";
        result.steps.add(new ExecutionResult.StepResult("GET", true, "OK", 5));
        assertFalse(result.steps.get(0).stepId.isBlank());
    }
}
