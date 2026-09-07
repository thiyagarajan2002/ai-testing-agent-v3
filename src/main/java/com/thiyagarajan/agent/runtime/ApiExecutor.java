package com.thiyagarajan.agent.runtime;

import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;
import io.restassured.response.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.given;

public class ApiExecutor {
    private final Map<String, String> variables = new ConcurrentHashMap<>();
    private final Config config;

    public ApiExecutor() { this(Config.load()); }
    public ApiExecutor(Config config) { this.config = config; }

    public ExecutionResult execute(TestPlan plan) {
        if (plan == null) throw new IllegalArgumentException("API plan cannot be null");
        if (plan.steps == null || plan.steps.isEmpty()) throw new IllegalArgumentException("API plan contains no steps");
        if (plan.baseUrl == null || plan.baseUrl.isBlank()) throw new IllegalArgumentException("API baseUrl is required");

        variables.clear();
        if (plan.variables != null) plan.variables.forEach((k, v) -> variables.put(k, String.valueOf(v)));

        ExecutionResult result = new ExecutionResult();
        result.testName = plan.name;
        result.passed = true;

        for (TestStep step : plan.steps) {
            int retries = step.retryCount == null ? 0 : step.retryCount;
            if (retries < 0) throw new IllegalArgumentException("retryCount cannot be negative");
            int maxAttempts = retries + 1;
            long stepStart = System.currentTimeMillis();
            boolean completed = false;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                long attemptStart = System.currentTimeMillis();
                try {
                    String path = substitute(step.path);
                    String body = substitute(step.body);
                    String url = buildUrl(plan.baseUrl, path, step.query);

                    var request = given().headers(step.headers == null ? Map.of() : substituteMap(step.headers));
                    int timeout = step.timeoutMs > 0 ? step.timeoutMs : config.defaultTimeoutMs();
                    request = request.config(io.restassured.config.RestAssuredConfig.config()
                            .httpClient(io.restassured.config.HttpClientConfig.httpClientConfig()
                                    .setParam("http.connection.timeout", timeout)
                                    .setParam("http.socket.timeout", timeout)));

                    Response response = switch (step.action.toUpperCase()) {
                        case "GET" -> request.when().get(url);
                        case "POST" -> request.contentType("application/json").body(body).when().post(url);
                        case "PUT" -> request.contentType("application/json").body(body).when().put(url);
                        case "PATCH" -> request.contentType("application/json").body(body).when().patch(url);
                        case "DELETE" -> request.when().delete(url);
                        default -> throw new IllegalArgumentException("Unsupported API action: " + step.action);
                    };

                    long attemptDuration = System.currentTimeMillis() - attemptStart;
                    boolean ok = validate(response, step, attemptDuration);
                    String details = "HTTP " + response.statusCode()
                            + "; attempt=" + attempt + "/" + maxAttempts
                            + "; attempts=" + attempt
                            + "; durationMs=" + attemptDuration;

                    if (ok) {
                        if (step.save != null) {
                            for (var entry : step.save.entrySet()) {
                                Object value = response.jsonPath().get(entry.getValue());
                                if (value == null) throw new AssertionError("JSON extraction failed: " + entry.getValue());
                                variables.put(entry.getKey(), String.valueOf(value));
                                details += "; saved=" + entry.getKey();
                            }
                        }
                        details += "; totalDurationMs=" + (System.currentTimeMillis() - stepStart)
                                + "; response=" + abbreviate(response.asString(), 1000);
                        result.steps.add(new ExecutionResult.StepResult(step.action, true, details,
                                System.currentTimeMillis() - stepStart));
                        completed = true;
                        break;
                    }

                    if (attempt == maxAttempts) {
                        details += "; totalDurationMs=" + (System.currentTimeMillis() - stepStart)
                                + "; response=" + abbreviate(response.asString(), 1000);
                        result.steps.add(new ExecutionResult.StepResult(step.action, false, details,
                                System.currentTimeMillis() - stepStart));
                        result.passed = false;
                    }
                } catch (Exception e) {
                    if (attempt == maxAttempts) {
                        String details = "attempt=" + attempt + "/" + maxAttempts
                                + "; attempts=" + attempt
                                + "; totalDurationMs=" + (System.currentTimeMillis() - stepStart)
                                + "; error=" + e;
                        result.steps.add(new ExecutionResult.StepResult(step.action, false, details,
                                System.currentTimeMillis() - stepStart));
                        result.passed = false;
                    }
                }
            }

            if (!completed && !result.passed) break;
        }
        return result;
    }

    private boolean validate(Response response, TestStep step, long durationMs) {
        var assertion = step.assertSpec;
        if (assertion == null) return true;
        boolean ok = true;
        if (assertion.status != null) ok &= response.statusCode() == assertion.status;
        if (assertion.contains != null && !assertion.contains.isBlank())
            ok &= response.asString().contains(substitute(assertion.contains));
        if (assertion.jsonPath != null && !assertion.jsonPath.isBlank()) {
            Object actual = response.jsonPath().get(assertion.jsonPath);
            ok &= actual != null;
            if (assertion.equals != null) ok &= String.valueOf(actual).equals(substitute(assertion.equals));
        }
        if (assertion.responseTimeMs != null) ok &= durationMs <= assertion.responseTimeMs;
        return ok;
    }

    private Map<String, String> substituteMap(Map<String, String> source) {
        Map<String, String> out = new LinkedHashMap<>();
        source.forEach((k, v) -> out.put(substitute(k), substitute(v)));
        return out;
    }

    private String buildUrl(String base, String path, Map<String, String> query) {
        String target = path == null ? "" : path;
        String url = target.startsWith("http://") || target.startsWith("https://")
                ? target
                : base.replaceAll("/$", "") + "/" + target.replaceFirst("^/", "");
        if (query == null || query.isEmpty()) return substitute(url);
        StringBuilder q = new StringBuilder(url.contains("?") ? "&" : "?");
        boolean first = true;
        for (var entry : query.entrySet()) {
            if (!first) q.append('&');
            first = false;
            q.append(URLEncoder.encode(substitute(entry.getKey()), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(substitute(entry.getValue()), StandardCharsets.UTF_8));
        }
        return substitute(url) + q;
    }

    private String substitute(String input) {
        if (input == null) return "";
        String out = input;
        for (var entry : variables.entrySet()) out = out.replace("${" + entry.getKey() + "}", entry.getValue());
        return out;
    }

    private String abbreviate(String text, int max) {
        return text == null ? "" : text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
