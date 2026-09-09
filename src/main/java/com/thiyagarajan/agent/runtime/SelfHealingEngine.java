package com.thiyagarajan.agent.runtime;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Safe, deterministic UI locator healing with confidence thresholds and evidence. */
public final class SelfHealingEngine {
    public static final double DEFAULT_MIN_CONFIDENCE = 0.90;
    private SelfHealingEngine() { }

    public static HealingResult heal(Page page, String originalLocator) {
        return heal(page, originalLocator, DEFAULT_MIN_CONFIDENCE);
    }

    public static HealingResult heal(Page page, String originalLocator, double minConfidence) {
        if (page == null || originalLocator == null || originalLocator.isBlank()) {
            return new HealingResult(originalLocator, null, 0.0, "No page or locator supplied", List.of());
        }
        if (Double.isNaN(minConfidence) || minConfidence < 0.0 || minConfidence > 1.0) {
            throw new IllegalArgumentException("minConfidence must be between 0 and 1");
        }
        List<Evidence> evidence = new ArrayList<>();
        for (Candidate candidate : candidates(originalLocator)) {
            if (candidate.confidence() < minConfidence) continue;
            try {
                Locator locator = page.locator(candidate.locator());
                int count = locator.count();
                boolean visible = count == 1 && locator.isVisible();
                evidence.add(new Evidence(candidate.locator(), count, visible, candidate.confidence(), candidate.reason()));
                if (visible) {
                    return new HealingResult(originalLocator, candidate.locator(), candidate.confidence(), candidate.reason(), evidence);
                }
            } catch (Exception ignored) {
                evidence.add(new Evidence(candidate.locator(), -1, false, candidate.confidence(), "Candidate evaluation failed"));
            }
        }
        return new HealingResult(originalLocator, null, 0.0, "No unique visible candidate met the confidence threshold", evidence);
    }

    public static List<Candidate> candidates(String locator) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<Candidate> result = new ArrayList<>();
        if (locator == null || locator.isBlank()) return result;
        String trimmed = locator.trim();
        if (trimmed.startsWith("#") && trimmed.length() > 1) {
            String id = cssValue(trimmed.substring(1));
            add(result, seen, "[data-testid=\"" + id + "\"]", 0.96, "Same id expressed as data-testid");
            add(result, seen, "[name=\"" + id + "\"]", 0.90, "Same id expressed as name");
            add(result, seen, "[aria-label=\"" + id + "\"]", 0.84, "Same id expressed as aria-label");
        }
        if (trimmed.startsWith("[data-testid=") && trimmed.endsWith("]")) {
            String value = quotedAttributeValue(trimmed, "data-testid");
            if (value != null) add(result, seen, "[id=\"" + cssValue(value) + "\"]", 0.91, "Same test id expressed as id");
        }
        if (trimmed.startsWith("[name=") && trimmed.endsWith("]")) {
            String value = quotedAttributeValue(trimmed, "name");
            if (value != null) add(result, seen, "[id=\"" + cssValue(value) + "\"]", 0.88, "Same name expressed as id");
        }
        return result;
    }

    private static void add(List<Candidate> result, LinkedHashSet<String> seen, String locator, double confidence, String reason) {
        if (seen.add(locator)) result.add(new Candidate(locator, confidence, reason));
    }
    private static String quotedAttributeValue(String locator, String attribute) {
        String prefix = "[" + attribute + "=\"";
        if (!locator.startsWith(prefix) || !locator.endsWith("\"]")) return null;
        return locator.substring(prefix.length(), locator.length() - 2);
    }
    private static String cssValue(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }

    public record Candidate(String locator, double confidence, String reason) { }
    public record Evidence(String locator, int matchCount, boolean visible, double confidence, String reason) { }
    public record HealingResult(String originalLocator, String healedLocator, double confidence, String reason, List<Evidence> evidence) {
        public HealingResult(String originalLocator, String healedLocator, double confidence, String reason) {
            this(originalLocator, healedLocator, confidence, reason, List.of());
        }
        public boolean healed() { return healedLocator != null && !healedLocator.isBlank(); }
    }
}
