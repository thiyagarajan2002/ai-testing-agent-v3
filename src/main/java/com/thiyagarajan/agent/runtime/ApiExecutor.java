package com.thiyagarajan.agent.runtime;

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
            long start = System.currentTimeMillis();
            try {
                String path = substitute(step.path);
                String body = substitute(step.body);
                String url = buildUrl(plan.baseUrl, path, step.query);
                var request = given().headers(step.headers == null ? Map.of() : substituteMap(step.headers));
                Response response = switch (step.action.toUpperCase()) {
                    case "GET" -> request.when().get(url);
                    case "POST" -> request.contentType("application/json").body(body).when().post(url);
                    case "PUT" -> request.contentType("application/json").body(body).when().put(url);
                    case "PATCH" -> request.contentType("application/json").body(body).when().patch(url);
                    case "DELETE" -> request.when().delete(url);
                    default -> throw new IllegalArgumentException("Unsupported API action: " + step.action);
                };
                long duration = System.currentTimeMillis() - start;
                boolean ok = validate(response, step, duration);
                String details = "HTTP " + response.statusCode() + "; durationMs=" + duration + "; response=" + abbreviate(response.asString(), 1000);
                if (step.save != null) for (var entry : step.save.entrySet()) {
                    Object value = response.jsonPath().get(entry.getValue());
                    if (value == null) throw new AssertionError("JSON extraction failed: " + entry.getValue());
                    variables.put(entry.getKey(), String.valueOf(value));
                    details += "; saved=" + entry.getKey();
                }
                result.steps.add(new ExecutionResult.StepResult(step.action, ok, details, duration));
                if (!ok) { result.passed = false; break; }
            } catch (Exception e) {
                result.passed = false;
                result.steps.add(new ExecutionResult.StepResult(step.action, false, e.toString(), System.currentTimeMillis() - start));
                break;
            }
        }
        return result;
    }

    private boolean validate(Response response, TestStep step, long duration) {
        var a = step.assertSpec;
        if (a == null) return true;
        boolean ok = true;
        if (a.status != null) ok &= response.statusCode() == a.status;
        if (a.contains != null && !a.contains.isBlank()) ok &= response.asString().contains(substitute(a.contains));
        if (a.jsonPath != null && !a.jsonPath.isBlank()) {
            Object actual = response.jsonPath().get(a.jsonPath);
            ok &= actual != null;
            if (a.equals != null) ok &= String.valueOf(actual).equals(substitute(a.equals));
        }
        if (a.responseTimeMs != null) ok &= duration <= a.responseTimeMs;
        return ok;
    }

    private Map<String, String> substituteMap(Map<String, String> source) {
        Map<String, String> out = new LinkedHashMap<>();
        source.forEach((k, v) -> out.put(substitute(k), substitute(v)));
        return out;
    }

    private String buildUrl(String base, String path, Map<String, String> query) {
        String target = path == null ? "" : path;
        String url = target.startsWith("http://") || target.startsWith("https://") ? target : base.replaceAll("/$", "") + "/" + target.replaceFirst("^/", "");
        if (query == null || query.isEmpty()) return substitute(url);
        StringBuilder q = new StringBuilder(url.contains("?") ? "&" : "?");
        boolean first = true;
        for (var e : query.entrySet()) {
            if (!first) q.append('&'); first = false;
            q.append(URLEncoder.encode(substitute(e.getKey()), StandardCharsets.UTF_8)).append('=').append(URLEncoder.encode(substitute(e.getValue()), StandardCharsets.UTF_8));
        }
        return substitute(url) + q;
    }

    private String substitute(String input) {
        if (input == null) return "";
        String out = input;
        for (var e : variables.entrySet()) out = out.replace("${" + e.getKey() + "}", e.getValue());
        return out;
    }

    private String abbreviate(String text, int max) { return text == null ? "" : text.length() <= max ? text : text.substring(0, max) + "..."; }
}
