package com.screentime.ui;

import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

/**
 * Custom JavaFX circular progress ring component that visualizes screen time usage vs daily limit
 * with dynamic color transitions (Green -> Amber/Yellow -> Red/Critical).
 */
public class ProgressRing extends StackPane {

    private final Canvas canvas;
    private final Label centerValueLabel;
    private final Label centerSubLabel;

    private double progress = 0.0; // 0.0 to 1.0 (or > 1.0 for overtime)
    private final double size;
    private final double strokeWidth;

    public ProgressRing() {
        this(160, 14);
    }

    public ProgressRing(double size, double strokeWidth) {
        this.size = size;
        this.strokeWidth = strokeWidth;

        this.canvas = new Canvas(size, size);

        this.centerValueLabel = new Label("0h 00m");
        this.centerValueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #f8fafc;");

        this.centerSubLabel = new Label("0% Used");
        this.centerSubLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");

        javafx.scene.layout.VBox labelContainer = new javafx.scene.layout.VBox(2, centerValueLabel, centerSubLabel);
        labelContainer.setAlignment(Pos.CENTER);
        labelContainer.setMouseTransparent(true);

        this.getChildren().addAll(canvas, labelContainer);
        this.setAlignment(Pos.CENTER);
        this.setPrefSize(size, size);
        this.setMaxSize(size, size);

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
        draw();
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, size, size);

        double center = size / 2.0;
        double radius = (size - strokeWidth) / 2.0;

        // 1. Draw track (background circle)
        gc.setLineWidth(strokeWidth);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setStroke(Color.web("#1e293b")); // Dark track
        gc.strokeOval(strokeWidth / 2.0, strokeWidth / 2.0, size - strokeWidth, size - strokeWidth);

        // 2. Determine dynamic stroke color based on progress percentage
        Color ringColor;
        if (progress >= 1.0) {
            ringColor = Color.web("#ef4444"); // Red (Critical/Limit reached)
        } else if (progress >= 0.75) {
            ringColor = Color.web("#f59e0b"); // Amber/Yellow (Warning threshold)
        } else {
            ringColor = Color.web("#38bdf8"); // Sky Blue / Emerald Green (Normal)
        }

        // 3. Draw active arc
        double angle = Math.min(360.0, Math.max(0.0, progress * 360.0));
        if (angle > 0) {
            gc.setStroke(ringColor);
            // Starting from 12 o'clock (-90 degrees) moving clockwise (negative extent in JavaFX Canvas)
            gc.strokeArc(strokeWidth / 2.0, strokeWidth / 2.0, size - strokeWidth, size - strokeWidth, 90, -angle, javafx.scene.shape.ArcType.OPEN);
        }

        // Update center label text color for limit reached
        if (progress >= 1.0) {
            centerValueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #f87171;");
            centerSubLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #f87171;");
        } else if (progress >= 0.75) {
            centerValueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #fbbf24;");
            centerSubLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;");
        } else {
            centerValueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #f8fafc;");
            centerSubLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #38bdf8;");
        }
    }

    public double getProgress() {
        return progress;
    }
}
