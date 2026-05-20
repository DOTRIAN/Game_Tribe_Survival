package buildsystem.core;

import javafx.scene.image.Image;

/**
 * BuildAssetResolver:
 * - Contract nho de buildsystem lay sprite/icon ma khong phu thuoc game cu hay demo.
 * - Game chinh, demo, editor chi can cung cap implementation nay la BuildManager render duoc preview/object.
 */
public interface BuildAssetResolver {
    Image getSprite(String spriteKey);

    default Image getIcon(BuildDefinition definition) {
        if (definition == null) {
            return null;
        }
        return getSprite(definition.getIconSpriteKey());
    }
}
