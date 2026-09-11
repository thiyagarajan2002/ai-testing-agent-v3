package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.thiyagarajan.agent.ai.AiProvider;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.model.TestPlan;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class LocatorlessUiGenerationE2ERegressionTest {
    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", LocatorlessUiGenerationE2ERegressionTest::servePage);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    @AfterAll
    static void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    @Tag("regression")
    void completesLocatorlessPlanAgainstChangingLiveDomAndGeneratesConcreteCode() throws Exception {
        String requirement = "Open the local demo, search java tutorial, then play the first video";
        AiProvider ai = prompt -> {
            if (prompt.contains("You are a software testing planner.")) {
                return """
                        {
                          "name":"Locatorless YouTube-style flow",
                          "type":"UI",
                          "baseUrl":"%s",
                          "steps":[
                            {"action":"navigate","value":"%s"},
                            {"action":"fill","target":"search input","value":"java tutorial"},
                            {"action":"press","target":"search input","value":"Enter"},
                            {"action":"click","target":"first video"}
                          ]
                        }
                        """.formatted(baseUrl, baseUrl);
            }
            if (prompt.contains("search input")) {
                return "{\"locator\":\"input[aria-label='Search']\",\"confidence\":0.98,\"reason\":\"stable accessible search input\",\"alternatives\":[\"input[name='q']\"]}";
            }
            if (prompt.contains("first video")) {
                return "{\"locator\":\"a[data-video-id='first']\",\"confidence\":0.97,\"reason\":\"stable video identifier for first visible result\",\"alternatives\":[\"#results a\"]}";
            }
            throw new AssertionError("Unexpected AI prompt: " + prompt);
        };

        Config config = new Config("http://localhost:11434", "test-model", true,
                10000, 0, 1, "target/test-reports", "target/test-screenshots");

        String source = new AgentRunner(ai, new ObjectMapper(), config)
                .generateUiCode(requirement, "LocatorlessGeneratedTest");

        assertTrue(source.contains("public class LocatorlessGeneratedTest"));
        assertTrue(source.contains("page.navigate("));
        assertTrue(source.contains("input[aria-label='Search']"));
        assertTrue(source.contains("a[data-video-id='first']"));
        assertFalse(source.contains("page.locator(\"search input\")"));
        assertFalse(source.contains("page.locator(\"first video\")"));
    }

    @Test
    @Tag("sanity")
    void plannerProducesLocatorlessUiStepsBeforeResolution() throws Exception {
        AiProvider ai = prompt -> """
                {"name":"Sanity UI","type":"UI","baseUrl":"http://example.test/",
                 "steps":[{"action":"navigate","value":"http://example.test/"},
                 {"action":"click","target":"Login button"}]}
                """;

        TestPlan plan = new AgentRunner(ai, new ObjectMapper()).plan("Open example.test and click the Login button");

        assertEquals("UI", plan.type);
        assertEquals("Login button", plan.steps.get(1).target);
        assertTrue(plan.steps.get(1).locator == null || plan.steps.get(1).locator.isBlank());
    }

    private static void servePage(HttpExchange exchange) throws IOException {
        String html = """
                <!doctype html>
                <html><body>
                  <label for='search'>Search</label>
                  <input id='search' aria-label='Search' name='q'>
                  <div id='results' aria-live='polite'>No results yet</div>
                  <script>
                    document.querySelector('#search').addEventListener('keydown', function(event) {
                      if (event.key === 'Enter') {
                        document.querySelector('#results').innerHTML =
                          '<article><a data-video-id="first" href="/video/first">Java Tutorial - First Video</a></article>' +
                          '<article><a data-video-id="second" href="/video/second">Java Tutorial - Second Video</a></article>';
                      }
                    });
                  </script>
                </body></html>
                """;
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
