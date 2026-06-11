package ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public final class TimeUI {
    private TimeUI() {
    }

    public static void render(GraphicsContext graphicsContext,
                              double viewportWidth,
                              String icon,
                              String title,
                              String clock,
                              String announcement) {
        if (graphicsContext == null || title == null || clock == null) {
            return;
        }

        double panelWidth = 148.0;
        double panelHeight = 58.0;
        double x = Math.max(12.0, viewportWidth - panelWidth - 18.0);
        double y = 16.0;

        graphicsContext.save();
        graphicsContext.setFill(Color.color(0.04, 0.05, 0.04, 0.62));
        graphicsContext.fillRoundRect(x, y, panelWidth, panelHeight, 16, 16);
        graphicsContext.setStroke(Color.color(1.0, 0.88, 0.52, 0.50));
        graphicsContext.setLineWidth(1.2);
        graphicsContext.strokeRoundRect(x + 0.5, y + 0.5, panelWidth - 1, panelHeight - 1, 16, 16);

        graphicsContext.setFill(Color.web("#ffe18a"));
        graphicsContext.setFont(Font.font("Consolas", FontWeight.BOLD, 17));
        graphicsContext.fillText((icon == null ? "" : icon + " ") + title, x + 14, y + 24);
        graphicsContext.setFill(Color.web("#f7f0d1"));
        graphicsContext.setFont(Font.font("Consolas", FontWeight.BOLD, 21));
        graphicsContext.fillText(clock, x + 44, y + 49);

        if (announcement != null && !announcement.isBlank()) {
            double warningWidth = 360.0;
            double warningHeight = 68.0;
            double warningX = Math.max(20.0, (viewportWidth - warningWidth) * 0.5);
            double warningY = 82.0;
            graphicsContext.setFill(Color.color(0.02, 0.02, 0.02, 0.68));
            graphicsContext.fillRoundRect(warningX, warningY, warningWidth, warningHeight, 18, 18);
            graphicsContext.setStroke(Color.color(1.0, 0.35, 0.22, 0.72));
            graphicsContext.strokeRoundRect(warningX + 0.5, warningY + 0.5, warningWidth - 1, warningHeight - 1, 18, 18);
            graphicsContext.setFill(Color.web("#fff0cc"));
            graphicsContext.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
            String[] lines = announcement.split("\\R", 3);
            for (int i = 0; i < Math.min(2, lines.length); i++) {
                graphicsContext.fillText(lines[i], warningX + 18, warningY + 27 + i * 22);
            }
        }
        graphicsContext.restore();
    }
}
