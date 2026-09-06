package com.thiyagarajan.agent.runtime;

import com.microsoft.playwright.*;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;

public class UiExecutor {
    public ExecutionResult execute(TestPlan plan) {
        ExecutionResult result = new ExecutionResult();
        result.testName = plan.name;
        result.passed = true;

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            for (TestStep step : plan.steps) {
                long start = System.currentTimeMillis();
                try {
                    switch (step.action) {
                        case "navigate" -> page.navigate(resolve(plan.baseUrl, step.value));
                        case "click" -> page.locator(step.locator).click();
                        case "fill" -> page.locator(step.locator).fill(step.value);
                        case "press" -> page.locator(step.locator).press(step.value);
                        case "selectOption" -> page.locator(step.locator).selectOption(step.value);
                        case "assertVisible" -> {
                            if (!page.locator(step.locator).isVisible())
                                throw new AssertionError("Not visible: " + step.locator);
                        }
                        case "assertText" -> {
                            String actual = page.locator(step.locator).innerText();
                            if (!actual.contains(step.value))
                                throw new AssertionError("Expected text '" + step.value
                                        + "', actual='" + actual + "'");
                        }
                        case "screenshot" -> page.screenshot(
                                new Page.ScreenshotOptions().setPath(java.nio.file.Path.of(
                                        "reports", safe(step.value, "screenshot.png"))));
                        default -> throw new IllegalArgumentException(
                                "Unsupported UI action: " + step.action);
                    }
                    result.steps.add(new ExecutionResult.StepResult(
                            step.action, true, "OK", System.currentTimeMillis() - start));
                } catch (Exception e) {
                    result.passed = false;
                    result.steps.add(new ExecutionResult.StepResult(
                            step.action, false, e.toString(), System.currentTimeMillis() - start));
                }
            }
            browser.close();
        } catch (Exception e) {
            result.passed = false;
            result.steps.add(new ExecutionResult.StepResult(
                    "browser-start", false, e.toString(), 0));
        }
        return result;
    }

    private String resolve(String base, String value) {
        if (value.startsWith("http://") || value.startsWith("https://")) return value;
        return base.replaceAll("/$", "") + "/" + value.replaceFirst("^/", "");
    }

    private String safe(String name, String fallback) {
        return name == null || name.isBlank() ? fallback : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
