package com.screentime.ai;
import java.util.List;

public class HealthSuggestionService {
    private final GeminiClient geminiClient;
    public HealthSuggestionService() { this(new GeminiClient()); }
    public HealthSuggestionService(GeminiClient gc) { this.geminiClient = gc; }
    public HealthAdvisorOutput generateOfflineInsights(long sec, List<com.screentime.data.AppUsage> apps) {
        return new HealthAdvisorOutput("Overview", List.of(), List.of("Take breaks"), "Offline Rules");
    }
}
