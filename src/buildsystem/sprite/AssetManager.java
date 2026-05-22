package buildsystem.sprite;

import buildsystem.core.BuildAssetResolver;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

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
    private static final Path WALL_ASSET_ROOT = Path.of("assets", "stone_wall");
    private static final Path TORCH_SHEET_PATH = Path.of("assets", "Torch.png");
    private static final int TORCH_SHEET_COLUMNS = 4;
    private static final int TORCH_SHEET_ROWS = 2;
    private static final Path ARCHER_SHEET_PATH = Path.of("assets", "thap_ban_cung", "thap_cung.png");
    private static final Path ARCHER_ARROW_PATH = Path.of("assets", "thap_ban_cung", "Arrow01(32x32).png");
    private static final int ARCHER_SHEET_ROWS = 2;
    private static final int ARCHER_IDLE_FRAME_COUNT = 7;
    private static final int ARCHER_ATTACK_FRAME_COUNT = 8;

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
        loadArcherTowerSprites();
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
        Image[] frames = animation.SpriteSheetLoader.loadGrid(
                TORCH_SHEET_PATH.toUri().toString(),
                TORCH_SHEET_COLUMNS,
                TORCH_SHEET_ROWS
        );
        if (frames.length == 0) {
            return;
        }
        animationCache.put("torch", frames);
        spriteCache.put("torch_icon", transparentNearWhite(frames[0]));
        spriteCache.put("torch_frame_0", transparentNearWhite(frames[0]));
        for (int index = 0; index < frames.length; index++) {
            spriteCache.put("torch_frame_" + index, transparentNearWhite(frames[index]));
        }
    }

    private void loadArcherTowerSprites() {
        Image sheet = new Image(ARCHER_SHEET_PATH.toUri().toString());
        if (sheet.isError()) {
            Image fallback = spriteCache.get("wall_icon");
            if (fallback != null) {
                animationCache.put("archer_tower_idle", new Image[]{fallback});
                animationCache.put("archer_tower_fire", new Image[]{fallback});
                spriteCache.put("archer_tower_icon", fallback);
            }
            return;
        }

        int frameHeight = Math.max(1, (int) Math.round(sheet.getHeight() / ARCHER_SHEET_ROWS));
        Image cleanedSheet = transparentNearWhite(sheet);
        Image[] idleFrames = splitVisibleFramesInRow(cleanedSheet, 0, frameHeight, ARCHER_IDLE_FRAME_COUNT);
        Image[] fireFrames = cropArcherRow(sheet, frameHeight, frameHeight, ARCHER_ATTACK_FRAME_COUNT);
        if (idleFrames.length == 0 || fireFrames.length != ARCHER_ATTACK_FRAME_COUNT) {
            Image fallback = spriteCache.get("wall_icon");
            if (fallback != null) {
                animationCache.put("archer_tower_idle", new Image[]{fallback});
                animationCache.put("archer_tower_fire", new Image[]{fallback});
                spriteCache.put("archer_tower_icon", fallback);
            }
            return;
        }
        idleFrames = normalizeFramesToCanvas(idleFrames);
        fireFrames = normalizeFramesToCanvas(fireFrames);

        animationCache.put("archer_tower_idle", idleFrames);
        animationCache.put("archer_tower_fire", fireFrames);
        spriteCache.put("archer_tower_icon", idleFrames[0]);
        for (int index = 0; index < idleFrames.length; index++) {
            spriteCache.put("archer_tower_idle_" + index, idleFrames[index]);
        }
        for (int index = 0; index < fireFrames.length; index++) {
            spriteCache.put("archer_tower_fire_" + index, fireFrames[index]);
        }

        Image arrow = new Image(ARCHER_ARROW_PATH.toUri().toString());
        if (!arrow.isError()) {
            spriteCache.put("archer_arrow", arrow);
        }
    }

    private Image[] cropArcherRow(Image sheet, int startY, int frameHeight, int frameCount) {
        PixelReader reader = sheet.getPixelReader();
        if (reader == null || frameHeight <= 0 || frameCount <= 0) {
            return new Image[0];
        }
        int sheetWidth = (int) Math.round(sheet.getWidth());
        Image[] frames = new Image[frameCount];
        for (int index = 0; index < frameCount; index++) {
            int x = (int) Math.round(index * sheetWidth / (double) frameCount);
            int nextX = (int) Math.round((index + 1) * sheetWidth / (double) frameCount);
            int width = Math.max(1, nextX - x);
            frames[index] = transparentNearWhite(new WritableImage(reader, x, startY, width, frameHeight));
        }
        return frames;
    }

    private Image[] normalizeFramesToCanvas(Image[] frames) {
        if (frames == null || frames.length == 0) {
            return new Image[0];
        }
        int targetWidth = 0;
        int targetHeight = 0;
        for (Image frame : frames) {
            if (frame == null || frame.isError()) {
                continue;
            }
            targetWidth = Math.max(targetWidth, (int) Math.round(frame.getWidth()));
            targetHeight = Math.max(targetHeight, (int) Math.round(frame.getHeight()));
        }
        if (targetWidth <= 0 || targetHeight <= 0) {
            return frames;
        }

        Image[] normalized = new Image[frames.length];
        for (int index = 0; index < frames.length; index++) {
            Image frame = frames[index];
            if (frame == null || frame.isError()) {
                normalized[index] = frame;
                continue;
            }
            PixelReader reader = frame.getPixelReader();
            if (reader == null) {
                normalized[index] = frame;
                continue;
            }
            int sourceWidth = (int) Math.round(frame.getWidth());
            int sourceHeight = (int) Math.round(frame.getHeight());
            WritableImage canvas = new WritableImage(targetWidth, targetHeight);
            PixelWriter writer = canvas.getPixelWriter();
            int offsetX = Math.max(0, (targetWidth - sourceWidth) / 2);
            int offsetY = Math.max(0, targetHeight - sourceHeight);
            for (int y = 0; y < sourceHeight; y++) {
                for (int x = 0; x < sourceWidth; x++) {
                    writer.setColor(offsetX + x, offsetY + y, reader.getColor(x, y));
                }
            }
            normalized[index] = canvas;
        }
        return normalized;
    }

    private Image[] splitVisibleFramesInRow(Image sheet, int startY, int rowHeight, int maxFrames) {
        PixelReader reader = sheet.getPixelReader();
        if (reader == null || rowHeight <= 0 || maxFrames <= 0) {
            return new Image[0];
        }
        int sheetWidth = (int) Math.round(sheet.getWidth());
        java.util.List<Image> frames = new java.util.ArrayList<>();
        boolean inside = false;
        int startX = 0;
        for (int x = 0; x < sheetWidth; x++) {
            boolean hasPixel = false;
            for (int y = startY; y < startY + rowHeight; y += 2) {
                if (reader.getColor(x, y).getOpacity() > 0.05) {
                    hasPixel = true;
                    break;
                }
            }
            if (hasPixel && !inside) {
                inside = true;
                startX = x;
            } else if (!hasPixel && inside) {
                addVisibleFrame(reader, frames, startX, x - 1, startY, rowHeight, sheetWidth);
                inside = false;
            }
        }
        if (inside) {
            addVisibleFrame(reader, frames, startX, sheetWidth - 1, startY, rowHeight, sheetWidth);
        }
        if (frames.size() > maxFrames) {
            frames = frames.subList(0, maxFrames);
        }
        return frames.toArray(new Image[0]);
    }

    private void addVisibleFrame(PixelReader reader,
                                 java.util.List<Image> frames,
                                 int startX,
                                 int endX,
                                 int startY,
                                 int rowHeight,
                                 int sheetWidth) {
        int padding = 1;
        int x = Math.max(0, startX - padding);
        int right = Math.min(sheetWidth - 1, endX + padding);
        int width = Math.max(1, right - x + 1);
        if (width < 40) {
            return;
        }
        frames.add(new WritableImage(reader, x, startY, width, rowHeight));
    }

    private Image[] loadGridOrSingle(Path path, int columns, int rows) {
        Image sheet = new Image(path.toUri().toString());
        if (sheet.isError()) {
            return new Image[0];
        }
        Image[] frames = animation.SpriteSheetLoader.loadGrid(path.toUri().toString(), columns, rows);
        if (frames.length > 0) {
            return frames;
        }
        return new Image[]{sheet};
    }

    private Image transparentNearWhite(Image source) {
        if (source == null || source.isError()) {
            return source;
        }
        int width = (int) Math.round(source.getWidth());
        int height = (int) Math.round(source.getHeight());
        if (width <= 0 || height <= 0) {
            return source;
        }
        PixelReader reader = source.getPixelReader();
        if (reader == null) {
            return source;
        }
        WritableImage cleaned = new WritableImage(width, height);
        PixelWriter writer = cleaned.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                if (isNearWhite(color)) {
                    writer.setColor(x, y, Color.TRANSPARENT);
                } else {
                    writer.setColor(x, y, color);
                }
            }
        }
        return cleaned;
    }

    private boolean isNearWhite(Color color) {
        if (color == null || color.getOpacity() <= 0.001) {
            return false;
        }
        return color.getRed() >= 0.96
                && color.getGreen() >= 0.96
                && color.getBlue() >= 0.96;
    }
}
