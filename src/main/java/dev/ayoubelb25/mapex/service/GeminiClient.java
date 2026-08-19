package dev.ayoubelb25.mapex.service;

import jakarta.annotation.PostConstruct;

//import jakarta.enterprise.context.ApplicationScoped;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

@Component
public class GeminiClient implements ModelClient {

    private static final String BASE_URL
            = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int TIMEOUT_S = 60;

    private String apiKey;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (is != null) {
                props.load(is);
                apiKey = props.getProperty("gemini.api.key");
            }
        } catch (Exception e) {
            System.err.println("[GeminiClient] Could not load API key: " + e.getMessage());
        }
    }

    public String generate(String model, String prompt, String overrideKey, String overrideUrl) throws Exception {
        String keyToUse = (overrideKey != null && !overrideKey.isBlank()) ? overrideKey : apiKey;
        String baseUrl = (overrideUrl != null && !overrideUrl.isBlank()) ? overrideUrl : BASE_URL;

        if (keyToUse == null || keyToUse.isBlank()) {
            throw new IllegalStateException("No Gemini API key configured.");
        }

        String url = baseUrl + model + ":generateContent?key=" + keyToUse;
        String body = buildRequestBody(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(TIMEOUT_S))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini returned HTTP " + response.statusCode()
                    + ": " + response.body());
        }

        return stripMarkdownFences(extractResponseText(response.body()));
    }

    @Override
    public String generate(String model, String prompt) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Gemini API key not configured. Add gemini.api.key to db.properties.");
        }

        String url = BASE_URL + model + ":generateContent?key=" + apiKey;
        String body = buildRequestBody(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(TIMEOUT_S))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini returned HTTP " + response.statusCode()
                    + ": " + response.body());
        }

        String extracted = extractResponseText(response.body());
        return stripMarkdownFences(extracted);
    }

    private String stripMarkdownFences(String text) {
        if (text == null) {
            return text;
        }
        return text.replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*$", "")
                .trim();
    }

    private String buildRequestBody(String prompt) {
        String escaped = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
        return "{\"contents\":[{\"parts\":[{\"text\":\"" + escaped + "\"}]}]}";
    }

    /**
     * Extracts the first response text without a full JSON parse.
     */
    private String extractResponseText(String geminiJson) {
        int textIdx = geminiJson.indexOf("\"text\"");
        if (textIdx == -1) {
            throw new RuntimeException("Unexpected Gemini response shape: " + geminiJson);
        }
        int colonIdx = geminiJson.indexOf(':', textIdx);
        int quoteStart = geminiJson.indexOf('"', colonIdx + 1);
        if (colonIdx == -1 || quoteStart == -1) {
            throw new RuntimeException("Unexpected Gemini response shape: " + geminiJson);
        }

        int start = quoteStart + 1;
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start; i < geminiJson.length(); i++) {
            char c = geminiJson.charAt(i);
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
        return sb.toString().trim();
    }
}
