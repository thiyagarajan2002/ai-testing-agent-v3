package com.thiyagarajan.agent.runtime;

import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestPlanValidatorTest {
    @Test
    void validApiPlanHasNoErrors() {
        TestPlan plan = new TestPlan();
        plan.type = "API";
        plan.baseUrl = "https://example.test";
        TestStep step = new TestStep();
        step.action = "GET";
        step.path = "/health";
        plan.steps.add(step);

        var result = TestPlanValidator.validate(plan);
        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void reportsMultiplePlanProblemsWithoutExecution() {
        TestPlan plan = new TestPlan();
        plan.type = "SOAP";
        plan.baseUrl = "";
        TestStep step = new TestStep();
        step.action = "GETT";
        step.timeoutMs = 0;
        step.retryCount = -1;
        plan.steps.add(step);

        var result = TestPlanValidator.validate(plan);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Unsupported plan type")));
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("baseUrl is required")));
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("timeoutMs")));
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("retryCount")));
    }

    @Test
    void warnsWhenApiPathIsBlank() {
        TestPlan plan = new TestPlan();
        plan.type = "API";
        plan.baseUrl = "https://example.test";
        TestStep step = new TestStep();
        step.action = "GET";
        plan.steps.add(step);

        var result = TestPlanValidator.validate(plan);
        assertTrue(result.valid());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("API path is blank")));
    }

    @Test
    void nullPlanIsInvalid() {
        var result = TestPlanValidator.validate(null);
        assertFalse(result.valid());
        assertEquals("Test plan cannot be null", result.errors().get(0));
    }
}
