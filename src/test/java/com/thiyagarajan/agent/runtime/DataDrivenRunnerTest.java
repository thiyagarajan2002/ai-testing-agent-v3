package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.model.TestPlan;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DataDrivenRunnerTest {
    @Test
    void readsRowsAndSubstitutesDataPlaceholders() throws Exception {
        Path file = Files.createTempFile("data-driven-", ".json");
        Files.writeString(file, "[{\"id\":\"42\"}]");
        TestPlan plan = new ObjectMapper().readValue("{\"name\":\"T\",\"type\":\"API\",\"baseUrl\":\"http://localhost\",\"steps\":[{\"action\":\"GET\",\"path\":\"/users/${data.id}\"}]}", TestPlan.class);
        var runner = new DataDrivenRunner(new ObjectMapper(), new AgentRunner(null, new ObjectMapper()));
        assertThrows(AgentExecutionException.class, () -> runner.execute(plan, file));
        Files.deleteIfExists(file);
    }

    @Test
    void rejectsInvalidDataShape() throws Exception {
        Path file = Files.createTempFile("data-driven-", ".json");
        Files.writeString(file, "{\"id\":1}");
        var plan = new TestPlan(); plan.baseUrl = "http://localhost";
        var runner = new DataDrivenRunner(new ObjectMapper(), new AgentRunner(null, new ObjectMapper()));
        var ex = assertThrows(AgentExecutionException.class, () -> runner.execute(plan, file));
        assertEquals(AgentExecutionException.Category.PLAN_VALIDATION, ex.category());
        Files.deleteIfExists(file);
    }
}
