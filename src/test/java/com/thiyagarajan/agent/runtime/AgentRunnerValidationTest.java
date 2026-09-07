package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentRunnerValidationTest {
    private final AgentRunner runner = new AgentRunner(null, new ObjectMapper());

    @Test
    void rejectsNullPlan() {
        AgentExecutionException ex = assertThrows(AgentExecutionException.class, () -> runner.execute(null));
        assertEquals(AgentExecutionException.Category.PLAN_VALIDATION, ex.category());
    }

    @Test
    void rejectsUnsupportedPlanType() {
        TestPlan plan = new TestPlan();
        plan.type = "UNKNOWN";
        plan.baseUrl = "https://example.com";
        plan.steps.add(step("GET"));

        AgentExecutionException ex = assertThrows(AgentExecutionException.class, () -> runner.execute(plan));
        assertEquals(AgentExecutionException.Category.PLAN_VALIDATION, ex.category());
    }

    @Test
    void rejectsInvalidApiAction() {
        TestPlan plan = new TestPlan();
        plan.type = "API";
        plan.baseUrl = "https://example.com";
        plan.steps.add(step("INVALID"));

        AgentExecutionException ex = assertThrows(AgentExecutionException.class, () -> runner.execute(plan));
        assertEquals(AgentExecutionException.Category.PLAN_VALIDATION, ex.category());
    }

    private TestStep step(String action) {
        TestStep step = new TestStep();
        step.action = action;
        return step;
    }
}
