package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.thiyagarajan.agent.ai.AiProvider;
import com.thiyagarajan.agent.model.TestStep;

import java.util.ArrayList;
import java.util.List;

/** Resolves natural-language UI targets to Playwright locators using live DOM evidence. */
public final class SemanticLocatorResolver {
    private static final int MAX_DOM_CHARS = 60000;
    private final AiProvider ai;
    private final ObjectMapper mapper;

    public SemanticLocatorResolver(AiProvider ai, ObjectMapper mapper) {
        if (ai == null) throw new IllegalArgumentException("AI provider cannot be null");
        this.ai = ai;
        this.mapper = mapper == null ? new ObjectMapper() : mapper;
    }

    public Resolution resolve(Page page, TestStep step) throws Exception {
        if (page == null) throw new IllegalArgumentException("Page cannot be null");
        if (step == null) throw new IllegalArgumentException("UI step cannot be null");
        if (step.target == null || step.target.isBlank()) {
            return new Resolution(step.locator == null ? "" : step.locator, 1.0, "explicit locator", List.of());
        }
        String html = page.content();
        if (html.length() > MAX_DOM_CHARS) html = html.substring(0, MAX_DOM_CHARS);
        String prompt = """
                You are a UI automation locator resolver.
                Resolve ONE natural-language UI target to a Playwright CSS locator using ONLY evidence in the supplied DOM.
                Return ONLY valid JSON:
                {"locator":"string","confidence":0.0,"reason":"string","alternatives":["string"]}

                Rules:
                1. Never invent an element absent from the DOM.
                2. Prefer stable semantic attributes, accessible names, ids, names, labels, and exact text.
                3. The locator must be valid Playwright CSS.
                4. Confidence must be between 0 and 1.
                5. If no reliable target exists, return an empty locator and confidence 0.

                ACTION: """ + escape(step.action) + "\nTARGET: " + escape(step.target) + "\nDOM:\n" + html;
        JsonNode node = mapper.readTree(cleanJson(ai.generate(prompt)));
        String locator = node.path("locator").asText("").trim();
        double confidence = node.path("confidence").asDouble(0.0);
        if (!Double.isFinite(confidence)) confidence = 0.0;
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        String reason = node.path("reason").asText("").trim();
        List<String> alternatives = new ArrayList<>();
        JsonNode alt = node.path("alternatives");
        if (alt.isArray()) for (JsonNode item : alt) if (item.isTextual() && !item.asText().isBlank()) alternatives.add(item.asText());
        if (locator.isBlank()) throw new IllegalStateException("AI could not resolve UI target: " + step.target);
        return new Resolution(locator, confidence, reason, List.copyOf(alternatives));
    }

    private String cleanJson(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalStateException("AI returned an empty locator response");
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```(?:json)?\\s*", "");
            s = s.replaceFirst("\\s*```$", "");
        }
        int first = s.indexOf('{');
        int last = s.lastIndexOf('}');
        return first >= 0 && last > first ? s.substring(first, last + 1) : s;
    }

    private String escape(String value) { return value == null ? "" : value.replace("\"", "\\\""); }

    public record Resolution(String locator, double confidence, String reason, List<String> alternatives) { }
}
