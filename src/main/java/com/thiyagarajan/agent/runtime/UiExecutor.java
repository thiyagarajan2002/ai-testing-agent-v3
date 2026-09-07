package com.thiyagarajan.agent.runtime;

import com.microsoft.playwright.*;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Executes UI plans and captures evidence after every successful step and on failure. */
public class UiExecutor {
    private final Config config;
    private final FailureArtifactManager artifacts;

    public UiExecutor() { this(Config.load()); }
    public UiExecutor(Config config) {
        this.config = config;
        this.artifacts = new FailureArtifactManager(config);
    }

    public ExecutionResult execute(TestPlan plan) {
        if (plan == null) throw new IllegalArgumentException("UI plan cannot be null");
        if (plan.steps == null || plan.steps.isEmpty()) throw new IllegalArgumentException("UI plan contains no steps");
        if (plan.baseUrl == null || plan.baseUrl.isBlank()) throw new IllegalArgumentException("UI baseUrl is required");
        ExecutionResult result = new ExecutionResult();
        result.testName = plan.name;
        result.passed = true;
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(config.headless()));
            Page page = browser.newPage();
            page.setDefaultTimeout(config.defaultTimeoutMs());
            for (int index = 0; index < plan.steps.size(); index++) {
                TestStep step = plan.steps.get(index);
                long start = System.currentTimeMillis();
                try {
                    if (step.timeoutMs > 0) page.setDefaultTimeout(step.timeoutMs);
                    String value = step.value == null ? "" : step.value;
                    String locator = step.locator == null ? "" : step.locator;
                    switch (step.action.toLowerCase()) {
                        case "navigate" -> page.navigate(resolve(plan.baseUrl, value));
                        case "click" -> page.locator(locator).click();
                        case "fill" -> page.locator(locator).fill(value);
                        case "press" -> page.locator(locator).press(value);
                        case "selectoption" -> page.locator(locator).selectOption(value);
                        case "assertvisible" -> { if (!page.locator(locator).isVisible()) throw new AssertionError("Not visible: " + locator); }
                        case "asserttext" -> { String actual = page.locator(locator).innerText(); if (!actual.contains(value)) throw new AssertionError("Expected text '" + value + "', actual='" + actual + "'"); }
                        case "assertvalue" -> { String actual = page.locator(locator).inputValue(); if (!actual.equals(value)) throw new AssertionError("Expected value '" + value + "', actual='" + actual + "'"); }
                        case "waitfor" -> page.waitForTimeout(Long.parseLong(value));
                        case "screenshot" -> { /* automatic evidence capture below makes this explicit action optional */ }
                        default -> throw new IllegalArgumentException("Unsupported UI action: " + step.action);
                    }
                    Path screenshot = captureStep(page, plan.name, index, step.action);
                    String details = "OK; screenshot=" + (screenshot == null ? "unavailable" : screenshot);
                    result.steps.add(new ExecutionResult.StepResult(step.action, true, details,
                            System.currentTimeMillis() - start,
                            screenshot == null ? List.of() : List.of(screenshot.toString())));
                } catch (Exception e) {
                    result.passed = false;
                    Path screenshot = captureFailure(page, plan.name, index);
                    Path metadata = artifacts.createFailureMetadata(plan.name, index, step.action, e.toString());
                    List<String> files = screenshot == null ? List.of(metadata.toString()) : List.of(screenshot.toString(), metadata.toString());
                    result.steps.add(new ExecutionResult.StepResult(step.action, false, e.toString(), System.currentTimeMillis() - start, files));
                    break;
                }
            }
            browser.close();
        } catch (Exception e) {
            result.passed = false;
            result.steps.add(new ExecutionResult.StepResult("browser-start", false, e.toString(), 0));
        }
        return result;
    }

    private String resolve(String base, String value) {
        if (value.startsWith("http://") || value.startsWith("https://")) return value;
        return base.replaceAll("/$", "") + "/" + value.replaceFirst("^/", "");
    }

    private Path captureStep(Page page, String testName, int index, String action) {
        try {
            Path dir = Path.of(config.reportsDir(), config.screenshotsDir(), "ui", FailureArtifactManager.safe(testName, "test"));
            Files.createDirectories(dir);
            String actionName = FailureArtifactManager.safe(action, "step");
            Path file = dir.resolve(String.format("%03d-%s.png", index + 1, actionName));
            page.screenshot(new Page.ScreenshotOptions().setPath(file).setFullPage(true));
            return file;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Path captureFailure(Page page, String testName, int index) {
        try {
            Path dir = Path.of(config.reportsDir(), config.screenshotsDir(), "ui", "failures");
            Files.createDirectories(dir);
            String name = FailureArtifactManager.safe(testName, "test") + "-step-" + (index + 1) + "-failure.png";
            Path file = dir.resolve(name);
            page.screenshot(new Page.ScreenshotOptions().setPath(file).setFullPage(true));
            return file;
        } catch (Exception ignored) {
            return null;
        }
    }
}
