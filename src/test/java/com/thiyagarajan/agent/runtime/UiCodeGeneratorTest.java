package com.thiyagarajan.agent.runtime;

import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UiCodeGeneratorTest {
    @Test
    void generatesResolvedLocatorsAndSemanticMethodNames() {
        TestPlan plan = new TestPlan();
        plan.type = "UI";
        plan.baseUrl = "https://www.youtube.com";

        TestStep navigate = step("navigate", "", "");
        navigate.value = "https://www.youtube.com";
        TestStep fill = step("fill", "search input", "java tutorial");
        fill.locator = "input[name='search_query']";
        TestStep press = step("press", "search input", "Enter");
        press.locator = "input[name='search_query']";
        TestStep click = step("click", "first video", "");
        click.locator = "ytd-video-renderer a#video-title";

        plan.steps.add(navigate);
        plan.steps.add(fill);
        plan.steps.add(press);
        plan.steps.add(click);

        String source = new UiCodeGenerator().generate(plan, "YoutubeGeneratedTest");

        assertTrue(source.contains("public class YoutubeGeneratedTest"));
        assertTrue(source.contains("page.navigate(\"https://www.youtube.com\")"));
        assertTrue(source.contains("page.locator(\"input[name='search_query']\").fill(\"java tutorial\")"));
        assertTrue(source.contains("page.locator(\"input[name='search_query']\").press(\"Enter\")"));
        assertTrue(source.contains("page.locator(\"ytd-video-renderer a#video-title\").click()"));
        assertTrue(source.contains("fillSearchInput();"));
        assertTrue(source.contains("pressSearchInput();"));
        assertTrue(source.contains("clickFirstVideo();"));
    }

    @Test
    void doesNotEmitNaturalLanguageTargetAsLocator() {
        TestPlan plan = new TestPlan();
        plan.type = "UI";
        plan.baseUrl = "https://example.com";
        TestStep step = step("click", "Login button", "");
        step.locator = "button[type='submit']";
        plan.steps.add(step);

        String source = new UiCodeGenerator().generate(plan, "GeneratedUiTest");

        assertTrue(source.contains("button[type='submit']"));
        assertFalse(source.contains("page.locator(\"Login button\")"));
    }

    private TestStep step(String action, String target, String value) {
        TestStep step = new TestStep();
        step.action = action;
        step.target = target;
        step.value = value;
        return step;
    }
}
