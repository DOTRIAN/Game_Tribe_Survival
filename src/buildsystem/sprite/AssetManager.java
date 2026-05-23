package buildsystem.sprite;

import buildsystem.core.BuildAssetResolver;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AssetManager:
 * - Load asset build theo duong dan tuong doi trong project.
 * - Khi sau nay co sprite atlas rieng cho trap/chest/torch/turret, chi can mo rong implementation nay.
 */
public class AssetManager implements BuildAssetResolver {
    private static final Path WOOD_FENCE_SHEET_PATH = Path.of("assets", "woodFence", "woodFence.png");
    private static final Path TORCH_SHEET_PATH = Path.of("assets", "Torch.png");
    private static final int TORCH_SHEET_COLUMNS = 4;
    private static final int TORCH_SHEET_ROWS = 2;
    private static final Path ARCHER_SHEET_PATH = Path.of("assets", "thap_ban_cung", "thap_cung.png");
    private static final Path ARCHER_ARROW_PATH = Path.of("assets", "thap_ban_cung", "Arrow01(32x32).png");
    private static final List<Path> BOMB_SHEET_CANDIDATES = List.of(
            Path.of("assets", "bom", "png"),
            Path.of("assets", "bom", "png", "bom.png"),
            Path.of("assets", "bom", "bom.png"),
            Path.of("assets", "bom.png")
    );
    private static final int BOMB_SHEET_COLUMNS = 7;
    private static final int BOMB_SHEET_ROWS = 1;
    private static final Path FIRE_BOMB_ICON_PATH = Path.of("assets", "firebomb", "bomb.png");
    private static final Path FIRE_BOMB_SHOP_ICON_PATH = Path.of("assets", "firebomb", "image_bomb.png");
    private static final Path FIRE_BOMB_BLAST_PATH = Path.of("assets", "firebomb", "fire.png");
    private static final Path FIRE_BOMB_EMBER_PATH = Path.of("assets", "firebomb", "fire_tan.png");
    private static final Path FIRE_BOMB_ANIMATION_PATH = Path.of("assets", "firebomb", "animation_fire.png");
    private static final Path FIRE_BOMB_FINAL_1_PATH = Path.of("assets", "firebomb", "animation_final_1.png");
    private static final Path FIRE_BOMB_FINAL_2_PATH = Path.of("assets", "firebomb", "animation_final_2.png");
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
        loadWoodFenceSprites();
        loadTorchSpriteSheet();
        loadArcherTowerSprites();
        loadBombTrapSprites();
        loadThrowableFireBombSprites();
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

    private void loadWoodFenceSprites() {
        Image sheet = new Image(WOOD_FENCE_SHEET_PATH.toUri().toString());
        if (sheet.isError()) {
            System.out.println("Failed to load wood fence sheet: " + WOOD_FENCE_SHEET_PATH);
            return;
        }
        Image fenceSingle = cropAndKeyBackground(sheet, 477, 246, 266, 195, false);
        if (fenceSingle == null) {
            return;
        }
        spriteCache.put("wood_fence_single", fenceSingle);
        spriteCache.put("wood_fence_icon", fenceSingle);
        // Legacy aliases so old keys do not break while stone_wall assets are removed.
        spriteCache.put("wall_icon", fenceSingle);
        spriteCache.put("wall_single", fenceSingle);
        spriteCache.put("wall_straight_base", fenceSingle);
        loadWoodFencePostSprites(sheet);
    }

    private void loadWoodFencePostSprites(Image sheet) {
        for (int mask = 0; mask <= 15; mask++) {
            spriteCache.put("wood_fence_mask_" + mask, createWoodFencePostSprite(sheet, mask));
        }
        spriteCache.put("fence_single", spriteCache.get("wood_fence_mask_0"));
        spriteCache.put("horizontal_single", spriteCache.get("wood_fence_single"));
        spriteCache.put("vertical_single", spriteCache.get("wood_fence_mask_12"));
        spriteCache.put("horizontal_left_end", spriteCache.get("wood_fence_mask_2"));
        spriteCache.put("horizontal_middle", spriteCache.get("wood_fence_mask_3"));
        spriteCache.put("horizontal_right_end", spriteCache.get("wood_fence_mask_1"));
        spriteCache.put("vertical_top_end", spriteCache.get("wood_fence_mask_8"));
        spriteCache.put("vertical_middle", spriteCache.get("wood_fence_mask_12"));
        spriteCache.put("vertical_bottom_end", spriteCache.get("wood_fence_mask_4"));
        spriteCache.put("corner_top_left", spriteCache.get("wood_fence_mask_10"));
        spriteCache.put("corner_top_right", spriteCache.get("wood_fence_mask_9"));
        spriteCache.put("corner_bottom_left", spriteCache.get("wood_fence_mask_6"));
        spriteCache.put("corner_bottom_right", spriteCache.get("wood_fence_mask_5"));
        spriteCache.put("t_up", spriteCache.get("wood_fence_mask_11"));
        spriteCache.put("t_down", spriteCache.get("wood_fence_mask_7"));
        spriteCache.put("t_left", spriteCache.get("wood_fence_mask_13"));
        spriteCache.put("t_right", spriteCache.get("wood_fence_mask_14"));
        spriteCache.put("cross", spriteCache.get("wood_fence_mask_15"));
    }

    private Image createWoodFencePostSprite(Image sheet, int mask) {
        Canvas canvas = new Canvas(16, 16);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setImageSmoothing(false);

        boolean left = (mask & 1) != 0;
        boolean right = (mask & 2) != 0;
        boolean up = (mask & 4) != 0;
        boolean down = (mask & 8) != 0;

        // Rail texture sampled from the horizontal fence asset. It is drawn before the post
        // so every corner/T/cross keeps one shared post at the tile center.
        if (left) {
            graphics.drawImage(sheet, 570, 318, 80, 40, 0, 5, 8, 6);
        }
        if (right) {
            graphics.drawImage(sheet, 570, 318, 80, 40, 8, 5, 8, 6);
        }
        if (up) {
            graphics.drawImage(sheet, 570, 318, 80, 40, 5, 0, 6, 8);
        }
        if (down) {
            graphics.drawImage(sheet, 570, 318, 80, 40, 5, 8, 6, 8);
        }

        graphics.drawImage(sheet, 516, 246, 55, 195, 4, 1, 8, 14);
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        WritableImage image = new WritableImage(16, 16);
        canvas.snapshot(parameters, image);
        return transparentBrightBackground(image, false);
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

    private void loadBombTrapSprites() {
        Path sheetPath = resolveBombSheetPath();
        if (sheetPath == null) {
            System.out.println("Failed to resolve bomb trap sheet path");
            return;
        }
        Image sheet = new Image(sheetPath.toUri().toString());
        if (sheet.isError()) {
            System.out.println("Failed to load bomb trap sheet: " + sheetPath);
            return;
        }
        Image[] frames = splitBombSheet(sheet, BOMB_SHEET_COLUMNS, BOMB_SHEET_ROWS);
        if (frames.length == 0) {
            return;
        }
        animationCache.put("bomb_trap", frames);
        spriteCache.put("bomb_trap_icon", frames[0]);
        for (int i = 0; i < frames.length; i++) {
            spriteCache.put("bomb_trap_" + i, frames[i]);
        }
    }

    private Path resolveBombSheetPath() {
        for (Path candidate : BOMB_SHEET_CANDIDATES) {
            if (candidate == null) {
                continue;
            }
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private void loadThrowableFireBombSprites() {
        Image shopIcon = safeLoad(FIRE_BOMB_SHOP_ICON_PATH);
        Image croppedShopIcon = cropAndCleanFullImage(shopIcon, 3);
        if (croppedShopIcon != null) {
            spriteCache.put("fire_bomb_shop_icon", croppedShopIcon);
        }
        Image[] bombFrames = loadFireBombThrowFrames();
        if (bombFrames.length > 0) {
            animationCache.put("fire_bomb_throw", bombFrames);
            Image cleanedIcon = bombFrames[0];
            spriteCache.put("fire_bomb_icon", cleanedIcon);
            spriteCache.put("fire_bomb_projectile", cleanedIcon);
            if (!spriteCache.containsKey("fire_bomb_shop_icon")) {
                spriteCache.put("fire_bomb_shop_icon", cleanedIcon);
            }
        }
        Image[] blastFrames = loadFireSpreadFrames();
        if (blastFrames.length > 0) {
            animationCache.put("fire_bomb_blast", blastFrames);
            spriteCache.put("fire_bomb_blast", blastFrames[0]);
        }
        Image[] emberFrames = loadFireEmberFrames();
        if (emberFrames.length > 0) {
            animationCache.put("fire_bomb_ember", emberFrames);
            spriteCache.put("fire_bomb_ember", emberFrames[0]);
        }
    }

    private Image[] loadFireBombThrowFrames() {
        Image sheet = safeLoad(FIRE_BOMB_ICON_PATH);
        if (sheet == null) {
            return new Image[0];
        }
        return extractGridFrames(sheet, 6, 2, 3, true);
    }

    private Image[] loadFireSpreadFrames() {
        Image finalSheet = safeLoad(FIRE_BOMB_FINAL_1_PATH);
        Image sheet = finalSheet;
        if (sheet == null) {
            sheet = safeLoad(FIRE_BOMB_ANIMATION_PATH);
        }
        if (sheet == null) {
            sheet = safeLoad(FIRE_BOMB_BLAST_PATH);
        }
        if (sheet == null) {
            return new Image[0];
        }
        if (finalSheet != null) {
            List<Image> frames = new java.util.ArrayList<>();
            addFrames(frames, extractGridRowFrames(sheet, 2, 4, 0, 4, true));
            addFrames(frames, extractGridRowFrames(sheet, 2, 4, 1, 4, true));
            addFrames(frames, extractGridRowFrames(sheet, 2, 4, 2, 4, true));
            addFrames(frames, extractGridRowFrames(sheet, 2, 4, 3, 4, true));
            return normalizeFramesToCanvas(frames.toArray(new Image[0]));
        }
        List<Image> frames = new java.util.ArrayList<>();
        addFrames(frames, extractGridRowFrames(sheet, 5, 4, 0, 6, true));
        addFrames(frames, extractGridRowFrames(sheet, 5, 4, 1, 6, true));
        addFrames(frames, extractGridRowFrames(sheet, 4, 4, 2, 6, true));
        return normalizeFramesToCanvas(frames.toArray(new Image[0]));
    }

    private Image[] loadFireEmberFrames() {
        Image finalSheet = safeLoad(FIRE_BOMB_FINAL_2_PATH);
        Image sheet = finalSheet;
        if (sheet == null) {
            sheet = safeLoad(FIRE_BOMB_ANIMATION_PATH);
        }
        if (sheet == null) {
            sheet = safeLoad(FIRE_BOMB_EMBER_PATH);
        }
        if (sheet == null) {
            return new Image[0];
        }
        if (finalSheet != null) {
            List<Image> frames = new java.util.ArrayList<>();
            addFrames(frames, extractGridRowFrames(sheet, 2, 4, 0, 4, true));
            addFrames(frames, extractGridRowFrames(sheet, 2, 4, 1, 4, true));
            addFrames(frames, extractGridRowFrames(sheet, 2, 4, 2, 4, true));
            addFrames(frames, extractGridRowFrames(sheet, 2, 4, 3, 4, true));
            return normalizeFramesToCanvas(frames.toArray(new Image[0]));
        }
        return extractGridRowFrames(sheet, 6, 4, 3, 6, true);
    }

    private Image cropAndCleanFullImage(Image image, int padding) {
        if (image == null || image.isError()) {
            return null;
        }
        Image cropped = cropAlphaBounds(image, 0, Math.max(0, (int) Math.round(image.getWidth()) - 1), padding);
        return cleanBombFrameBackground(cropped);
    }

    private Image[] extractGridFrames(Image sheet, int columns, int rows, int padding, boolean cleanBackground) {
        PixelReader reader = sheet == null ? null : sheet.getPixelReader();
        if (reader == null || columns <= 0 || rows <= 0) {
            return new Image[0];
        }
        int sheetWidth = (int) Math.round(sheet.getWidth());
        int sheetHeight = (int) Math.round(sheet.getHeight());
        Image[] frames = new Image[columns * rows];
        int index = 0;
        for (int row = 0; row < rows; row++) {
            int y = (int) Math.round(row * sheetHeight / (double) rows);
            int nextY = (int) Math.round((row + 1) * sheetHeight / (double) rows);
            int frameHeight = Math.max(1, nextY - y);
            for (int col = 0; col < columns; col++) {
                int x = (int) Math.round(col * sheetWidth / (double) columns);
                int nextX = (int) Math.round((col + 1) * sheetWidth / (double) columns);
                int frameWidth = Math.max(1, nextX - x);
                Image frame = new WritableImage(reader, x, y, frameWidth, frameHeight);
                frame = cropAlphaBounds(frame, 0, frameWidth - 1, padding);
                if (cleanBackground) {
                    frame = cleanBombFrameBackground(frame);
                }
                frames[index++] = frame;
            }
        }
        return normalizeFramesToCanvas(frames);
    }

    private Image[] extractGridRowFrames(Image sheet,
                                         int columnsInRow,
                                         int totalRows,
                                         int rowIndex,
                                         int padding,
                                         boolean cleanBackground) {
        PixelReader reader = sheet == null ? null : sheet.getPixelReader();
        if (reader == null || columnsInRow <= 0 || totalRows <= 0 || rowIndex < 0 || rowIndex >= totalRows) {
            return new Image[0];
        }
        int sheetWidth = (int) Math.round(sheet.getWidth());
        int sheetHeight = (int) Math.round(sheet.getHeight());
        int y = (int) Math.round(rowIndex * sheetHeight / (double) totalRows);
        int nextY = (int) Math.round((rowIndex + 1) * sheetHeight / (double) totalRows);
        int frameHeight = Math.max(1, nextY - y);
        Image[] frames = new Image[columnsInRow];
        for (int col = 0; col < columnsInRow; col++) {
            int x = (int) Math.round(col * sheetWidth / (double) columnsInRow);
            int nextX = (int) Math.round((col + 1) * sheetWidth / (double) columnsInRow);
            int frameWidth = Math.max(1, nextX - x);
            Image frame = new WritableImage(reader, x, y, frameWidth, frameHeight);
            frame = cropAlphaBounds(frame, 0, frameWidth - 1, padding);
            if (cleanBackground) {
                frame = cleanBombFrameBackground(frame);
            }
            frames[col] = frame;
        }
        return normalizeFramesToCanvas(frames);
    }

    private void addFrames(List<Image> target, Image[] source) {
        if (target == null || source == null) {
            return;
        }
        for (Image frame : source) {
            if (frame != null) {
                target.add(frame);
            }
        }
    }

    private Image safeLoad(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return null;
        }
        Image image = new Image(path.toUri().toString());
        if (image.isError()) {
            return null;
        }
        return image;
    }

    private Image[] splitBombSheet(Image sheet, int columns, int rows) {
        PixelReader reader = sheet.getPixelReader();
        if (reader == null || columns <= 0 || rows <= 0) {
            return new Image[0];
        }
        Image[] detectedFrames = splitBombSheetByAlpha(sheet, columns);
        if (detectedFrames.length == columns) {
            return normalizeFramesToCanvas(detectedFrames);
        }
        int sheetWidth = (int) Math.round(sheet.getWidth());
        int sheetHeight = (int) Math.round(sheet.getHeight());
        int totalFrames = columns * rows;
        Image[] frames = new Image[totalFrames];
        int frameIndex = 0;
        for (int row = 0; row < rows; row++) {
            int y = (int) Math.round(row * sheetHeight / (double) rows);
            int nextY = (int) Math.round((row + 1) * sheetHeight / (double) rows);
            int frameHeight = Math.max(1, nextY - y);
            for (int col = 0; col < columns; col++) {
                int x = (int) Math.round(col * sheetWidth / (double) columns);
                int nextX = (int) Math.round((col + 1) * sheetWidth / (double) columns);
                int frameWidth = Math.max(1, nextX - x);
                WritableImage frame = new WritableImage(reader, x, y, frameWidth, frameHeight);
                frames[frameIndex++] = cleanBombFrameBackground(frame);
            }
        }
        return normalizeFramesToCanvas(frames);
    }

    private Image[] splitBombSheetByAlpha(Image sheet, int expectedFrames) {
        return splitFramesByAlphaClusters(sheet, expectedFrames, 12, 0.04, 6);
    }

    private Image[] splitFramesByAlphaClusters(Image sheet,
                                               int expectedFrames,
                                               int gapTolerance,
                                               double alphaThreshold,
                                               int padding) {
        PixelReader reader = sheet == null ? null : sheet.getPixelReader();
        if (reader == null || expectedFrames <= 0) {
            return new Image[0];
        }
        int width = (int) Math.round(sheet.getWidth());
        int height = (int) Math.round(sheet.getHeight());
        java.util.List<int[]> clusters = new java.util.ArrayList<>();
        boolean inside = false;
        int startX = 0;
        int previousSolidX = 0;
        int gap = 0;
        for (int x = 0; x < width; x++) {
            int alphaCount = 0;
            for (int y = 0; y < height; y++) {
                if (reader.getColor(x, y).getOpacity() > alphaThreshold) {
                    alphaCount++;
                }
            }
            boolean solid = alphaCount > 40;
            if (solid) {
                if (!inside) {
                    inside = true;
                    startX = x;
                }
                previousSolidX = x;
                gap = 0;
            } else if (inside) {
                gap++;
                if (gap > gapTolerance) {
                    clusters.add(new int[]{startX, previousSolidX});
                    inside = false;
                    gap = 0;
                }
            }
        }
        if (inside) {
            clusters.add(new int[]{startX, previousSolidX});
        }
        if (clusters.size() != expectedFrames) {
            return new Image[0];
        }

        Image[] frames = new Image[expectedFrames];
        for (int i = 0; i < expectedFrames; i++) {
            int[] cluster = clusters.get(i);
            frames[i] = transparentNearWhite(cropAlphaBounds(sheet, cluster[0], cluster[1], padding));
        }
        return normalizeFramesToCanvas(frames);
    }

    private Image cropAlphaBounds(Image source, int minSearchX, int maxSearchX, int padding) {
        PixelReader reader = source == null ? null : source.getPixelReader();
        if (reader == null) {
            return source;
        }
        int sourceWidth = (int) Math.round(source.getWidth());
        int sourceHeight = (int) Math.round(source.getHeight());
        int minX = sourceWidth;
        int minY = sourceHeight;
        int maxX = -1;
        int maxY = -1;
        int safeMinX = Math.max(0, minSearchX);
        int safeMaxX = Math.min(sourceWidth - 1, maxSearchX);
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = safeMinX; x <= safeMaxX; x++) {
                if (reader.getColor(x, y).getOpacity() <= 0.04) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < minX || maxY < minY) {
            return new WritableImage(reader, safeMinX, 0, Math.max(1, safeMaxX - safeMinX + 1), sourceHeight);
        }
        int x = Math.max(0, minX - padding);
        int y = Math.max(0, minY - padding);
        int right = Math.min(sourceWidth - 1, maxX + padding);
        int bottom = Math.min(sourceHeight - 1, maxY + padding);
        return new WritableImage(reader, x, y, Math.max(1, right - x + 1), Math.max(1, bottom - y + 1));
    }

    private Image cleanBombFrameBackground(Image frame) {
        if (frame == null || frame.isError()) {
            return frame;
        }
        PixelReader reader = frame.getPixelReader();
        if (reader == null) {
            return frame;
        }
        int width = (int) Math.round(frame.getWidth());
        int height = (int) Math.round(frame.getHeight());
        WritableImage cleaned = new WritableImage(width, height);
        PixelWriter writer = cleaned.getPixelWriter();
        Color background = sampleBombBackground(reader, width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                if (isBombBackground(background, color)) {
                    writer.setColor(x, y, Color.TRANSPARENT);
                } else {
                    writer.setColor(x, y, color);
                }
            }
        }
        return trimTransparentBounds(cleaned);
    }

    private Color sampleBombBackground(PixelReader reader, int width, int height) {
        if (reader == null || width <= 0 || height <= 0) {
            return Color.TRANSPARENT;
        }
        Color topLeft = reader.getColor(0, 0);
        Color topRight = reader.getColor(Math.max(0, width - 1), 0);
        Color bottomLeft = reader.getColor(0, Math.max(0, height - 1));
        Color bottomRight = reader.getColor(Math.max(0, width - 1), Math.max(0, height - 1));
        return Color.color(
                (topLeft.getRed() + topRight.getRed() + bottomLeft.getRed() + bottomRight.getRed()) / 4.0,
                (topLeft.getGreen() + topRight.getGreen() + bottomLeft.getGreen() + bottomRight.getGreen()) / 4.0,
                (topLeft.getBlue() + topRight.getBlue() + bottomLeft.getBlue() + bottomRight.getBlue()) / 4.0,
                1.0
        );
    }

    private boolean isBombBackground(Color base, Color current) {
        if (base == null || current == null) {
            return false;
        }
        double dr = Math.abs(base.getRed() - current.getRed());
        double dg = Math.abs(base.getGreen() - current.getGreen());
        double db = Math.abs(base.getBlue() - current.getBlue());
        return current.getOpacity() > 0.001 && dr + dg + db <= 0.22;
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

    private Image cropAndKeyBackground(Image source, int x, int y, int width, int height) {
        return cropAndKeyBackground(source, x, y, width, height, true);
    }

    private Image cropAndKeyBackground(Image source, int x, int y, int width, int height, boolean trim) {
        if (source == null || source.isError()) {
            return null;
        }
        PixelReader reader = source.getPixelReader();
        if (reader == null) {
            return null;
        }
        int safeX = Math.max(0, x);
        int safeY = Math.max(0, y);
        int safeWidth = Math.max(1, Math.min(width, (int) source.getWidth() - safeX));
        int safeHeight = Math.max(1, Math.min(height, (int) source.getHeight() - safeY));
        WritableImage cropped = new WritableImage(reader, safeX, safeY, safeWidth, safeHeight);
        return transparentBrightBackground(cropped, trim);
    }

    private Image transparentBrightBackground(Image source) {
        return transparentBrightBackground(source, true);
    }

    private Image transparentBrightBackground(Image source, boolean trim) {
        if (source == null || source.isError()) {
            return source;
        }
        int width = (int) Math.round(source.getWidth());
        int height = (int) Math.round(source.getHeight());
        PixelReader reader = source.getPixelReader();
        if (reader == null) {
            return source;
        }
        WritableImage cleaned = new WritableImage(width, height);
        PixelWriter writer = cleaned.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                if (isBrightLowSaturation(color)) {
                    writer.setColor(x, y, Color.TRANSPARENT);
                } else {
                    writer.setColor(x, y, color);
                }
            }
        }
        return trim ? trimTransparentBounds(cleaned) : cleaned;
    }

    private Image trimTransparentBounds(Image source) {
        int width = (int) Math.round(source.getWidth());
        int height = (int) Math.round(source.getHeight());
        PixelReader reader = source.getPixelReader();
        if (reader == null) {
            return source;
        }
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (reader.getColor(x, y).getOpacity() <= 0.02) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < minX || maxY < minY) {
            return source;
        }
        return new WritableImage(reader, minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private boolean isNearWhite(Color color) {
        if (color == null || color.getOpacity() <= 0.001) {
            return false;
        }
        return color.getRed() >= 0.96
                && color.getGreen() >= 0.96
                && color.getBlue() >= 0.96;
    }

    private boolean isBrightLowSaturation(Color color) {
        if (color == null) {
            return false;
        }
        double max = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()));
        double min = Math.min(color.getRed(), Math.min(color.getGreen(), color.getBlue()));
        double saturation = max <= 0.0001 ? 0.0 : (max - min) / max;
        return color.getOpacity() > 0.001
                && max >= 0.82
                && saturation <= 0.18;
    }
}
