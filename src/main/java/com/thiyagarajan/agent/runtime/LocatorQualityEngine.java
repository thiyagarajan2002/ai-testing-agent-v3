package com.thiyagarajan.agent.runtime;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.Locale;

/**
 * Deterministically evaluates a locator against the live page before execution.
 * AI may suggest candidates, but this engine decides whether a candidate is usable.
 */
public final class LocatorQualityEngine {

    public Evaluation evaluate(Page page, String selector, String action) {
        if (page == null) throw new IllegalArgumentException("Page cannot be null");
        if (selector == null || selector.isBlank()) return Evaluation.rejected(selector, "empty selector");

        try {
            Locator locator = page.locator(selector);
            int matches = locator.count();
            if (matches == 0) return Evaluation.rejected(selector, "selector matched no elements");

            Locator first = locator.first();
            boolean visible = first.isVisible();
            if (!visible) return new Evaluation(selector, 0.20, Quality.REJECTED, matches, false, false, false,
                    "matched element is not visible");

            boolean unique = matches == 1;
            boolean enabled = safelyEnabled(first);
            boolean actionCompatible = actionCompatible(first, action);
            boolean stable = looksStable(selector);

            double score = 0.35;
            score += unique ? 0.20 : 0.05;
            score += enabled ? 0.10 : 0.0;
            score += actionCompatible ? 0.20 : 0.0;
            score += stable ? 0.15 : 0.05;
            score = Math.min(1.0, score);

            Quality quality = score >= 0.90 ? Quality.EXCELLENT
                    : score >= 0.75 ? Quality.GOOD
                    : score >= 0.60 ? Quality.ACCEPTABLE
                    : score >= 0.50 ? Quality.WEAK
                    : Quality.REJECTED;

            String reason = "matches=" + matches
                    + ", visible=true"
                    + ", unique=" + unique
                    + ", enabled=" + enabled
                    + ", actionCompatible=" + actionCompatible
                    + ", stable=" + stable;

            return new Evaluation(selector, score, quality, matches, true, unique, actionCompatible, reason);
        } catch (Exception e) {
            return Evaluation.rejected(selector, "invalid selector: " + e.getClass().getSimpleName());
        }
    }

    private boolean safelyEnabled(Locator locator) {
        try {
            return locator.isEnabled();
        } catch (Exception ignored) {
            return true;
        }
    }

    private boolean actionCompatible(Locator locator, String action) {
        String normalized = action == null ? "" : action.toLowerCase(Locale.ROOT);
        String tag = safeAttribute(locator, "tagName");
        String type = safeAttribute(locator, "type");

        if (normalized.equals("fill")) {
            return tag.equals("input") || tag.equals("textarea") || safeAttribute(locator, "contenteditable").equals("true");
        }
        if (normalized.equals("check") || normalized.equals("uncheck")) {
            return tag.equals("input") && (type.equals("checkbox") || type.equals("radio"));
        }
        if (normalized.equals("selectoption")) {
            return tag.equals("select");
        }
        if (normalized.equals("click") || normalized.equals("press") || normalized.equals("hover")
                || normalized.startsWith("assert") || normalized.equals("waitfor")) {
            return true;
        }
        return true;
    }

    private String safeAttribute(Locator locator, String name) {
        try {
            if ("tagName".equals(name)) {
                Object value = locator.evaluate("el => el.tagName.toLowerCase()");
                return value == null ? "" : value.toString().toLowerCase(Locale.ROOT);
            }
            String value = locator.getAttribute(name);
            return value == null ? "" : value.toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean looksStable(String selector) {
        String s = selector.toLowerCase(Locale.ROOT);
        boolean semanticAttribute = s.contains("aria-label") || s.contains("name=") || s.contains("title=")
                || s.contains("data-testid") || s.contains("data-test") || s.contains("data-qa") || s.startsWith("#");
        boolean fragile = s.contains(":nth-child") || s.contains(":nth-of-type") || s.matches(".*\\.[a-z0-9_-]{12,}.*");
        return semanticAttribute && !fragile;
    }

    public enum Quality {
        EXCELLENT,
        GOOD,
        ACCEPTABLE,
        WEAK,
        REJECTED
    }

    public record Evaluation(
            String selector,
            double score,
            Quality quality,
            int matches,
            boolean visible,
            boolean unique,
            boolean actionCompatible,
            String reason
    ) {
        public static Evaluation rejected(String selector, String reason) {
            return new Evaluation(selector == null ? "" : selector, 0.0, Quality.REJECTED, 0, false, false, false, reason);
        }

        public boolean usable() {
            return visible && actionCompatible && quality != Quality.REJECTED;
        }
    }
}
