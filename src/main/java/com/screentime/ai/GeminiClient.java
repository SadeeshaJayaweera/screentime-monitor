package com.screentime.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.screentime.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP Client stub for communicating with the Gemini AI REST API.
 * Uses Java standard HttpClient and reads credentials securely from ConfigManager.
 */
public class GeminiClient {

    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private final HttpClient httpClient;
    private final Gson gson;
    private final ConfigManager configManager;

    public GeminiClient() {
        this(ConfigManager.getInstance());
    }

    public GeminiClient(ConfigManager configManager) {
        this.configManager = configManager;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
    }

    /**
     * Checks whether an API key is configured either via config.json or environment variable.
     */
    public boolean isApiKeyAvailable() {
        String key = configManager.getEffectiveGeminiApiKey();
        return key != null && !key.isBlank();
    }

    /**
     * Sends a synchronous prompt request to the Gemini API.
     */
    public String generateContent(String prompt) {
        try {
            return generateContentAsync(prompt).get(java.util.concurrent.TimeUnit.SECONDS.toSeconds(15), java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("Synchronous Gemini call failed or timed out: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Sends an asynchronous prompt request to the Gemini API.
     */
    public CompletableFuture<String> generateContentAsync(String prompt) {
        String apiKey = configManager.getEffectiveGeminiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("Gemini API key is not configured. Returning default advice.");
            return CompletableFuture.completedFuture("Gemini API key is not set. Please configure your key in Settings.");
        }

        try {
            URI endpoint = URI.create(GEMINI_BASE_URL + "?key=" + apiKey);

            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", prompt);

            JsonArray parts = new JsonArray();
            parts.add(textPart);

            JsonObject contentObj = new JsonObject();
            contentObj.add("parts", parts);

            JsonArray contentsArray = new JsonArray();
            contentsArray.add(contentObj);

            JsonObject requestBody = new JsonObject();
            requestBody.add("contents", contentsArray);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(endpoint)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            return response.body();
                        } else {
                            logger.error("Gemini API error (HTTP {}): {}", response.statusCode(), response.body());
                            return "Error from Gemini API: HTTP " + response.statusCode();
                        }
                    })
                    .exceptionally(ex -> {
                        logger.error("Failed to connect to Gemini API", ex);
                        return "Failed to contact Gemini API: " + ex.getMessage();
                    });
        } catch (Exception e) {
            logger.error("Error creating Gemini request", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
