package build;

import javafx.scene.image.Image;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AssetManager:
 * - Chi load DUY NHAT 1 sprite sheet tuong.jpg.
 * - Sau do cat sprite va map lai theo ten cu/ten moi de phan con lai cua game van goi theo key.
 */
public class AssetManager {
    private static final Path WALL_ASSET_ROOT = Path.of("D:\\IT\\Game_TEST\\assets\\stone_wall");

    // spriteCache:
    // - key la ten sprite logic.
    // - value la Image da crop tu sprite sheet.
    private final Map<String, Image> spriteCache;

    public AssetManager() {
        this.spriteCache = new LinkedHashMap<>();
        loadWallSpriteSheet();
    }

    /**
     * getWallImage:
     * - Input: ten sprite logic.
     * - Output: Image da crop/cache.
     * - Tac dong gameplay: hotbar, wall that va resolver dung chung cung mot nguon sprite.
     */
    public Image getWallImage(String assetKey) {
        if (assetKey == null || assetKey.isBlank()) {
            throw new IllegalArgumentException("assetKey must not be blank");
        }
        Image image = spriteCache.get(assetKey);
        if (image == null) {
            System.out.println("Missing wall sprite key: " + assetKey);
        }
        return image;
    }

    public Map<String, Image> getLoadedSprites() {
        return Collections.unmodifiableMap(spriteCache);
    }

    private void loadWallSpriteSheet() {
        Path imagePath = WALL_ASSET_ROOT.resolve(WallSpriteConfig.SHEET_FILE_NAME);
        Image sheet = new Image(imagePath.toUri().toString());
        if (sheet.isError()) {
            System.out.println("Failed to load wall sprite sheet: " + imagePath);
            return;
        }

        spriteCache.clear();
        spriteCache.putAll(SpriteSheetLoader.cropAll(sheet, WallSpriteConfig.getRegions()));
    }
}
