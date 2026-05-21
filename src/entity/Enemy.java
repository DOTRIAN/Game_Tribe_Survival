package entity;

import animation.SpriteAnimation;
import animation.SpriteSheetLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import system.CollisionSystem;
import system.DamageSystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Enemy (abstract):
 * - Lop cha cho tat ca quai trong game.
 * - Chua state + hanh vi chung:
 *   1) Vi tri/kich thuoc/toc do
 *   2) HP, sat thuong, cooldown tan cong
 *   3) AI co ban: duoi theo player
 *   4) Render animation run/idle + lat trai/phai
 *
 * Muc tieu OOP:
 * - Class con (OrcEnemy, SkeletonEnemy,...) chi can truyen asset + stat rieng.
 * - Gameplay core trong Game xu ly bang da hinh (List<Enemy>).
 */
public abstract class Enemy extends Entity {
    protected int damage;

    private final SpriteAnimation runAnimation;
    private final SpriteAnimation idleAnimation;
    private Image currentFrame;
    private boolean facingRight;
    private boolean huggingTarget;
    private long lastAttackAtNs;
    private final long attackCooldownNs;
    // Nguong chong lat huong lien tuc khi enemy o sat tam player.
    // Chi doi huong neu chenh lech X du lon.
    private static final double FACE_FLIP_THRESHOLD_X = 3.0;
    // Khi da rat sat player thi dung nhich them de giam dao dong qua lai.
    private static final double APPROACH_STOP_DISTANCE = 6.0;
    // Cache sprite da tint de tranh tao image moi moi frame.
    private static final Map<String, Image> TINT_CACHE = new HashMap<>();

    protected Enemy(double x,
                    double y,
                    double width,
                    double height,
                    double speed,
                    int maxHp,
                    int damage,
                    long attackCooldownNs,
                    String runSheetPath,
                    int runColumns,
                    int runRows,
                    long runFrameNs,
                    String idleSheetPath,
                    int idleColumns,
                    int idleRows,
                    long idleFrameNs) {
        super(x, y, width, height, speed, maxHp);
        this.damage = Math.max(1, damage);
        this.attackCooldownNs = Math.max(1L, attackCooldownNs);

        Image[] runFrames = SpriteSheetLoader.loadGrid(runSheetPath, runColumns, runRows);
        Image[] idleFrames = SpriteSheetLoader.loadGrid(idleSheetPath, idleColumns, idleRows);
        int normalizedFrameWidth = maxFrameWidth(runFrames, idleFrames);
        int normalizedFrameHeight = maxFrameHeight(runFrames, idleFrames);

        this.runAnimation = new SpriteAnimation(
                normalizeFrameCanvas(runFrames, normalizedFrameWidth, normalizedFrameHeight),
                runFrameNs
        );
        this.idleAnimation = new SpriteAnimation(
                normalizeFrameCanvas(idleFrames, normalizedFrameWidth, normalizedFrameHeight),
                idleFrameNs
        );
        this.currentFrame = idleAnimation.getCurrentFrame();
        this.facingRight = true;
        this.huggingTarget = false;
        this.lastAttackAtNs = 0L;
    }

    public void update(long now, Player player, double worldWidth, double worldHeight) {
        if (!isAlive() || player == null) {
            return;
        }

        double oldX = x;
        double oldY = y;
        moveToward(player);
        clampPosition(0, 0, worldWidth, worldHeight);

        boolean moving = oldX != x || oldY != y;
        if (moving || huggingTarget) {
            runAnimation.update(now, true);
            currentFrame = runAnimation.getCurrentFrame();
        } else {
            idleAnimation.update(now, true);
            currentFrame = idleAnimation.getCurrentFrame();
        }
    }

    // AI co ban: duoi thang vao tam player.
    protected void moveToward(Player player) {
        double targetX = player.getCenterX();
        double targetY = player.getCenterY();
        double selfX = getCenterX();
        double selfY = getCenterY();

        double dx = targetX - selfX;
        double dy = targetY - selfY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        huggingTarget = distance <= APPROACH_STOP_DISTANCE;
        // O rat gan thi dung doi huong + dung di chuyen de tranh rung giat.
        if (huggingTarget) {
            return;
        }

        x += (dx / distance) * speed;
        y += (dy / distance) * speed;
        if (Math.abs(dx) >= FACE_FLIP_THRESHOLD_X) {
            facingRight = dx >= 0;
        }
    }

    // Tan cong player khi va cham va da qua cooldown.
    public void tryAttackPlayer(Player player, long now) {
        if (!isAlive() || player == null) {
            return;
        }
        if (!CollisionSystem.intersects(this, player)) {
            return;
        }
        if (now - lastAttackAtNs < attackCooldownNs) {
            return;
        }
        DamageSystem.applyDamage(this, player, damage);
        lastAttackAtNs = now;
    }

    public void draw(GraphicsContext graphicsContext, double cameraX, double cameraY, long nowNs) {
        if (!isAlive()) {
            return;
        }

        // Snap pixel de giam blur/nhin "phinh" khi camera va sprite dang o toa do le.
        double screenX = Math.round(x - cameraX);
        double screenY = Math.round(y - cameraY);
        if (currentFrame == null || currentFrame.isError()) {
            graphicsContext.setFill(Color.CRIMSON);
            graphicsContext.fillRect(screenX, screenY, width, height);
            return;
        }

        if (facingRight) {
            graphicsContext.drawImage(currentFrame, screenX, screenY, width, height);
        } else {
            graphicsContext.save();
            graphicsContext.translate(screenX + width, screenY);
            graphicsContext.scale(-1, 1);
            graphicsContext.drawImage(currentFrame, 0, 0, width, height);
            graphicsContext.restore();
        }

        // Neu vua bi danh thi phu mot lop mau do mo trong 120ms de tao feedback trung don.
        if (isHitFlashActive(nowNs)) {
            Image tinted = buildTintedBySourceAlpha(currentFrame, getHitFlashColor());
            if (tinted != null) {
                graphicsContext.save();
                // Giam do dam theo yeu cau: nhin ro nhung khong bi gat.
                graphicsContext.setGlobalAlpha(0.62);
                if (facingRight) {
                    graphicsContext.drawImage(tinted, screenX, screenY, width, height);
                } else {
                    graphicsContext.translate(screenX + width, screenY);
                    graphicsContext.scale(-1, 1);
                    graphicsContext.drawImage(tinted, 0, 0, width, height);
                }
                graphicsContext.restore();
            }
        }
    }

    // Dung cho spawn manager phan loai loai quai.
    public abstract String getEnemyType();

    // Tao image tint chi dua tren alpha pixel cua sprite goc:
    // - pixel trong suot van giu trong suot
    // - pixel co alpha se duoc to mau flash
    // => hieu ung om dung hinh dang sprite, khong tao hinh vuong.
    private Image buildTintedBySourceAlpha(Image source, Color tint) {
        if (source == null || source.isError() || tint == null) {
            return null;
        }
        int w = (int) source.getWidth();
        int h = (int) source.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }

        int tintR = (int) Math.round(tint.getRed() * 255.0);
        int tintG = (int) Math.round(tint.getGreen() * 255.0);
        int tintB = (int) Math.round(tint.getBlue() * 255.0);
        String key = System.identityHashCode(source) + ":" + tintR + ":" + tintG + ":" + tintB;
        Image cached = TINT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        PixelReader reader = source.getPixelReader();
        if (reader == null) {
            return null;
        }
        WritableImage tinted = new WritableImage(w, h);
        PixelWriter writer = tinted.getPixelWriter();
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                Color src = reader.getColor(px, py);
                double a = src.getOpacity();
                if (a <= 0.001) {
                    writer.setColor(px, py, Color.TRANSPARENT);
                    continue;
                }
                writer.setColor(px, py, Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), a));
            }
        }
        TINT_CACHE.put(key, tinted);
        return tinted;
    }

    private Image[] normalizeFrameCanvas(Image[] frames, int targetWidth, int targetHeight) {
        if (frames == null || frames.length == 0 || targetWidth <= 0 || targetHeight <= 0) {
            return frames == null ? new Image[0] : frames;
        }
        Image[] normalized = new Image[frames.length];
        for (int index = 0; index < frames.length; index++) {
            Image frame = frames[index];
            if (frame == null || frame.isError()) {
                normalized[index] = frame;
                continue;
            }
            int frameWidth = (int) Math.round(frame.getWidth());
            int frameHeight = (int) Math.round(frame.getHeight());
            if (frameWidth == targetWidth && frameHeight == targetHeight) {
                normalized[index] = frame;
                continue;
            }
            WritableImage padded = new WritableImage(targetWidth, targetHeight);
            PixelReader reader = frame.getPixelReader();
            PixelWriter writer = padded.getPixelWriter();
            if (reader == null) {
                normalized[index] = frame;
                continue;
            }
            int offsetX = Math.max(0, (targetWidth - frameWidth) / 2);
            int offsetY = Math.max(0, targetHeight - frameHeight);
            for (int py = 0; py < frameHeight; py++) {
                for (int px = 0; px < frameWidth; px++) {
                    writer.setColor(offsetX + px, offsetY + py, reader.getColor(px, py));
                }
            }
            normalized[index] = padded;
        }
        return normalized;
    }

    private int maxFrameWidth(Image[] primaryFrames, Image[] secondaryFrames) {
        return Math.max(maxFrameDimension(primaryFrames, true), maxFrameDimension(secondaryFrames, true));
    }

    private int maxFrameHeight(Image[] primaryFrames, Image[] secondaryFrames) {
        return Math.max(maxFrameDimension(primaryFrames, false), maxFrameDimension(secondaryFrames, false));
    }

    private int maxFrameDimension(Image[] frames, boolean widthDimension) {
        int max = 0;
        if (frames == null) {
            return max;
        }
        for (Image frame : frames) {
            if (frame == null || frame.isError()) {
                continue;
            }
            int value = (int) Math.round(widthDimension ? frame.getWidth() : frame.getHeight());
            if (value > max) {
                max = value;
            }
        }
        return max;
    }
}

