package ui;

import entity.Enemy;
import entity.Player;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * MinimapOverlay:
 * - Compact map overlay in the bottom-right corner.
 * - Draws player (cyan) and enemies (crimson) positions procedurally.
 * - Managed and styled via CSS classes and M-key triggers.
 */
public class MinimapOverlay extends StackPane {
    private static final double PANEL_WIDTH = 160.0;
    private static final double PANEL_HEIGHT = 114.0;
    private static final double CANVAS_WIDTH = 144.0;
    private static final double CANVAS_HEIGHT = 96.0;

    private final Canvas canvas;

    public MinimapOverlay() {
        getStyleClass().add("minimap-panel");
        
        // Prevent layout inflation loop
        setPrefSize(PANEL_WIDTH, PANEL_HEIGHT);
        setMinSize(PANEL_WIDTH, PANEL_HEIGHT);
        setMaxSize(PANEL_WIDTH, PANEL_HEIGHT);

        this.canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        canvas.setMouseTransparent(true);
        getChildren().add(canvas);
    }

    public void render(double worldW,
                       double worldH,
                       double cameraX,
                       double cameraY,
                       double cameraZoom,
                       double viewportWidth,
                       double viewportHeight,
                       Player player,
                       List<Enemy> enemies) {
        double drawWidth = CANVAS_WIDTH;
        double drawHeight = CANVAS_HEIGHT;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, drawWidth, drawHeight);
        
        // Transparent modern glass backdrop
        gc.setFill(Color.color(0.06, 0.08, 0.1, 0.85));
        gc.fillRoundRect(0, 0, drawWidth, drawHeight, 12, 12);
        
        // Gold border
        gc.setStroke(Color.color(223 / 255.0, 183 / 255.0, 108 / 255.0, 0.3));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(0, 0, drawWidth, drawHeight, 12, 12);

        if (player == null || worldW <= 0 || worldH <= 0) {
            return;
        }

        double padding = 8;
        double mapX = padding;
        double mapY = padding;
        double mapW = drawWidth - padding * 2;
        double mapH = drawHeight - padding * 2;

        // Innermost radar field
        gc.setFill(Color.color(0.1, 0.12, 0.14, 1.0));
        gc.fillRoundRect(mapX, mapY, mapW, mapH, 8, 8);
        gc.setStroke(Color.color(255, 255, 255, 0.05));
        gc.strokeRoundRect(mapX, mapY, mapW, mapH, 8, 8);

        double scaleX = mapW / worldW;
        double scaleY = mapH / worldH;

        // Draw camera viewport bounds
        double viewWorldW = viewportWidth / cameraZoom;
        double viewWorldH = viewportHeight / cameraZoom;
        gc.setStroke(Color.color(223 / 255.0, 183 / 255.0, 108 / 255.0, 0.25));
        gc.setLineWidth(0.8);
        gc.strokeRect(mapX + cameraX * scaleX, mapY + cameraY * scaleY, viewWorldW * scaleX, viewWorldH * scaleY);

        // Render Player Dot (Vibrant Cyan)
        drawDot(gc, mapX + (player.getX() + player.getWidth() * 0.5) * scaleX, 
                   mapY + (player.getY() + player.getHeight() * 0.5) * scaleY, 
                   3.5, Color.web("#00d2ff"));

        // Render Enemy Dots (Crimson Red, capped to 24 for performance)
        if (enemies != null) {
            int count = 0;
            for (Enemy enemy : enemies) {
                if (enemy == null || !enemy.isAlive()) {
                    continue;
                }
                drawDot(gc, mapX + (enemy.getX() + enemy.getWidth() * 0.5) * scaleX, 
                           mapY + (enemy.getY() + enemy.getHeight() * 0.5) * scaleY, 
                           2.5, Color.web("#ff5252"));
                count++;
                if (count >= 24) {
                    break;
                }
            }
        }
    }

    private void drawDot(GraphicsContext gc, double x, double y, double radius, Color fill) {
        gc.setFill(fill);
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        gc.setStroke(Color.color(0, 0, 0, 0.75));
        gc.setLineWidth(0.8);
        gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
    }
}
