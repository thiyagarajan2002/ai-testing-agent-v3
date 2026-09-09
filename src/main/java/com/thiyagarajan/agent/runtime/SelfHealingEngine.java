package com.thiyagarajan.agent.runtime;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Safe, deterministic UI locator healing based only on selectors already implied by the page. */
public final class SelfHealingEngine {
    private SelfHealingEngine() { }

    public static HealingResult heal(Page page, String originalLocator) {
        if (page == null || originalLocator == null || originalLocator.isBlank()) {
            return new HealingResult(originalLocator, null, 0.0, "No page or locator supplied");
        }
        List<Candidate> candidates = candidates(originalLocator);
        for (Candidate candidate : candidates) {
            try {
                Locator locator = page.locator(candidate.locator());
                if (locator.count() == 1 && locator.isVisible()) {
                    return new HealingResult(originalLocator, candidate.locator(), candidate.confidence(), candidate.reason());
                }
            } catch (Exception ignored) {
                // An invalid candidate is rejected; healing never blocks the original failure path.
            }
        }
        return new HealingResult(originalLocator, null, 0.0, "No unique visible safe alternative found");
    }

    static List<Candidate> candidates(String locator) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<Candidate> result = new ArrayList<>();
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

    private static String cssValue(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record Candidate(String locator, double confidence, String reason) { }
    public record HealingResult(String originalLocator, String healedLocator, double confidence, String reason) {
        public boolean healed() { return healedLocator != null && !healedLocator.isBlank(); }
    }
}
