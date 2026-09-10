package com.thiyagarajan.agent;

import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;
import com.thiyagarajan.agent.runtime.UiCodeGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UiCodeGeneratorTest {
    @Test
    void generatesReadablePlaywrightMethodsFromResolvedTargets() {
        TestPlan plan = new TestPlan();
        plan.type = "UI";
        plan.baseUrl = "https://www.youtube.com";

        TestStep open = new TestStep();
        open.action = "navigate";
        plan.steps.add(open);

        TestStep search = new TestStep();
        search.action = "fill";
        search.target = "search input";
        search.locator = "input[name=search_query]";
        search.value = "java tutorial";
        plan.steps.add(search);

        TestStep play = new TestStep();
        play.action = "click";
        play.target = "first video";
        play.locator = "ytd-video-renderer a#video-title";
        plan.steps.add(play);

        String source = new UiCodeGenerator().generate(plan, "YouTubeGeneratedTest");
        assertTrue(source.contains("class YouTubeGeneratedTest"));
        assertTrue(source.contains("page.navigate(\"https://www.youtube.com\")"));
        assertTrue(source.contains("input[name=search_query]"));
        assertTrue(source.contains("ytd-video-renderer a#video-title"));
        assertTrue(source.contains("fill(\"java tutorial\")"));
    }
}
