package com.screentime.ui.onboarding;

import com.screentime.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OnboardingModelTest {

    private OnboardingModel model;

    @BeforeEach
    void setUp() {
        model = new OnboardingModel();
    }

    @Test
    void testValidateDailyLimit() {
        assertTrue(OnboardingModel.validateDailyLimit("480").valid());
        assertTrue(OnboardingModel.validateDailyLimit("60").valid());
        assertTrue(OnboardingModel.validateDailyLimit("1440").valid());

        assertFalse(OnboardingModel.validateDailyLimit("0").valid());
        assertFalse(OnboardingModel.validateDailyLimit("-30").valid());
        assertFalse(OnboardingModel.validateDailyLimit("1500").valid());
        assertFalse(OnboardingModel.validateDailyLimit("abc").valid());
        assertFalse(OnboardingModel.validateDailyLimit("").valid());
        assertFalse(OnboardingModel.validateDailyLimit(null).valid());
    }

    @Test
    void testValidateThresholds() {
        assertTrue(OnboardingModel.validateThresholds("50, 75, 90, 100").valid());
        assertTrue(OnboardingModel.validateThresholds("75, 90, 100").valid());
        assertTrue(OnboardingModel.validateThresholds("50, 80").valid());
        assertTrue(OnboardingModel.validateThresholds("100").valid());

        // Invalid: not ascending
        assertFalse(OnboardingModel.validateThresholds("100, 50").valid());
        assertFalse(OnboardingModel.validateThresholds("50, 50, 90").valid());

        // Invalid: out of bounds
        assertFalse(OnboardingModel.validateThresholds("0, 50, 100").valid());
        assertFalse(OnboardingModel.validateThresholds("50, 75, 120").valid());

        // Invalid formatting
        assertFalse(OnboardingModel.validateThresholds("50, abc, 100").valid());
        assertFalse(OnboardingModel.validateThresholds("").valid());
        assertFalse(OnboardingModel.validateThresholds(null).valid());
    }

    @Test
    void testValidateIdleThreshold() {
        assertTrue(OnboardingModel.validateIdleThreshold("60").valid());
        assertTrue(OnboardingModel.validateIdleThreshold("30").valid());
        assertTrue(OnboardingModel.validateIdleThreshold("1800").valid());

        assertFalse(OnboardingModel.validateIdleThreshold("5").valid());
        assertFalse(OnboardingModel.validateIdleThreshold("2000").valid());
        assertFalse(OnboardingModel.validateIdleThreshold("xyz").valid());
        assertFalse(OnboardingModel.validateIdleThreshold("").valid());
        assertFalse(OnboardingModel.validateIdleThreshold(null).valid());
    }

    @Test
    void testValidateExtensions() {
        assertTrue(OnboardingModel.validateExtensions("3", "120").valid());
        assertTrue(OnboardingModel.validateExtensions("0", "0").valid());
        assertTrue(OnboardingModel.validateExtensions("5", "300").valid());

        assertFalse(OnboardingModel.validateExtensions("-1", "120").valid());
        assertFalse(OnboardingModel.validateExtensions("3", "-50").valid());
        assertFalse(OnboardingModel.validateExtensions("25", "120").valid());
        assertFalse(OnboardingModel.validateExtensions("3", "800").valid());
        assertFalse(OnboardingModel.validateExtensions("a", "b").valid());
    }

    @Test
    void testApplyToConfig() {
        AppConfig config = new AppConfig();
        config.setOnboardingCompleted(false);

        model.setDailyLimitMinutes(360);
        model.setWarningThresholds(List.of(60, 80, 100));
        model.setIdleThresholdSeconds(45);
        model.setAllowExtensions(true);
        model.setMaxExtensionsPerDay(4);
        model.setMaxExtensionMinutesPerDay(180);
        model.setAiEnabled(true);
        model.setGeminiApiKey("test_key_123");

        model.applyToConfig(config);

        assertEquals(360, config.getDailyLimitMinutes());
        assertEquals(List.of(60, 80, 100), config.getWarningThresholds());
        assertEquals(45, config.getIdleThresholdSeconds());
        assertEquals(4, config.getMaxExtensionsPerDay());
        assertEquals(180, config.getMaxExtensionMinutesPerDay());
        assertTrue(config.isAiEnabled());
        assertEquals("test_key_123", config.getGeminiApiKey());
        assertTrue(config.isOnboardingCompleted());
    }

    @Test
    void testApplyToConfigWhenExtensionsDisabled() {
        AppConfig config = new AppConfig();
        model.setAllowExtensions(false);
        model.setMaxExtensionsPerDay(4);
        model.setMaxExtensionMinutesPerDay(180);

        model.applyToConfig(config);

        assertEquals(0, config.getMaxExtensionsPerDay());
        assertEquals(0, config.getMaxExtensionMinutesPerDay());
    }
}
