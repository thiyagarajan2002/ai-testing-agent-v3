package com.thiyagarajan.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.runtime.AgentExecutionException;
import com.thiyagarajan.agent.runtime.AgentRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlanValidationTest {
    @Test
    void jacksonCanReadPlan() throws Exception {
        String json = """
        {
          "name":"Health Check",
          "type":"API",
          "baseUrl":"https://example.com",
          "steps":[
            {"action":"GET","path":"/health",
             "assertSpec":{"status":200}}
          ]
        }
        """;
        TestPlan plan = new ObjectMapper().readValue(json, TestPlan.class);
        assertEquals("Health Check", plan.name);
        assertEquals("GET", plan.steps.get(0).action);
        assertEquals(200, plan.steps.get(0).assertSpec.status);
        assertNull(plan.steps.get(0).retryCount);
    }

    @Test
    void jacksonReadsPerStepRetryCount() throws Exception {
        String json = """
        {
          "name":"Retry Check",
          "type":"API",
          "baseUrl":"https://example.com",
          "steps":[{"action":"GET","path":"/health","retryCount":3}]
        }
        """;
        TestPlan plan = new ObjectMapper().readValue(json, TestPlan.class);
        assertEquals(3, plan.steps.get(0).retryCount);
    }

    @Test
    void negativeRetryCountIsRejectedBeforeExecution() throws Exception {
        TestPlan plan = new ObjectMapper().readValue("""
        {
          "name":"Invalid Retry",
          "type":"API",
          "baseUrl":"https://example.com",
          "steps":[{"action":"GET","path":"/health","retryCount":-1}]
        }
        """, TestPlan.class);

        AgentRunner runner = new AgentRunner(null, new ObjectMapper());
        AgentExecutionException error = assertThrows(AgentExecutionException.class, () -> runner.execute(plan));
        assertEquals(AgentExecutionException.Category.PLAN_VALIDATION, error.category());
        assertEquals("retryCount cannot be negative", error.getMessage());
    }
}
