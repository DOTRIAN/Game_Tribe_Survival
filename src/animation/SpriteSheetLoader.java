package animation;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class SpriteSheetLoader {
    private static final Map<String, Image> SHEET_CACHE = new HashMap<>();
    private static final Map<String, Image[]> FRAME_CACHE = new HashMap<>();

    private SpriteSheetLoader() {
    }

    public static synchronized Image[] loadGrid(String imagePath, int columns, int rows) {
        String cacheKey = "grid-auto|" + imagePath + "|" + columns + "|" + rows;
        Image[] cached = FRAME_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Image spriteSheet = loadSheet(imagePath);
        int frameWidth = (int) spriteSheet.getWidth() / columns;
        int frameHeight = (int) spriteSheet.getHeight() / rows;
        Image[] frames = loadGrid(imagePath, columns, rows, frameWidth, frameHeight);
        FRAME_CACHE.put(cacheKey, frames);
        return frames;
    }

    public static synchronized Image[] loadGrid(String imagePath, int columns, int rows, int frameWidth, int frameHeight) {
        String cacheKey = "grid|" + imagePath + "|" + columns + "|" + rows + "|" + frameWidth + "|" + frameHeight;
        Image[] cached = FRAME_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Image spriteSheet = loadSheet(imagePath);
        PixelReader pixelReader = spriteSheet.getPixelReader();

        Image[] frames = new Image[columns * rows];
        int index = 0;

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int x = column * frameWidth;
                int y = row * frameHeight;
                frames[index] = new WritableImage(pixelReader, x, y, frameWidth, frameHeight);
                index++;
            }
        }

        FRAME_CACHE.put(cacheKey, frames);
        return frames;
    }

    public static synchronized Image[] loadHorizontalStrip(String imagePath, int frameCount) {
        String cacheKey = "strip|" + imagePath + "|" + frameCount;
        Image[] cached = FRAME_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Image spriteSheet = loadSheet(imagePath);
        PixelReader pixelReader = spriteSheet.getPixelReader();
        if (pixelReader == null || frameCount <= 0) {
            return new Image[0];
        }

        int sheetWidth = (int) Math.round(spriteSheet.getWidth());
        int sheetHeight = (int) Math.round(spriteSheet.getHeight());
        Image[] frames = new Image[frameCount];
        for (int index = 0; index < frameCount; index++) {
            int startX = (int) Math.round(index * sheetWidth / (double) frameCount);
            int endX = (int) Math.round((index + 1) * sheetWidth / (double) frameCount);
            int frameWidth = Math.max(1, endX - startX);
            if (startX + frameWidth > sheetWidth) {
                frameWidth = Math.max(1, sheetWidth - startX);
            }
            frames[index] = new WritableImage(pixelReader, startX, 0, frameWidth, sheetHeight);
        }
        FRAME_CACHE.put(cacheKey, frames);
        return frames;
    }

    // Cat theo region bat dau tu (startX,startY) de lay dung row mong muon trong spritesheet.
    public static synchronized Image[] loadGridRegion(String imagePath,
                                                      int startX,
                                                      int startY,
                                                      int columns,
                                                      int rows,
                                                      int frameWidth,
                                                      int frameHeight) {
        String cacheKey = "region|" + imagePath + "|" + startX + "|" + startY + "|" + columns + "|" + rows + "|" + frameWidth + "|" + frameHeight;
        Image[] cached = FRAME_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Image spriteSheet = loadSheet(imagePath);
        PixelReader pixelReader = spriteSheet.getPixelReader();

        Image[] frames = new Image[columns * rows];
        int index = 0;

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int x = startX + column * frameWidth;
                int y = startY + row * frameHeight;
                frames[index] = new WritableImage(pixelReader, x, y, frameWidth, frameHeight);
                index++;
            }
        }

        FRAME_CACHE.put(cacheKey, frames);
        return frames;
    }

    private static Image loadSheet(String imagePath) {
        return SHEET_CACHE.computeIfAbsent(imagePath, SpriteSheetLoader::createImage);
    }

    private static Image createImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return new WritableImage(1, 1);
        }

        if (looksLikeExternalUrl(imagePath)) {
            return new Image(imagePath, false);
        }

        Path localPath = Path.of(imagePath);
        if (Files.exists(localPath)) {
            return new Image(localPath.toUri().toString(), false);
        }

        var resource = SpriteSheetLoader.class.getClassLoader().getResource(imagePath);
        if (resource != null) {
            return new Image(resource.toExternalForm(), false);
        }

        return new Image(imagePath, false);
    }

    private static boolean looksLikeExternalUrl(String imagePath) {
        return imagePath.startsWith("file:")
                || imagePath.startsWith("jar:")
                || imagePath.startsWith("http:")
                || imagePath.startsWith("https:");
    }
}
