package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ApiExecutorExecutionRegressionTest {
    private static HttpServer server;
    private static int port;
    private static Path reports;

    @BeforeAll
    static void startServer() throws IOException {
        reports = Files.createTempDirectory("agent-api-regression-");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/item", exchange -> respond(exchange, 200, "{\"id\":42,\"name\":\"java\"}"));
        server.createContext("/api/echo", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            respond(exchange, 201, body.isBlank() ? "{\"ok\":true}" : body);
        });
        server.createContext("/api/retry", new RetryHandler());
        server.createContext("/api/options", exchange -> {
            exchange.getResponseHeaders().add("Allow", "GET,POST,OPTIONS");
            respond(exchange, 204, "");
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    @Tag("sanity")
    void getAndSaveVariableExecutes() {
        TestPlan plan = plan();
        TestStep step = step("GET", "/api/item");
        step.assertSpec.status = 200;
        step.assertSpec.jsonPath = "id";
        step.save.put("itemId", "id");
        plan.steps.add(step);

        ExecutionResult result = executor().execute(plan);

        assertTrue(result.passed, result.steps.toString());
        assertEquals(1, result.steps.size());
        assertTrue(result.steps.get(0).details().contains("saved=itemId"));
    }

    @Test
    @Tag("regression")
    void postWithJsonAndHeaderExecutes() {
        TestPlan plan = plan();
        TestStep step = step("POST", "/api/echo");
        step.headers.put("X-Test", "regression");
        step.body = "{\"name\":\"java\"}";
        step.assertSpec.status = 201;
        step.assertSpec.contains = "java";
        plan.steps.add(step);

        ExecutionResult result = executor().execute(plan);

        assertTrue(result.passed, result.steps.toString());
    }

    @Test
    @Tag("regression")
    void allSupportedHttpMethodsRemainSupported() {
        TestPlan plan = plan();
        for (String method : new String[]{"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"}) {
            TestStep step = step(method, method.equals("OPTIONS") ? "/api/options" : "/api/item");
            step.assertSpec.status = method.equals("POST") ? 201 : method.equals("HEAD") ? 200 : method.equals("OPTIONS") ? 204 : 200;
            if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH") || method.equals("DELETE")) {
                step.body = "{\"method\":\"" + method + "\"}";
                if (!method.equals("POST")) step.assertSpec.status = 200;
            }
            plan.steps.add(step);
        }

        ExecutionResult result = executor().execute(plan);

        assertTrue(result.passed, result.steps.toString());
        assertEquals(7, result.steps.size());
    }

    @Test
    @Tag("regression")
    void retryRecoversFromTransientServerFailure() {
        RetryHandler.CALLS.set(0);
        TestPlan plan = plan();
        TestStep step = step("GET", "/api/retry");
        step.retryCount = 1;
        step.assertSpec.status = 200;
        plan.steps.add(step);

        ExecutionResult result = executor().execute(plan);

        assertTrue(result.passed, result.steps.toString());
        assertTrue(result.steps.get(0).details().contains("attempt=2/2"));
        assertEquals(2, RetryHandler.CALLS.get());
    }

    @Test
    @Tag("regression")
    void failedAssertionProducesFailureResultInsteadOfFalsePass() {
        TestPlan plan = plan();
        TestStep step = step("GET", "/api/item");
        step.assertSpec.status = 404;
        plan.steps.add(step);

        ExecutionResult result = executor().execute(plan);

        assertFalse(result.passed);
        assertEquals(1, result.steps.size());
        assertFalse(result.steps.get(0).passed());
    }

    private static TestPlan plan() {
        TestPlan plan = new TestPlan();
        plan.name = "API execution regression";
        plan.type = "API";
        plan.baseUrl = "http://127.0.0.1:" + port;
        return plan;
    }

    private static TestStep step(String action, String path) {
        TestStep step = new TestStep();
        step.action = action;
        step.path = path;
        return step;
    }

    private static ApiExecutor executor() {
        Config config = new Config("http://localhost:11434", "test", true, 5000, 0, 1,
                reports.toString(), "screenshots");
        return new ApiExecutor(config);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static final class RetryHandler implements com.sun.net.httpserver.HttpHandler {
        private static final AtomicInteger CALLS = new AtomicInteger();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            int call = CALLS.incrementAndGet();
            respond(exchange, call == 1 ? 503 : 200, "{\"ok\":true}");
        }
    }
}
