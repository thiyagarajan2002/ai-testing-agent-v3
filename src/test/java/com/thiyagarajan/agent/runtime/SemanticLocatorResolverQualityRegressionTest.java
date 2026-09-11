package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.thiyagarajan.agent.ai.AiProvider;
import com.thiyagarajan.agent.model.TestStep;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SemanticLocatorResolverQualityRegressionTest {
    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", SemanticLocatorResolverQualityRegressionTest::servePage);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    @AfterAll
    static void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    @Tag("sanity")
    void resolvesStableAccessibleNameFromLiveDom() throws Exception {
        withPage(page -> {
            TestStep step = step("click", "login button");
            AiProvider ai = prompt -> "{\"locator\":\"button[aria-label='Login']\",\"confidence\":0.96,\"reason\":\"stable accessible attribute\",\"alternatives\":[]}";

            SemanticLocatorResolver.Resolution result = new SemanticLocatorResolver(ai, new ObjectMapper()).resolve(page, step);

            assertEquals("button[aria-label='Login']", result.locator());
            assertEquals(0.96, result.confidence(), 0.001);
        });
    }

    @Test
    @Tag("regression")
    void fallsBackToFirstVerifiedAlternativeWhenPrimaryIsInvalid() throws Exception {
        withPage(page -> {
            TestStep step = step("click", "login button");
            AiProvider ai = prompt -> "{\"locator\":\"button[data-ai-invented='x']\",\"confidence\":0.91,\"reason\":\"primary\",\"alternatives\":[\"button[aria-label='Login']\"]}";

            SemanticLocatorResolver.Resolution result = new SemanticLocatorResolver(ai, new ObjectMapper()).resolve(page, step);

            assertEquals("button[aria-label='Login']", result.locator());
            assertEquals(0.86, result.confidence(), 0.001);
        });
    }

    @Test
    @Tag("regression")
    void rejectsLowConfidenceEvenWhenSelectorExists() throws Exception {
        withPage(page -> {
            TestStep step = step("click", "login button");
            AiProvider ai = prompt -> "{\"locator\":\"button[aria-label='Login']\",\"confidence\":0.49,\"reason\":\"weak match\",\"alternatives\":[]}";

            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> new SemanticLocatorResolver(ai, new ObjectMapper()).resolve(page, step));

            assertTrue(error.getMessage().contains("confidence below threshold"));
        });
    }

    @Test
    @Tag("regression")
    void rejectsXPathAndJavaCandidatesEvenIfPlaywrightCouldInterpretThem() throws Exception {
        withPage(page -> {
            TestStep step = step("click", "login button");
            AiProvider ai = prompt -> "{\"locator\":\"//button[@aria-label='Login']\",\"confidence\":0.99,\"reason\":\"xpath\",\"alternatives\":[\"page.locator(\\\"button[aria-label='Login']\\\")\"]}";

            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> new SemanticLocatorResolver(ai, new ObjectMapper()).resolve(page, step));

            assertTrue(error.getMessage().contains("could not resolve"));
        });
    }

    @Test
    @Tag("regression")
    void preservesPreviousStepContextInAiPrompt() throws Exception {
        withPage(page -> {
            TestStep previous = step("click", "first result");
            previous.locator = "article:nth-of-type(1) button";
            TestStep current = step("click", "button beside the selected result");
            final String[] captured = {""};
            AiProvider ai = prompt -> {
                captured[0] = prompt;
                return "{\"locator\":\"article:nth-of-type(1) button\",\"confidence\":0.90,\"reason\":\"contextual match\",\"alternatives\":[]}";
            };

            SemanticLocatorResolver.Resolution result = new SemanticLocatorResolver(ai, new ObjectMapper()).resolve(page, current, List.of(previous));

            assertEquals("article:nth-of-type(1) button", result.locator());
            assertTrue(captured[0].contains("first result"));
            assertTrue(captured[0].contains("article:nth-of-type(1) button"));
        });
    }

    private static TestStep step(String action, String target) {
        TestStep step = new TestStep();
        step.action = action;
        step.target = target;
        return step;
    }

    private static void withPage(PageConsumer consumer) throws Exception {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            try (Page page = browser.newPage()) {
                page.navigate(baseUrl);
                consumer.accept(page);
            } finally {
                browser.close();
            }
        }
    }

    private static void servePage(HttpExchange exchange) throws IOException {
        String html = """
                <!doctype html><html><body>
                <button aria-label='Login'>Login</button>
                <article><h2>First result</h2><button>Open</button></article>
                <article><h2>Second result</h2><button>Open</button></article>
                </body></html>
                """;
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }

    @FunctionalInterface
    private interface PageConsumer {
        void accept(Page page) throws Exception;
    }
}
