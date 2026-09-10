package com.thiyagarajan.agent.config;

import com.thiyagarajan.agent.runtime.SecurityRedactor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves ${secret.NAME} placeholders from process environment without exposing secret values. */
public final class SecretManager {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{secret\\.([A-Za-z0-9_.-]+)}");
    private final Map<String, String> secrets;

    public SecretManager() { this(System.getenv()); }

    public SecretManager(Map<String, String> source) {
        this.secrets = new LinkedHashMap<>();
        if (source != null) source.forEach((k, v) -> { if (k != null && v != null) secrets.put(k, v); });
    }

    public String resolve(String expression) {
        if (expression == null) return null;
        Matcher m = PLACEHOLDER.matcher(expression);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String value = secrets.get(m.group(1));
            if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing secret: " + m.group(1));
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }

    public boolean containsSecretPlaceholder(String value) { return value != null && PLACEHOLDER.matcher(value).find(); }

    public String redact(String value) {
        if (value == null) return null;
        String result = value;
        for (String secret : secrets.values()) if (secret != null && !secret.isBlank()) result = result.replace(secret, "[REDACTED]");
        return SecurityRedactor.redactText(result);
    }

    public Map<String, String> safeSnapshot() {
        Map<String, String> result = new LinkedHashMap<>();
        secrets.keySet().forEach(k -> result.put(k, "[REDACTED]"));
        return result;
    }
}
