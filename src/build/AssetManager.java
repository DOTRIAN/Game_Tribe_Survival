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
    private static final Path TORCH_SHEET_PATH = Path.of("D:\\IT\\Game_TEST\\assets\\Torch.png");

    // spriteCache:
    // - key la ten sprite logic.
    // - value la Image da crop tu sprite sheet.
    private final Map<String, Image> spriteCache;
    private final Map<String, Image[]> animationCache;

    public AssetManager() {
        this.spriteCache = new LinkedHashMap<>();
        this.animationCache = new LinkedHashMap<>();
        loadWallSpriteSheet();
        loadTorchSpriteSheet();
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

    public Image[] getAnimationFrames(String animationKey) {
        if (animationKey == null || animationKey.isBlank()) {
            return new Image[0];
        }
        Image[] frames = animationCache.get(animationKey);
        if (frames == null) {
            return new Image[0];
        }
        return frames.clone();
    }

    public Image getAnimationFrame(String animationKey, long nowNs, long frameDurationNs) {
        Image[] frames = animationCache.get(animationKey);
        if (frames == null || frames.length == 0) {
            return null;
        }
        if (frameDurationNs <= 0) {
            return frames[0];
        }
        long frameIndex = Math.max(0L, nowNs / frameDurationNs) % frames.length;
        return frames[(int) frameIndex];
    }

    private void loadWallSpriteSheet() {
        Path imagePath = WALL_ASSET_ROOT.resolve(WallSpriteConfig.SHEET_FILE_NAME);
        Image sheet = new Image(imagePath.toUri().toString());
        if (sheet.isError()) {
            System.out.println("Failed to load wall sprite sheet: " + imagePath);
            return;
        }

        spriteCache.putAll(SpriteSheetLoader.cropAll(sheet, WallSpriteConfig.getRegions()));
    }

    private void loadTorchSpriteSheet() {
        Image sheet = new Image(TORCH_SHEET_PATH.toUri().toString());
        if (sheet.isError()) {
            System.out.println("Failed to load torch sprite sheet: " + TORCH_SHEET_PATH);
            return;
        }
        Image[] frames = animation.SpriteSheetLoader.loadGrid(TORCH_SHEET_PATH.toUri().toString(), 4, 2);
        if (frames.length == 0) {
            return;
        }
        animationCache.put("torch", frames);
        spriteCache.put("torch_icon", frames[0]);
        spriteCache.put("torch_frame_0", frames[0]);
        for (int index = 0; index < frames.length; index++) {
            spriteCache.put("torch_frame_" + index, frames[index]);
        }
    }
}
