package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.model.TestPlan;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataDrivenRunnerTest {
    @Test
    void readsRowsAndSubstitutesDataPlaceholders() throws Exception {
        Path file = Files.createTempFile("data-driven-", ".json");
        try {
            Files.writeString(file, "[{\"id\":\"42\"}]");
            TestPlan plan = new ObjectMapper().readValue(
                    "{\"name\":\"T\",\"type\":\"API\",\"baseUrl\":\"http://localhost\",\"steps\":[{\"action\":\"GET\",\"path\":\"/users/${data.id}\"}]}",
                    TestPlan.class);
            var runner = new DataDrivenRunner(new ObjectMapper(), new AgentRunner(null, new ObjectMapper()));

            DataDrivenExecutionResult result = runner.execute(plan, file);

            assertEquals(1, result.totalIterations);
            assertEquals(1, result.failedIterations);
            assertEquals(0, result.passedIterations);
            assertFalse(result.iterations.get(0).passed);
            assertNotNull(result.iterations.get(0).execution);
            assertEquals(1, result.iterations.get(0).execution.steps.size());
            assertTrue(result.iterations.get(0).execution.steps.get(0).details.contains("/users/42"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void rejectsInvalidDataShape() throws Exception {
        Path file = Files.createTempFile("data-driven-", ".json");
        try {
            Files.writeString(file, "{\"id\":1}");
            var plan = new TestPlan();
            plan.baseUrl = "http://localhost";
            var runner = new DataDrivenRunner(new ObjectMapper(), new AgentRunner(null, new ObjectMapper()));
            var ex = assertThrows(AgentExecutionException.class, () -> runner.execute(plan, file));
            assertEquals(AgentExecutionException.Category.PLAN_VALIDATION, ex.category());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void filterWithNoMatchingRowsIsRejected() throws Exception {
        Path file = Files.createTempFile("data-driven-filter-", ".json");
        try {
            Files.writeString(file, "[{\"id\":\"42\",\"env\":\"qa\"}]");
            var plan = new TestPlan();
            plan.baseUrl = "http://localhost";
            var runner = new DataDrivenRunner(new ObjectMapper(), new AgentRunner(null, new ObjectMapper()));
            var ex = assertThrows(AgentExecutionException.class,
                    () -> runner.execute(plan, file, Map.of("env", "prod")));
            assertEquals(AgentExecutionException.Category.PLAN_VALIDATION, ex.category());
            assertTrue(ex.getMessage().contains("No dataset rows matched"));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
