package com.thiyagarajan.agent.ai;

import com.thiyagarajan.agent.runtime.ExecutionResult;

import java.util.Locale;

/** Deterministic failure classification used before optional LLM analysis/retry. */
public final class FailureIntelligence {
    public enum Category { ASSERTION, TIMEOUT, AUTHENTICATION, NETWORK, LOCATOR, VALIDATION, SERVER, UNKNOWN }
    public enum RetryRecommendation { RETRY, DO_NOT_RETRY, RETRY_WITH_BACKOFF, HEAL_LOCATOR }

    private FailureIntelligence() { }

    public static Analysis analyze(ExecutionResult result) {
        if (result == null || result.passed()) return new Analysis(Category.UNKNOWN, RetryRecommendation.DO_NOT_RETRY, "No failed execution to analyze");
        String text = String.valueOf(result.failureAnalysis()) + " " + result.steps.stream()
                .filter(s -> !s.passed)
                .map(s -> String.valueOf(s.details))
                .reduce("", (a, b) -> a + " " + b);
        String value = text.toLowerCase(Locale.ROOT);
        if (contains(value, "timeout", "timed out", "deadline"))
            return new Analysis(Category.TIMEOUT, RetryRecommendation.RETRY_WITH_BACKOFF, "Transient timing failure is a candidate for bounded backoff retry");
        if (contains(value, "401", "403", "unauthorized", "forbidden", "authentication", "credentials"))
            return new Analysis(Category.AUTHENTICATION, RetryRecommendation.DO_NOT_RETRY, "Authentication/authorization failures normally require credential or permission correction");
        if (contains(value, "locator", "not visible", "no element", "strict mode"))
            return new Analysis(Category.LOCATOR, RetryRecommendation.HEAL_LOCATOR, "Locator failure is a candidate for selector review or self-healing");
        if (contains(value, "connection", "connect", "socket", "dns", "eof", "reset by peer"))
            return new Analysis(Category.NETWORK, RetryRecommendation.RETRY_WITH_BACKOFF, "Network failures can be transient and should use bounded backoff");
        if (contains(value, "assert", "expected", "actual", "status code", "jsonpath", "xmlpath"))
            return new Analysis(Category.ASSERTION, RetryRecommendation.DO_NOT_RETRY, "Assertion failures usually represent product or expectation defects rather than transient failures");
        if (contains(value, " 500", " 502", " 503", " 504", "internal server", "bad gateway", "service unavailable"))
            return new Analysis(Category.SERVER, RetryRecommendation.RETRY_WITH_BACKOFF, "Transient server-side failure is a candidate for bounded retry");
        if (contains(value, "invalid", "validation", "required", "unsupported"))
            return new Analysis(Category.VALIDATION, RetryRecommendation.DO_NOT_RETRY, "Plan/input validation should be corrected before retrying");
        return new Analysis(Category.UNKNOWN, RetryRecommendation.DO_NOT_RETRY, "No safe automatic retry strategy was identified");
    }

    private static boolean contains(String text, String... terms) {
        for (String term : terms) if (text.contains(term)) return true;
        return false;
    }

    public record Analysis(Category category, RetryRecommendation recommendation, String rationale) { }
}
