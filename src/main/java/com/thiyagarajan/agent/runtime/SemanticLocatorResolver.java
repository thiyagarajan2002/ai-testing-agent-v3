package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.thiyagarajan.agent.ai.AiProvider;
import com.thiyagarajan.agent.model.TestStep;

import java.util.ArrayList;
import java.util.List;

/** Resolves natural-language UI intent to a verified Playwright locator using live DOM evidence. */
public final class SemanticLocatorResolver {
    private static final int MAX_DOM_CHARS = 50000;
    private static final int MAX_TEXT_CHARS = 12000;
    private final AiProvider ai;
    private final ObjectMapper mapper;

    public SemanticLocatorResolver(AiProvider ai, ObjectMapper mapper) {
        if (ai == null) throw new IllegalArgumentException("AI provider cannot be null");
        this.ai = ai;
        this.mapper = mapper == null ? new ObjectMapper() : mapper;
    }

    public Resolution resolve(Page page, TestStep step) throws Exception {
        return resolve(page, step, List.of());
    }

    /** Resolves with previous semantic steps so references such as "this video" or "beside it" retain context. */
    public Resolution resolve(Page page, TestStep step, List<TestStep> previousSteps) throws Exception {
        if (page == null) throw new IllegalArgumentException("Page cannot be null");
        if (step == null) throw new IllegalArgumentException("UI step cannot be null");
        String explicit = step.locator == null ? "" : step.locator.trim();
        if (step.target == null || step.target.isBlank()) return new Resolution(explicit, 1.0, "explicit locator", List.of());

        String html = page.content();
        if (html.length() > MAX_DOM_CHARS) html = html.substring(0, MAX_DOM_CHARS);
        String visibleText;
        try { visibleText = page.locator("body").innerText(); }
        catch (Exception ignored) { visibleText = ""; }
        if (visibleText.length() > MAX_TEXT_CHARS) visibleText = visibleText.substring(0, MAX_TEXT_CHARS);

        String context = previousSteps == null ? "" : previousSteps.stream()
                .filter(s -> s != null && s.action != null)
                .map(s -> s.action + " target=" + safe(s.target) + " locator=" + safe(s.locator))
                .reduce((a, b) -> a + "\n" + b).orElse("");

        String prompt = """
                You are the semantic locator engine of a UI testing framework.
                Resolve ONE natural-language UI target to a Playwright CSS locator using ONLY the supplied live DOM and visible text.
                Return ONLY valid JSON:
                {"locator":"string","confidence":0.0,"reason":"string","alternatives":["string"]}

                Rules:
                1. Never invent an element or attribute absent from the evidence.
                2. Prefer stable attributes, accessible-name-like attributes (aria-label, title, name), labels, exact text, and structural relationships.
                3. Interpret ordinal intent such as "first video" from the DOM order, not from guesswork.
                4. Use the action to distinguish targets: fill needs an editable control; click needs an actionable element; assertions need the described content.
                5. Return a CSS selector that Playwright can execute. Do not return XPath, Java, or markdown.
                6. If the target is ambiguous, return the strongest candidate only when evidence clearly supports it; otherwise return empty locator and confidence 0.
                7. Alternatives must also be evidence-backed CSS selectors.
                8. Confidence must be between 0 and 1.

                ACTION: """ + safe(step.action) + "\nTARGET: " + safe(step.target)
                + "\nPREVIOUS STEPS:\n" + context
                + "\nVISIBLE TEXT:\n" + visibleText
                + "\nLIVE DOM:\n" + html;

        JsonNode node = mapper.readTree(cleanJson(ai.generate(prompt)));
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, node.path("locator").asText(""));
        JsonNode alternatives = node.path("alternatives");
        if (alternatives.isArray()) for (JsonNode item : alternatives) addCandidate(candidates, item.asText(""));

        double confidence = node.path("confidence").asDouble(0.0);
        if (!Double.isFinite(confidence)) confidence = 0.0;
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        String reason = node.path("reason").asText("").trim();

        for (int i = 0; i < candidates.size(); i++) {
            String candidate = candidates.get(i);
            try {
                Locator locator = page.locator(candidate);
                int count = locator.count();
                if (count > 0) {
                    boolean visible = locator.first().isVisible();
                    if (visible) {
                        double selectedConfidence = i == 0 ? confidence : Math.max(0.0, confidence - (i * 0.05));
                        return new Resolution(candidate, selectedConfidence, reason.isBlank() ? "AI semantic match verified against live DOM" : reason, List.copyOf(candidates.subList(i + 1, candidates.size())));
                    }
                }
            } catch (Exception ignored) { }
        }
        throw new IllegalStateException("AI could not resolve a verified UI target: " + step.target);
    }

    private void addCandidate(List<String> candidates, String value) {
        String candidate = value == null ? "" : value.trim();
        if (!candidate.isBlank() && !candidates.contains(candidate)) candidates.add(candidate);
    }

    private String cleanJson(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalStateException("AI returned an empty locator response");
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```(?:json)?\\s*", "");
            s = s.replaceFirst("\\s*```$", "");
        }
        int first = s.indexOf('{'), last = s.lastIndexOf('}');
        return first >= 0 && last > first ? s.substring(first, last + 1) : s;
    }

    private String safe(String value) { return value == null ? "" : SecurityRedactor.redactText(value); }

    public record Resolution(String locator, double confidence, String reason, List<String> alternatives) {
        public Resolution {
            alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
        }
    }
}
