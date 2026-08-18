package com.screentime.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates full health advisor assessment, concerns, and actionable suggestions.
 */
public class HealthAdvisorOutput {

    private final String assessment;
    private final List<String> concerns;
    private final List<String> suggestions;
    private final boolean liveGemini;

    public HealthAdvisorOutput(String assessment, List<String> concerns, List<String> suggestions, boolean liveGemini) {
        this.assessment = assessment != null ? assessment : "Screen time wellness analysis.";
        this.concerns = concerns != null ? concerns : new ArrayList<>();
        this.suggestions = suggestions != null ? suggestions : new ArrayList<>();
        this.liveGemini = liveGemini;
    }

    public String getAssessment() {
        return assessment;
    }

    public List<String> getConcerns() {
        return concerns;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public boolean isLiveGemini() {
        return liveGemini;
    }
}
