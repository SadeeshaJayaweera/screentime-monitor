package com.screentime.ai;

import java.time.Instant;

/**
 * Model representing an AI-generated health or posture suggestion.
 */
public class HealthSuggestion {

    private String title;
    private String message;
    private String category;
    private Instant createdAt;

    public HealthSuggestion() {
        this.createdAt = Instant.now();
    }

    public HealthSuggestion(String title, String message, String category) {
        this.title = title;
        this.message = message;
        this.category = category;
        this.createdAt = Instant.now();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "HealthSuggestion{" +
                "title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", category='" + category + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
