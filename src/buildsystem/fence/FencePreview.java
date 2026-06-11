package buildsystem.fence;

import buildsystem.core.BuildPreview;
import buildsystem.core.BuildType;
import javafx.scene.image.Image;

public final class FencePreview {
    private FencePreview() {
    }

    public static void apply(BuildPreview preview,
                             BuildType type,
                             int tileX,
                             int tileY,
                             int tileWidth,
                             int tileHeight,
                             String spriteKey,
                             Image image,
                             int neighborMask) {
        preview.setVisible(true);
        preview.setType(type);
        preview.setTileX(tileX);
        preview.setTileY(tileY);
        preview.setRotationDegrees(0.0);
        preview.setRotationLabel("N");
        preview.setSpriteKey(spriteKey);
        preview.setNeighborMask(neighborMask);
        preview.setImage(image);
        double renderWidth = FenceEntity.computeRenderWidth(spriteKey, tileWidth, tileHeight);
        double renderHeight = FenceEntity.computeRenderHeight(spriteKey, tileHeight);
        preview.setWidth(renderWidth);
        preview.setHeight(renderHeight);
        preview.setRenderX(FenceEntity.computeRenderX(tileX, tileWidth, renderWidth, spriteKey));
        preview.setRenderY(FenceEntity.computeRenderY(tileY, tileHeight, renderHeight));
    }
}
