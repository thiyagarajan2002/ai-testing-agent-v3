package com.thiyagarajan.agent.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

public class OllamaClient implements AiProvider {
    /** Time allowed to establish the TCP/TLS connection to the Ollama server. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    /** Time allowed for the full generate() call, including model inference. */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final String baseUrl;
    private final String model;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public OllamaClient(String baseUrl, String model) {
        if (baseUrl == null || baseUrl.isBlank()) throw new IllegalArgumentException("Ollama base URL cannot be blank");
        if (model == null || model.isBlank()) throw new IllegalArgumentException("Ollama model cannot be blank");
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.model = model;
    }

    @Override
    public String generate(String prompt) throws Exception {
        if (prompt == null || prompt.isBlank()) throw new IllegalArgumentException("AI prompt cannot be blank");
        String json = mapper.createObjectNode()
                .put("model", model)
                .put("prompt", prompt)
                .put("stream", false)
                .toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new IllegalStateException("Ollama request timed out after " + REQUEST_TIMEOUT.toSeconds()
                    + "s (base URL: " + baseUrl + ")", e);
        }
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Ollama HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode node = mapper.readTree(response.body());
        return node.path("response").asText();
    }
}
