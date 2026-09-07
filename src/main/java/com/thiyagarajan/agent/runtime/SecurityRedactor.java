package com.thiyagarajan.agent.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Centralized redaction for credentials and sensitive values written to logs, reports, and AI prompts. */
public final class SecurityRedactor {
    public static final String MASK = "***REDACTED***";

    private static final Pattern JSON_SECRET = Pattern.compile(
            "(?i)(\\\"(?:password|passwd|pwd|secret|token|access_token|refresh_token|id_token|api[_-]?key|client[_-]?secret|authorization|cookie|set-cookie)\\\"\\s*:\\s*\\\")(.*?)(\\\")");
    private static final Pattern HEADER_SECRET = Pattern.compile(
            "(?im)(\\b(?:authorization|proxy-authorization|x-api-key|x-auth-token|x-access-token|cookie|set-cookie)\\s*[:=]\\s*)([^\\r\\n,;]+)");
    private static final Pattern KEY_VALUE_SECRET = Pattern.compile(
            "(?i)(\\b(?:password|passwd|pwd|secret|token|access[_-]?token|refresh[_-]?token|id[_-]?token|api[_-]?key|client[_-]?secret)\\s*[=:]\\s*)([^\\s,;&]+)");
    private static final Pattern BEARER = Pattern.compile("(?i)(\\bBearer\\s+)[A-Za-z0-9._~+/=-]+");
    private static final Pattern BASIC = Pattern.compile("(?i)(\\bBasic\\s+)[A-Za-z0-9+/=]+");

    private SecurityRedactor() {}

    public static String redactText(String input) {
        if (input == null || input.isEmpty()) return input;
        String out = input;
        out = JSON_SECRET.matcher(out).replaceAll("$1" + MASK + "$3");
        out = HEADER_SECRET.matcher(out).replaceAll("$1" + MASK);
        out = KEY_VALUE_SECRET.matcher(out).replaceAll("$1" + MASK);
        out = BEARER.matcher(out).replaceAll("$1" + MASK);
        out = BASIC.matcher(out).replaceAll("$1" + MASK);
        return redactEnvironmentSecrets(out);
    }

    /** Redacts values from explicitly supplied maps, preserving keys and structure. */
    public static String redactMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) return "{}";
        List<String> entries = new ArrayList<>();
        values.forEach((key, value) -> entries.add(key + "=" + (isSensitiveKey(key) ? MASK : redactText(value))));
        return String.join(", ", entries);
    }

    public static boolean isSensitiveKey(String key) {
        if (key == null) return false;
        String normalized = key.toLowerCase().replaceAll("[^a-z0-9]", "");
        return normalized.contains("password") || normalized.contains("passwd") || normalized.contains("secret")
                || normalized.equals("token") || normalized.contains("accesstoken") || normalized.contains("refreshtoken")
                || normalized.contains("idtoken") || normalized.contains("apikey") || normalized.contains("clientsecret")
                || normalized.equals("authorization") || normalized.equals("cookie") || normalized.equals("setcookie");
    }

    private static String redactEnvironmentSecrets(String text) {
        String out = text;
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!isSensitiveKey(key) || value == null || value.length() < 6) continue;
            out = out.replace(value, MASK);
        }
        return out;
    }
}
