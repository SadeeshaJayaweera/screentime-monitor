package com.screentime.ai;

import com.screentime.config.ConfigManager;
import com.screentime.data.DailyUsageSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for orchestrating screen-time health suggestions and AI insights.
 */
public class HealthSuggestionService {

    private static final Logger logger = LoggerFactory.getLogger(HealthSuggestionService.class);

    private final GeminiClient geminiClient;
    private final ConfigManager configManager;

    public HealthSuggestionService() {
        this(new GeminiClient(), ConfigManager.getInstance());
    }

    public HealthSuggestionService(GeminiClient geminiClient, ConfigManager configManager) {
        this.geminiClient = geminiClient;
        this.configManager = configManager;
    }

    /**
     * Generates comprehensive screen time wellness insights (assessment, concerns, suggestions).
     */
    public HealthAdvisorOutput generateInsights(List<DailyUsageSummary> history, long todayActiveSeconds, String topApp) {
        long activeMinutesToday = todayActiveSeconds / 60;

        if (!configManager.getConfig().isAiEnabled() || !geminiClient.isApiKeyAvailable()) {
            return getFallbackInsights(activeMinutesToday, topApp);
        }

        try {
            String prompt = String.format("""
                You are a digital wellness and occupational health advisor.
                The user has logged %d minutes of screen time today. Top application is '%s'.
                Provide:
                1. A brief overall health assessment (1-2 sentences).
                2. Three concise, bulleted actionable ergonomic or wellness tips.
                Keep it encouraging and practical.
                """, activeMinutesToday, topApp != null ? topApp : "General Applications");

            String response = geminiClient.generateContent(prompt);
            if (response != null && !response.isBlank()) {
                List<String> suggestions = new ArrayList<>();
                String[] lines = response.split("\n");
                StringBuilder assessment = new StringBuilder();

                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.matches("^\\d+\\..*")) {
                        suggestions.add(trimmed.replaceFirst("^[-*\\d.]+\\s*", ""));
                    } else if (!trimmed.isEmpty() && assessment.length() < 250) {
                        if (assessment.length() > 0) assessment.append(" ");
                        assessment.append(trimmed);
                    }
                }

                if (suggestions.isEmpty()) {
                    suggestions = List.of(
                            "Follow the 20-20-20 rule: rest your eyes every 20 minutes.",
                            "Stay hydrated and adjust chair lumbar support.",
                            "Perform quick shoulder and wrist rolls."
                    );
                }

                return new HealthAdvisorOutput(
                        assessment.length() > 0 ? assessment.toString() : "Screen time within healthy monitoring parameters.",
                        List.of("Eye fatigue", "Sedentary posture"),
                        suggestions,
                        true
                );
            }
        } catch (Throwable t) {
            logger.warn("Failed to generate live Gemini insights: {}", t.getMessage());
        }

        return getFallbackInsights(activeMinutesToday, topApp);
    }

    private HealthAdvisorOutput getFallbackInsights(long activeMinutesToday, String topApp) {
        String assessment;
        if (activeMinutesToday > 360) {
            assessment = String.format("High screen time detected (%d mins today). Regular posture checks and eye rests are strongly advised.", activeMinutesToday);
        } else if (activeMinutesToday > 180) {
            assessment = String.format("Moderate screen time logged (%d mins today). Maintain hydration and take periodic micro-breaks.", activeMinutesToday);
        } else {
            assessment = String.format("Light screen time recorded so far (%d mins today). Great pace!", activeMinutesToday);
        }

        List<String> suggestions = List.of(
                "20-20-20 Rule: Every 20 minutes, look at an object 20 feet away for 20 seconds.",
                "Ergonomic Alignment: Position your screen at eye level and relax your shoulder muscles.",
                "Hydration Cadence: Drink a fresh glass of water to keep energy and focus high.",
                "Physical Mobility: Stand up and do light stretches every 45-60 minutes."
        );

        return new HealthAdvisorOutput(
                assessment,
                List.of("Prolonged focus", "Neck strain"),
                suggestions,
                false
        );
    }

    /**
     * Generates a single context-aware health suggestion for notifications or widget display.
     */
    public CompletableFuture<HealthSuggestion> generateSuggestion(long activeMinutesToday, String topApp) {
        if (!configManager.getConfig().isAiEnabled() || !geminiClient.isApiKeyAvailable()) {
            return CompletableFuture.completedFuture(getDefaultSuggestion(activeMinutesToday));
        }

        String prompt = String.format(
                "You are an ergonomic and digital wellness assistant. The user has used their screen for %d minutes today, " +
                "predominantly on '%s'. Give a brief (2 sentences), actionable, encouraging wellness tip (e.g. 20-20-20 rule, posture, hydration).",
                activeMinutesToday, topApp
        );

        return geminiClient.generateContentAsync(prompt)
                .thenApply(response -> new HealthSuggestion("AI Health Tip", response, "Ergonomics"))
                .exceptionally(ex -> getDefaultSuggestion(activeMinutesToday));
    }

    public HealthSuggestion getDefaultSuggestion(long activeMinutesToday) {
        List<HealthSuggestion> fallbackTips = List.of(
                new HealthSuggestion("20-20-20 Rule", "Every 20 minutes, look at something 20 feet away for at least 20 seconds to reduce eye strain.", "Eye Health"),
                new HealthSuggestion("Hydration Reminder", "Drink a glass of water to stay hydrated and refreshed during long work sessions.", "General Wellness"),
                new HealthSuggestion("Posture Check", "Check your posture: relax your shoulders, align your neck, and ensure feet are flat on the floor.", "Ergonomics"),
                new HealthSuggestion("Quick Stretch", "Stand up, stretch your arms above your head, and roll your wrists to relieve muscle tension.", "Physical Health")
        );
        int index = (int) ((activeMinutesToday / 30) % fallbackTips.size());
        return fallbackTips.get(index);
    }
}
