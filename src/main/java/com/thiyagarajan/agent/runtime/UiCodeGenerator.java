package com.thiyagarajan.agent.runtime;

import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;

import java.util.Locale;

/** Generates readable Java + Playwright source from an AI test plan with resolved locators. */
public final class UiCodeGenerator {
    public String generate(TestPlan plan, String className) {
        if (plan == null || plan.steps == null || plan.steps.isEmpty()) throw new IllegalArgumentException("UI plan contains no steps");
        String safeClass = className == null || className.isBlank() ? "GeneratedUiTest" : className.replaceAll("[^A-Za-z0-9_]", "_");
        if (!Character.isJavaIdentifierStart(safeClass.charAt(0))) safeClass = "GeneratedUiTest";
        StringBuilder out = new StringBuilder();
        out.append("import com.microsoft.playwright.*;\n")
           .append("import org.junit.jupiter.api.*;\n\n")
           .append("public class ").append(safeClass).append(" {\n")
           .append("    private Playwright playwright;\n    private Browser browser;\n    private Page page;\n\n")
           .append("    @BeforeEach\n    void setUp() {\n")
           .append("        playwright = Playwright.create();\n        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));\n        page = browser.newPage();\n    }\n\n")
           .append("    @AfterEach\n    void tearDown() {\n        if (browser != null) browser.close();\n        if (playwright != null) playwright.close();\n    }\n\n")
           .append("    @Test\n    void generatedScenario() {\n");
        for (TestStep step : plan.steps) {
            out.append("        ").append(methodName(step)).append("();\n");
        }
        out.append("    }\n\n");
        for (TestStep step : plan.steps) appendMethod(out, step);
        out.append("}\n");
        return out.toString();
    }

    private void appendMethod(StringBuilder out, TestStep step) {
        String action = step.action == null ? "" : step.action.toLowerCase(Locale.ROOT);
        String locator = step.locator == null ? "" : step.locator;
        String value = step.value == null ? "" : step.value;
        out.append("    private void ").append(methodName(step)).append("() {\n");
        switch (action) {
            case "navigate" -> out.append("        page.navigate(\"").append(java(planUrl(step))).append("\");\n");
            case "click" -> out.append("        page.locator(\"").append(java(locator)).append("\").click();\n");
            case "fill" -> out.append("        page.locator(\"").append(java(locator)).append("\").fill(\"").append(java(value)).append("\");\n");
            case "press" -> out.append("        page.locator(\"").append(java(locator)).append("\").press(\"").append(java(value)).append("\");\n");
            case "selectoption" -> out.append("        page.locator(\"").append(java(locator)).append("\").selectOption(\"").append(java(value)).append("\");\n");
            case "hover" -> out.append("        page.locator(\"").append(java(locator)).append("\").hover();\n");
            case "check" -> out.append("        page.locator(\"").append(java(locator)).append("\").check();\n");
            case "uncheck" -> out.append("        page.locator(\"").append(java(locator)).append("\").uncheck();\n");
            case "assertvisible" -> out.append("        Assertions.assertTrue(page.locator(\"").append(java(locator)).append("\").isVisible());\n");
            case "asserttext" -> out.append("        Assertions.assertTrue(page.locator(\"").append(java(locator)).append("\").innerText().contains(\"").append(java(value)).append("\"));\n");
            case "asserttitle" -> out.append("        Assertions.assertTrue(page.title().contains(\"").append(java(value)).append("\"));\n");
            case "asserturl" -> out.append("        Assertions.assertTrue(page.url().contains(\"").append(java(value)).append("\"));\n");
            case "waitfor" -> out.append("        page.waitForTimeout(").append(java(value)).append(");\n");
            case "screenshot" -> out.append("        page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get(\"screenshot.png\")));\n");
            default -> out.append("        // Unsupported generated action: ").append(java(step.action)).append("\n");
        }
        out.append("    }\n\n");
    }

    private String methodName(TestStep step) {
        String action = step.action == null ? "step" : step.action;
        String target = step.target == null ? "" : step.target;
        String raw = action + " " + target;
        String[] parts = raw.replaceAll("[^A-Za-z0-9]+", " ").trim().split(" +");
        StringBuilder b = new StringBuilder();
        for (String p : parts) if (!p.isBlank()) b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        String name = b.length() == 0 ? "Step" : b.toString();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private String planUrl(TestStep step) { return step.path == null || step.path.isBlank() ? "" : step.path; }
    private String java(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"); }
}
