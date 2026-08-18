package com.screentime.ui;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Utility to generate high-resolution application icon assets.
 */
public class IconGenerator {

    public static BufferedImage generateAppIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Background rounded square with dark gradient
        GradientPaint bgGradient = new GradientPaint(
                0, 0, new Color(15, 23, 42),
                size, size, new Color(30, 41, 59)
        );
        g2.setPaint(bgGradient);
        int cornerRadius = (int) (size * 0.22);
        g2.fill(new RoundRectangle2D.Double(0, 0, size, size, cornerRadius, cornerRadius));

        // Border glow
        g2.setColor(new Color(56, 189, 248, 80));
        g2.setStroke(new BasicStroke((float) (size * 0.02)));
        g2.draw(new RoundRectangle2D.Double(size * 0.01, size * 0.01, size * 0.98, size * 0.98, cornerRadius, cornerRadius));

        // Central circular progress ring
        double ringInset = size * 0.20;
        double ringSize = size * 0.60;
        double strokeWidth = size * 0.07;

        g2.setStroke(new BasicStroke((float) strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Inactive ring track
        g2.setColor(new Color(51, 65, 85));
        g2.drawOval((int) ringInset, (int) ringInset, (int) ringSize, (int) ringSize);

        // Active cyan progress arc
        GradientPaint arcGradient = new GradientPaint(
                (float) ringInset, (float) ringInset, new Color(56, 189, 248),
                (float) (ringInset + ringSize), (float) (ringInset + ringSize), new Color(34, 197, 94)
        );
        g2.setPaint(arcGradient);
        g2.drawArc((int) ringInset, (int) ringInset, (int) ringSize, (int) ringSize, 90, -270);

        // Center clock hands
        double centerX = size / 2.0;
        double centerY = size / 2.0;
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke((float) (size * 0.04), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // Hour hand (pointing to ~2 o'clock)
        g2.drawLine((int) centerX, (int) centerY, (int) (centerX + size * 0.12), (int) (centerY - size * 0.08));
        // Minute hand (pointing to 12 o'clock)
        g2.drawLine((int) centerX, (int) centerY, (int) centerX, (int) (centerY - size * 0.18));

        // Center dot
        g2.setColor(new Color(56, 189, 248));
        int dotSize = (int) (size * 0.07);
        g2.fillOval((int) (centerX - dotSize / 2.0), (int) (centerY - dotSize / 2.0), dotSize, dotSize);

        g2.dispose();
        return image;
    }

    public static void main(String[] args) throws IOException {
        File dir = new File("src/main/resources/com/screentime/ui/assets");
        dir.mkdirs();
        BufferedImage icon = generateAppIcon(256);
        File iconFile = new File(dir, "icon.png");
        ImageIO.write(icon, "PNG", iconFile);
        System.out.println("Generated icon asset at: " + iconFile.getAbsolutePath());
    }
}
