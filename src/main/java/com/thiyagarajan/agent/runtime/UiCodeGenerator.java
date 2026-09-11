package com.thiyagarajan.agent.runtime;

import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Generates readable Java + Playwright source from an AI plan after locator resolution. */
public final class UiCodeGenerator {
    public String generate(TestPlan plan, String className) {
        if (plan == null || plan.steps == null || plan.steps.isEmpty()) {
            throw new IllegalArgumentException("UI plan contains no steps");
        }
        String safeClass = className == null || className.isBlank()
                ? "GeneratedUiTest"
                : className.replaceAll("[^A-Za-z0-9_]", "_");
        if (safeClass.isBlank() || !Character.isJavaIdentifierStart(safeClass.charAt(0))) {
            safeClass = "GeneratedUiTest";
        }

        List<String> methodNames = uniqueMethodNames(plan.steps);

        StringBuilder out = new StringBuilder();
        out.append("import com.microsoft.playwright.*;\n")
           .append("import org.junit.jupiter.api.*;\n\n")
           .append("public class ").append(safeClass).append(" {\n")
           .append("    private Playwright playwright;\n")
           .append("    private Browser browser;\n")
           .append("    private Page page;\n\n")
           .append("    @BeforeEach\n")
           .append("    void setUp() {\n")
           .append("        playwright = Playwright.create();\n")
           .append("        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));\n")
           .append("        page = browser.newPage();\n")
           .append("    }\n\n")
           .append("    @AfterEach\n")
           .append("    void tearDown() {\n")
           .append("        if (browser != null) browser.close();\n")
           .append("        if (playwright != null) playwright.close();\n")
           .append("    }\n\n")
           .append("    @Test\n")
           .append("    void generatedScenario() {\n");

        for (String methodName : methodNames) {
            out.append("        ").append(methodName).append("();\n");
        }
        out.append("    }\n\n");

        for (int i = 0; i < plan.steps.size(); i++) {
            appendMethod(out, plan, plan.steps.get(i), methodNames.get(i));
        }
        out.append("}\n");
        return out.toString();
    }

    private void appendMethod(StringBuilder out, TestPlan plan, TestStep step, String methodName) {
        String action = step.action == null ? "" : step.action.toLowerCase(Locale.ROOT);
        String locator = step.locator == null ? "" : step.locator;
        String value = step.value == null ? "" : step.value;

        out.append("    private void ").append(methodName).append("() {\n");
        switch (action) {
            case "navigate" -> out.append("        page.navigate(\"")
                    .append(java(resolveUrl(plan.baseUrl, value)))
                    .append("\");\n");
            case "click" -> appendLocatorAction(out, locator, "click()", step);
            case "fill" -> appendLocatorAction(out, locator, "fill(\"" + java(value) + "\")", step);
            case "press" -> appendLocatorAction(out, locator, "press(\"" + java(value) + "\")", step);
            case "selectoption" -> appendLocatorAction(out, locator, "selectOption(\"" + java(value) + "\")", step);
            case "hover" -> appendLocatorAction(out, locator, "hover()", step);
            case "check" -> appendLocatorAction(out, locator, "check()", step);
            case "uncheck" -> appendLocatorAction(out, locator, "uncheck()", step);
            case "assertvisible" -> appendAssertTrue(out, locator, "isVisible()", step);
            case "asserttext" -> appendAssertTrue(out, locator,
                    "innerText().contains(\"" + java(value) + "\")", step);
            case "assertvalue" -> appendAssertEquals(out, locator, value, step);
            case "asserttitle" -> out.append("        Assertions.assertTrue(page.title().contains(\"")
                    .append(java(value)).append("\"));\n");
            case "asserturl" -> out.append("        Assertions.assertTrue(page.url().contains(\"")
                    .append(java(value)).append("\"));\n");
            case "waitfor" -> out.append("        page.waitForTimeout(")
                    .append(java(value)).append(");\n");
            case "screenshot" -> out.append("        page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get(\"screenshot.png\")));\n");
            default -> out.append("        throw new IllegalStateException(\"Unsupported generated action: ")
                    .append(java(step.action)).append("\");\n");
        }
        out.append("    }\n\n");
    }

    private void appendLocatorAction(StringBuilder out, String locator, String expression, TestStep step) {
        requireLocator(locator, step);
        out.append("        page.locator(\"")
                .append(java(locator))
                .append("\").")
                .append(expression)
                .append(";\n");
    }

    private void appendAssertTrue(StringBuilder out, String locator, String expression, TestStep step) {
        requireLocator(locator, step);
        out.append("        Assertions.assertTrue(page.locator(\"")
                .append(java(locator))
                .append("\").")
                .append(expression)
                .append(");\n");
    }

    private void appendAssertEquals(StringBuilder out, String locator, String expected, TestStep step) {
        requireLocator(locator, step);
        out.append("        Assertions.assertEquals(\"")
                .append(java(expected))
                .append("\", page.locator(\"")
                .append(java(locator))
                .append("\").inputValue());\n");
    }

    private void requireLocator(String locator, TestStep step) {
        if (locator == null || locator.isBlank()) {
            throw new IllegalStateException(
                    "Cannot generate UI code without a resolved locator for target: " + step.target);
        }
    }

    private List<String> uniqueMethodNames(List<TestStep> steps) {
        Map<String, Integer> counts = new HashMap<>();
        List<String> result = new ArrayList<>(steps.size());
        for (TestStep step : steps) {
            String base = methodName(step);
            int occurrence = counts.merge(base, 1, Integer::sum);
            result.add(occurrence == 1 ? base : base + occurrence);
        }
        return result;
    }

    private String methodName(TestStep step) {
        String raw = (step.action == null ? "step" : step.action)
                + " " + (step.target == null ? "" : step.target);
        String[] parts = raw.replaceAll("[^A-Za-z0-9]+", " ").trim().split(" +");
        StringBuilder b = new StringBuilder();
        for (String p : parts) {
            if (!p.isBlank()) {
                b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
        }
        String name = b.length() == 0 ? "Step" : b.toString();
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            name = "Step" + name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private String resolveUrl(String base, String value) {
        if (value == null || value.isBlank()) return base == null ? "" : base;
        if (value.startsWith("http://") || value.startsWith("https://")) return value;
        if (base == null || base.isBlank()) return value;
        return base.replaceAll("/$", "") + "/" + value.replaceFirst("^/", "");
    }

    private String java(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
