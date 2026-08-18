package com.screentime.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HealthSuggestionServiceTest {

    @Test
    void testGetDefaultSuggestion() {
        HealthSuggestionService service = new HealthSuggestionService();
        HealthSuggestion tip1 = service.getDefaultSuggestion(15);
        assertNotNull(tip1);
        assertNotNull(tip1.getTitle());
        assertNotNull(tip1.getMessage());

        HealthSuggestion tip2 = service.getDefaultSuggestion(70);
        assertNotNull(tip2);
    }
}
