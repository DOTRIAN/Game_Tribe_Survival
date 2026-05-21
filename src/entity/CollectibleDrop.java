package entity;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CollectibleDrop extends Entity {
    private static final long FRAME_DURATION_NS = 100_000_000L;
    private static final String COIN_BASE_PATH = "assets/coin&xp/coin";
    private static final String XP_BASE_PATH = "assets/coin&xp/xp";
    private static final List<Image> COIN_FRAMES = loadDropFrames(COIN_BASE_PATH, 16, 16, 5);
    private static final List<Image> XP_FRAMES = loadDropFrames(XP_BASE_PATH, 16, 16, 4);

    private final DropType type;
    private final int value;
    private final ImageView imageView;
    private final List<Image> frames;
    private int currentFrame;
    private final Rectangle2D hitbox;

    public CollectibleDrop(DropType type, double x, double y, int value, double size) {
        super(x, y, Math.max(4.0, size), Math.max(4.0, size), 0, 1);
        this.type = type == null ? DropType.COIN : type;
        this.value = Math.max(0, value);
        this.imageView = new ImageView();
        this.imageView.setFitWidth(Math.max(4.0, size));
        this.imageView.setFitHeight(Math.max(4.0, size));
        this.imageView.setPreserveRatio(true);
        this.imageView.setLayoutX(x);
        this.imageView.setLayoutY(y);

        this.frames = this.type == DropType.COIN ? COIN_FRAMES : XP_FRAMES;
        this.currentFrame = 0;
        if (!frames.isEmpty()) {
            this.imageView.setImage(frames.get(0));
        }
        this.hitbox = new Rectangle2D(x, y, getWidth(), getHeight());
    }

    public DropType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public List<Image> getFrames() {
        return frames;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public Rectangle2D getHitbox() {
        return hitbox;
    }

    public static void preloadAssets() {
        // Touch static frame lists before gameplay so the first attack does not load/crop PNGs.
        int loadedFrameCount = COIN_FRAMES.size() + XP_FRAMES.size();
        if (loadedFrameCount <= 0) {
            System.out.println("Cannot load drop sprite: no coin/xp frames loaded");
        }
    }

    public void updateAnimation(long nowNs) {
        if (frames.isEmpty()) {
            return;
        }
        int nextFrame = (int) ((nowNs / FRAME_DURATION_NS) % frames.size());
        if (nextFrame == currentFrame && imageView.getImage() != null) {
            return;
        }
        currentFrame = nextFrame;
        imageView.setImage(frames.get(currentFrame));
    }

    @Override
    public void triggerHitFlash(long nowNs, long durationNs, Color color) {
        // Drop khong nhan damage.
    }

    private static List<Image> loadDropFrames(String basePath, int frameWidth, int frameHeight, int frameCount) {
        String spritePath = resolveSpriteSheetPath(basePath);
        if (spritePath == null) {
            System.out.println("Cannot load drop sprite: " + basePath);
            return List.of();
        }
        return loadSpriteSheet(spritePath, frameWidth, frameHeight, frameCount);
    }

    private static String resolveSpriteSheetPath(String basePath) {
        File exactFile = new File(basePath);
        if (exactFile.exists() && exactFile.isFile()) {
            return exactFile.getPath().replace('\\', '/');
        }
        File pngFile = new File(basePath + ".png");
        if (pngFile.exists() && pngFile.isFile()) {
            return pngFile.getPath().replace('\\', '/');
        }
        File dir = new File(basePath);
        if (dir.exists() && dir.isDirectory()) {
            File[] pngFiles = dir.listFiles(file -> file != null && file.isFile() && file.getName().toLowerCase().endsWith(".png"));
            if (pngFiles != null && pngFiles.length > 0) {
                for (File candidate : pngFiles) {
                    String lower = candidate.getName().toLowerCase();
                    if (!lower.contains("preview") && !lower.contains("thumb") && !lower.contains("icon")) {
                        return candidate.getPath().replace('\\', '/');
                    }
                }
                return pngFiles[0].getPath().replace('\\', '/');
            }
        }
        return null;
    }

    public static List<Image> loadSpriteSheet(String path, int frameWidth, int frameHeight, int frameCount) {
        if (path == null || path.isBlank()) {
            System.out.println("Cannot load drop sprite: " + path);
            return List.of();
        }
        Image spriteSheet = new Image("file:" + path);
        if (spriteSheet.isError() || spriteSheet.getPixelReader() == null) {
            System.out.println("Cannot load drop sprite: " + path);
            return List.of();
        }

        int safeFrameWidth = Math.max(1, frameWidth);
        int safeFrameHeight = Math.max(1, frameHeight);
        int maxFramesByWidth = Math.max(1, (int) (spriteSheet.getWidth() / safeFrameWidth));
        if (spriteSheet.getHeight() < safeFrameHeight) {
            System.out.println("Cannot load drop sprite: " + path);
            return List.of();
        }
        int safeFrameCount = Math.max(1, Math.min(frameCount, maxFramesByWidth));

        List<Image> result = new ArrayList<>(safeFrameCount);
        for (int i = 0; i < safeFrameCount; i++) {
            int sx = i * safeFrameWidth;
            int sy = 0;
            WritableImage frame = new WritableImage(spriteSheet.getPixelReader(), sx, sy, safeFrameWidth, safeFrameHeight);
            result.add(frame);
        }
        return result;
    }
}
