package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.config.Config;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TestOrchestratorTest {
    private static Config config(Path root) {
        return new Config("http://localhost:11434", "test", true, 1000, 0, 1,
                root.resolve("reports").toString(), root.resolve("screenshots").toString());
    }

    @Test
    void rejectsNullDependencies() {
        Path root = Path.of("target", "orchestrator-test");
        AgentExecutionException configError = assertThrows(AgentExecutionException.class,
                () -> new TestOrchestrator(null, new ObjectMapper(), null));
        assertEquals(AgentExecutionException.Category.CONFIGURATION, configError.category());

        AgentExecutionException mapperError = assertThrows(AgentExecutionException.class,
                () -> new TestOrchestrator(config(root), null, null));
        assertEquals(AgentExecutionException.Category.CONFIGURATION, mapperError.category());
    }

    @Test
    void missingPlanIsCategorizedBeforeExecution() throws Exception {
        Path root = Files.createTempDirectory("orchestrator-");
        try (TestOrchestrator orchestrator = new TestOrchestrator(config(root), new ObjectMapper(), null)) {
            AgentExecutionException error = assertThrows(AgentExecutionException.class,
                    () -> orchestrator.executePlan(root.resolve("missing.json").toString()));
            assertEquals(AgentExecutionException.Category.PLAN_VALIDATION, error.category());
        }
    }

    @Test
    void missingSuiteIsCategorizedBeforeExecution() throws Exception {
        Path root = Files.createTempDirectory("orchestrator-");
        try (TestOrchestrator orchestrator = new TestOrchestrator(config(root), new ObjectMapper(), null)) {
            AgentExecutionException error = assertThrows(AgentExecutionException.class,
                    () -> orchestrator.executeSuite(root.resolve("missing-suite.json").toString()));
            assertEquals(AgentExecutionException.Category.SUITE_VALIDATION, error.category());
        }
    }
}
