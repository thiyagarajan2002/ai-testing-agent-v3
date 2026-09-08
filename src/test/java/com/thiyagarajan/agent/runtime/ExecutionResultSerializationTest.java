package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionResultSerializationTest {

    @Test
    void stepResultsCanBeDeserializedFromPersistedJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

        ExecutionResult original = new ExecutionResult();
        original.testName = "history-test";
        original.passed = true;
        original.steps.add(new ExecutionResult.StepResult(
                "GET /users", true, "200 OK", 125, List.of("response.json")));

        String json = mapper.writeValueAsString(original);
        ExecutionResult restored = mapper.readValue(json, ExecutionResult.class);

        assertEquals("history-test", restored.testName);
        assertTrue(restored.passed);
        assertEquals(1, restored.steps.size());
        assertEquals("GET /users", restored.steps.get(0).action);
        assertTrue(restored.steps.get(0).passed);
        assertEquals("200 OK", restored.steps.get(0).details);
        assertEquals(125, restored.steps.get(0).durationMs);
        assertEquals(List.of("response.json"), restored.steps.get(0).artifacts);
    }
}
