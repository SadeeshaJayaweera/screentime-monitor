package com.screentime.ui;

import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/**
 * Custom high-resolution circular progress ring component that visualizes screen time usage vs daily limit
 * with dynamic multi-stop gradient color transitions, ambient glow drop-shadows, and anti-aliased SVG arc rendering.
 */
public class ProgressRing extends StackPane {

    private final Canvas canvas;
    private final Label centerValueLabel;
    private final Label centerSubLabel;
    private final Label centerPercentBadge;

    private double progress = 0.0; // 0.0 to 1.0 (or > 1.0 for overtime)
    private final double size;
    private final double strokeWidth;

    public ProgressRing() {
        this(170, 14);
    }

    public ProgressRing(double size, double strokeWidth) {
        this.size = size;
        this.strokeWidth = strokeWidth;

        this.canvas = new Canvas(size, size);

        this.centerPercentBadge = new Label("0%");
        this.centerPercentBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #38bdf8; -fx-background-color: rgba(56, 189, 248, 0.15); -fx-background-radius: 12px; -fx-padding: 2px 8px;");

        this.centerValueLabel = new Label("0h 00m");
        this.centerValueLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: #f8fafc; -fx-letter-spacing: -0.5px;");

        this.centerSubLabel = new Label("Active Time");
        this.centerSubLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #64748b; -fx-letter-spacing: 0.4px;");

        VBox labelContainer = new VBox(2, centerPercentBadge, centerValueLabel, centerSubLabel);
        labelContainer.setAlignment(Pos.CENTER);
        labelContainer.setMouseTransparent(true);

        this.getChildren().addAll(canvas, labelContainer);
        this.setAlignment(Pos.CENTER);
        this.setPrefSize(size, size);
        this.setMaxSize(size, size);

        // Ambient glow effect for the entire gauge
        DropShadow ambientGlow = new DropShadow();
        ambientGlow.setRadius(14);
        ambientGlow.setSpread(0.1);
        ambientGlow.setColor(Color.rgb(56, 189, 248, 0.25));
        canvas.setEffect(ambientGlow);

        draw();
    }

    /**
     * Updates the progress percentage and center labels.
     *
     * @param progress Decimal progress (0.0 to 1.0+)
     * @param centerValue Formatted used time text (e.g. "2h 15m")
     * @param centerSub Formatted subtitle text (e.g. "75% Used")
     */
    public void setProgress(double progress, String centerValue, String centerSub) {
        this.progress = progress;
        if (centerValueLabel != null && centerValue != null) {
            centerValueLabel.setText(centerValue);
        }
        if (centerSubLabel != null && centerSub != null) {
            centerSubLabel.setText(centerSub);
        }
        int pctInt = (int) Math.round(progress * 100);
        if (centerPercentBadge != null) {
            centerPercentBadge.setText(pctInt + "%");
        }
        draw();
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, size, size);

        double offset = strokeWidth / 2.0;
        double arcSize = size - strokeWidth;

        // 1. Draw track (background circle)
        gc.setLineWidth(strokeWidth);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setStroke(Color.web("#1e293b")); // Dark track
        gc.strokeOval(offset, offset, arcSize, arcSize);

        // 2. Determine dynamic stroke gradient & glow based on progress percentage
        LinearGradient arcGradient;
        DropShadow glowEffect = (DropShadow) canvas.getEffect();

        if (progress >= 1.0) {
            // Critical / Limit Reached (Crimson to Rose)
            arcGradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#f43f5e")),
                    new Stop(1, Color.web("#be123c"))
            );
            if (glowEffect != null) glowEffect.setColor(Color.rgb(244, 63, 94, 0.45));
            centerPercentBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #f87171; -fx-background-color: rgba(239, 68, 68, 0.18); -fx-background-radius: 12px; -fx-padding: 2px 8px;");
            centerValueLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: #f87171; -fx-letter-spacing: -0.5px;");
        } else if (progress >= 0.75) {
            // Warning Threshold (Amber to Orange)
            arcGradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#fbbf24")),
                    new Stop(1, Color.web("#f59e0b"))
            );
            if (glowEffect != null) glowEffect.setColor(Color.rgb(245, 158, 11, 0.4));
            centerPercentBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #fbbf24; -fx-background-color: rgba(245, 158, 11, 0.18); -fx-background-radius: 12px; -fx-padding: 2px 8px;");
            centerValueLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: #fbbf24; -fx-letter-spacing: -0.5px;");
        } else {
            // Normal Healthy Progress (Cyan to Emerald)
            arcGradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#38bdf8")),
                    new Stop(1, Color.web("#0284c7"))
            );
            if (glowEffect != null) glowEffect.setColor(Color.rgb(56, 189, 248, 0.3));
            centerPercentBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #38bdf8; -fx-background-color: rgba(56, 189, 248, 0.15); -fx-background-radius: 12px; -fx-padding: 2px 8px;");
            centerValueLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: #f8fafc; -fx-letter-spacing: -0.5px;");
        }

        // 3. Draw active progress arc
        double angle = Math.min(360.0, Math.max(0.0, progress * 360.0));
        if (angle > 0) {
            gc.setStroke(arcGradient);
            gc.strokeArc(offset, offset, arcSize, arcSize, 90, -angle, ArcType.OPEN);
        }
    }

    public double getProgress() {
        return progress;
    }
}
