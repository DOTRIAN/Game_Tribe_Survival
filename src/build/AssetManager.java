package build;

import buildsystem.core.BuildAssetResolver;
import javafx.scene.image.Image;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AssetManager:
 * - Hien tai van load sprite sheet tuong.jpg, nhung da expose qua BuildAssetResolver de buildsystem khong phu thuoc package cu.
 * - Khi sau nay co sprite atlas rieng cho trap/chest/torch/turret, chi can mo rong implementation nay.
 */
public class AssetManager implements BuildAssetResolver {
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
     * - Alias cu giu tuong thich code renderer/UI cu.
     */
    public Image getWallImage(String assetKey) {
        return getSprite(assetKey);
    }

    @Override
    public Image getSprite(String assetKey) {
        if (assetKey == null || assetKey.isBlank()) {
            return null;
        }
        Image image = spriteCache.get(assetKey);
        if (image == null) {
            System.out.println("Missing build sprite key: " + assetKey);
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
