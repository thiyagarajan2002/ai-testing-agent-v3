package com.thiyagarajan.agent;

import com.thiyagarajan.agent.runtime.UiExecutor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UiExecutorValidationTest {
    @Test
    void rejectsNullConfig() {
        assertThrows(IllegalArgumentException.class, () -> new UiExecutor(null));
    }

    @Test
    void rejectsNullPlanBeforeStartingPlaywright() {
        UiExecutor executor = new UiExecutor();
        assertThrows(IllegalArgumentException.class, () -> executor.execute(null));
    }
}
