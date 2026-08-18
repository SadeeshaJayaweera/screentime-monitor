package com.screentime.ai;

public class HealthSuggestionService {
    private final GeminiClient geminiClient;
    public HealthSuggestionService() { this(new GeminiClient()); }
    public HealthSuggestionService(GeminiClient gc) { this.geminiClient = gc; }
}
