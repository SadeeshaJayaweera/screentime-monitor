package com.screentime.ui;

import com.screentime.ai.HealthAdvisorOutput;
import com.screentime.ai.HealthSuggestionService;
import com.screentime.config.AppConfig;
import com.screentime.config.ConfigManager;
import com.screentime.core.ActivityState;
import com.screentime.core.TrackingEngine;
import com.screentime.core.TrackingListener;
import com.screentime.core.TrackingSession;
import com.screentime.core.WindowInfo;
import com.screentime.data.AppUsage;
import com.screentime.data.DailyUsageSummary;
import com.screentime.data.UsageDao;
import com.screentime.restriction.RestrictionConfig;
import com.screentime.restriction.RestrictionEngine;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for the primary JavaFX dashboard view.
 */
public class MainViewController implements TrackingListener {

    private static final Logger logger = LoggerFactory.getLogger(MainViewController.class);
    private static final DateTimeFormatter DATE_HEADER_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter CHART_DATE_FMT = DateTimeFormatter.ofPattern("MM/dd");

    // Injected Singletons / Services
    private TrackingEngine trackingEngine;
    private UsageDao usageDao;
    private RestrictionEngine restrictionEngine;
    private HealthSuggestionService healthSuggestionService;

    // Sidebar Navigation Buttons
    @FXML private Button btnNavToday;
    @FXML private Button btnNavHistory;
    @FXML private Button btnNavAi;
    @FXML private Button btnNavSettings;
    @FXML private Label sidebarStatusBadge;

    // Content Views
    @FXML private VBox todayView;
    @FXML private VBox historyView;
    @FXML private VBox aiView;
    @FXML private VBox settingsView;

    // Today View Elements
    @FXML private Label todayDateLabel;
    @FXML private Label currentAppBadge;
    @FXML private Label warningBanner;
    @FXML private StackPane progressRingContainer;
    @FXML private Label todayActiveTimeLabel;
    @FXML private ProgressBar todayProgressBar;
    @FXML private Label progressPercentLabel;
    @FXML private Label todayIdleTimeLabel;
    @FXML private Label todayRemainingTimeLabel;
    @FXML private Label todayLimitLabel;
    @FXML private Label todayExtensionsLabel;
    @FXML private VBox topAppsContainer;
    @FXML private Button btnPauseTracking;

    // History View Elements
    @FXML private Button btnRange7;
    @FXML private Button btnRange14;
    @FXML private Button btnRange30;
    @FXML private BarChart<String, Number> historyBarChart;
    @FXML private CategoryAxis historyCategoryAxis;
    @FXML private NumberAxis historyNumberAxis;
    @FXML private TableView<HistoryRow> historyTableView;
    @FXML private TableColumn<HistoryRow, String> colDate;
    @FXML private TableColumn<HistoryRow, String> colActiveTime;
    @FXML private TableColumn<HistoryRow, String> colIdleTime;
    @FXML private TableColumn<HistoryRow, String> colTopApp;

    // AI Insights View Elements
    @FXML private Label aiStatusBadge;
    @FXML private Label aiAssessmentText;
    @FXML private HBox aiConcernsContainer;
    @FXML private VBox aiSuggestionsContainer;
    @FXML private ProgressIndicator aiLoadingIndicator;

    private ProgressRing progressRing;

    // Settings View Elements
    @FXML private TextField settingDailyLimit;
    @FXML private TextField settingWarningThresholds;
    @FXML private TextField settingIdleThreshold;
    @FXML private CheckBox settingHardBlock;
    @FXML private TextField settingMaxExtensions;
    @FXML private TextField settingMaxExtensionMinutes;
    @FXML private CheckBox settingAutostart;
    @FXML private CheckBox settingAiEnabled;
    @FXML private PasswordField settingApiKey;
    @FXML private Button btnToggleApiKey;
    @FXML private CheckBox settingNotifications;
    @FXML private Label settingsSavedLabel;

    private int currentHistoryDays = 7;
    private boolean apiKeyMasked = true;
    private String rawApiKey = "";

    @FXML
    public void initialize() {
        if (usageDao == null) usageDao = new UsageDao();
        if (restrictionEngine == null) restrictionEngine = new RestrictionEngine();
        if (healthSuggestionService == null) healthSuggestionService = new HealthSuggestionService();

        if (progressRingContainer != null) {
            progressRing = new ProgressRing(165, 13);
            progressRingContainer.getChildren().add(progressRing);
        }

        setupHistoryTable();
        loadSettingsIntoUI();
        refreshTodayView();
    }

    public void injectServices(TrackingEngine trackingEngine,
                               UsageDao usageDao,
                               RestrictionEngine restrictionEngine,
                               HealthSuggestionService healthSuggestionService) {
        this.trackingEngine = trackingEngine;
        this.usageDao = usageDao;
        this.restrictionEngine = restrictionEngine;
        this.healthSuggestionService = healthSuggestionService;

        if (trackingEngine != null) {
            trackingEngine.addListener(this);
        }

        refreshTodayView();
        refreshTrackingStatusBadge();
    }

    // --- TRACKING LISTENER CALLBACKS ---

    @Override
    public void onActiveSecondsTick(long totalActiveSecondsToday) {
        Platform.runLater(() -> updateTodayMetrics(totalActiveSecondsToday));
    }

    @Override
    public void onWindowChanged(WindowInfo windowInfo) {
        Platform.runLater(() -> {
            if (currentAppBadge != null && windowInfo != null) {
                String glyph = resolveAppGlyph(windowInfo.getAppName());
                currentAppBadge.setText(glyph + "  Active: " + windowInfo.getAppName());
            }
        });
    }

    @Override
    public void onStateChanged(ActivityState oldState, ActivityState newState) {
        Platform.runLater(this::refreshTrackingStatusBadge);
    }

    @Override
    public void onSessionClosed(TrackingSession session) {
        Platform.runLater(this::refreshTopAppsList);
    }

    // --- NAVIGATION LOGIC ---

    @FXML
    public void showTodayTab() {
        setNavActive(btnNavToday);
        showView(todayView);
        refreshTodayView();
    }

    @FXML
    public void showHistoryTab() {
        setNavActive(btnNavHistory);
        showView(historyView);
        loadHistoryData(currentHistoryDays);
    }

    @FXML
    public void showAiTab() {
        setNavActive(btnNavAi);
        showView(aiView);
        loadAiInsights();
    }

    @FXML
    public void showSettingsTab() {
        setNavActive(btnNavSettings);
        showView(settingsView);
        loadSettingsIntoUI();
    }

    private void setNavActive(Button activeButton) {
        List<Button> buttons = List.of(btnNavToday, btnNavHistory, btnNavAi, btnNavSettings);
        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-button-active");
            }
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("nav-button-active")) {
            activeButton.getStyleClass().add("nav-button-active");
        }
    }

    private void showView(VBox targetView) {
        List<VBox> views = List.of(todayView, historyView, aiView, settingsView);
        for (VBox v : views) {
            if (v != null) {
                v.setVisible(v == targetView);
                v.setManaged(v == targetView);
            }
        }
    }

    // --- TODAY VIEW LOGIC ---

    public void refreshTodayView() {
        if (todayDateLabel != null) {
            todayDateLabel.setText(LocalDate.now().format(DATE_HEADER_FMT));
        }

        long activeSeconds = 0;
        if (usageDao != null) {
            activeSeconds = usageDao.getTodayUsageSeconds();
        }
        if (trackingEngine != null) {
            activeSeconds = Math.max(activeSeconds, trackingEngine.getTodayActiveSeconds());
        }

        updateTodayMetrics(activeSeconds);
        refreshTopAppsList();
    }

    private void updateTodayMetrics(long activeSeconds) {
        long activeMinutes = activeSeconds / 60;
        int effectiveLimit = restrictionEngine != null ? restrictionEngine.getEffectiveDailyLimit() : 480;
        long limitSeconds = effectiveLimit * 60L;

        if (todayActiveTimeLabel != null) {
            todayActiveTimeLabel.setText(TimeFormatUtils.formatDurationSeconds(activeSeconds));
        }

        long remainingMinutes = Math.max(0, effectiveLimit - activeMinutes);
        if (todayRemainingTimeLabel != null) {
            todayRemainingTimeLabel.setText(TimeFormatUtils.formatDurationMinutes(remainingMinutes));
        }

        if (todayLimitLabel != null) {
            todayLimitLabel.setText(TimeFormatUtils.formatDurationMinutes(effectiveLimit));
        }

        if (todayExtensionsLabel != null && restrictionEngine != null) {
            int extMins = restrictionEngine.getCumulativeExtensionMinutesToday();
            int extCount = restrictionEngine.getExtensionCountToday();
            if (extCount > 0) {
                todayExtensionsLabel.setText(String.format("%d granted (+%dm)", extCount, extMins));
            } else {
                todayExtensionsLabel.setText("None (+0m)");
            }
        }

        double progress = limitSeconds > 0 ? (double) activeSeconds / limitSeconds : 0.0;
        if (todayProgressBar != null) {
            todayProgressBar.setProgress(Math.min(1.0, progress));
        }

        int percentUsed = limitSeconds > 0 ? (int) ((activeSeconds * 100) / limitSeconds) : 0;
        if (progressPercentLabel != null) {
            progressPercentLabel.setText(percentUsed + "% of Daily Goal");
        }

        if (progressRing != null) {
            String activeTimeStr = TimeFormatUtils.formatDurationSeconds(activeSeconds);
            String percentStr = percentUsed + "% Used";
            progressRing.setProgress(progress, activeTimeStr, percentStr);
        }

        // Update dynamic warning banner & colors
        updateWarningBannerAndColors(percentUsed, remainingMinutes, effectiveLimit);
    }

    private void updateWarningBannerAndColors(int percentUsed, long remainingMinutes, int effectiveLimit) {
        if (warningBanner == null) return;

        if (percentUsed >= 100) {
            warningBanner.setText(String.format("🚨 Daily Limit Reached (%d mins). Please take an ergonomic break or request an extension!", effectiveLimit));
            warningBanner.getStyleClass().setAll("banner-critical");
            warningBanner.setVisible(true);
            warningBanner.setManaged(true);
            if (todayActiveTimeLabel != null) todayActiveTimeLabel.setStyle("-fx-text-fill: #f87171;");
        } else if (percentUsed >= 75) {
            warningBanner.setText(String.format("⚠️ Approaching Screen Limit: %d%% used (%d minutes remaining).", percentUsed, remainingMinutes));
            warningBanner.getStyleClass().setAll("banner-warning");
            warningBanner.setVisible(true);
            warningBanner.setManaged(true);
            if (todayActiveTimeLabel != null) todayActiveTimeLabel.setStyle("-fx-text-fill: #fbbf24;");
        } else {
            warningBanner.setVisible(false);
            warningBanner.setManaged(false);
            if (todayActiveTimeLabel != null) todayActiveTimeLabel.setStyle("-fx-text-fill: #38bdf8;");
        }
    }

    private void refreshTopAppsList() {
        if (topAppsContainer == null || usageDao == null) return;

        topAppsContainer.getChildren().clear();
        List<AppUsage> topApps = usageDao.getTopAppsForDate(LocalDate.now(), 5);

        if (topApps.isEmpty()) {
            Label emptyLabel = new Label("No application activity recorded yet today. Start working to see usage analytics.");
            emptyLabel.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic; -fx-padding: 12 0;");
            topAppsContainer.getChildren().add(emptyLabel);
            return;
        }

        long totalSecondsToday = topApps.stream().mapToLong(AppUsage::getSecondsUsed).sum();
        if (totalSecondsToday <= 0) totalSecondsToday = 1;

        int rank = 1;
        for (AppUsage app : topApps) {
            VBox row = new VBox(6);
            row.getStyleClass().add("app-row");

            HBox labelBox = new HBox(8);
            labelBox.setAlignment(Pos.CENTER_LEFT);

            // Rank Badge
            Label rankLabel = new Label("#" + rank);
            rankLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-color: rgba(56, 189, 248, 0.12); -fx-padding: 2 6; -fx-background-radius: 4;");

            // Glyph + App Name
            String glyph = resolveAppGlyph(app.getAppName());
            Label nameLabel = new Label(glyph + "  " + app.getAppName());
            nameLabel.getStyleClass().add("app-name-label");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Percent & Formatted Time
            int pct = (int) Math.round(((double) app.getSecondsUsed() / totalSecondsToday) * 100);
            Label pctLabel = new Label(pct + "%");
            pctLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: 600;");

            Label timeLabel = new Label(TimeFormatUtils.formatDurationSeconds(app.getSecondsUsed()));
            timeLabel.getStyleClass().add("app-time-label");

            labelBox.getChildren().addAll(rankLabel, nameLabel, spacer, pctLabel, timeLabel);

            ProgressBar bar = new ProgressBar((double) app.getSecondsUsed() / totalSecondsToday);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setStyle("-fx-pref-height: 6px;");

            row.getChildren().addAll(labelBox, bar);
            topAppsContainer.getChildren().add(row);
            rank++;
        }
    }

    private String resolveAppGlyph(String appName) {
        if (appName == null) return "⚡";
        String lower = appName.toLowerCase();
        if (lower.contains("code") || lower.contains("intellij") || lower.contains("studio") || lower.contains("terminal") || lower.contains("xcode") || lower.contains("vim")) return "💻";
        if (lower.contains("chrome") || lower.contains("safari") || lower.contains("firefox") || lower.contains("edge") || lower.contains("brave")) return "🌐";
        if (lower.contains("slack") || lower.contains("teams") || lower.contains("discord") || lower.contains("zoom") || lower.contains("telegram") || lower.contains("whatsapp")) return "💬";
        if (lower.contains("word") || lower.contains("excel") || lower.contains("pages") || lower.contains("notion") || lower.contains("notes") || lower.contains("obsidian")) return "📄";
        if (lower.contains("spotify") || lower.contains("music") || lower.contains("youtube") || lower.contains("vlc")) return "🎵";
        if (lower.contains("finder") || lower.contains("explorer") || lower.contains("files")) return "📁";
        return "⚡";
    }

    @FXML
    public void onRequestExtensionClicked() {
        if (restrictionEngine == null) return;
        ExtensionDialog.show(restrictionEngine, this::refreshTodayView);
    }

    @FXML
    public void onTogglePauseTracking() {
        if (trackingEngine == null) return;

        if (trackingEngine.isPaused()) {
            trackingEngine.resume();
        } else {
            trackingEngine.pause();
        }
        refreshTrackingStatusBadge();
    }

    public void refreshTrackingStatusBadge() {
        if (sidebarStatusBadge == null || btnPauseTracking == null) return;

        if (trackingEngine != null && trackingEngine.isPaused()) {
            sidebarStatusBadge.setText("⏸️ TRACKING PAUSED");
            sidebarStatusBadge.getStyleClass().setAll("badge-idle");
            btnPauseTracking.setText("▶️ Resume Tracking");
        } else {
            sidebarStatusBadge.setText("● TRACKING ACTIVE");
            sidebarStatusBadge.getStyleClass().setAll("badge-active");
            btnPauseTracking.setText("⏸️ Pause Tracking");
        }
    }

    // --- HISTORY VIEW LOGIC ---

    private void setupHistoryTable() {
        if (colDate == null) return;

        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().date()));
        colActiveTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().activeTime()));
        colIdleTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().idleTime()));
        colTopApp.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().topApp()));
    }

    @FXML
    public void onRange7Selected() {
        currentHistoryDays = 7;
        updateRangeButtonStyles(btnRange7);
        loadHistoryData(7);
    }

    @FXML
    public void onRange14Selected() {
        currentHistoryDays = 14;
        updateRangeButtonStyles(btnRange14);
        loadHistoryData(14);
    }

    @FXML
    public void onRange30Selected() {
        currentHistoryDays = 30;
        updateRangeButtonStyles(btnRange30);
        loadHistoryData(30);
    }

    private void updateRangeButtonStyles(Button selected) {
        List<Button> buttons = List.of(btnRange7, btnRange14, btnRange30);
        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().setAll(btn == selected ? "btn-filter-active" : "btn-filter-inactive");
            }
        }
    }

    private void loadHistoryData(int days) {
        if (usageDao == null || historyBarChart == null) return;

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);

        List<DailyUsageSummary> summaries = usageDao.getUsageForDateRange(start, end);

        // Populate Bar Chart
        historyBarChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        ObservableList<HistoryRow> tableRows = FXCollections.observableArrayList();

        for (DailyUsageSummary summary : summaries) {
            double hours = (double) summary.getTotalActiveSeconds() / 3600.0;
            String dateLabel = summary.getDate().format(CHART_DATE_FMT);
            series.getData().add(new XYChart.Data<>(dateLabel, hours));

            String topAppName = "None";
            if (!summary.getAppBreakdown().isEmpty()) {
                topAppName = summary.getAppBreakdown().get(0).getAppName();
            }

            tableRows.add(new HistoryRow(
                    summary.getDate().toString(),
                    TimeFormatUtils.formatDurationSeconds(summary.getTotalActiveSeconds()),
                    TimeFormatUtils.formatDurationSeconds(summary.getTotalIdleSeconds()),
                    topAppName
            ));
        }

        historyBarChart.getData().add(series);
        if (historyTableView != null) {
            historyTableView.setItems(tableRows);
        }
    }

    public record HistoryRow(String date, String activeTime, String idleTime, String topApp) {}

    // --- AI INSIGHTS VIEW LOGIC ---

    @FXML
    public void onRefreshAiInsights() {
        loadAiInsights();
    }

    private void loadAiInsights() {
        if (healthSuggestionService == null || usageDao == null) return;

        if (aiLoadingIndicator != null) aiLoadingIndicator.setVisible(true);

        Task<HealthAdvisorOutput> task = new Task<>() {
            @Override
            protected HealthAdvisorOutput call() throws Exception {
                LocalDate end = LocalDate.now();
                LocalDate start = end.minusDays(6);
                List<DailyUsageSummary> summaries = usageDao.getUsageForDateRange(start, end);
                long todayActive = usageDao.getTodayUsageSeconds();
                List<AppUsage> topApps = usageDao.getTopAppsForDate(end, 1);
                String topApp = topApps.isEmpty() ? "Applications" : topApps.get(0).getAppName();
                return healthSuggestionService.generateInsights(summaries, todayActive, topApp);
            }
        };

        task.setOnSucceeded(e -> {
            if (aiLoadingIndicator != null) aiLoadingIndicator.setVisible(false);
            HealthAdvisorOutput output = task.getValue();
            displayAiSuggestion(output);
        });

        task.setOnFailed(e -> {
            if (aiLoadingIndicator != null) aiLoadingIndicator.setVisible(false);
            logger.error("Failed to load AI suggestions", task.getException());
            if (aiAssessmentText != null) {
                aiAssessmentText.setText("Unable to generate wellness insights at this time.");
            }
        });

        Thread thread = new Thread(task, "ScreenTime-AI-Worker");
        thread.setDaemon(true);
        thread.start();
    }

    private void displayAiSuggestion(HealthAdvisorOutput output) {
        if (output == null) return;

        if (aiAssessmentText != null) {
            aiAssessmentText.setText(output.getAssessment());
        }

        if (aiStatusBadge != null) {
            String source = output.isLiveGemini() ? "Google Gemini 1.5" : "Clinical Offline Rules";
            aiStatusBadge.setText("✨ " + source);
        }

        if (aiConcernsContainer != null) {
            aiConcernsContainer.getChildren().clear();
            if (output.getConcerns() == null || output.getConcerns().isEmpty()) {
                Label badge = new Label("✅ No severe optical or posture strain detected");
                badge.getStyleClass().setAll("badge-active");
                aiConcernsContainer.getChildren().add(badge);
            } else {
                for (String concern : output.getConcerns()) {
                    Label badge = new Label("⚠️ " + concern);
                    badge.getStyleClass().setAll("badge-warning");
                    aiConcernsContainer.getChildren().add(badge);
                }
            }
        }

        if (aiSuggestionsContainer != null) {
            aiSuggestionsContainer.getChildren().clear();

            for (String tip : output.getSuggestions()) {
                HBox tipCard = new HBox(12);
                tipCard.setAlignment(Pos.CENTER_LEFT);
                tipCard.getStyleClass().add("ai-suggestion-card");

                Label bullet = new Label("💡");
                bullet.setStyle("-fx-font-size: 18px;");

                Label text = new Label(tip);
                text.setWrapText(true);
                text.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-font-weight: 500;");

                tipCard.getChildren().addAll(bullet, text);
                aiSuggestionsContainer.getChildren().add(tipCard);
            }
        }
    }

    // --- SETTINGS VIEW LOGIC ---

    private void loadSettingsIntoUI() {
        AppConfig config = ConfigManager.getInstance().getConfig();

        if (settingDailyLimit != null) settingDailyLimit.setText(String.valueOf(config.getDailyLimitMinutes()));
        if (settingWarningThresholds != null) {
            String thresholds = config.getWarningThresholds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            settingWarningThresholds.setText(thresholds);
        }
        if (settingIdleThreshold != null) settingIdleThreshold.setText(String.valueOf(config.getIdleThresholdSeconds()));
        if (settingHardBlock != null) settingHardBlock.setSelected(config.isHardBlockEnabled());
        if (settingMaxExtensions != null) settingMaxExtensions.setText(String.valueOf(config.getMaxExtensionsPerDay()));
        if (settingMaxExtensionMinutes != null) settingMaxExtensionMinutes.setText(String.valueOf(config.getMaxExtensionMinutesPerDay()));
        if (settingAiEnabled != null) settingAiEnabled.setSelected(config.isAiEnabled());
        if (settingNotifications != null) settingNotifications.setSelected(config.isNotificationsEnabled());
        if (settingAutostart != null) {
            settingAutostart.setSelected(com.screentime.config.AutostartManager.getInstance().isAutostartEnabled());
        }

        rawApiKey = config.getGeminiApiKey() != null ? config.getGeminiApiKey() : "";
        if (settingApiKey != null) settingApiKey.setText(rawApiKey);
    }

    @FXML
    public void onToggleApiKeyVisibility() {
        apiKeyMasked = !apiKeyMasked;
        if (btnToggleApiKey != null) {
            btnToggleApiKey.setText(apiKeyMasked ? "👁️" : "🔒");
        }
    }

    @FXML
    public void onSaveSettings() {
        try {
            AppConfig config = ConfigManager.getInstance().getConfig();

            int limit = Integer.parseInt(settingDailyLimit.getText().trim());
            int idle = Integer.parseInt(settingIdleThreshold.getText().trim());
            int maxExt = Integer.parseInt(settingMaxExtensions.getText().trim());
            int maxExtMins = Integer.parseInt(settingMaxExtensionMinutes.getText().trim());

            List<Integer> thresholds = Arrays.stream(settingWarningThresholds.getText().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            config.setDailyLimitMinutes(limit);
            config.setIdleThresholdSeconds(idle);
            config.setMaxExtensionsPerDay(maxExt);
            config.setMaxExtensionMinutesPerDay(maxExtMins);
            config.setWarningThresholds(thresholds);
            config.setHardBlockEnabled(settingHardBlock.isSelected());
            config.setAiEnabled(settingAiEnabled.isSelected());
            config.setNotificationsEnabled(settingNotifications.isSelected());

            if (settingAutostart != null) {
                com.screentime.config.AutostartManager.getInstance().setAutostartEnabled(settingAutostart.isSelected());
            }

            if (settingApiKey != null) {
                config.setGeminiApiKey(settingApiKey.getText().trim());
            }

            ConfigManager.getInstance().saveConfig();

            // Propagate updates to running restriction engine & tracking engine
            if (restrictionEngine != null) {
                RestrictionConfig rConfig = restrictionEngine.getConfig();
                rConfig.setDailyLimitMinutes(limit);
                rConfig.setWarningThresholds(thresholds);
                rConfig.setHardBlockEnabled(config.isHardBlockEnabled());
                rConfig.setMaxExtensionsPerDay(maxExt);
                rConfig.setMaxExtensionMinutesPerDay(maxExtMins);
            }
            if (trackingEngine != null) {
                trackingEngine.setIdleThresholdSeconds(idle);
            }

            if (settingsSavedLabel != null) {
                settingsSavedLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold; -fx-font-size: 13px;");
                settingsSavedLabel.setText("✅ Preferences saved successfully!");
            }
            refreshTodayView();
            logger.info("Saved and applied new application settings.");
        } catch (Exception e) {
            logger.error("Failed to save settings: {}", e.getMessage());
            if (settingsSavedLabel != null) {
                settingsSavedLabel.setStyle("-fx-text-fill: #f87171;");
                settingsSavedLabel.setText("❌ Failed to save: Check numeric input fields.");
            }
        }
    }
}
