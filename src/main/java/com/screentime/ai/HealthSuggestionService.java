package com.screentime.ai;
import java.util.List;

public class HealthSuggestionService {
    public HealthAdvisorOutput generateOfflineInsights(long sec, List<com.screentime.data.AppUsage> apps) {
        return new HealthAdvisorOutput("Overview", List.of(), List.of("Posture checks", "Ergonomic alignment"), "Offline Rules");
    }
}
