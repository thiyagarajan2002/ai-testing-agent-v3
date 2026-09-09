package com.thiyagarajan.agent.runtime;

import com.thiyagarajan.agent.ai.FailureIntelligence;

/** Bounded retry policy with deterministic exponential backoff. */
public final class AdaptiveRetryPolicy {
    public static final int DEFAULT_BASE_DELAY_MS = 250;
    public static final int DEFAULT_MAX_DELAY_MS = 4000;

    private AdaptiveRetryPolicy() { }

    public static boolean shouldRetry(FailureIntelligence.Analysis analysis, int retryNumber, int maxRetries) {
        if (analysis == null || retryNumber < 1 || maxRetries < 1 || retryNumber > maxRetries) return false;
        return analysis.recommendation() == FailureIntelligence.RetryRecommendation.RETRY
                || analysis.recommendation() == FailureIntelligence.RetryRecommendation.RETRY_WITH_BACKOFF;
    }

    public static long delayMs(int retryNumber) {
        return delayMs(retryNumber, DEFAULT_BASE_DELAY_MS, DEFAULT_MAX_DELAY_MS);
    }

    public static long delayMs(int retryNumber, int baseDelayMs, int maxDelayMs) {
        if (retryNumber < 1) throw new IllegalArgumentException("retryNumber must be at least 1");
        if (baseDelayMs < 0) throw new IllegalArgumentException("baseDelayMs cannot be negative");
        if (maxDelayMs < baseDelayMs) throw new IllegalArgumentException("maxDelayMs must be >= baseDelayMs");
        long multiplier = 1L << Math.min(retryNumber - 1, 30);
        return Math.min(maxDelayMs, baseDelayMs * multiplier);
    }

    public static void sleep(int retryNumber) throws InterruptedException {
        Thread.sleep(delayMs(retryNumber));
    }
}
