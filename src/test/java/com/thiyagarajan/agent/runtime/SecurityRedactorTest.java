package com.thiyagarajan.agent.runtime;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecurityRedactorTest {
    @Test
    void redactsJsonSecrets() {
        String input = "{\"username\":\"demo\",\"password\":\"super-secret\",\"token\":\"abc123\"}";
        String output = SecurityRedactor.redactText(input);
        assertTrue(output.contains("\"username\":\"demo\""));
        assertFalse(output.contains("super-secret"));
        assertFalse(output.contains("abc123"));
        assertEquals(2, count(output, SecurityRedactor.MASK));
    }

    @Test
    void redactsAuthorizationAndKeyValueForms() {
        String input = "Authorization: Bearer eyJhbGciOi.test-token\napi_key=secret-value password=hunter2";
        String output = SecurityRedactor.redactText(input);
        assertFalse(output.contains("eyJhbGciOi.test-token"));
        assertFalse(output.contains("secret-value"));
        assertFalse(output.contains("hunter2"));
        assertTrue(output.contains(SecurityRedactor.MASK));
    }

    @Test
    void redactsSensitiveMapKeys() {
        String output = SecurityRedactor.redactMap(Map.of("Authorization", "Bearer secret-token", "Accept", "application/json", "password", "pw-value"));
        assertTrue(output.contains("Authorization=" + SecurityRedactor.MASK));
        assertTrue(output.contains("Accept=application/json"));
        assertTrue(output.contains("password=" + SecurityRedactor.MASK));
        assertFalse(output.contains("secret-token"));
        assertFalse(output.contains("pw-value"));
    }

    @Test
    void leavesOrdinaryTextUnchanged() {
        String input = "HTTP 200; response={\"id\":1,\"name\":\"Alice\"}";
        assertEquals(input, SecurityRedactor.redactText(input));
    }

    private int count(String text, String value) {
        int count = 0, index = 0;
        while ((index = text.indexOf(value, index)) >= 0) { count++; index += value.length(); }
        return count;
    }
}
