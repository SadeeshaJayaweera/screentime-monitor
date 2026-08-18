package com.screentime.ui;

import com.screentime.restriction.ExtensionResult;
import com.screentime.restriction.RestrictionEngine;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modal dialog enabling users to request screen time extensions (+15, +30, +60, or custom minutes).
 */
public class ExtensionDialog {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionDialog.class);

    public static void show(RestrictionEngine restrictionEngine, Runnable onExtensionGranted) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Request Screen Time Extension");
        dialogStage.setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #0f172a; -fx-font-family: sans-serif;");
        root.setAlignment(Pos.TOP_LEFT);

        Label headerLabel = new Label("⏳ Request Screen Time Extension");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Label descLabel = new Label("Need extra time today? Choose a preset or specify custom minutes.");
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        descLabel.setWrapText(true);

        // Quick presets
        Label presetsLabel = new Label("Quick Presets:");
        presetsLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");

        HBox presetBox = new HBox(10);
        presetBox.setAlignment(Pos.CENTER_LEFT);

        TextField minutesInput = new TextField("30");
        minutesInput.setPromptText("Minutes (e.g. 15, 30, 45)");
        minutesInput.setPrefWidth(140);
        minutesInput.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 6;");

        Button btn15 = createPresetButton("+15 Mins", 15, minutesInput);
        Button btn30 = createPresetButton("+30 Mins", 30, minutesInput);
        Button btn60 = createPresetButton("+60 Mins", 60, minutesInput);
        presetBox.getChildren().addAll(btn15, btn30, btn60);

        // Reason input
        Label reasonLabel = new Label("Reason (Optional):");
        reasonLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");

        TextField reasonInput = new TextField();
        reasonInput.setPromptText("e.g. Finishing project task, urgent communication");
        reasonInput.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 6;");

        // Status message
        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        // Action buttons
        HBox actionBox = new HBox(12);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #e2e8f0; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialogStage.close());

        Button submitBtn = new Button("Submit Request");
        submitBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 18; -fx-background-radius: 6; -fx-cursor: hand;");
        submitBtn.setOnAction(e -> {
            try {
                int minutes = Integer.parseInt(minutesInput.getText().trim());
                String reason = reasonInput.getText().trim();

                ExtensionResult result = restrictionEngine.requestExtension(minutes, reason);
                if (result.isGranted()) {
                    statusLabel.setStyle("-fx-text-fill: #4ade80;");
                    statusLabel.setText("✅ " + result.getMessage());
                    if (onExtensionGranted != null) onExtensionGranted.run();
                    submitBtn.setDisable(true);
                } else {
                    statusLabel.setStyle("-fx-text-fill: #f87171;");
                    statusLabel.setText("❌ " + result.getMessage());
                }
            } catch (NumberFormatException ex) {
                statusLabel.setStyle("-fx-text-fill: #f87171;");
                statusLabel.setText("❌ Please enter a valid number of minutes.");
            }
        });

        actionBox.getChildren().addAll(cancelBtn, submitBtn);

        root.getChildren().addAll(
                headerLabel,
                descLabel,
                presetsLabel,
                presetBox,
                new Label("Custom Minutes:") {{ setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;"); }},
                minutesInput,
                reasonLabel,
                reasonInput,
                statusLabel,
                actionBox
        );

        Scene scene = new Scene(root, 440, 440);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private static Button createPresetButton(String text, int minutes, TextField targetInput) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;");
        btn.setOnAction(e -> targetInput.setText(String.valueOf(minutes)));
        return btn;
    }
}
