package buildsystem.preview;

import javafx.scene.paint.Color;

/**
 * PreviewMaterial:
 * - Chua thong so visual cho ghost preview.
 */
public class PreviewMaterial {
    private final double opacity;
    private final Color invalidTint;

    public PreviewMaterial(double opacity, Color invalidTint) {
        this.opacity = opacity;
        this.invalidTint = invalidTint;
    }

    public double getOpacity() { return opacity; }
    public Color getInvalidTint() { return invalidTint; }
}
