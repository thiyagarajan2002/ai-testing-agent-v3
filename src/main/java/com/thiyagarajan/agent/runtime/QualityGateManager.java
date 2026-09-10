package com.thiyagarajan.agent.runtime;

import java.util.ArrayList;
import java.util.List;

/** Evaluates deterministic CI quality gates against a completed suite. */
public final class QualityGateManager {
    private QualityGateManager() {}

    public record Policy(double minPassRate, int maxFailures, long maxDurationMs, int maxRegressions) {
        public Policy {
            if (minPassRate < 0 || minPassRate > 100) throw new IllegalArgumentException("minPassRate must be 0..100");
            if (maxFailures < 0 || maxDurationMs < 0 || maxRegressions < 0) throw new IllegalArgumentException("Quality gate limits cannot be negative");
        }
    }

    public record Result(boolean passed, double passRate, List<String> failures) {
        public int failureCount() { return failures.size(); }
    }

    public static Result evaluate(SuiteExecutionResult suite, Policy policy, int regressions) {
        if (suite == null) throw new IllegalArgumentException("Suite result cannot be null");
        if (policy == null) throw new IllegalArgumentException("Quality gate policy cannot be null");
        List<String> failures = new ArrayList<>();
        double rate = suite.totalTests == 0 ? 0.0 : suite.passedTests * 100.0 / suite.totalTests;
        if (rate < policy.minPassRate()) failures.add(String.format("Pass rate %.2f%% is below %.2f%%", rate, policy.minPassRate()));
        if (suite.failedTests > policy.maxFailures()) failures.add("Failures " + suite.failedTests + " exceed " + policy.maxFailures());
        if (suite.durationMs > policy.maxDurationMs() && policy.maxDurationMs() > 0) failures.add("Duration " + suite.durationMs + "ms exceeds " + policy.maxDurationMs() + "ms");
        if (regressions > policy.maxRegressions()) failures.add("Regressions " + regressions + " exceed " + policy.maxRegressions());
        return new Result(failures.isEmpty(), rate, List.copyOf(failures));
    }
}
