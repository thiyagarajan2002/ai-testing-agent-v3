package com.thiyagarajan.agent.runtime;

import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;
import io.restassured.response.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.given;

public class ApiExecutor {
    private final Map<String, String> variables = new ConcurrentHashMap<>();

    public ExecutionResult execute(TestPlan plan) {
        ExecutionResult result = new ExecutionResult();
        result.testName = plan.name;
        result.passed = true;

        for (TestStep step : plan.steps) {
            long start = System.currentTimeMillis();
            try {
                String path = substitute(step.path);
                String body = substitute(step.body);
                String url = resolveUrl(plan.baseUrl, path);

                Response response = switch (step.action.toUpperCase()) {
                    case "GET" -> given().when().get(url);
                    case "POST" -> given().contentType("application/json").body(body).when().post(url);
                    case "PUT" -> given().contentType("application/json").body(body).when().put(url);
                    case "PATCH" -> given().contentType("application/json").body(body).when().patch(url);
                    case "DELETE" -> given().when().delete(url);
                    default -> throw new IllegalArgumentException("Unsupported API action: " + step.action);
                };

                boolean ok = true;
                StringBuilder details = new StringBuilder("HTTP ").append(response.statusCode());

                if (step.assertSpec != null && step.assertSpec.status != null) {
                    ok &= response.statusCode() == step.assertSpec.status;
                    details.append("; expectedStatus=").append(step.assertSpec.status);
                }
                if (step.assertSpec != null && step.assertSpec.contains != null
                        && !step.assertSpec.contains.isBlank()) {
                    ok &= response.asString().contains(step.assertSpec.contains);
                    details.append("; contains=").append(step.assertSpec.contains);
                }
                if (step.assertSpec != null && step.assertSpec.jsonPath != null
                        && !step.assertSpec.jsonPath.isBlank()) {
                    Object actual = response.jsonPath().get(step.assertSpec.jsonPath);
                    if (step.assertSpec.equals != null) {
                        ok &= String.valueOf(actual).equals(step.assertSpec.equals);
                    } else {
                        ok &= actual != null;
                    }
                    details.append("; jsonPath=").append(step.assertSpec.jsonPath)
                           .append("; actual=").append(actual);
                }

                if (step.save != null) {
                    for (var entry : step.save.entrySet()) {
                        Object value = response.jsonPath().get(entry.getValue());
                        variables.put(entry.getKey(), String.valueOf(value));
                        details.append("; saved ").append(entry.getKey());
                    }
                }

                result.steps.add(new ExecutionResult.StepResult(
                        step.action, ok, details.toString(), System.currentTimeMillis() - start));

                if (!ok) result.passed = false;
            } catch (Exception e) {
                result.passed = false;
                result.steps.add(new ExecutionResult.StepResult(
                        step.action, false, e.toString(), System.currentTimeMillis() - start));
            }
        }
        return result;
    }

    private String substitute(String input) {
        if (input == null) return "";
        String out = input;
        for (var e : variables.entrySet()) {
            out = out.replace("${" + e.getKey() + "}", e.getValue());
        }
        return out;
    }

    private String resolveUrl(String base, String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        if (base == null || base.isBlank())
            throw new IllegalArgumentException("baseUrl is required for relative API paths");
        return base.replaceAll("/$", "") + "/" + path.replaceFirst("^/", "");
    }
}
