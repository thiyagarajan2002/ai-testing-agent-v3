package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.thiyagarajan.agent.ai.AiProvider;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class UiExecutorExecutionRegressionTest {
    private static HttpServer server;
    private static int port;
    private static Path reports;

    @BeforeAll
    static void setup() throws Exception {
        reports = Files.createTempDirectory("agent-ui-regression-");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", UiExecutorExecutionRegressionTest::servePage);
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterAll
    static void cleanup() {
        if (server != null) server.stop(0);
    }

    @Test
    @Tag("sanity")
    void explicitLocatorsExecuteNavigationFillPressAndClick() {
        TestPlan plan = plan();
        TestStep navigate = step("navigate", "");
        navigate.value = plan.baseUrl;
        plan.steps.add(navigate);
        TestStep fill = step("fill", "#search"); fill.value = "java tutorial"; plan.steps.add(fill);
        TestStep press = step("press", "#search"); press.value = "Enter"; plan.steps.add(press);
        plan.steps.add(step("click", "#result"));
        TestStep assertion = step("asserttext", "#status"); assertion.value = "clicked"; plan.steps.add(assertion);

        ExecutionResult result = execute(plan, null);

        assertTrue(result.passed, result.steps.toString());
        assertEquals(5, result.steps.size());
        assertTrue(result.steps.stream().allMatch(step -> step.passed));
    }

    @Test
    @Tag("regression")
    void semanticTargetsAreResolvedAgainstLiveDomForEachStateChange() {
        TestPlan plan = plan();
        TestStep navigate = step("navigate", ""); navigate.value = plan.baseUrl; plan.steps.add(navigate);
        TestStep first = step("click", ""); first.target = "the first action button"; plan.steps.add(first);
        TestStep second = step("click", ""); second.target = "the continue button that appears after the action"; plan.steps.add(second);
        TestStep assertion = step("asserttext", "#status"); assertion.value = "completed"; plan.steps.add(assertion);

        AiProvider ai = prompt -> prompt.contains("continue button that appears after the action")
                ? "{\"locator\":\"#continue\",\"confidence\":0.99,\"reason\":\"live DOM contains the continue button\",\"alternatives\":[]}"
                : "{\"locator\":\"#action\",\"confidence\":0.99,\"reason\":\"live DOM contains the first action button\",\"alternatives\":[]}";

        ExecutionResult result = execute(plan, ai);

        assertTrue(result.passed, result.steps.toString());
        assertTrue(result.steps.get(1).details.contains("locator=#action"));
        assertTrue(result.steps.get(2).details.contains("locator=#continue"));
    }

    @Test
    @Tag("regression")
    void invalidSemanticLocatorIsRejectedWithoutExecutingAnInventedSelector() {
        TestPlan plan = plan();
        TestStep navigate = step("navigate", ""); navigate.value = plan.baseUrl; plan.steps.add(navigate);
        TestStep click = step("click", ""); click.target = "element that does not exist"; plan.steps.add(click);
        AiProvider ai = prompt -> "{\"locator\":\"#does-not-exist\",\"confidence\":0.99,\"reason\":\"not actually in DOM\",\"alternatives\":[]}";

        ExecutionResult result = execute(plan, ai);

        assertFalse(result.passed);
        assertEquals(2, result.steps.size());
        assertFalse(result.steps.get(1).passed);
    }

    private static ExecutionResult execute(TestPlan plan, AiProvider ai) {
        Config config = new Config("http://localhost:11434", "test", true, 5000, 0, 1, reports.toString(), "screenshots");
        return new UiExecutor(config, ai, new ObjectMapper()).execute(plan);
    }

    private static TestPlan plan() {
        TestPlan plan = new TestPlan(); plan.name = "UI execution regression"; plan.type = "UI"; plan.baseUrl = "http://127.0.0.1:" + port + "/"; return plan;
    }

    private static TestStep step(String action, String locator) { TestStep step = new TestStep(); step.action = action; step.locator = locator; return step; }

    private static void servePage(HttpExchange exchange) throws IOException {
        String html = """
                <!doctype html><html><body>
                <input id='search' aria-label='Search' />
                <button id='action' onclick="document.getElementById('action').remove();document.getElementById('continue').hidden=false;document.getElementById('status').textContent='action done'">First Action</button>
                <button id='continue' hidden onclick="document.getElementById('status').textContent='completed'">Continue</button>
                <button id='result' onclick="document.getElementById('status').textContent='clicked'">Result</button>
                <div id='status'>ready</div>
                </body></html>
                """;
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }
}
