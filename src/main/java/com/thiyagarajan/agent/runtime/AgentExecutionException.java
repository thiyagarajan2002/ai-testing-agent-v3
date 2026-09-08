package com.thiyagarajan.agent.runtime;

/** Standardized runtime failure for the agent execution pipeline. */
public class AgentExecutionException extends RuntimeException {
    public enum Category {
        CONFIGURATION,
        PLAN_VALIDATION,
        SUITE_VALIDATION,
        API_EXECUTION,
        UI_EXECUTION,
        ASSERTION,
        AI_GENERATION,
        REPORTING,
        INFRASTRUCTURE
    }

    private final Category category;

    public AgentExecutionException(Category category, String message) {
        super(message);
        this.category = requireCategory(category);
    }

    public AgentExecutionException(Category category, String message, Throwable cause) {
        super(message, cause);
        this.category = requireCategory(category);
    }

    public Category category() {
        return category;
    }

    private static Category requireCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Exception category cannot be null");
        }
        return category;
    }
}
