package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.config.EnvironmentManager;
import com.thiyagarajan.agent.model.EnvironmentProfile;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestSuite;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Executes independent test plans sequentially or in parallel with failure isolation. */
public final class SuiteExecutionEngine {
    private final ObjectMapper mapper;
    private final AgentRunner runner;
    private final int parallelism;
    private final EnvironmentManager environments;
    private final EnvironmentProfile profile;

    public SuiteExecutionEngine(ObjectMapper mapper, AgentRunner runner, int parallelism) {
        this(mapper, runner, parallelism, null, null);
    }

    public SuiteExecutionEngine(ObjectMapper mapper, AgentRunner runner, int parallelism,
                                EnvironmentManager environments, EnvironmentProfile profile) {
        if (mapper == null) throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "ObjectMapper cannot be null");
        if (runner == null) throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "Runner cannot be null");
        if (parallelism < 1) throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "Parallelism must be at least 1");
        this.mapper = mapper;
        this.runner = runner;
        this.parallelism = parallelism;
        this.environments = environments;
        this.profile = profile;
    }

    public SuiteExecutionResult execute(TestSuite suite, Path suiteDirectory) throws Exception {
        validateSuite(suite, suiteDirectory);
        long started = System.currentTimeMillis();
        SuiteExecutionResult out = new SuiteExecutionResult();
        out.suiteName = suite.name;

        List<Callable<SuiteExecutionResult.TestExecution>> tasks = new ArrayList<>();
        for (int i = 0; i < suite.plans.size(); i++) {
            final int index = i;
            final String file = suite.plans.get(i);
            tasks.add(() -> executeSafely(index, file, suiteDirectory));
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(parallelism, tasks.size()));
        try {
            List<Future<SuiteExecutionResult.TestExecution>> futures = executor.invokeAll(tasks);
            for (int i = 0; i < futures.size(); i++) {
                try {
                    out.tests.add(futures.get(i).get());
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    out.tests.add(failedInfrastructureResult(i, suite.plans.get(i), cause));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentExecutionException(AgentExecutionException.Category.INFRASTRUCTURE, "Suite execution was interrupted", e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) executor.shutdownNow();
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        out.tests.sort(Comparator.comparingInt(t -> t.index));
        out.totalTests = out.tests.size();
        out.passedTests = (int) out.tests.stream().filter(t -> "PASS".equals(t.status)).count();
        out.failedTests = out.totalTests - out.passedTests;
        out.status = out.failedTests == 0 ? "PASS" : "FAIL";
        out.durationMs = System.currentTimeMillis() - started;
        out.completedAt = Instant.now().toString();
        return out;
    }

    private SuiteExecutionResult.TestExecution executeSafely(int index, String planFile, Path dir) {
        long started = System.currentTimeMillis();
        try {
            return executeOne(index, planFile, dir, started);
        } catch (Exception e) {
            return failedInfrastructureResult(index, planFile, e, System.currentTimeMillis() - started);
        }
    }

    private SuiteExecutionResult.TestExecution executeOne(int index, String planFile, Path dir, long started) throws Exception {
        Path resolved = resolvePlan(dir, planFile);
        TestPlan plan;
        try {
            plan = mapper.readValue(Files.readString(resolved), TestPlan.class);
        } catch (Exception e) {
            throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Invalid test plan JSON: " + resolved, e);
        }
        if (environments != null && profile != null) environments.apply(plan, profile);
        ExecutionResult result = runner.execute(plan);
        if (!result.passed()) {
            try { runner.analyzeFailure(plan, result); }
            catch (Exception e) { result.failureAnalysis("AI failure analysis unavailable: " + e.getMessage()); }
        }
        return new SuiteExecutionResult.TestExecution(index, planFile, plan.name,
                result.passed() ? "PASS" : "FAIL", System.currentTimeMillis() - started, result);
    }

    private SuiteExecutionResult.TestExecution failedInfrastructureResult(int index, String file, Throwable e) {
        return failedInfrastructureResult(index, file, e, 0);
    }

    private SuiteExecutionResult.TestExecution failedInfrastructureResult(int index, String file, Throwable e, long durationMs) {
        ExecutionResult result = new ExecutionResult();
        result.testName = file;
        result.passed = false;
        String message = e.getMessage() == null ? e.toString() : e.getMessage();
        result.failureAnalysis("Suite execution error [" + categoryOf(e) + "]: " + message);
        result.steps.add(new ExecutionResult.StepResult("suite-execution", false, message, durationMs));
        return new SuiteExecutionResult.TestExecution(index, file, file, "FAIL", durationMs, result);
    }

    private AgentExecutionException.Category categoryOf(Throwable e) {
        return e instanceof AgentExecutionException a ? a.category() : AgentExecutionException.Category.INFRASTRUCTURE;
    }

    private void validateSuite(TestSuite suite, Path dir) {
        if (suite == null) throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Suite cannot be null");
        if (suite.plans == null || suite.plans.isEmpty()) throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Suite contains no plans");
        if (dir == null) throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Suite directory is required");
        for (String file : suite.plans) {
            if (file == null || file.isBlank()) throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Suite contains a blank plan path");
            resolvePlan(dir, file);
        }
    }

    /**
     * Resolves a plan relative to the suite directory.
     * The parent of the suite directory is the suite workspace, so sibling
     * directories such as ../plans are valid while paths escaping that workspace
     * remain blocked. This works for both the repository examples/suites layout
     * and isolated temporary test workspaces.
     */
    private Path resolvePlan(Path dir, String file) {
        Path root = dir.toAbsolutePath().normalize();
        Path resolved = root.resolve(file).normalize();
        Path workspace = root.getParent() == null ? root : root.getParent();
        if (!resolved.startsWith(workspace)) {
            throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION,
                    "Plan path escapes suite workspace: " + file);
        }
        if (!Files.isRegularFile(resolved)) {
            throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION,
                    "Plan file not found: " + resolved);
        }
        return resolved;
    }
}
