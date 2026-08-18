package com.screentime.ai;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HealthSuggestionServiceTest {
    @Test void testOffline() {
        assertNotNull(new HealthSuggestionService().generateOfflineInsights(3600, List.of()));
    }
}
