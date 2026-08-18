package com.screentime.ui.onboarding;

import com.screentime.config.AppConfig;
import com.screentime.config.ConfigManager;
import com.screentime.restriction.RestrictionConfig;
import com.screentime.restriction.RestrictionEngine;
import com.screentime.ui.TimeFormatUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * First-run onboarding wizard providing a 7-step guided setup for screen time limits,
 * warning thresholds, idle sensitivity, extension rules, and optional AI features.
 */
public class OnboardingWizard {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingWizard.class);
    private static final int TOTAL_STEPS = 7;

    private final Stage stage;
    private final OnboardingModel model;
    private final Runnable onComplete;
    private final RestrictionEngine restrictionEngine;

    private int currentStep = 1;

    // Header UI elements
    private Label stepIndicatorLabel;
    private Label stepTitleLabel;
    private ProgressBar stepProgressBar;

    // Content container
    private StackPane contentArea;

    // Footer UI elements
    private Label errorLabel;
    private Button btnBack;
    private Button btnNext;

    // Step 2 controls
    private TextField txtDailyLimit;

    // Step 3 controls
    private TextField txtThresholds;

    // Step 4 controls
    private TextField txtIdleTimeout;

    // Step 5 controls
    private CheckBox chkAllowExtensions;
    private TextField txtMaxExtensions;
    private TextField txtMaxExtensionMinutes;

    // Step 6 controls
    private CheckBox chkAiEnabled;
    private PasswordField txtApiKey;
    private Button btnToggleApiKey;
    private boolean apiKeyMasked = true;

    // Step 7 summary labels
    private Label lblSummaryLimit;
    private Label lblSummaryThresholds;
    private Label lblSummaryIdle;
    private Label lblSummaryExtensions;
    private Label lblSummaryAi;

    public OnboardingWizard(Stage parentStage, RestrictionEngine restrictionEngine, Runnable onComplete) {
        this.restrictionEngine = restrictionEngine;
        this.onComplete = onComplete;
        this.model = new OnboardingModel();

        this.stage = new Stage();
        this.stage.setTitle("ScreenTime Monitor — Setup Wizard");
        if (parentStage != null) {
            this.stage.initModality(Modality.APPLICATION_MODAL);
            this.stage.initOwner(parentStage);
        }
        this.stage.setResizable(false);

        buildUi();
    }

    public static void show(Stage parentStage, RestrictionEngine restrictionEngine, Runnable onComplete) {
        OnboardingWizard wizard = new OnboardingWizard(parentStage, restrictionEngine, onComplete);
        wizard.stage.show();
    }

    private void buildUi() {
        BorderPane root = new BorderPane();
        root.setPrefSize(680, 520);
        root.setStyle("-fx-background-color: #0b0f19; -fx-font-family: sans-serif;");

        // --- TOP HEADER ---
        VBox headerBox = new VBox(6);
        headerBox.setPadding(new Insets(20, 28, 12, 28));
        headerBox.setStyle("-fx-background-color: #111827; -fx-border-color: #1f2937; -fx-border-width: 0 0 1px 0;");

        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label brandLabel = new Label("⏱️ ScreenTime Monitor");
        brandLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        stepIndicatorLabel = new Label("Step 1 of " + TOTAL_STEPS);
        stepIndicatorLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");

        titleRow.getChildren().addAll(brandLabel, headerSpacer, stepIndicatorLabel);

        stepTitleLabel = new Label("Welcome & Privacy Commitment");
        stepTitleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #f8fafc;");

        stepProgressBar = new ProgressBar(1.0 / TOTAL_STEPS);
        stepProgressBar.setMaxWidth(Double.MAX_VALUE);
        stepProgressBar.setStyle("-fx-pref-height: 5px; -fx-accent: #38bdf8;");

        headerBox.getChildren().addAll(titleRow, stepTitleLabel, stepProgressBar);
        root.setTop(headerBox);

        // --- CENTER CONTENT ---
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(20, 28, 16, 28));
        root.setCenter(contentArea);

        // --- BOTTOM FOOTER ---
        HBox footerBox = new HBox(12);
        footerBox.setAlignment(Pos.CENTER_LEFT);
        footerBox.setPadding(new Insets(14, 28, 18, 28));
        footerBox.setStyle("-fx-background-color: #111827; -fx-border-color: #1f2937; -fx-border-width: 1px 0 0 0;");

        errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px; -fx-font-weight: bold;");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        btnBack = new Button("← Back");
        btnBack.setDisable(true);
        btnBack.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #cbd5e1; -fx-padding: 8 16; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        btnBack.setOnAction(e -> goBack());

        btnNext = new Button("Next →");
        btnNext.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        btnNext.setOnAction(e -> goNext());

        footerBox.getChildren().addAll(errorLabel, footerSpacer, btnBack, btnNext);
        root.setBottom(footerBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        loadStep(1);
    }

    private void loadStep(int step) {
        currentStep = step;
        errorLabel.setText("");
        stepIndicatorLabel.setText("Step " + step + " of " + TOTAL_STEPS);
        stepProgressBar.setProgress((double) step / TOTAL_STEPS);
        btnBack.setDisable(step == 1);

        if (step == TOTAL_STEPS) {
            btnNext.setText("🚀 Finish Setup & Open ScreenTime");
            btnNext.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            btnNext.setText("Next →");
            btnNext.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        }

        contentArea.getChildren().clear();

        switch (step) {
            case 1 -> {
                stepTitleLabel.setText("Welcome & Privacy Commitment");
                contentArea.getChildren().add(createStep1View());
            }
            case 2 -> {
                stepTitleLabel.setText("Set Your Daily Screen Time Limit");
                contentArea.getChildren().add(createStep2View());
            }
            case 3 -> {
                stepTitleLabel.setText("Configure Warning Thresholds");
                contentArea.getChildren().add(createStep3View());
            }
            case 4 -> {
                stepTitleLabel.setText("Idle Detection Sensitivity");
                contentArea.getChildren().add(createStep4View());
            }
            case 5 -> {
                stepTitleLabel.setText("Daily Extension Rules");
                contentArea.getChildren().add(createStep5View());
            }
            case 6 -> {
                stepTitleLabel.setText("AI Health & Wellness (Optional)");
                contentArea.getChildren().add(createStep6View());
            }
            case 7 -> {
                stepTitleLabel.setText("Configuration Summary");
                contentArea.getChildren().add(createStep7View());
            }
        }
    }

    private void goNext() {
        errorLabel.setText("");

        // Validate current step before advancing
        switch (currentStep) {
            case 2 -> {
                OnboardingModel.ValidationResult res = OnboardingModel.validateDailyLimit(txtDailyLimit.getText());
                if (!res.valid()) {
                    errorLabel.setText(res.errorMessage());
                    return;
                }
                model.setDailyLimitMinutes(Integer.parseInt(txtDailyLimit.getText().trim()));
            }
            case 3 -> {
                OnboardingModel.ValidationResult res = OnboardingModel.validateThresholds(txtThresholds.getText());
                if (!res.valid()) {
                    errorLabel.setText(res.errorMessage());
                    return;
                }
                List<Integer> list = Arrays.stream(txtThresholds.getText().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                model.setWarningThresholds(list);
            }
            case 4 -> {
                OnboardingModel.ValidationResult res = OnboardingModel.validateIdleThreshold(txtIdleTimeout.getText());
                if (!res.valid()) {
                    errorLabel.setText(res.errorMessage());
                    return;
                }
                model.setIdleThresholdSeconds(Integer.parseInt(txtIdleTimeout.getText().trim()));
            }
            case 5 -> {
                OnboardingModel.ValidationResult res = OnboardingModel.validateExtensions(
                        txtMaxExtensions.getText(), txtMaxExtensionMinutes.getText());
                if (!res.valid()) {
                    errorLabel.setText(res.errorMessage());
                    return;
                }
                model.setAllowExtensions(chkAllowExtensions.isSelected());
                model.setMaxExtensionsPerDay(Integer.parseInt(txtMaxExtensions.getText().trim()));
                model.setMaxExtensionMinutesPerDay(Integer.parseInt(txtMaxExtensionMinutes.getText().trim()));
            }
            case 6 -> {
                model.setAiEnabled(chkAiEnabled.isSelected());
                model.setGeminiApiKey(txtApiKey.getText().trim());
            }
            case 7 -> {
                finishOnboarding();
                return;
            }
        }

        loadStep(currentStep + 1);
    }

    private void goBack() {
        if (currentStep > 1) {
            loadStep(currentStep - 1);
        }
    }

    private void finishOnboarding() {
        logger.info("Finishing onboarding wizard and applying configuration...");

        // 1. Update centralized AppConfig & save to disk
        AppConfig config = ConfigManager.getInstance().getConfig();
        model.applyToConfig(config);
        ConfigManager.getInstance().saveConfig();

        // 2. Synchronize running restriction engine
        if (restrictionEngine != null) {
            RestrictionConfig rConfig = restrictionEngine.getConfig();
            rConfig.setDailyLimitMinutes(model.getDailyLimitMinutes());
            rConfig.setWarningThresholds(model.getWarningThresholds());
            rConfig.setMaxExtensionsPerDay(model.isAllowExtensions() ? model.getMaxExtensionsPerDay() : 0);
            rConfig.setMaxExtensionMinutesPerDay(model.isAllowExtensions() ? model.getMaxExtensionMinutesPerDay() : 0);
        }

        stage.close();

        if (onComplete != null) {
            onComplete.run();
        }
    }

    // --- STEP VIEW BUILDERS ---

    private VBox createStep1View() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.TOP_LEFT);

        Label desc = new Label("ScreenTime Monitor tracks your daily active screen time, reminds you to take ergonomic breaks, and helps you maintain healthy digital habits.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-line-spacing: 3px;");

        VBox privacyCard = new VBox(10);
        privacyCard.setStyle("-fx-background-color: #131d31; -fx-padding: 16; -fx-background-radius: 10; -fx-border-color: #1e293b; -fx-border-radius: 10;");

        Label privacyTitle = new Label("🔒 100% Privacy & Local Storage Commitment");
        privacyTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4ade80;");

        Label privacyBody = new Label(
                "• All tracked applications and sessions are stored strictly on your local disk in an encrypted/private SQLite database.\n" +
                "• No telemetry, user habits, or analytics ever leave your device.\n" +
                "• If you choose to enable AI wellness tips (Step 6), only anonymized screen time duration numbers are sent to Google Gemini."
        );
        privacyBody.setWrapText(true);
        privacyBody.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-line-spacing: 4px;");

        privacyCard.getChildren().addAll(privacyTitle, privacyBody);

        box.getChildren().addAll(desc, privacyCard);
        return box;
    }

    private VBox createStep2View() {
        VBox box = new VBox(14);

        Label desc = new Label("Choose your target daily screen time limit. Once reached, ScreenTime Monitor will alert you with visual warnings.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        Label presetLabel = new Label("Quick Presets:");
        presetLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");

        txtDailyLimit = new TextField(String.valueOf(model.getDailyLimitMinutes()));
        txtDailyLimit.setPromptText("Minutes (e.g. 480 for 8h)");
        txtDailyLimit.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 6;");

        HBox presetBox = new HBox(10);
        presetBox.getChildren().addAll(
                createPresetBtn("4 Hours (240m)", 240, txtDailyLimit),
                createPresetBtn("6 Hours (360m)", 360, txtDailyLimit),
                createPresetBtn("8 Hours (480m)", 480, txtDailyLimit),
                createPresetBtn("10 Hours (600m)", 600, txtDailyLimit)
        );

        Label customLabel = new Label("Custom Daily Limit (in Minutes):");
        customLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");

        box.getChildren().addAll(desc, presetLabel, presetBox, customLabel, txtDailyLimit);
        return box;
    }

    private VBox createStep3View() {
        VBox box = new VBox(14);

        Label desc = new Label("Choose at what percentages of your daily limit you would like to receive progressive desktop warning notifications.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        Label presetLabel = new Label("Common Threshold Configurations:");
        presetLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");

        String currentStr = model.getWarningThresholds().stream().map(String::valueOf).collect(Collectors.joining(", "));
        txtThresholds = new TextField(currentStr);
        txtThresholds.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 6;");

        HBox presetBox = new HBox(10);
        presetBox.getChildren().addAll(
                createThresholdPresetBtn("Standard (50, 75, 90, 100%)", "50, 75, 90, 100", txtThresholds),
                createThresholdPresetBtn("Gentle (75, 90, 100%)", "75, 90, 100", txtThresholds),
                createThresholdPresetBtn("Strict (50, 70, 85, 95, 100%)", "50, 70, 85, 95, 100", txtThresholds)
        );

        Label customLabel = new Label("Comma-Separated Threshold Percentages:");
        customLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");

        box.getChildren().addAll(desc, presetLabel, presetBox, customLabel, txtThresholds);
        return box;
    }

    private VBox createStep4View() {
        VBox box = new VBox(14);

        Label desc = new Label("Idle sensitivity defines how many seconds of zero keyboard or mouse interaction must pass before active screen time tracking pauses automatically.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        Label presetLabel = new Label("Sensitivity Presets:");
        presetLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");

        txtIdleTimeout = new TextField(String.valueOf(model.getIdleThresholdSeconds()));
        txtIdleTimeout.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 6;");

        HBox presetBox = new HBox(10);
        presetBox.getChildren().addAll(
                createPresetBtn("30s (Strict)", 30, txtIdleTimeout),
                createPresetBtn("60s (Recommended)", 60, txtIdleTimeout),
                createPresetBtn("120s (Relaxed)", 120, txtIdleTimeout),
                createPresetBtn("300s (5 Mins)", 300, txtIdleTimeout)
        );

        Label customLabel = new Label("Idle Timeout Threshold (Seconds):");
        customLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");

        box.getChildren().addAll(desc, presetLabel, presetBox, customLabel, txtIdleTimeout);
        return box;
    }

    private VBox createStep5View() {
        VBox box = new VBox(14);

        Label desc = new Label("Allow requesting temporary screen time extensions on busy days, while maintaining healthy daily boundaries.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        chkAllowExtensions = new CheckBox("Allow user to request temporary screen time extensions");
        chkAllowExtensions.setSelected(model.isAllowExtensions());
        chkAllowExtensions.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 13px;");

        VBox limitsBox = new VBox(10);
        limitsBox.setStyle("-fx-background-color: #131d31; -fx-padding: 14; -fx-background-radius: 8;");

        Label countLabel = new Label("Maximum Extension Requests Allowed Per Day:");
        countLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");

        txtMaxExtensions = new TextField(String.valueOf(model.getMaxExtensionsPerDay()));
        txtMaxExtensions.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 6 10; -fx-background-radius: 6;");

        Label minsLabel = new Label("Maximum Cumulative Extension Minutes Per Day:");
        minsLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");

        txtMaxExtensionMinutes = new TextField(String.valueOf(model.getMaxExtensionMinutesPerDay()));
        txtMaxExtensionMinutes.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 6 10; -fx-background-radius: 6;");

        limitsBox.getChildren().addAll(countLabel, txtMaxExtensions, minsLabel, txtMaxExtensionMinutes);

        chkAllowExtensions.setOnAction(e -> limitsBox.setDisable(!chkAllowExtensions.isSelected()));

        box.getChildren().addAll(desc, chkAllowExtensions, limitsBox);
        return box;
    }

    private VBox createStep6View() {
        VBox box = new VBox(14);

        Label desc = new Label("ScreenTime Monitor can generate intelligent ergonomic break tips, eye-fatigue warnings, and posture recommendations using Google Gemini AI.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        chkAiEnabled = new CheckBox("Enable AI Health & Ergonomic Suggestions");
        chkAiEnabled.setSelected(model.isAiEnabled());
        chkAiEnabled.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label apiLabel = new Label("Gemini API Key (Optional):");
        apiLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");

        txtApiKey = new PasswordField();
        txtApiKey.setText(model.getGeminiApiKey());
        txtApiKey.setPromptText("Paste your Gemini API key here");
        txtApiKey.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 6;");

        btnToggleApiKey = new Button("👁️");
        btnToggleApiKey.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 6;");

        HBox apiRow = new HBox(8, txtApiKey, btnToggleApiKey);
        HBox.setHgrow(txtApiKey, Priority.ALWAYS);

        Label noteLabel = new Label("💡 Note: A Gemini API key is free from Google AI Studio (aistudio.google.com). If you don't have one right now, you can leave it blank and add it later in the Settings tab.");
        noteLabel.setWrapText(true);
        noteLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-style: italic;");

        box.getChildren().addAll(desc, chkAiEnabled, apiLabel, apiRow, noteLabel);
        return box;
    }

    private VBox createStep7View() {
        VBox box = new VBox(14);

        Label desc = new Label("Review your selected preferences below. You can reconfigure any of these settings anytime from the Settings tab.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        VBox summaryCard = new VBox(10);
        summaryCard.setStyle("-fx-background-color: #131d31; -fx-padding: 16; -fx-background-radius: 10; -fx-border-color: #1e293b; -fx-border-radius: 10;");

        lblSummaryLimit = new Label("• Daily Screen Time Limit: " + TimeFormatUtils.formatDurationMinutes(model.getDailyLimitMinutes()) + " (" + model.getDailyLimitMinutes() + " mins)");
        lblSummaryThresholds = new Label("• Warning Thresholds: " + model.getWarningThresholds().stream().map(s -> s + "%").collect(Collectors.joining(", ")));
        lblSummaryIdle = new Label("• Idle Sensitivity: " + model.getIdleThresholdSeconds() + " seconds");
        lblSummaryExtensions = new Label("• Extensions: " + (model.isAllowExtensions() ? ("Up to " + model.getMaxExtensionsPerDay() + " / day (" + model.getMaxExtensionMinutesPerDay() + "m max)") : "Disabled"));
        lblSummaryAi = new Label("• AI Wellness Insights: " + (model.isAiEnabled() ? "Enabled" : "Disabled"));

        for (Label lbl : List.of(lblSummaryLimit, lblSummaryThresholds, lblSummaryIdle, lblSummaryExtensions, lblSummaryAi)) {
            lbl.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-font-weight: 600;");
        }

        summaryCard.getChildren().addAll(lblSummaryLimit, lblSummaryThresholds, lblSummaryIdle, lblSummaryExtensions, lblSummaryAi);

        box.getChildren().addAll(desc, summaryCard);
        return box;
    }

    private Button createPresetBtn(String label, int value, TextField target) {
        Button btn = new Button(label);
        btn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;");
        btn.setOnAction(e -> target.setText(String.valueOf(value)));
        return btn;
    }

    private Button createThresholdPresetBtn(String label, String value, TextField target) {
        Button btn = new Button(label);
        btn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;");
        btn.setOnAction(e -> target.setText(value));
        return btn;
    }
}
