package wallbuilder.asset;

import javafx.scene.image.Image;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * AssetManager:
 * - Noi DUY NHAT khai bao duong dan goc cua bo asset wall.
 * - Moi class khac chi lay anh qua AssetManager, tranh hardcode lap lai.
 */
public class AssetManager {
    private static final Path WALL_ASSET_ROOT = Path.of("D:\\IT\\Game_TEST\\assets\\stone_wall");

    private final Map<String, Image> imageCache;

    public AssetManager() {
        this.imageCache = new HashMap<>();
    }

    /**
     * getWallImage:
     * - Input: ten asset nhu stoneWall_S, stoneWallHalf_E, stoneWallRound_N.
     * - Output: anh da load va da cache.
     * - Tac dong gameplay: hotbar va world dung cung mot asset manager.
     */
    public Image getWallImage(String assetKey) {
        if (assetKey == null || assetKey.isBlank()) {
            throw new IllegalArgumentException("assetKey must not be blank");
        }
        return imageCache.computeIfAbsent(assetKey, this::loadWallImage);
    }

    public Path getWallRoot() {
        return WALL_ASSET_ROOT;
    }

    private Image loadWallImage(String assetKey) {
        Path path = WALL_ASSET_ROOT.resolve(assetKey + ".png");
        return new Image(path.toUri().toString());
    }
}
