package com.thiyagarajan.agent;

import com.sun.net.httpserver.HttpServer;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;
import com.thiyagarajan.agent.runtime.ApiExecutor;
import com.thiyagarajan.agent.runtime.ExecutionResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ApiExecutorPhase3Test {
    private HttpServer server;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("api-phase3-");
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/secure", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            boolean valid = "Bearer test-token".equals(auth) && contentType != null && contentType.startsWith("application/json") && body.contains("hello");
            byte[] response = (valid ? "{\"ok\":true,\"message\":\"hello\"}" : "{\"ok\":false}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("X-Test", "phase3");
            exchange.sendResponseHeaders(valid ? 200 : 401, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/form", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] response = (body.contains("name=agent") ? "{\"ok\":true}" : "{\"ok\":false}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(body.contains("name=agent") ? 200 : 400, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void supportsBearerAuthJsonBodyAndAssertions() {
        TestPlan plan = new TestPlan();
        plan.name = "phase3-secure";
        plan.baseUrl = "http://localhost:" + server.getAddress().getPort();

        TestStep step = new TestStep();
        step.action = "POST";
        step.path = "/secure";
        step.body = "{\"message\":\"hello\"}";
        step.auth.type = "bearer";
        step.auth.token = "test-token";
        step.assertSpec.status = 200;
        step.assertions.add(assertion("headerEquals", "X-Test", "phase3"));
        step.assertions.add(assertion("jsonPathEquals", "ok", "true"));
        plan.steps.add(step);

        ExecutionResult result = new ApiExecutor(new Config("http://localhost:11434", "test", true, 5000, 0, 1,
                tempDir.toString(), tempDir.resolve("screenshots").toString())).execute(plan);

        assertTrue(result.passed, () -> result.steps.toString());
        assertEquals(1, result.steps.size());
        assertTrue(result.steps.get(0).passed);
    }

    @Test
    void supportsFormUrlEncodedBody() {
        TestPlan plan = new TestPlan();
        plan.name = "phase3-form";
        plan.baseUrl = "http://localhost:" + server.getAddress().getPort();

        TestStep step = new TestStep();
        step.action = "POST";
        step.path = "/form";
        step.form.put("name", "agent");
        step.assertSpec.status = 200;
        plan.steps.add(step);

        ExecutionResult result = new ApiExecutor(new Config("http://localhost:11434", "test", true, 5000, 0, 1,
                tempDir.toString(), tempDir.resolve("screenshots").toString())).execute(plan);

        assertTrue(result.passed, () -> result.steps.toString());
    }

    private static TestStep.Assertion assertion(String type, String path, String expected) {
        TestStep.Assertion assertion = new TestStep.Assertion();
        assertion.type = type;
        assertion.path = path;
        assertion.expected = expected;
        return assertion;
    }
}
