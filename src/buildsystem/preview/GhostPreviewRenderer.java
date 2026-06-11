package buildsystem.preview;

import buildsystem.core.BuildPreview;

/**
 * GhostPreviewRenderer:
 * - Layer render trung gian cho preview.
 * - Hien tai tra ve BuildPreview runtime de Renderer ve.
 */
public class GhostPreviewRenderer {
    private final PreviewMaterial material;

    public GhostPreviewRenderer(PreviewMaterial material) {
        this.material = material;
    }

    public void applyMaterial(BuildPreview preview, boolean valid) {
        preview.setOpacity(material.getOpacity());
        preview.setValid(valid);
    }
}
