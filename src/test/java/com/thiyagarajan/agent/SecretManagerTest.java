package com.thiyagarajan.agent;

import com.thiyagarajan.agent.config.SecretManager;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SecretManagerTest {
    @Test void resolvesAndRedactsSecrets() {
        SecretManager manager = new SecretManager(Map.of("API_TOKEN", "top-secret"));
        assertEquals("Bearer top-secret", manager.resolve("Bearer ${secret.API_TOKEN}"));
        assertEquals("Bearer [REDACTED]", manager.redact("Bearer top-secret"));
        assertTrue(manager.containsSecretPlaceholder("${secret.API_TOKEN}"));
        assertEquals("[REDACTED]", manager.safeSnapshot().get("API_TOKEN"));
    }

    @Test void missingSecretFailsClearly() {
        SecretManager manager = new SecretManager(Map.of());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> manager.resolve("${secret.MISSING}"));
        assertTrue(error.getMessage().contains("Missing secret: MISSING"));
    }
}
