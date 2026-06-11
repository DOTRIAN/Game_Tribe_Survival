package ui;

import buildsystem.core.BuildPreview;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class BombPreviewRenderer {
    private BombPreviewRenderer() {
    }

    public static void renderPlacementTint(GraphicsContext graphics, BuildPreview preview, double screenX, double screenY) {
        if (graphics == null || preview == null) {
            return;
        }
        if (preview.isValid()) {
            graphics.setFill(Color.color(0.20, 1.0, 0.30, 0.20));
            graphics.fillRect(screenX, screenY, preview.getWidth(), preview.getHeight());
            graphics.setStroke(Color.color(0.35, 1.0, 0.45, 0.90));
            graphics.strokeRect(screenX, screenY, preview.getWidth(), preview.getHeight());
            return;
        }
        graphics.setFill(Color.color(1.0, 0.15, 0.15, 0.20));
        graphics.fillRect(screenX, screenY, preview.getWidth(), preview.getHeight());
        graphics.setStroke(Color.color(1.0, 0.15, 0.15, 0.90));
        graphics.strokeRect(screenX, screenY, preview.getWidth(), preview.getHeight());
    }
}

