package com.thiyagarajan.agent.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExecutionResult {
    public String runId = "";
    public String testId = "";
    public String testName;
    public boolean passed;
    public String failureAnalysis = "";
    public List<StepResult> steps = new ArrayList<>();

    public ExecutionResult() {
        runId = RunContext.currentRunId();
        if (!runId.isBlank()) testId = runId + "-test-1";
    }

    /** Attach the current run identity while keeping deserialization backward compatible. */
    public void ensureIdentity() {
        if (runId.isBlank()) runId = RunContext.currentRunId();
        if (testId.isBlank() && !runId.isBlank()) testId = runId + "-test-1";
        for (StepResult step : steps) {
            if (step != null && step.stepId.isBlank()) step.stepId = newStepId();
        }
    }

    private static String newStepId() {
        return "step-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public boolean passed() { return passed; }
    public String failureAnalysis() { return failureAnalysis; }
    public void failureAnalysis(String value) { this.failureAnalysis = SecurityRedactor.redactText(value); }

    public static class StepResult {
        public String stepId = "";
        public String action;
        public boolean passed;
        public String details;
        public long durationMs;
        public List<String> artifacts = new ArrayList<>();

        public StepResult() {
            // Required by Jackson when loading persisted execution history.
        }

        public StepResult(String action, boolean passed, String details, long durationMs) {
            this.action = action;
            this.passed = passed;
            this.details = SecurityRedactor.redactText(details);
            this.durationMs = durationMs;
            this.stepId = newStepId();
        }

        public StepResult(String action, boolean passed, String details, long durationMs, List<String> artifacts) {
            this(action, passed, details, durationMs);
            if (artifacts != null) this.artifacts.addAll(artifacts);
        }

        public StepResult withStepId(String value) {
            this.stepId = value == null ? "" : value;
            return this;
        }
    }
}
