package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.thiyagarajan.agent.ai.AiProvider;
import com.thiyagarajan.agent.model.TestStep;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Resolves natural-language UI intent to a verified Playwright locator using live DOM evidence. */
public final class SemanticLocatorResolver {
    private static final int MAX_DOM_CHARS = 50000;
    private static final int MAX_TEXT_CHARS = 12000;
    private static final double MIN_CONFIDENCE = 0.50;
    private final AiProvider ai;
    private final ObjectMapper mapper;
    private final LocatorQualityEngine qualityEngine;

    public SemanticLocatorResolver(AiProvider ai, ObjectMapper mapper) {
        this(ai, mapper, new LocatorQualityEngine());
    }

    SemanticLocatorResolver(AiProvider ai, ObjectMapper mapper, LocatorQualityEngine qualityEngine) {
        if (ai == null) throw new IllegalArgumentException("AI provider cannot be null");
        this.ai = ai;
        this.mapper = mapper == null ? new ObjectMapper() : mapper;
        this.qualityEngine = qualityEngine == null ? new LocatorQualityEngine() : qualityEngine;
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
                Resolve ONE natural-language UI target to Playwright CSS locator candidates using ONLY the supplied live DOM and visible text.
                Return ONLY valid JSON:
                {"locator":"string","confidence":0.0,"reason":"string","alternatives":["string"]}

                Rules:
                1. Never invent an element or attribute absent from the evidence.
                2. Prefer stable attributes, accessible-name-like attributes (aria-label, title, name), labels, exact text, and structural relationships.
                3. Interpret ordinal intent such as "first video" from DOM order, not guesswork.
                4. Use the action to distinguish targets: fill needs an editable control; click needs an actionable element; assertions need the described content.
                5. Return CSS selectors only. Do not return XPath, Java, or markdown.
                6. If the target is ambiguous, return an empty locator and confidence 0 unless the evidence clearly supports candidates.
                7. Alternatives must also be evidence-backed CSS selectors and should be ordered from strongest to weakest.
                8. Confidence must be between 0 and 1.
                9. The framework will independently verify and rank every candidate against the live page.

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
        if (confidence < MIN_CONFIDENCE) throw new IllegalStateException("AI locator confidence below threshold: " + confidence + " for target: " + step.target);
        String reason = node.path("reason").asText("").trim();

        List<RankedCandidate> ranked = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            String candidate = candidates.get(i);
            if (!isCssCandidate(candidate)) continue;

            double candidateConfidence = Math.max(0.0, confidence - (i * 0.05));
            if (candidateConfidence < MIN_CONFIDENCE) continue;

            LocatorQualityEngine.Evaluation quality = qualityEngine.evaluate(page, candidate, step.action);
            if (!quality.usable()) continue;

            double rankScore = (quality.score() * 0.65) + (candidateConfidence * 0.35);
            ranked.add(new RankedCandidate(candidate, candidateConfidence, rankScore, quality, i));
        }

        ranked.sort(Comparator.comparingDouble(RankedCandidate::rankScore).reversed()
                .thenComparingInt(RankedCandidate::originalIndex));

        if (!ranked.isEmpty()) {
            RankedCandidate selected = ranked.get(0);
            List<String> remaining = ranked.stream().skip(1).map(RankedCandidate::locator).toList();
            String resolvedReason = reason.isBlank() ? "AI semantic match" : reason;
            resolvedReason += "; quality=" + selected.quality().quality()
                    + "; score=" + String.format(java.util.Locale.ROOT, "%.2f", selected.quality().score())
                    + "; " + selected.quality().reason();
            return new Resolution(selected.locator(), selected.confidence(), resolvedReason, remaining);
        }

        throw new IllegalStateException("AI could not resolve a verified UI target: " + step.target);
    }

    private boolean isCssCandidate(String candidate) {
        if (candidate == null || candidate.isBlank()) return false;
        String s = candidate.trim().toLowerCase();
        return !s.startsWith("/") && !s.startsWith("xpath=") && !s.startsWith("java ")
                && !s.contains("```") && !s.contains("page.locator(");
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

    private record RankedCandidate(
            String locator,
            double confidence,
            double rankScore,
            LocatorQualityEngine.Evaluation quality,
            int originalIndex
    ) { }

    public record Resolution(String locator, double confidence, String reason, List<String> alternatives) {
        public Resolution {
            alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
        }
    }
}
