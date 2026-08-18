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
        this.stage.setTitle("ScreenTime Monitor — First-Run Setup");
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
        root.setPrefSize(720, 560);
        root.setStyle("-fx-background-color: #0b0f19; -fx-font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;");

        // --- TOP HEADER ---
        VBox headerBox = new VBox(8);
        headerBox.setPadding(new Insets(24, 32, 16, 32));
        headerBox.setStyle("-fx-background-color: linear-gradient(to bottom, #111827, #0d131f); -fx-border-color: #1f2937; -fx-border-width: 0 0 1px 0;");

        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label brandLabel = new Label("⏱️  ScreenTime Monitor");
        brandLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #38bdf8; -fx-letter-spacing: -0.3px;");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        stepIndicatorLabel = new Label("Step 1 of " + TOTAL_STEPS);
        stepIndicatorLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #38bdf8; -fx-background-color: rgba(56, 189, 248, 0.14); -fx-background-radius: 12px; -fx-padding: 3px 10px;");

        titleRow.getChildren().addAll(brandLabel, headerSpacer, stepIndicatorLabel);

        stepTitleLabel = new Label("Welcome & Privacy Commitment");
        stepTitleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #f8fafc; -fx-letter-spacing: -0.4px;");

        stepProgressBar = new ProgressBar(1.0 / TOTAL_STEPS);
        stepProgressBar.setMaxWidth(Double.MAX_VALUE);
        stepProgressBar.setStyle("-fx-pref-height: 5px; -fx-accent: #38bdf8;");

        headerBox.getChildren().addAll(titleRow, stepTitleLabel, stepProgressBar);
        root.setTop(headerBox);

        // --- CENTER CONTENT ---
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(24, 32, 20, 32));
        root.setCenter(contentArea);

        // --- BOTTOM FOOTER ---
        HBox footerBox = new HBox(14);
        footerBox.setAlignment(Pos.CENTER_LEFT);
        footerBox.setPadding(new Insets(16, 32, 20, 32));
        footerBox.setStyle("-fx-background-color: #111827; -fx-border-color: #1f2937; -fx-border-width: 1px 0 0 0;");

        errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px; -fx-font-weight: 700;");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        btnBack = new Button("← Back");
        btnBack.setDisable(true);
        btnBack.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #cbd5e1; -fx-padding: 9 18; -fx-background-radius: 8; -fx-font-weight: 700; -fx-cursor: hand; -fx-border-color: #334155; -fx-border-radius: 8;");
        btnBack.setOnAction(e -> goBack());

        btnNext = new Button("Continue →");
        btnNext.setStyle("-fx-background-color: linear-gradient(to bottom, #38bdf8, #0284c7); -fx-text-fill: white; -fx-padding: 9 22; -fx-background-radius: 8; -fx-font-weight: 800; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(2, 132, 199, 0.35), 8, 0, 0, 2);");
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
            btnNext.setText("🚀 Launch ScreenTime");
            btnNext.setStyle("-fx-background-color: linear-gradient(to bottom, #22c55e, #16a34a); -fx-text-fill: white; -fx-padding: 9 24; -fx-background-radius: 8; -fx-font-weight: 800; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(34, 197, 94, 0.35), 8, 0, 0, 2);");
        } else {
            btnNext.setText("Continue →");
            btnNext.setStyle("-fx-background-color: linear-gradient(to bottom, #38bdf8, #0284c7); -fx-text-fill: white; -fx-padding: 9 22; -fx-background-radius: 8; -fx-font-weight: 800; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(2, 132, 199, 0.35), 8, 0, 0, 2);");
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
        VBox box = new VBox(16);
        box.setAlignment(Pos.TOP_LEFT);

        Label desc = new Label("ScreenTime Monitor tracks your active foreground applications, encourages ergonomic posture breaks, and helps maintain a balanced digital lifestyle.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-line-spacing: 4px;");

        VBox privacyCard = new VBox(12);
        privacyCard.setStyle("-fx-background-color: linear-gradient(to bottom, #131d31, #0f1626); -fx-padding: 18 20; -fx-background-radius: 12; -fx-border-color: rgba(74, 222, 128, 0.3); -fx-border-radius: 12;");

        Label privacyTitle = new Label("🔒 100% Local Storage & Zero-Telemetry Pledge");
        privacyTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #4ade80;");

        Label privacyBody = new Label(
                "• All tracked window titles and application logs are saved exclusively in your local SQLite database.\n" +
                "• No telemetry, behavioral analytics, or sensitive personal data ever leave your computer.\n" +
                "• When AI suggestions are active (Step 6), only aggregated time duration numbers are sent to Google Gemini."
        );
        privacyBody.setWrapText(true);
        privacyBody.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-line-spacing: 5px;");

        privacyCard.getChildren().addAll(privacyTitle, privacyBody);
        box.getChildren().addAll(desc, privacyCard);
        return box;
    }

    private VBox createStep2View() {
        VBox box = new VBox(16);

        Label desc = new Label("Set your daily computer screen time limit. Once reached, ScreenTime Monitor delivers notifications and optional full-screen break prompts.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        Label presetLabel = new Label("QUICK PRESETS");
        presetLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-letter-spacing: 0.8px;");

        txtDailyLimit = new TextField(String.valueOf(model.getDailyLimitMinutes()));
        txtDailyLimit.setPromptText("Minutes (e.g. 480 for 8 hours)");
        txtDailyLimit.setStyle("-fx-background-color: #131d31; -fx-text-fill: white; -fx-padding: 9 14; -fx-background-radius: 8; -fx-border-color: #27354a; -fx-border-radius: 8; -fx-font-size: 13px; -fx-font-weight: 700;");

        HBox presetBox = new HBox(8);
        presetBox.getChildren().addAll(
                createPresetBtn("4h (240m)", 240, txtDailyLimit),
                createPresetBtn("6h (360m)", 360, txtDailyLimit),
                createPresetBtn("8h (480m)", 480, txtDailyLimit),
                createPresetBtn("10h (600m)", 600, txtDailyLimit)
        );

        Label customLabel = new Label("CUSTOM DAILY LIMIT (MINUTES):");
        customLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #cbd5e1; -fx-letter-spacing: 0.8px;");

        box.getChildren().addAll(desc, presetLabel, presetBox, customLabel, txtDailyLimit);
        return box;
    }

    private VBox createStep3View() {
        VBox box = new VBox(16);

        Label desc = new Label("Choose progressive warning milestones (% of daily goal) at which you would like to receive non-intrusive desktop alerts.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        Label presetLabel = new Label("THRESHOLD PROFILES");
        presetLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-letter-spacing: 0.8px;");

        String currentStr = model.getWarningThresholds().stream().map(String::valueOf).collect(Collectors.joining(", "));
        txtThresholds = new TextField(currentStr);
        txtThresholds.setStyle("-fx-background-color: #131d31; -fx-text-fill: white; -fx-padding: 9 14; -fx-background-radius: 8; -fx-border-color: #27354a; -fx-border-radius: 8; -fx-font-size: 13px; -fx-font-weight: 700;");

        HBox presetBox = new HBox(8);
        presetBox.getChildren().addAll(
                createThresholdPresetBtn("Standard (50, 75, 90, 100%)", "50, 75, 90, 100", txtThresholds),
                createThresholdPresetBtn("Gentle (75, 90, 100%)", "75, 90, 100", txtThresholds),
                createThresholdPresetBtn("Strict (50, 70, 85, 95, 100%)", "50, 70, 85, 95, 100", txtThresholds)
        );

        Label customLabel = new Label("COMMA-SEPARATED PERCENTAGES:");
        customLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #cbd5e1; -fx-letter-spacing: 0.8px;");

        box.getChildren().addAll(desc, presetLabel, presetBox, customLabel, txtThresholds);
        return box;
    }

    private VBox createStep4View() {
        VBox box = new VBox(16);

        Label desc = new Label("Idle sensitivity defines how many seconds of zero keyboard or mouse interaction must elapse before screen time tracking pauses automatically.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        Label presetLabel = new Label("SENSITIVITY PRESETS");
        presetLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-letter-spacing: 0.8px;");

        txtIdleTimeout = new TextField(String.valueOf(model.getIdleThresholdSeconds()));
        txtIdleTimeout.setStyle("-fx-background-color: #131d31; -fx-text-fill: white; -fx-padding: 9 14; -fx-background-radius: 8; -fx-border-color: #27354a; -fx-border-radius: 8; -fx-font-size: 13px; -fx-font-weight: 700;");

        HBox presetBox = new HBox(8);
        presetBox.getChildren().addAll(
                createPresetBtn("30s (Strict)", 30, txtIdleTimeout),
                createPresetBtn("60s (Recommended)", 60, txtIdleTimeout),
                createPresetBtn("120s (Relaxed)", 120, txtIdleTimeout),
                createPresetBtn("300s (5 Mins)", 300, txtIdleTimeout)
        );

        Label customLabel = new Label("IDLE TIMEOUT THRESHOLD (SECONDS):");
        customLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #cbd5e1; -fx-letter-spacing: 0.8px;");

        box.getChildren().addAll(desc, presetLabel, presetBox, customLabel, txtIdleTimeout);
        return box;
    }

    private VBox createStep5View() {
        VBox box = new VBox(16);

        Label desc = new Label("Allow requesting temporary screen time extensions on demanding workdays, while enforcing strict daily caps.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        chkAllowExtensions = new CheckBox("Allow user to request temporary screen time extensions");
        chkAllowExtensions.setSelected(model.isAllowExtensions());
        chkAllowExtensions.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: 700; -fx-font-size: 13px;");

        VBox limitsBox = new VBox(12);
        limitsBox.setStyle("-fx-background-color: #131d31; -fx-padding: 16; -fx-background-radius: 10; -fx-border-color: #1e293b; -fx-border-radius: 10;");

        Label countLabel = new Label("MAXIMUM EXTENSION REQUESTS PER DAY:");
        countLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 700; -fx-letter-spacing: 0.8px;");

        txtMaxExtensions = new TextField(String.valueOf(model.getMaxExtensionsPerDay()));
        txtMaxExtensions.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #27354a; -fx-border-radius: 6; -fx-font-size: 13px;");

        Label minsLabel = new Label("MAXIMUM CUMULATIVE EXTENSION MINUTES PER DAY:");
        minsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 700; -fx-letter-spacing: 0.8px;");

        txtMaxExtensionMinutes = new TextField(String.valueOf(model.getMaxExtensionMinutesPerDay()));
        txtMaxExtensionMinutes.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #27354a; -fx-border-radius: 6; -fx-font-size: 13px;");

        limitsBox.getChildren().addAll(countLabel, txtMaxExtensions, minsLabel, txtMaxExtensionMinutes);
        chkAllowExtensions.setOnAction(e -> limitsBox.setDisable(!chkAllowExtensions.isSelected()));

        box.getChildren().addAll(desc, chkAllowExtensions, limitsBox);
        return box;
    }

    private VBox createStep6View() {
        VBox box = new VBox(16);

        Label desc = new Label("ScreenTime Monitor can synthesize daily computer habits to deliver personalized optical fatigue warnings (20-20-20 rule) and posture checks using Google Gemini AI.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        chkAiEnabled = new CheckBox("Enable AI Health & Ergonomic Suggestions");
        chkAiEnabled.setSelected(model.isAiEnabled());
        chkAiEnabled.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: 700; -fx-font-size: 13px;");

        Label apiLabel = new Label("GOOGLE GEMINI API KEY (OPTIONAL):");
        apiLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-letter-spacing: 0.8px;");

        txtApiKey = new PasswordField();
        txtApiKey.setText(model.getGeminiApiKey());
        txtApiKey.setPromptText("Paste your Gemini API key here (leave blank for offline rules)");
        txtApiKey.setStyle("-fx-background-color: #131d31; -fx-text-fill: white; -fx-padding: 9 14; -fx-background-radius: 8; -fx-border-color: #27354a; -fx-border-radius: 8; -fx-font-size: 13px;");

        btnToggleApiKey = new Button("👁️");
        btnToggleApiKey.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 9 14; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-radius: 8; -fx-cursor: hand;");

        HBox apiRow = new HBox(8, txtApiKey, btnToggleApiKey);
        HBox.setHgrow(txtApiKey, Priority.ALWAYS);

        Label noteLabel = new Label("💡 Note: Free Gemini API keys can be obtained at aistudio.google.com. If skipped, built-in offline clinical rules will provide break and ergonomic tips automatically.");
        noteLabel.setWrapText(true);
        noteLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-style: italic; -fx-line-spacing: 3px;");

        box.getChildren().addAll(desc, chkAiEnabled, apiLabel, apiRow, noteLabel);
        return box;
    }

    private VBox createStep7View() {
        VBox box = new VBox(16);

        Label desc = new Label("Review your configured preferences below. All settings can be adjusted at any time in the Preferences tab.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        VBox summaryCard = new VBox(12);
        summaryCard.setStyle("-fx-background-color: linear-gradient(to bottom, #131d31, #0f1626); -fx-padding: 18 20; -fx-background-radius: 12; -fx-border-color: #1e293b; -fx-border-radius: 12;");

        lblSummaryLimit = new Label("• Daily Screen Time Goal: " + TimeFormatUtils.formatDurationMinutes(model.getDailyLimitMinutes()) + " (" + model.getDailyLimitMinutes() + " mins)");
        lblSummaryThresholds = new Label("• Warning Thresholds: " + model.getWarningThresholds().stream().map(s -> s + "%").collect(Collectors.joining(", ")));
        lblSummaryIdle = new Label("• Idle Sensitivity: " + model.getIdleThresholdSeconds() + " seconds");
        lblSummaryExtensions = new Label("• Extensions: " + (model.isAllowExtensions() ? ("Up to " + model.getMaxExtensionsPerDay() + " / day (" + model.getMaxExtensionMinutesPerDay() + "m max)") : "Disabled"));
        lblSummaryAi = new Label("• AI Wellness Coach: " + (model.isAiEnabled() ? "Active" : "Disabled"));

        for (Label lbl : List.of(lblSummaryLimit, lblSummaryThresholds, lblSummaryIdle, lblSummaryExtensions, lblSummaryAi)) {
            lbl.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-font-weight: 600;");
        }

        summaryCard.getChildren().addAll(lblSummaryLimit, lblSummaryThresholds, lblSummaryIdle, lblSummaryExtensions, lblSummaryAi);
        box.getChildren().addAll(desc, summaryCard);
        return box;
    }

    private Button createPresetBtn(String label, int value, TextField target) {
        Button btn = new Button(label);
        btn.setStyle("-fx-background-color: #162032; -fx-text-fill: #38bdf8; -fx-font-weight: 800; -fx-font-size: 12px; -fx-padding: 7 14; -fx-background-radius: 20px; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 20px; -fx-cursor: hand;");
        btn.setOnAction(e -> target.setText(String.valueOf(value)));
        return btn;
    }

    private Button createThresholdPresetBtn(String label, String value, TextField target) {
        Button btn = new Button(label);
        btn.setStyle("-fx-background-color: #162032; -fx-text-fill: #38bdf8; -fx-font-weight: 800; -fx-font-size: 12px; -fx-padding: 7 14; -fx-background-radius: 20px; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 20px; -fx-cursor: hand;");
        btn.setOnAction(e -> target.setText(value));
        return btn;
    }
}
