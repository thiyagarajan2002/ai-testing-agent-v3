package com.thiyagarajan.agent.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.runtime.SecurityRedactor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Loads reusable JSON test data and resolves {{variables}} without mutating source data. */
public final class TestDataManager {
    private final ObjectMapper mapper;
    public TestDataManager() { this(new ObjectMapper().findAndRegisterModules()); }
    public TestDataManager(ObjectMapper mapper) { this.mapper = Objects.requireNonNull(mapper, "ObjectMapper cannot be null"); }

    public List<Map<String, String>> load(Path file) throws Exception {
        if (file == null || !Files.isRegularFile(file)) throw new IllegalArgumentException("Data file does not exist: " + file);
        Object value = mapper.readValue(file.toFile(), Object.class);
        if (!(value instanceof List<?> list)) throw new IllegalArgumentException("Test data JSON must contain an array");
        List<Map<String, String>> result = new ArrayList<>();
        for (Object row : list) {
            if (!(row instanceof Map<?, ?> raw)) throw new IllegalArgumentException("Each test data row must be an object");
            Map<String, String> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : raw.entrySet()) normalized.put(String.valueOf(e.getKey()), e.getValue() == null ? "" : String.valueOf(e.getValue()));
            result.add(Collections.unmodifiableMap(normalized));
        }
        return result;
    }

    public String resolve(String template, Map<String, String> data) {
        if (template == null || template.isEmpty()) return template == null ? "" : template;
        Map<String, String> values = data == null ? Map.of() : data;
        var matcher = java.util.regex.Pattern.compile("\\{\\{([^{}]+)}}").matcher(template);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).trim(); String value = values.get(key);
            if (value == null) throw new IllegalArgumentException("Missing test data variable: " + key);
            matcher.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(SecurityRedactor.redactText(value)));
        }
        matcher.appendTail(out); return out.toString();
    }

    public Map<String, String> copy(Map<String, String> row) { return row == null ? new LinkedHashMap<>() : new LinkedHashMap<>(row); }
}
