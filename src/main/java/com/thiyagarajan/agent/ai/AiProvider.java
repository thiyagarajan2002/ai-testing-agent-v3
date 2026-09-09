package com.thiyagarajan.agent.ai;

/** Minimal AI provider contract so intelligence features are provider-agnostic and testable. */
@FunctionalInterface
public interface AiProvider {
    String generate(String prompt) throws Exception;
}
