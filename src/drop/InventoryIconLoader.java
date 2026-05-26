package drop;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InventoryIconLoader {
    private static final Map<String, Image> ICON_CACHE = new LinkedHashMap<>();

    private InventoryIconLoader() {
    }

    public static synchronized Image loadMainIcon(String directory) {
        if (directory == null || directory.isBlank()) {
            return null;
        }
        if (ICON_CACHE.containsKey(directory)) {
            return ICON_CACHE.get(directory);
        }

        File file = new File(directory, "main.png");
        if (!file.exists() || !file.isFile()) {
            ICON_CACHE.put(directory, null);
            return null;
        }

        Image raw = new Image(file.toURI().toString(), false);
        if (raw.isError() || raw.getWidth() <= 0 || raw.getHeight() <= 0 || raw.getPixelReader() == null) {
            ICON_CACHE.put(directory, null);
            return null;
        }

        Image prepared = DropSpriteLoader.prepareInventoryIcon(raw);
        ICON_CACHE.put(directory, prepared);
        return prepared;
    }

    public static synchronized Image loadFileIcon(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String cacheKey = "file|" + path;
        if (ICON_CACHE.containsKey(cacheKey)) {
            return ICON_CACHE.get(cacheKey);
        }

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            ICON_CACHE.put(cacheKey, null);
            return null;
        }

        Image raw = new Image(file.toURI().toString(), false);
        if (raw.isError() || raw.getWidth() <= 0 || raw.getHeight() <= 0 || raw.getPixelReader() == null) {
            ICON_CACHE.put(cacheKey, null);
            return null;
        }

        Image prepared = DropSpriteLoader.prepareInventoryIcon(raw);
        ICON_CACHE.put(cacheKey, prepared);
        return prepared;
    }

    public static synchronized Image loadSpriteSheetIcon(String path, int columns, int rows, int frameIndex) {
        if (path == null || path.isBlank() || columns <= 0 || rows <= 0 || frameIndex < 0) {
            return null;
        }
        String cacheKey = "sheet|" + path + "|" + columns + "|" + rows + "|" + frameIndex;
        if (ICON_CACHE.containsKey(cacheKey)) {
            return ICON_CACHE.get(cacheKey);
        }

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            ICON_CACHE.put(cacheKey, null);
            return null;
        }

        Image raw = new Image(file.toURI().toString(), false);
        PixelReader reader = raw.getPixelReader();
        if (raw.isError() || raw.getWidth() <= 0 || raw.getHeight() <= 0 || reader == null) {
            ICON_CACHE.put(cacheKey, null);
            return null;
        }

        int frameWidth = Math.max(1, (int) raw.getWidth() / columns);
        int frameHeight = Math.max(1, (int) raw.getHeight() / rows);
        int maxFrameCount = columns * rows;
        int safeFrameIndex = Math.min(frameIndex, Math.max(0, maxFrameCount - 1));
        int frameX = (safeFrameIndex % columns) * frameWidth;
        int frameY = (safeFrameIndex / columns) * frameHeight;

        Image cropped = new WritableImage(reader, frameX, frameY, frameWidth, frameHeight);
        Image prepared = DropSpriteLoader.prepareInventoryIcon(cropped);
        ICON_CACHE.put(cacheKey, prepared);
        return prepared;
    }
}
