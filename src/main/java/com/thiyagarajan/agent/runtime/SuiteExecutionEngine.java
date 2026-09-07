package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestSuite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Executes independent test plans sequentially or in parallel with failure isolation. */
public final class SuiteExecutionEngine {
    private final ObjectMapper mapper;
    private final AgentRunner runner;
    private final int parallelism;

    public SuiteExecutionEngine(ObjectMapper mapper, AgentRunner runner, int parallelism) {
        if (mapper == null) throw new IllegalArgumentException("ObjectMapper cannot be null");
        if (runner == null) throw new IllegalArgumentException("AgentRunner cannot be null");
        if (parallelism < 1) throw new IllegalArgumentException("Parallelism must be at least 1");
        this.mapper = mapper;
        this.runner = runner;
        this.parallelism = parallelism;
    }

    public SuiteExecutionResult execute(TestSuite suite, Path suiteDirectory) throws Exception {
        validateSuite(suite, suiteDirectory);
        long started = System.currentTimeMillis();
        SuiteExecutionResult suiteResult = new SuiteExecutionResult();
        suiteResult.suiteName = suite.name;

        List<Callable<SuiteExecutionResult.TestExecution>> tasks = new ArrayList<>();
        for (int i = 0; i < suite.plans.size(); i++) {
            final int index = i;
            final String planFile = suite.plans.get(i);
            tasks.add(() -> executeOne(index, planFile, suiteDirectory));
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(parallelism, tasks.size()));
        try {
            List<Future<SuiteExecutionResult.TestExecution>> futures = executor.invokeAll(tasks);
            for (Future<SuiteExecutionResult.TestExecution> future : futures) {
                try {
                    suiteResult.tests.add(future.get());
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    suiteResult.tests.add(failedInfrastructureResult(suiteResult.tests.size(), "unknown", cause));
                }
            }
        } finally {
            executor.shutdown();
        }

        suiteResult.tests.sort(java.util.Comparator.comparingInt(t -> t.index));
        suiteResult.totalTests = suiteResult.tests.size();
        suiteResult.passedTests = (int) suiteResult.tests.stream().filter(t -> "PASS".equals(t.status)).count();
        suiteResult.failedTests = suiteResult.totalTests - suiteResult.passedTests;
        suiteResult.status = suiteResult.failedTests == 0 ? "PASS" : "FAIL";
        suiteResult.durationMs = System.currentTimeMillis() - started;
        suiteResult.completedAt = Instant.now().toString();
        return suiteResult;
    }

    private SuiteExecutionResult.TestExecution executeOne(int index, String planFile, Path suiteDirectory) throws Exception {
        Path resolved = resolvePlan(suiteDirectory, planFile);
        long started = System.currentTimeMillis();
        TestPlan plan = mapper.readValue(Files.readString(resolved), TestPlan.class);
        ExecutionResult result = runner.execute(plan);
        if (!result.passed()) {
            try {
                runner.analyzeFailure(plan, result);
            } catch (Exception e) {
                result.failureAnalysis("AI failure analysis unavailable: " + e.getMessage());
            }
        }
        return new SuiteExecutionResult.TestExecution(index, planFile, plan.name,
                result.passed() ? "PASS" : "FAIL", System.currentTimeMillis() - started, result);
    }

    private SuiteExecutionResult.TestExecution failedInfrastructureResult(int index, String planFile, Throwable error) {
        ExecutionResult result = new ExecutionResult();
        result.testName = planFile;
        result.passed = false;
        result.failureAnalysis = "Suite execution error: " + error;
        result.steps.add(new ExecutionResult.StepResult("suite-execution", false, error.toString(), 0));
        return new SuiteExecutionResult.TestExecution(index, planFile, planFile, "FAIL", 0, result);
    }

    private void validateSuite(TestSuite suite, Path suiteDirectory) {
        if (suite == null) throw new IllegalArgumentException("Suite cannot be null");
        if (suite.plans == null || suite.plans.isEmpty()) throw new IllegalArgumentException("Suite contains no plans");
        if (suiteDirectory == null) throw new IllegalArgumentException("Suite directory is required");
        for (String planFile : suite.plans) {
            if (planFile == null || planFile.isBlank()) throw new IllegalArgumentException("Suite contains a blank plan path");
            resolvePlan(suiteDirectory, planFile);
        }
    }

    private Path resolvePlan(Path suiteDirectory, String planFile) {
        Path root = suiteDirectory.toAbsolutePath().normalize();
        Path resolved = root.resolve(planFile).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("Plan path escapes suite directory: " + planFile);
        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) throw new IllegalArgumentException("Plan file not found: " + resolved);
        return resolved;
    }
}
