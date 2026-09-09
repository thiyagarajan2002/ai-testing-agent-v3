package com.thiyagarajan.agent.runtime;

import java.time.Instant;
import java.util.UUID;

/** Correlates one CLI execution, its tests and individual steps. */
public final class RunContext implements AutoCloseable {
    private static final ThreadLocal<RunContext> CURRENT = new ThreadLocal<>();

    private final String runId;
    private final Instant startedAt;

    private RunContext(String runId) {
        this.runId = runId;
        this.startedAt = Instant.now();
    }

    public static RunContext start() {
        RunContext context = new RunContext(newRunId());
        CURRENT.set(context);
        return context;
    }

    public static RunContext current() {
        return CURRENT.get();
    }

    public static String currentRunId() {
        RunContext context = CURRENT.get();
        return context == null ? "" : context.runId;
    }

    public static String newRunId() {
        return "run-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public String runId() { return runId; }
    public Instant startedAt() { return startedAt; }

    public String testId(int testIndex) {
        return runId + "-test-" + Math.max(1, testIndex + 1);
    }

    public String stepId(int testIndex, int stepIndex) {
        return testId(testIndex) + "-step-" + Math.max(1, stepIndex + 1);
    }

    @Override
    public void close() {
        if (CURRENT.get() == this) CURRENT.remove();
    }
}
