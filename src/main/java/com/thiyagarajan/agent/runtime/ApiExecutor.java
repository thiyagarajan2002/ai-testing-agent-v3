package com.thiyagarajan.agent.runtime;

import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.io.ApiRunLogger;
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
    private final FailureArtifactManager artifacts;
    private final ApiAssertionEngine assertionEngine = new ApiAssertionEngine();

    public ApiExecutor() { this(Config.load()); }
    public ApiExecutor(Config config) { this.config = config; this.artifacts = new FailureArtifactManager(config); }

    public ExecutionResult execute(TestPlan plan) {
        if (plan == null) throw new IllegalArgumentException("API plan cannot be null");
        if (plan.steps == null || plan.steps.isEmpty()) throw new IllegalArgumentException("API plan contains no steps");
        if (plan.baseUrl == null || plan.baseUrl.isBlank()) throw new IllegalArgumentException("API baseUrl is required");
        variables.clear();
        if (plan.variables != null) plan.variables.forEach((k, v) -> variables.put(k, String.valueOf(v)));
        ExecutionResult result = new ExecutionResult(); result.testName = plan.name; result.passed = true;
        ApiRunLogger logger = null;
        try { logger = ApiRunLogger.start(config.reportsDir(), plan.name); }
        catch (Exception ignored) { /* Logging must never block API execution. */ }

        try {
            for (int index = 0; index < plan.steps.size(); index++) {
                TestStep step = plan.steps.get(index);
                int retries = step.retryCount == null ? config.retries() : step.retryCount;
                if (retries < 0) throw new IllegalArgumentException("retryCount cannot be negative");
                int maxAttempts = retries + 1;
                long stepStart = System.currentTimeMillis();
                boolean completed = false;
                for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                    long attemptStart = System.currentTimeMillis();
                    String url = "";
                    String body = substitute(step.body);
                    try {
                        String path = substitute(step.path);
                        url = buildUrl(plan.baseUrl, path, step.query);
                        if (logger != null) logger.log("REQUEST", "step=" + (index + 1) + "; attempt=" + attempt + "/" + maxAttempts + "; action=" + step.action + "; url=" + url + "; body=" + body);
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
                        String responseBody = response.asString();
                        if (logger != null) logger.log("RESPONSE", "step=" + (index + 1) + "; attempt=" + attempt + "/" + maxAttempts + "; status=" + response.statusCode() + "; durationMs=" + attemptDuration + "; body=" + abbreviate(responseBody, 5000));
                        var assertionFailures = assertionEngine.validate(response, step, attemptDuration, this::substitute);
                        String details = "HTTP " + response.statusCode() + "; attempt=" + attempt + "/" + maxAttempts
                                + "; attempts=" + attempt + "; durationMs=" + attemptDuration;
                        if (assertionFailures.isEmpty()) {
                            if (step.save != null) {
                                Map<String, String> extracted = new LinkedHashMap<>();
                                for (var entry : step.save.entrySet()) {
                                    Object value = response.jsonPath().get(entry.getValue());
                                    if (value == null) throw new AssertionError("JSON extraction failed: " + entry.getValue());
                                    extracted.put(entry.getKey(), String.valueOf(value));
                                }
                                extracted.forEach(variables::put);
                                if (!extracted.isEmpty()) details += "; saved=" + String.join(",", extracted.keySet());
                            }
                            details += "; totalDurationMs=" + (System.currentTimeMillis() - stepStart)
                                    + "; response=" + abbreviate(responseBody, 1000);
                            result.steps.add(new ExecutionResult.StepResult(step.action, true, details, System.currentTimeMillis() - stepStart));
                            completed = true; break;
                        }
                        details += "; assertionFailures=" + String.join(" | ", assertionFailures)
                                + "; totalDurationMs=" + (System.currentTimeMillis() - stepStart)
                                + "; response=" + abbreviate(responseBody, 1000);
                        if (attempt == maxAttempts) {
                            String artifact = createFailureArtifact(plan.name, index, step, url, body, details, responseBody);
                            result.steps.add(new ExecutionResult.StepResult(step.action, false, details, System.currentTimeMillis() - stepStart,
                                    artifact == null ? java.util.List.of() : java.util.List.of(artifact)));
                            result.passed = false;
                        }
                    } catch (Exception e) {
                        if (logger != null) logger.log("ERROR", "step=" + (index + 1) + "; attempt=" + attempt + "; error=" + e);
                        if (attempt == maxAttempts) {
                            String details = "attempt=" + attempt + "/" + maxAttempts + "; attempts=" + attempt
                                    + "; totalDurationMs=" + (System.currentTimeMillis() - stepStart) + "; error=" + e;
                            String artifact = createFailureArtifact(plan.name, index, step, url, body, details, "");
                            result.steps.add(new ExecutionResult.StepResult(step.action, false, details, System.currentTimeMillis() - stepStart,
                                    artifact == null ? java.util.List.of() : java.util.List.of(artifact)));
                            result.passed = false;
                        }
                    }
                }
                if (!completed && !result.passed) break;
            }
            return result;
        } finally {
            if (logger != null) logger.close();
        }
    }

    private String createFailureArtifact(String testName, int index, TestStep step, String url, String body,
                                         String details, String response) {
        try { return artifacts.createApiFailureArtifact(testName, index, step.action, url, body, details, response).toString(); }
        catch (Exception ignored) { return null; }
    }

    private Map<String, String> substituteMap(Map<String, String> source) { Map<String, String> out = new LinkedHashMap<>(); source.forEach((k,v)->out.put(substitute(k),substitute(v))); return out; }
    private String buildUrl(String base, String path, Map<String,String> query) {
        String target=path==null?"":path; String url=target.startsWith("http://")||target.startsWith("https://")?target:base.replaceAll("/$","")+"/"+target.replaceFirst("^/","");
        if(query==null||query.isEmpty()) return substitute(url); StringBuilder q=new StringBuilder(url.contains("?")?"&":"?"); boolean first=true;
        for(var e:query.entrySet()){if(!first)q.append('&');first=false;q.append(URLEncoder.encode(substitute(e.getKey()),StandardCharsets.UTF_8)).append('=').append(URLEncoder.encode(substitute(e.getValue()),StandardCharsets.UTF_8));}
        return substitute(url)+q;
    }
    private String substitute(String input){if(input==null)return "";String out=input;for(var e:variables.entrySet())out=out.replace("${"+e.getKey()+"}",e.getValue());return out;}
    private String abbreviate(String text,int max){return text==null?"":text.length()<=max?text:text.substring(0,max)+"...";}
}
