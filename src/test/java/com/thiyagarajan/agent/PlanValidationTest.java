package com.thiyagarajan.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.model.TestPlan;
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
    }
}
