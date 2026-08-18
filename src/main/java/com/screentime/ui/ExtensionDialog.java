package com.screentime.ui;

import com.screentime.restriction.ExtensionResult;
import com.screentime.restriction.RestrictionEngine;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modern modal dialog enabling users to request screen time extensions (+15, +30, +45, +60, or custom minutes).
 */
public class ExtensionDialog {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionDialog.class);

    public static void show(RestrictionEngine restrictionEngine, Runnable onExtensionGranted) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Request Screen Time Extension");
        dialogStage.setResizable(false);

        VBox root = new VBox(18);
        root.setPadding(new Insets(26));
        root.setStyle("-fx-background-color: #0b0f19; -fx-font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;");
        root.setAlignment(Pos.TOP_LEFT);

        // Header Card
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label("⏳");
        iconLabel.setStyle("-fx-font-size: 24px;");

        VBox headerText = new VBox(2);
        Label headerLabel = new Label("Screen Time Extension");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #f8fafc; -fx-letter-spacing: -0.4px;");

        Label descLabel = new Label("Request temporary additional screen time for today's session.");
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        headerText.getChildren().addAll(headerLabel, descLabel);
        headerBox.getChildren().addAll(iconLabel, headerText);

        // Quick Presets Capsule Row
        VBox presetsContainer = new VBox(8);
        Label presetsLabel = new Label("QUICK PRESETS");
        presetsLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-letter-spacing: 0.8px;");

        HBox presetBox = new HBox(8);
        presetBox.setAlignment(Pos.CENTER_LEFT);

        TextField minutesInput = new TextField("30");
        minutesInput.setPromptText("Minutes (e.g. 15, 30, 45)");
        minutesInput.setStyle("-fx-background-color: #131d31; -fx-text-fill: #f8fafc; -fx-padding: 9 14; -fx-background-radius: 8; -fx-border-color: #27354a; -fx-border-radius: 8; -fx-font-weight: 700; -fx-font-size: 13px;");

        Button btn15 = createPresetButton("+15m", 15, minutesInput);
        Button btn30 = createPresetButton("+30m", 30, minutesInput);
        Button btn45 = createPresetButton("+45m", 45, minutesInput);
        Button btn60 = createPresetButton("+60m", 60, minutesInput);
        presetBox.getChildren().addAll(btn15, btn30, btn45, btn60);
        presetsContainer.getChildren().addAll(presetsLabel, presetBox);

        // Custom Minutes Input Card
        VBox customInputBox = new VBox(6);
        Label customLabel = new Label("CUSTOM DURATION (MINUTES)");
        customLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-letter-spacing: 0.8px;");
        customInputBox.getChildren().addAll(customLabel, minutesInput);

        // Reason input
        VBox reasonBox = new VBox(6);
        Label reasonLabel = new Label("REASON (OPTIONAL)");
        reasonLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-letter-spacing: 0.8px;");

        TextField reasonInput = new TextField();
        reasonInput.setPromptText("e.g., Finishing project deliverable, urgent meeting");
        reasonInput.setStyle("-fx-background-color: #131d31; -fx-text-fill: #f8fafc; -fx-padding: 9 14; -fx-background-radius: 8; -fx-border-color: #27354a; -fx-border-radius: 8; -fx-font-size: 13px;");
        reasonBox.getChildren().addAll(reasonLabel, reasonInput);

        // Status message
        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700;");

        // Action buttons
        HBox actionBox = new HBox(12);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #cbd5e1; -fx-padding: 9 18; -fx-background-radius: 8; -fx-font-weight: 600; -fx-cursor: hand; -fx-border-color: #334155; -fx-border-radius: 8;");
        cancelBtn.setOnAction(e -> dialogStage.close());

        Button submitBtn = new Button("Grant Extension");
        submitBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #38bdf8, #0284c7); -fx-text-fill: white; -fx-font-weight: 800; -fx-padding: 9 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(2, 132, 199, 0.35), 8, 0, 0, 2);");
        submitBtn.setOnAction(e -> {
            try {
                int minutes = Integer.parseInt(minutesInput.getText().trim());
                String reason = reasonInput.getText().trim();

                ExtensionResult result = restrictionEngine.requestExtension(minutes, reason);
                if (result.isGranted()) {
                    statusLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: 700;");
                    statusLabel.setText("✅ " + result.getMessage());
                    if (onExtensionGranted != null) onExtensionGranted.run();
                    submitBtn.setDisable(true);
                } else {
                    statusLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: 700;");
                    statusLabel.setText("❌ " + result.getMessage());
                }
            } catch (NumberFormatException ex) {
                statusLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: 700;");
                statusLabel.setText("❌ Please enter a valid number of minutes.");
            }
        });

        actionBox.getChildren().addAll(cancelBtn, submitBtn);

        root.getChildren().addAll(
                headerBox,
                presetsContainer,
                customInputBox,
                reasonBox,
                statusLabel,
                actionBox
        );

        Scene scene = new Scene(root, 460, 430);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private static Button createPresetButton(String text, int minutes, TextField targetInput) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #162032; -fx-text-fill: #38bdf8; -fx-font-weight: 800; -fx-font-size: 12px; -fx-padding: 7 14; -fx-background-radius: 20px; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 20px; -fx-cursor: hand;");
        btn.setOnAction(e -> targetInput.setText(String.valueOf(minutes)));
        return btn;
    }
}
