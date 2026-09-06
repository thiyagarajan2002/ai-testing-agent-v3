package com.thiyagarajan.agent.runtime;

import com.microsoft.playwright.*;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;
import java.nio.file.Files;
import java.nio.file.Path;

public class UiExecutor {
    private final Config config;

    public UiExecutor() { this(Config.load()); }
    public UiExecutor(Config config) { this.config = config; }

    public ExecutionResult execute(TestPlan plan) {
        if (plan == null) throw new IllegalArgumentException("UI plan cannot be null");
        if (plan.steps == null || plan.steps.isEmpty()) throw new IllegalArgumentException("UI plan contains no steps");
        if (plan.baseUrl == null || plan.baseUrl.isBlank()) throw new IllegalArgumentException("UI baseUrl is required");
        ExecutionResult result = new ExecutionResult(); result.testName = plan.name; result.passed = true;
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(config.headless()));
            Page page = browser.newPage(); page.setDefaultTimeout(config.defaultTimeoutMs());
            for (TestStep step : plan.steps) {
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
                        case "asserttext" -> { String actual=page.locator(locator).innerText(); if (!actual.contains(value)) throw new AssertionError("Expected text '"+value+"', actual='"+actual+"'"); }
                        case "assertvalue" -> { String actual=page.locator(locator).inputValue(); if (!actual.equals(value)) throw new AssertionError("Expected value '"+value+"', actual='"+actual+"'"); }
                        case "waitfor" -> page.waitForTimeout(Long.parseLong(value));
                        case "screenshot" -> { Path dir=Path.of(config.reportsDir(), config.screenshotsDir()); Files.createDirectories(dir); page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(safe(value,"screenshot.png")))); }
                        default -> throw new IllegalArgumentException("Unsupported UI action: " + step.action);
                    }
                    result.steps.add(new ExecutionResult.StepResult(step.action,true,"OK",System.currentTimeMillis()-start));
                } catch (Exception e) {
                    result.passed=false; captureFailure(page,result.steps.size()); result.steps.add(new ExecutionResult.StepResult(step.action,false,e.toString(),System.currentTimeMillis()-start)); break;
                }
            }
            browser.close();
        } catch (Exception e) { result.passed=false; result.steps.add(new ExecutionResult.StepResult("browser-start",false,e.toString(),0)); }
        return result;
    }
    private String resolve(String base,String value){if(value.startsWith("http://")||value.startsWith("https://"))return value;return base.replaceAll("/$","")+"/"+value.replaceFirst("^/","");}
    private void captureFailure(Page page,int index){try{Path dir=Path.of(config.reportsDir(),config.screenshotsDir());Files.createDirectories(dir);page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve("failure-"+index+".png")));}catch(Exception ignored){}}
    private String safe(String name,String fallback){return name==null||name.isBlank()?fallback:name.replaceAll("[^a-zA-Z0-9._-]","_");}
}
