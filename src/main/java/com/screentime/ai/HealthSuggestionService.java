package com.screentime.ai;
import java.util.List;

public class HealthSuggestionService {
    public HealthAdvisorOutput generateOfflineInsights(long sec, List<com.screentime.data.AppUsage> apps) {
        return new HealthAdvisorOutput("Overview", List.of(), List.of("20-20-20 rule"), "Offline Rules");
    }
}
