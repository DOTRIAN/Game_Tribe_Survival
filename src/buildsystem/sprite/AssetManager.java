package buildsystem.sprite;

import buildsystem.core.BuildAssetResolver;
import buildsystem.fence.FenceRenderer;
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
    private static final Path TORCH_SHEET_PATH = Path.of("assets", "Torch.png");
    private static final int TORCH_SHEET_COLUMNS = 4;
    private static final int TORCH_SHEET_ROWS = 2;
    private static final Path ARCHER_SHEET_PATH = Path.of("assets", "thap_ban_cung", "thap_cung.png");
    private static final Path ARCHER_ARROW_PATH = Path.of("assets", "thap_ban_cung", "Arrow01(32x32).png");
    private static final Path FRIENDLY_ARCHER_IDLE_PATH = Path.of("assets", "Skeleton_Archer", "Idle.png");
    private static final Path FRIENDLY_ARCHER_WALK_PATH = Path.of("assets", "Skeleton_Archer", "Walk.png");
    private static final Path FRIENDLY_ARCHER_ATTACK_PATH = Path.of("assets", "Skeleton_Archer", "Attack.png");
    private static final Path FRIENDLY_ARCHER_SHOT_PATH = Path.of("assets", "Skeleton_Archer", "Shot.png");
    private static final Path FRIENDLY_ARCHER_EVASION_PATH = Path.of("assets", "Skeleton_Archer", "Evasion.png");
    private static final Path FRIENDLY_ARCHER_HURT_PATH = Path.of("assets", "Skeleton_Archer", "Hurt.png");
    private static final Path FRIENDLY_ARCHER_DEAD_PATH = Path.of("assets", "Skeleton_Archer", "Dead.png");
    private static final Path FRIENDLY_ARCHER_ARROW_PATH = Path.of("assets", "Skeleton_Archer", "Arrow.png");
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
    private static final Path CHEST_DIR = Path.of("assets", "chest");
    private static final int ARCHER_SHEET_ROWS = 1;
    private static final int ARCHER_IDLE_FRAME_COUNT = 4;
    private static final int FRIENDLY_ARCHER_IDLE_COLS = 7;
    private static final int FRIENDLY_ARCHER_WALK_COLS = 8;
    private static final int FRIENDLY_ARCHER_ATTACK_COLS = 5;
    private static final int FRIENDLY_ARCHER_SHOT_COLS = 15;
    private static final int FRIENDLY_ARCHER_EVASION_COLS = 6;
    private static final int FRIENDLY_ARCHER_HURT_COLS = 2;
    private static final int FRIENDLY_ARCHER_DEAD_COLS = 6;

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
        loadFriendlyArcherSprites();
        loadBombTrapSprites();
        loadThrowableFireBombSprites();
        loadChestSprites();
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
        Map<String, Image> fenceSprites = FenceRenderer.loadSprites();
        if (fenceSprites.isEmpty()) {
            return;
        }
        spriteCache.putAll(fenceSprites);
        Image single = fenceSprites.get(FenceRenderer.SINGLE_SPRITE_KEY);
        if (single != null) {
            spriteCache.put("wall_icon", single);
            spriteCache.put("wall_single", single);
            spriteCache.put("wall_straight_base", single);
        }
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
                spriteCache.put("archer_tower_icon", fallback);
            }
            return;
        }

        int frameHeight = Math.max(1, (int) Math.round(sheet.getHeight() / ARCHER_SHEET_ROWS));
        Image[] idleFrames = cropArcherRow(sheet, 0, frameHeight, ARCHER_IDLE_FRAME_COUNT);
        if (idleFrames.length != ARCHER_IDLE_FRAME_COUNT) {
            Image fallback = spriteCache.get("wall_icon");
            if (fallback != null) {
                animationCache.put("archer_tower_idle", new Image[]{fallback});
                spriteCache.put("archer_tower_icon", fallback);
            }
            return;
        }
        idleFrames = normalizeArcherFrames(idleFrames, new int[]{0, -1, 0, 1});

        animationCache.put("archer_tower_idle", idleFrames);
        spriteCache.put("archer_tower_icon", idleFrames[0]);
        for (int index = 0; index < idleFrames.length; index++) {
            spriteCache.put("archer_tower_idle_" + index, idleFrames[index]);
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

    private Image[] loadFriendlyArcherStrip(Path imagePath, int columns) {
        if (imagePath == null || columns <= 0 || !Files.isRegularFile(imagePath)) {
            return new Image[0];
        }
        Image sheet = new Image(imagePath.toUri().toString());
        if (sheet.isError()) {
            return new Image[0];
        }
        PixelReader reader = sheet.getPixelReader();
        if (reader == null) {
            return new Image[0];
        }
        int sheetWidth = (int) Math.round(sheet.getWidth());
        int frameWidth = Math.max(1, (int) Math.round(sheetWidth / (double) columns));
        int frameHeight = Math.max(1, (int) Math.round(sheet.getHeight()));
        Image[] frames = new Image[columns];
        for (int i = 0; i < columns; i++) {
            int x = Math.min(sheetWidth - 1, i * frameWidth);
            int width = Math.min(frameWidth, sheetWidth - x);
            frames[i] = cleanFriendlyArcherFrameBackground(new WritableImage(reader, x, 0, Math.max(1, width), frameHeight));
        }
        return normalizeFramesToCanvas(frames);
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

    private void loadChestSprites() {
        Path closedPath = findChestImagePath("closed");
        Path ajarPath = findChestImagePath("ajar");
        Path openPath = findChestImagePath("open");
        if (closedPath == null) {
            return;
        }

        Image closed = cleanBombFrameBackground(new Image(closedPath.toUri().toString()));
        if (closed == null || closed.isError()) {
            return;
        }
        Image ajar = ajarPath == null ? closed : cleanBombFrameBackground(new Image(ajarPath.toUri().toString()));
        Image open = openPath == null ? closed : cleanBombFrameBackground(new Image(openPath.toUri().toString()));

        Image[] frames = new Image[] {
                closed,
                ajar == null || ajar.isError() ? closed : ajar,
                open == null || open.isError() ? closed : open
        };
        animationCache.put("chest", frames);
        spriteCache.put("chest_closed", frames[0]);
        spriteCache.put("chest_ajar", frames[1]);
        spriteCache.put("chest_open", frames[2]);
        spriteCache.put("chest_icon", frames[2]);
    }

    private Path findChestImagePath(String keyword) {
        if (keyword == null || keyword.isBlank() || !Files.isDirectory(CHEST_DIR)) {
            return null;
        }
        try (var stream = Files.list(CHEST_DIR)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.contains(keyword.toLowerCase()) && name.endsWith(".png");
                    })
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
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
            frames[index] = cleanArcherFrameBackground(new WritableImage(reader, x, startY, width, frameHeight));
        }
        return frames;
    }

    private void loadFriendlyArcherSprites() {
        Image[] idle = loadFriendlyArcherStrip(FRIENDLY_ARCHER_IDLE_PATH, FRIENDLY_ARCHER_IDLE_COLS);
        Image[] walk = loadFriendlyArcherStrip(FRIENDLY_ARCHER_WALK_PATH, FRIENDLY_ARCHER_WALK_COLS);
        Image[] attack = loadFriendlyArcherStrip(FRIENDLY_ARCHER_ATTACK_PATH, FRIENDLY_ARCHER_ATTACK_COLS);
        Image[] shot = loadFriendlyArcherStrip(FRIENDLY_ARCHER_SHOT_PATH, FRIENDLY_ARCHER_SHOT_COLS);
        Image[] evasion = loadFriendlyArcherStrip(FRIENDLY_ARCHER_EVASION_PATH, FRIENDLY_ARCHER_EVASION_COLS);
        Image[] hurt = loadFriendlyArcherStrip(FRIENDLY_ARCHER_HURT_PATH, FRIENDLY_ARCHER_HURT_COLS);
        Image[] dead = loadFriendlyArcherStrip(FRIENDLY_ARCHER_DEAD_PATH, FRIENDLY_ARCHER_DEAD_COLS);

        if (idle.length > 0) {
            animationCache.put("friendly_archer_idle", idle);
            spriteCache.put("friendly_archer_icon", idle[0]);
            for (int i = 0; i < idle.length; i++) {
                spriteCache.put("friendly_archer_idle_" + i, idle[i]);
            }
        }
        if (walk.length > 0) {
            animationCache.put("friendly_archer_walk", walk);
        }
        if (attack.length > 0) {
            animationCache.put("friendly_archer_attack", attack);
        }
        if (shot.length > 0) {
            animationCache.put("friendly_archer_shot", shot);
        }
        if (evasion.length > 0) {
            animationCache.put("friendly_archer_evasion", evasion);
        }
        if (hurt.length > 0) {
            animationCache.put("friendly_archer_hurt", hurt);
        }
        if (dead.length > 0) {
            animationCache.put("friendly_archer_dead", dead);
        }

        Image arrow = new Image(FRIENDLY_ARCHER_ARROW_PATH.toUri().toString());
        if (!arrow.isError()) {
            spriteCache.put("friendly_archer_arrow", cleanFriendlyArcherFrameBackground(arrow));
        }
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

    private Image cleanArcherFrameBackground(Image source) {
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
                if (isArcherBackground(color)) {
                    writer.setColor(x, y, Color.TRANSPARENT);
                } else {
                    writer.setColor(x, y, color);
                }
            }
        }
        return cleaned;
    }

    private Image cleanFriendlyArcherFrameBackground(Image source) {
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
                if (isFriendlyArcherBackground(color)) {
                    writer.setColor(x, y, Color.TRANSPARENT);
                } else {
                    writer.setColor(x, y, color);
                }
            }
        }
        return trimTransparentBounds(cleaned);
    }

    private Image[] normalizeArcherFrames(Image[] frames, int[] yAdjustments) {
        if (frames == null || frames.length == 0) {
            return new Image[0];
        }

        java.util.List<FrameBox> boxes = new java.util.ArrayList<>();
        int targetHeight = 0;
        int maxLeftExtent = 0;
        int maxRightExtent = 0;

        for (Image frame : frames) {
            FrameBox box = computeFrameBox(frame, 2);
            boxes.add(box);
            if (box == null) {
                continue;
            }
            targetHeight = Math.max(targetHeight, box.height);
            maxLeftExtent = Math.max(maxLeftExtent, box.footCenterX);
            maxRightExtent = Math.max(maxRightExtent, box.width - box.footCenterX);
        }

        if (maxLeftExtent <= 0 || maxRightExtent <= 0 || targetHeight <= 0) {
            return frames;
        }

        int targetFootX = maxLeftExtent + 2;
        int targetWidth = targetFootX + maxRightExtent + 2;
        Image[] normalized = new Image[frames.length];
        for (int index = 0; index < frames.length; index++) {
            FrameBox box = boxes.get(index);
            Image frame = frames[index];
            if (box == null || frame == null || frame.isError()) {
                normalized[index] = frame;
                continue;
            }
            PixelReader reader = frame.getPixelReader();
            if (reader == null) {
                normalized[index] = frame;
                continue;
            }

            int adjustY = yAdjustments != null && index < yAdjustments.length ? yAdjustments[index] : 0;
            int offsetX = targetFootX - box.footCenterX;
            int offsetY = targetHeight - box.height + adjustY;
            offsetX = Math.max(0, Math.min(offsetX, Math.max(0, targetWidth - box.width)));
            offsetY = Math.max(0, Math.min(offsetY, Math.max(0, targetHeight - box.height)));
            WritableImage canvas = new WritableImage(targetWidth, targetHeight);
            PixelWriter writer = canvas.getPixelWriter();
            for (int y = 0; y < box.height; y++) {
                for (int x = 0; x < box.width; x++) {
                    writer.setColor(offsetX + x, offsetY + y, reader.getColor(box.minX + x, box.minY + y));
                }
            }
            normalized[index] = canvas;
        }
        return normalized;
    }

    private FrameBox computeFrameBox(Image frame, int padding) {
        if (frame == null || frame.isError()) {
            return null;
        }
        PixelReader reader = frame.getPixelReader();
        if (reader == null) {
            return null;
        }
        int width = (int) Math.round(frame.getWidth());
        int height = (int) Math.round(frame.getHeight());
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;
        int weightedX = 0;
        int weightedCount = 0;
        int footBandStart = Math.max(0, height - Math.max(12, height / 5));

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                if (color.getOpacity() <= 0.03) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                if (y >= footBandStart) {
                    weightedX += x;
                    weightedCount++;
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return null;
        }

        int safeMinX = Math.max(0, minX - padding);
        int safeMinY = Math.max(0, minY - padding);
        int safeMaxX = Math.min(width - 1, maxX + padding);
        int safeMaxY = Math.min(height - 1, maxY + padding);
        int footCenterX = weightedCount > 0 ? Math.round(weightedX / (float) weightedCount) : (safeMinX + safeMaxX) / 2;
        return new FrameBox(
                safeMinX,
                safeMinY,
                safeMaxX - safeMinX + 1,
                safeMaxY - safeMinY + 1,
                Math.max(0, footCenterX - safeMinX)
        );
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

    private boolean isArcherBackground(Color color) {
        if (color == null || color.getOpacity() <= 0.001) {
            return false;
        }
        double max = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()));
        double min = Math.min(color.getRed(), Math.min(color.getGreen(), color.getBlue()));
        return color.getRed() >= 0.88
                && color.getGreen() >= 0.88
                && color.getBlue() >= 0.88
                && (max - min) <= 0.10;
    }

    private boolean isFriendlyArcherBackground(Color color) {
        if (color == null || color.getOpacity() <= 0.001) {
            return false;
        }
        double max = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()));
        double min = Math.min(color.getRed(), Math.min(color.getGreen(), color.getBlue()));
        return color.getRed() >= 0.90
                && color.getGreen() >= 0.90
                && color.getBlue() >= 0.90
                && (max - min) <= 0.12;
    }

    private static final class FrameBox {
        private final int minX;
        private final int minY;
        private final int width;
        private final int height;
        private final int footCenterX;

        private FrameBox(int minX, int minY, int width, int height, int footCenterX) {
            this.minX = minX;
            this.minY = minY;
            this.width = width;
            this.height = height;
            this.footCenterX = footCenterX;
        }
    }

}
