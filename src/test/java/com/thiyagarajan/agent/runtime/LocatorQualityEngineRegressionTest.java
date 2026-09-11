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

import static org.junit.jupiter.api.Assertions.*;

class LocatorQualityEngineRegressionTest {
    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", LocatorQualityEngineRegressionTest::servePage);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    @AfterAll
    static void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    @Tag("sanity")
    void scoresStableUniqueAccessibleSelectorHigherThanGenericSelector() throws Exception {
        withPage(page -> {
            LocatorQualityEngine engine = new LocatorQualityEngine();

            LocatorQualityEngine.Evaluation stable = engine.evaluate(page, "button[aria-label='Login']", "click");
            LocatorQualityEngine.Evaluation generic = engine.evaluate(page, "button", "click");

            assertTrue(stable.usable());
            assertTrue(generic.usable());
            assertTrue(stable.score() > generic.score());
            assertTrue(stable.unique());
            assertFalse(generic.unique());
            assertEquals(LocatorQualityEngine.Quality.EXCELLENT, stable.quality());
        });
    }

    @Test
    @Tag("regression")
    void rejectsFillActionForNonEditableElement() throws Exception {
        withPage(page -> {
            LocatorQualityEngine.Evaluation result = new LocatorQualityEngine()
                    .evaluate(page, "button[aria-label='Login']", "fill");

            assertFalse(result.usable());
            assertFalse(result.actionCompatible());
        });
    }

    @Test
    @Tag("regression")
    void semanticResolverRanksVerifiedAlternativeAboveWeakGenericPrimary() throws Exception {
        withPage(page -> {
            TestStep step = new TestStep();
            step.action = "click";
            step.target = "login button";

            AiProvider ai = prompt -> "{\"locator\":\"button\",\"confidence\":0.90,\"reason\":\"candidates\",\"alternatives\":[\"button[aria-label='Login']\"]}";

            SemanticLocatorResolver.Resolution result = new SemanticLocatorResolver(ai, new ObjectMapper()).resolve(page, step);

            assertEquals("button[aria-label='Login']", result.locator());
            assertEquals(0.85, result.confidence(), 0.001);
            assertTrue(result.reason().contains("quality=EXCELLENT"));
        });
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
                <input name='search_query' aria-label='Search'>
                <button aria-label='Login'>Login</button>
                <article><button>Open first</button></article>
                <article><button>Open second</button></article>
                </body></html>
                """;
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    @FunctionalInterface
    private interface PageConsumer {
        void accept(Page page) throws Exception;
    }
}
