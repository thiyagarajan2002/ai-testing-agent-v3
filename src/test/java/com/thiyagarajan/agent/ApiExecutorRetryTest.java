package com.thiyagarajan.agent;

import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;
import com.thiyagarajan.agent.runtime.ApiExecutor;
import com.thiyagarajan.agent.runtime.ExecutionResult;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ApiExecutorRetryTest {
    @Test
    void retriesFailedHttpAssertionUntilSuccess() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/health", exchange -> {
            int attempt = requests.incrementAndGet();
            int status = attempt < 3 ? 500 : 200;
            byte[] body = (status == 200 ? "healthy" : "temporary failure").getBytes();
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();

        try {
            TestPlan plan = new TestPlan();
            plan.name = "Retry test";
            plan.type = "API";
            plan.baseUrl = "http://localhost:" + server.getAddress().getPort();

            TestStep step = new TestStep();
            step.action = "GET";
            step.path = "/health";
            step.retryCount = 2;
            step.assertSpec.status = 200;
            plan.steps.add(step);

            ExecutionResult result = new ApiExecutor().execute(plan);

            assertTrue(result.passed);
            assertEquals(3, requests.get());
            assertEquals(1, result.steps.size());
            assertTrue(result.steps.get(0).details.contains("attempt=3/3"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void zeroRetryMakesOnlyOneAttempt() throws IOException {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/health", exchange -> {
            requests.incrementAndGet();
            byte[] body = "failure".getBytes();
            exchange.sendResponseHeaders(500, body.length);
            try (OutputStream output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();

        try {
            TestPlan plan = new TestPlan();
            plan.baseUrl = "http://localhost:" + server.getAddress().getPort();
            TestStep step = new TestStep();
            step.action = "GET";
            step.path = "/health";
            step.retryCount = 0;
            step.assertSpec.status = 200;
            plan.steps.add(step);

            ExecutionResult result = new ApiExecutor().execute(plan);

            assertFalse(result.passed);
            assertEquals(1, requests.get());
            assertTrue(result.steps.get(0).details.contains("attempt=1/1"));
        } finally {
            server.stop(0);
        }
    }
}
