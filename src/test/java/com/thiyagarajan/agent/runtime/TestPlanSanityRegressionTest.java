package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.ai.PromptManager;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Fast non-network sanity and regression checks for both API and locatorless UI flows. */
class TestPlanSanityRegressionTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void apiSanityPlanIsValid() {
        TestPlan plan = new TestPlan();
        plan.name = "API health sanity";
        plan.type = "API";
        plan.baseUrl = "https://api.example.test";
        TestStep get = step("GET");
        get.path = "/health";
        plan.steps.add(get);

        var result = TestPlanValidator.validate(plan);
        assertTrue(result.valid(), result.errorsAsMessage());
    }

    @Test
    void apiRegressionRejectsUiAction() {
        TestPlan plan = new TestPlan();
        plan.type = "API";
        plan.baseUrl = "https://api.example.test";
        plan.steps.add(step("click"));

        var result = TestPlanValidator.validate(plan);
        assertFalse(result.valid());
        assertTrue(result.errorsAsMessage().contains("invalid API action"));
    }

    @Test
    void uiSanityPlanCanBeLocatorlessWithSemanticTargets() {
        TestPlan plan = new TestPlan();
        plan.name = "YouTube search sanity";
        plan.type = "UI";
        plan.baseUrl = "https://www.youtube.com";

        plan.steps.add(step("navigate"));
        TestStep fill = step("fill");
        fill.target = "search input";
        fill.value = "java tutorial";
        plan.steps.add(fill);
        TestStep press = step("press");
        press.target = "search input";
        press.value = "Enter";
        plan.steps.add(press);
        TestStep click = step("click");
        click.target = "first video";
        plan.steps.add(click);

        var result = TestPlanValidator.validate(plan);
        assertTrue(result.valid(), result.errorsAsMessage());
        assertTrue(plan.steps.stream().filter(s -> s.action.equals("click") || s.action.equals("fill") || s.action.equals("press"))
                .allMatch(s -> s.locator.isBlank() && !s.target.isBlank()));
    }

    @Test
    void uiRegressionStillAcceptsExplicitLocator() {
        TestPlan plan = new TestPlan();
        plan.type = "UI";
        plan.baseUrl = "https://example.test";
        TestStep click = step("click");
        click.locator = "button[type='submit']";
        plan.steps.add(click);

        assertTrue(TestPlanValidator.validate(plan).valid());
    }

    @Test
    void uiRegressionRejectsActionWithoutTargetOrLocator() {
        TestPlan plan = new TestPlan();
        plan.type = "UI";
        plan.baseUrl = "https://example.test";
        plan.steps.add(step("click"));

        var result = TestPlanValidator.validate(plan);
        assertFalse(result.valid());
        assertTrue(result.errorsAsMessage().contains("locator or semantic target is required"));
    }

    @Test
    void promptRegressionRequiresLocatorlessUiPlanning() {
        String prompt = PromptManager.planningPrompt("open youtube.com, search java tutorial, play first video");
        assertTrue(prompt.contains("prefer a natural-language target"));
        assertTrue(prompt.contains("leave locator empty"));
        assertTrue(prompt.contains("live DOM"));
    }

    @Test
    void jacksonRoundTripPreservesSemanticTargetAndLocator() throws Exception {
        TestPlan plan = new TestPlan();
        plan.type = "UI";
        plan.baseUrl = "https://example.test";
        TestStep step = step("click");
        step.target = "first result";
        step.locator = "a[data-testid='result']";
        plan.steps.add(step);

        TestPlan copy = mapper.readValue(mapper.writeValueAsString(plan), TestPlan.class);
        assertEquals("first result", copy.steps.getFirst().target);
        assertEquals("a[data-testid='result']", copy.steps.getFirst().locator);
    }

    private TestStep step(String action) {
        TestStep step = new TestStep();
        step.action = action;
        return step;
    }
}
