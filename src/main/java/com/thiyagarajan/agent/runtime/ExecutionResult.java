package com.thiyagarajan.agent.runtime;

import java.util.ArrayList;
import java.util.List;

public class ExecutionResult {
    public String testName;
    public boolean passed;
    public String failureAnalysis = "";
    public List<StepResult> steps = new ArrayList<>();

    public boolean passed() { return passed; }
    public String failureAnalysis() { return failureAnalysis; }
    public void failureAnalysis(String value) { this.failureAnalysis = SecurityRedactor.redactText(value); }

    public static class StepResult {
        public String action;
        public boolean passed;
        public String details;
        public long durationMs;
        public List<String> artifacts = new ArrayList<>();

        public StepResult(String action, boolean passed, String details, long durationMs) {
            this.action = action;
            this.passed = passed;
            this.details = SecurityRedactor.redactText(details);
            this.durationMs = durationMs;
        }

        public StepResult(String action, boolean passed, String details, long durationMs, List<String> artifacts) {
            this(action, passed, details, durationMs);
            if (artifacts != null) this.artifacts.addAll(artifacts);
        }
    }
}
