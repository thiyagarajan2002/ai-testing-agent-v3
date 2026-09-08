package com.thiyagarajan.agent.runtime;

import com.microsoft.playwright.*;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Executes UI plans with resilient Playwright waits, assertions and evidence. */
public class UiExecutor {
    private final Config config;
    private final FailureArtifactManager artifacts;

    public UiExecutor() { this(Config.load()); }
    public UiExecutor(Config config) {
        if (config == null) throw new IllegalArgumentException("Config cannot be null");
        this.config = config;
        this.artifacts = new FailureArtifactManager(config);
    }

    public ExecutionResult execute(TestPlan plan) {
        validate(plan);
        ExecutionResult result = new ExecutionResult();
        result.testName = plan.name;
        result.passed = true;
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(config.headless()));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
            Page page = context.newPage();
            page.setDefaultTimeout(config.defaultTimeoutMs());
            try {
                for (int index = 0; index < plan.steps.size(); index++) {
                    TestStep step = plan.steps.get(index);
                    long start = System.currentTimeMillis();
                    try {
                        executeStep(page, plan, step);
                        Path screenshot = captureStep(page, plan.name, index, step.action);
                        result.steps.add(new ExecutionResult.StepResult(step.action, true,
                                "OK; screenshot=" + (screenshot == null ? "unavailable" : screenshot),
                                System.currentTimeMillis() - start,
                                screenshot == null ? List.of() : List.of(screenshot.toString())));
                    } catch (Exception e) {
                        result.passed = false;
                        Path screenshot = captureFailure(page, plan.name, index);
                        Path metadata = artifacts.createFailureMetadata(plan.name, index, step.action, e.toString());
                        List<String> files = screenshot == null ? List.of(metadata.toString()) : List.of(screenshot.toString(), metadata.toString());
                        result.steps.add(new ExecutionResult.StepResult(step.action, false,
                                SecurityRedactor.redactText(e.toString()), System.currentTimeMillis() - start, files));
                        break;
                    }
                }
            } finally {
                context.close();
                browser.close();
            }
        } catch (Exception e) {
            result.passed = false;
            result.steps.add(new ExecutionResult.StepResult("browser-start", false,
                    SecurityRedactor.redactText(e.toString()), 0));
        }
        return result;
    }

    private void validate(TestPlan plan) {
        if (plan == null) throw new IllegalArgumentException("UI plan cannot be null");
        if (plan.steps == null || plan.steps.isEmpty()) throw new IllegalArgumentException("UI plan contains no steps");
        if (plan.baseUrl == null || plan.baseUrl.isBlank()) throw new IllegalArgumentException("UI baseUrl is required");
        for (TestStep step : plan.steps) {
            if (step == null || step.action == null || step.action.isBlank())
                throw new IllegalArgumentException("UI step action is required");
        }
    }

    private void executeStep(Page page, TestPlan plan, TestStep step) {
        if (step.timeoutMs > 0) page.setDefaultTimeout(step.timeoutMs);
        String action = step.action.toLowerCase(Locale.ROOT);
        String value = step.value == null ? "" : step.value;
        String locator = step.locator == null ? "" : step.locator;
        Locator target = locator.isBlank() ? null : page.locator(locator);
        switch (action) {
            case "navigate" -> page.navigate(resolve(plan.baseUrl, value), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            case "click" -> { requireLocator(action, target); target.click(); }
            case "fill" -> { requireLocator(action, target); target.fill(value); }
            case "press" -> { requireLocator(action, target); target.press(value); }
            case "selectoption" -> { requireLocator(action, target); target.selectOption(value); }
            case "hover" -> { requireLocator(action, target); target.hover(); }
            case "check" -> { requireLocator(action, target); target.check(); }
            case "uncheck" -> { requireLocator(action, target); target.uncheck(); }
            case "assertvisible" -> { requireLocator(action, target); if (!target.isVisible()) throw new AssertionError("Not visible: " + locator); }
            case "asserttext" -> { requireLocator(action, target); assertContains(target.innerText(), value, "text"); }
            case "assertvalue" -> { requireLocator(action, target); assertEquals(target.inputValue(), value, "value"); }
            case "asserttitle" -> assertContains(page.title(), value, "title");
            case "asserturl" -> assertContains(page.url(), value, "URL");
            case "waitfor" -> page.waitForTimeout(Long.parseLong(value));
            case "waitforvisible" -> { requireLocator(action, target); target.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)); }
            case "waitforhidden" -> { requireLocator(action, target); target.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN)); }
            case "screenshot" -> { }
            default -> throw new IllegalArgumentException("Unsupported UI action: " + step.action);
        }
    }

    private void requireLocator(String action, Locator target) {
        if (target == null) throw new IllegalArgumentException("Locator is required for UI action: " + action);
    }
    private void assertContains(String actual, String expected, String field) {
        if (actual == null || !actual.contains(expected)) throw new AssertionError("Expected " + field + " to contain '" + expected + "', actual='" + actual + "'");
    }
    private void assertEquals(String actual, String expected, String field) {
        if (!expected.equals(actual)) throw new AssertionError("Expected " + field + " '" + expected + "', actual='" + actual + "'");
    }
    private String resolve(String base, String value) {
        if (value.startsWith("http://") || value.startsWith("https://")) return value;
        return base.replaceAll("/$", "") + "/" + value.replaceFirst("^/", "");
    }
    private Path captureStep(Page page, String testName, int index, String action) {
        try {
            Path dir = Path.of(config.reportsDir(), config.screenshotsDir(), "ui", FailureArtifactManager.safe(testName, "test"));
            Files.createDirectories(dir);
            Path file = dir.resolve(String.format("%03d-%s.png", index + 1, FailureArtifactManager.safe(action, "step")));
            page.screenshot(new Page.ScreenshotOptions().setPath(file).setFullPage(true));
            return file;
        } catch (Exception ignored) { return null; }
    }
    private Path captureFailure(Page page, String testName, int index) {
        try {
            Path dir = Path.of(config.reportsDir(), config.screenshotsDir(), "ui", "failures");
            Files.createDirectories(dir);
            Path file = dir.resolve(FailureArtifactManager.safe(testName, "test") + "-step-" + (index + 1) + "-failure.png");
            page.screenshot(new Page.ScreenshotOptions().setPath(file).setFullPage(true));
            return file;
        } catch (Exception ignored) { return null; }
    }
}
