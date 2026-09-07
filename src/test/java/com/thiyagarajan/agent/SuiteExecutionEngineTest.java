package com.thiyagarajan.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.ai.OllamaClient;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestSuite;
import com.thiyagarajan.agent.runtime.AgentExecutionException;
import com.thiyagarajan.agent.runtime.AgentRunner;
import com.thiyagarajan.agent.runtime.ExecutionResult;
import com.thiyagarajan.agent.runtime.SuiteExecutionEngine;
import com.thiyagarajan.agent.runtime.SuiteExecutionResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SuiteExecutionEngineTest {
    @Test
    void executesSuiteInParallelAndAggregatesResults() throws Exception {
        Path dir = Files.createTempDirectory("suite-engine-");
        writePlan(dir.resolve("one.json"), "one");
        writePlan(dir.resolve("two.json"), "two");
        writePlan(dir.resolve("three.json"), "three-fail");

        TestSuite suite = new TestSuite();
        suite.name = "Parallel Smoke";
        suite.plans.add("one.json");
        suite.plans.add("two.json");
        suite.plans.add("three.json");

        AgentRunner runner = new AgentRunner((OllamaClient) null, new ObjectMapper()) {
            @Override
            public ExecutionResult execute(TestPlan plan) {
                ExecutionResult result = new ExecutionResult();
                result.testName = plan.name;
                result.passed = !plan.name.endsWith("fail");
                result.steps.add(new ExecutionResult.StepResult("mock", result.passed, result.passed ? "OK" : "failed", 1));
                return result;
            }
        };

        SuiteExecutionResult result = new SuiteExecutionEngine(new ObjectMapper(), runner, 2).execute(suite, dir);

        assertEquals(3, result.totalTests);
        assertEquals(2, result.passedTests);
        assertEquals(1, result.failedTests);
        assertEquals("FAIL", result.status);
        assertEquals(3, result.tests.size());
        assertEquals("one", result.tests.get(0).testName);
        assertEquals("two", result.tests.get(1).testName);
        assertEquals("three-fail", result.tests.get(2).testName);
    }

    @Test
    void rejectsPlanPathOutsideSuiteWorkspace() throws Exception {
        Path dir = Files.createTempDirectory("suite-path-");

        TestSuite suite = new TestSuite();
        suite.plans.add("../../outside-suite-plan.json");

        AgentRunner runner = new AgentRunner((OllamaClient) null, new ObjectMapper());
        AgentExecutionException error = assertThrows(AgentExecutionException.class,
                () -> new SuiteExecutionEngine(new ObjectMapper(), runner, 2).execute(suite, dir));
        assertEquals(AgentExecutionException.Category.SUITE_VALIDATION, error.category());
        assertTrue(error.getMessage().contains("escapes suite workspace"));
    }

    @Test
    void allowsPlanInSiblingDirectoryWithinSuiteWorkspace() throws Exception {
        Path workspace = Files.createTempDirectory("suite-workspace-");
        Path suiteDir = Files.createDirectories(workspace.resolve("suites"));
        Path plansDir = Files.createDirectories(workspace.resolve("plans"));
        writePlan(plansDir.resolve("one.json"), "one");

        TestSuite suite = new TestSuite();
        suite.plans.add("../plans/one.json");

        AgentRunner runner = new AgentRunner((OllamaClient) null, new ObjectMapper()) {
            @Override
            public ExecutionResult execute(TestPlan plan) {
                ExecutionResult result = new ExecutionResult();
                result.testName = plan.name;
                result.passed = true;
                return result;
            }
        };

        SuiteExecutionResult result = new SuiteExecutionEngine(new ObjectMapper(), runner, 1).execute(suite, suiteDir);
        assertEquals("PASS", result.status);
        assertEquals("one", result.tests.get(0).testName);
    }

    private void writePlan(Path path, String name) throws Exception {
        Files.writeString(path, "{\"name\":\"" + name + "\",\"type\":\"API\",\"baseUrl\":\"http://localhost\",\"steps\":[{\"action\":\"GET\",\"path\":\"/\"}]}" );
    }
}
