package dev.ayoubelb25.mapex.service;

//import jakarta.enterprise.context.ApplicationScoped;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class OllamaClient implements ModelClient {

    private static final String BASE_URL = "http://localhost:11434/api/generate";
    private static final int TIMEOUT_S = 300;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Sends a prompt to an Ollama model and returns the response text.
     *
     * @param model Ollama model tag
     * @param prompt full assembled prompt
     */
    public String generate(String model, String prompt) throws Exception {
        String body = buildRequestBody(model, prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(TIMEOUT_S))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama returned HTTP " + response.statusCode() + ": " + response.body());
        }

        return extractResponseField(response.body());
    }

    private String buildRequestBody(String model, String prompt) {
        String escaped = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");

        return "{\"model\":\"" + model + "\","
                + "\"prompt\":\"" + escaped + "\","
                + "\"stream\":false}";
    }

    /**
     * Extracts the response field from Ollama's JSON wrapper.
     */
    private String extractResponseField(String ollamaJson) {
        int start = ollamaJson.indexOf("\"response\":\"");
        if (start == -1) {
            throw new RuntimeException("Unexpected Ollama response shape: " + ollamaJson);
        }

        start += 12;
        StringBuilder sb = new StringBuilder();
        boolean escape = false;

        for (int i = start; i < ollamaJson.length(); i++) {
            char c = ollamaJson.charAt(i);
            if (escape) {
                if (c == 'n') {
                    sb.append('\n');
                } else if (c == '"') {
                    sb.append('"');
                } else {
                    sb.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }

        return stripThinkBlocks(sb.toString().trim());
    }

    /**
     * Removes <think> blocks emitted before the final answer.
     */
    private String stripThinkBlocks(String text) {
        return text.replaceAll("(?s)<think>.*?</think>", "").trim();
    }
}
