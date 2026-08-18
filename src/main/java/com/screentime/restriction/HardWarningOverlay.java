package com.screentime.restriction;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Always-on-top JavaFX warning dialog presented when the daily screen time limit is reached
 * and hardBlockEnabled is set to true.
 */
public class HardWarningOverlay {

    private static final Logger logger = LoggerFactory.getLogger(HardWarningOverlay.class);
    private static final AtomicBoolean showing = new AtomicBoolean(false);
    private static Stage overlayStage;

    public interface ExtensionRequestHandler {
        void onRequestExtension(int minutes);
    }

    /**
     * Displays the always-on-top warning dialog on the JavaFX application thread.
     */
    public static void show(int limitMinutes, long usedMinutes, ExtensionRequestHandler handler) {
        try {
            Platform.runLater(() -> {
                if (showing.compareAndSet(false, true)) {
                    try {
                        overlayStage = new Stage();
                        overlayStage.initStyle(StageStyle.UTILITY);
                        overlayStage.setAlwaysOnTop(true);
                        overlayStage.setTitle("Screen Time Limit Reached");

                        VBox root = new VBox(20);
                        root.setAlignment(Pos.CENTER);
                        root.setPadding(new Insets(28));
                        root.setStyle("-fx-background-color: #0f172a; -fx-font-family: sans-serif;");

                        Label titleLabel = new Label("⚠️ Daily Screen Time Limit Reached");
                        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f87171;");

                        Label descLabel = new Label(String.format(
                                "You have reached your configured daily limit of %d minutes (used %d minutes today).\nTake a break or request an extension below.",
                                limitMinutes, usedMinutes
                        ));
                        descLabel.setWrapText(true);
                        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1; -fx-text-alignment: center;");

                        HBox buttonBox = new HBox(12);
                        buttonBox.setAlignment(Pos.CENTER);

                        Button ext15Btn = new Button("+15 Mins");
                        ext15Btn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6;");
                        ext15Btn.setOnAction(e -> {
                            close();
                            if (handler != null) handler.onRequestExtension(15);
                        });

                        Button ext30Btn = new Button("+30 Mins");
                        ext30Btn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6;");
                        ext30Btn.setOnAction(e -> {
                            close();
                            if (handler != null) handler.onRequestExtension(30);
                        });

                        Button dismissBtn = new Button("Dismiss");
                        dismissBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #f1f5f9; -fx-padding: 8 16; -fx-background-radius: 6;");
                        dismissBtn.setOnAction(e -> close());

                        buttonBox.getChildren().addAll(ext15Btn, ext30Btn, dismissBtn);
                        root.getChildren().addAll(titleLabel, descLabel, buttonBox);

                        Scene scene = new Scene(root, 460, 240);
                        overlayStage.setScene(scene);
                        overlayStage.setOnCloseRequest(e -> showing.set(false));
                        overlayStage.show();

                        logger.info("HardWarningOverlay displayed.");
                    } catch (Throwable t) {
                        showing.set(false);
                        logger.warn("Could not display JavaFX HardWarningOverlay: {}", t.getMessage());
                    }
                }
            });
        } catch (IllegalStateException e) {
            // JavaFX toolkit not initialized (e.g. headless tests)
            logger.debug("Skipping HardWarningOverlay (JavaFX Toolkit not running): {}", e.getMessage());
        }
    }

    public static void close() {
        if (showing.compareAndSet(true, false)) {
            try {
                Platform.runLater(() -> {
                    if (overlayStage != null) {
                        overlayStage.close();
                        overlayStage = null;
                    }
                });
            } catch (Throwable ignored) {}
        }
    }

    public static boolean isShowing() {
        return showing.get();
    }
}
