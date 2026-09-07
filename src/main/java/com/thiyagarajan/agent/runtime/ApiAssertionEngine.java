package com.thiyagarajan.agent.runtime;

import com.thiyagarajan.agent.model.TestStep;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Evaluates legacy and v3.9 assertion definitions and returns diagnostic failures. */
public final class ApiAssertionEngine {
    public List<String> validate(Response response, TestStep step, long durationMs, java.util.function.Function<String, String> substitute) {
        List<String> failures = new ArrayList<>();
        TestStep.AssertionSpec a = step.assertSpec;
        if (a != null) {
            if (a.status != null && response.statusCode() != a.status)
                failures.add("status expected=" + a.status + ", actual=" + response.statusCode());
            if (a.contains != null && !a.contains.isBlank() && !response.asString().contains(substitute.apply(a.contains)))
                failures.add("body contains expected='" + substitute.apply(a.contains) + "'");
            if (a.jsonPath != null && !a.jsonPath.isBlank()) {
                Object actual = response.jsonPath().get(a.jsonPath);
                if (actual == null) failures.add("jsonPath not found='" + a.jsonPath + "'");
                else if (a.equals != null && !String.valueOf(actual).equals(substitute.apply(a.equals)))
                    failures.add("jsonPath '" + a.jsonPath + "' expected='" + substitute.apply(a.equals) + "', actual='" + actual + "'");
            }
            if (a.responseTimeMs != null && durationMs > a.responseTimeMs)
                failures.add("response time expected<=" + a.responseTimeMs + "ms, actual=" + durationMs + "ms");
        }
        if (step.assertions != null) {
            for (TestStep.Assertion assertion : step.assertions) evaluate(response, assertion, durationMs, substitute, failures);
        }
        return failures;
    }

    private void evaluate(Response response, TestStep.Assertion a, long durationMs,
                          java.util.function.Function<String, String> substitute, List<String> failures) {
        if (a == null || a.type == null || a.type.isBlank()) {
            failures.add("assertion type is required");
            return;
        }
        String type = a.type.trim().toLowerCase(Locale.ROOT);
        String expected = a.expected == null ? "" : substitute.apply(a.expected);
        try {
            switch (type) {
                case "status" -> {
                    int expectedStatus = Integer.parseInt(expected);
                    if (response.statusCode() != expectedStatus) failures.add("status expected=" + expectedStatus + ", actual=" + response.statusCode());
                }
                case "bodycontains" -> {
                    if (!response.asString().contains(expected)) failures.add("bodyContains expected='" + expected + "'");
                }
                case "bodynotcontains" -> {
                    if (response.asString().contains(expected)) failures.add("bodyNotContains unexpected='" + expected + "'");
                }
                case "bodyregex" -> {
                    if (!Pattern.compile(expected, Pattern.DOTALL).matcher(response.asString()).find()) failures.add("bodyRegex did not match='" + expected + "'");
                }
                case "headerequals" -> {
                    String actual = response.getHeader(a.path);
                    if (actual == null || !actual.equals(expected)) failures.add("header '" + a.path + "' expected='" + expected + "', actual='" + actual + "'");
                }
                case "jsonpathexists" -> {
                    Object actual = response.jsonPath().get(a.path);
                    boolean exists = actual != null;
                    boolean wanted = expected.isBlank() || Boolean.parseBoolean(expected);
                    if (exists != wanted) failures.add("jsonPathExists '" + a.path + "' expected=" + wanted + ", actual=" + exists);
                }
                case "jsonpathequals" -> {
                    Object actual = response.jsonPath().get(a.path);
                    if (actual == null || !String.valueOf(actual).equals(expected)) failures.add("jsonPathEquals '" + a.path + "' expected='" + expected + "', actual='" + actual + "'");
                }
                case "jsonpathcontains" -> {
                    Object actual = response.jsonPath().get(a.path);
                    if (actual == null || !String.valueOf(actual).contains(expected)) failures.add("jsonPathContains '" + a.path + "' expected='" + expected + "', actual='" + actual + "'");
                }
                case "jsonpathregex" -> {
                    Object actual = response.jsonPath().get(a.path);
                    if (actual == null || !Pattern.compile(expected).matcher(String.valueOf(actual)).find()) failures.add("jsonPathRegex '" + a.path + "' did not match='" + expected + "', actual='" + actual + "'");
                }
                case "responsetimems" -> {
                    long limit = Long.parseLong(expected);
                    if (durationMs > limit) failures.add("responseTimeMs expected<=" + limit + "ms, actual=" + durationMs + "ms");
                }
                default -> failures.add("unsupported assertion type='" + a.type + "'");
            }
        } catch (Exception e) {
            failures.add("assertion type='" + a.type + "' error=" + e.getMessage());
        }
    }
}
