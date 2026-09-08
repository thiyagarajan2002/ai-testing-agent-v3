package com.thiyagarajan.agent.config;

import com.thiyagarajan.agent.runtime.AgentExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigValidationTest {
    private static Config valid(int parallelism) {
        return new Config("http://localhost:11434", "test", true, 1000, 0,
                parallelism, "reports", "screenshots");
    }

    @Test
    void rejectsInvalidTimeout() {
        AgentExecutionException ex = assertThrows(AgentExecutionException.class, () ->
                new Config("http://localhost:11434", "test", true, 0, 0, 1, "reports", "screenshots"));
        assertEquals(AgentExecutionException.Category.CONFIGURATION, ex.category());
    }

    @Test
    void rejectsNegativeRetries() {
        AgentExecutionException ex = assertThrows(AgentExecutionException.class, () ->
                new Config("http://localhost:11434", "test", true, 1000, -1, 1, "reports", "screenshots"));
        assertEquals(AgentExecutionException.Category.CONFIGURATION, ex.category());
    }

    @Test
    void rejectsBlankModel() {
        AgentExecutionException ex = assertThrows(AgentExecutionException.class, () ->
                new Config("http://localhost:11434", "", true, 1000, 0, 1, "reports", "screenshots"));
        assertEquals(AgentExecutionException.Category.CONFIGURATION, ex.category());
    }

    @Test
    void acceptsMaximumParallelism() {
        assertEquals(Config.MAX_PARALLELISM, valid(Config.MAX_PARALLELISM).parallelism());
    }

    @Test
    void rejectsParallelismAboveMaximum() {
        AgentExecutionException ex = assertThrows(AgentExecutionException.class, () -> valid(Config.MAX_PARALLELISM + 1));
        assertEquals(AgentExecutionException.Category.CONFIGURATION, ex.category());
    }
}
