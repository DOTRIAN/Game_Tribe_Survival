package system.bomb;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * FireBombBurnZone:
 * - Vung lua ton tai sau khi fire bomb no.
 * - Render theo 3 pha: impact nho -> chay lien tuc -> tan dan.
 * - Damage tick giam dan theo thoi gian.
 */
public class FireBombBurnZone {
    private static final long IMPACT_DURATION_NS = 180_000_000L;
    private static final long SPREAD_TO_FULL_NS = 900_000_000L;

    private final double worldX;
    private final double worldY;
    private final double damageRadius;
    private final int baseTickDamage;
    private final long createdAtNs;
    private final long burnDurationNs;
    private final long fadeDurationNs;
    private final long damageTickNs;
    private final Image[] burnFrames;
    private final Image[] emberFrames;

    private long lastDamageTickNs;

    public FireBombBurnZone(double worldX,
                            double worldY,
                            double damageRadius,
                            int baseTickDamage,
                            long createdAtNs,
                            long burnDurationNs,
                            long fadeDurationNs,
                            long damageTickNs,
                            Image[] burnFrames,
                            Image[] emberFrames) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.damageRadius = Math.max(8.0, damageRadius);
        this.baseTickDamage = Math.max(1, baseTickDamage);
        this.createdAtNs = createdAtNs;
        this.burnDurationNs = Math.max(500_000_000L, burnDurationNs);
        this.fadeDurationNs = Math.max(200_000_000L, fadeDurationNs);
        this.damageTickNs = Math.max(80_000_000L, damageTickNs);
        this.burnFrames = burnFrames == null ? new Image[0] : burnFrames.clone();
        this.emberFrames = emberFrames == null ? new Image[0] : emberFrames.clone();
        this.lastDamageTickNs = createdAtNs - this.damageTickNs;
    }

    public boolean isExpired(long nowNs) {
        return nowNs - createdAtNs > getTotalLifetimeNs();
    }

    public boolean shouldApplyDamage(long nowNs) {
        if (nowNs - createdAtNs > getTotalLifetimeNs()) {
            return false;
        }
        return nowNs - lastDamageTickNs >= damageTickNs;
    }

    public void markDamageApplied(long nowNs) {
        lastDamageTickNs = nowNs;
    }

    public boolean contains(double targetX, double targetY) {
        double dx = targetX - worldX;
        double dy = targetY - worldY;
        return dx * dx + dy * dy <= damageRadius * damageRadius;
    }

    public int resolveTickDamage(long nowNs) {
        double progress = Math.max(0.0, Math.min(1.0, (nowNs - createdAtNs) / (double) getTotalLifetimeNs()));
        double scale = 1.0 - progress * 0.78;
        return Math.max(1, (int) Math.round(baseTickDamage * scale));
    }

    public void render(GraphicsContext graphics, double cameraX, double cameraY, double zoom, long nowNs) {
        long elapsed = Math.max(0L, nowNs - createdAtNs);
        double centerX = (worldX - cameraX) * zoom;
        double centerY = (worldY - cameraY) * zoom;
        double maxWidth = Math.max(20.0, damageRadius * zoom * 2.1);

        if (elapsed <= IMPACT_DURATION_NS) {
            double impactProgress = elapsed / (double) IMPACT_DURATION_NS;
            double flashRadius = 4.0 + impactProgress * 4.0;
            graphics.save();
            graphics.setGlobalAlpha(Math.max(0.0, 0.9 - impactProgress * 0.9));
            graphics.setFill(javafx.scene.paint.Color.web("#ffcf74"));
            graphics.fillOval(centerX - flashRadius, centerY - flashRadius, flashRadius * 2.0, flashRadius * 2.0);
            graphics.restore();
        }

        if (elapsed <= burnDurationNs) {
            double burnProgress = Math.min(1.0, elapsed / (double) burnDurationNs);
            double spreadProgress = Math.min(1.0, elapsed / (double) SPREAD_TO_FULL_NS);
            double width = maxWidth * (0.28 + spreadProgress * 0.72);
            Image frame = resolveBurnFrame(elapsed, burnProgress);
            drawFrame(graphics, frame, centerX, centerY, width, 0.92);
            return;
        }

        long fadeElapsed = elapsed - burnDurationNs;
        double fadeProgress = Math.max(0.0, Math.min(1.0, fadeElapsed / (double) fadeDurationNs));
        Image frame = resolveFrame(emberFrames, fadeProgress);
        double width = maxWidth * (1.0 - fadeProgress * 0.22);
        drawFrame(graphics, frame, centerX, centerY, width, Math.max(0.0, 0.82 - fadeProgress * 0.82));
    }

    private long getTotalLifetimeNs() {
        return burnDurationNs + fadeDurationNs;
    }

    private Image resolveBurnFrame(long elapsedNs, double burnProgress) {
        if (burnFrames.length == 0) {
            return null;
        }
        if (elapsedNs < SPREAD_TO_FULL_NS) {
            double spreadProgress = Math.max(0.0, Math.min(0.999, elapsedNs / (double) SPREAD_TO_FULL_NS));
            return resolveFrame(burnFrames, spreadProgress);
        }
        int frameStart = Math.max(0, burnFrames.length - Math.min(3, burnFrames.length));
        long phase = Math.max(0L, elapsedNs - SPREAD_TO_FULL_NS);
        int frameOffset = (int) ((phase / 140_000_000L) % Math.max(1, burnFrames.length - frameStart));
        return burnFrames[frameStart + frameOffset];
    }

    private void drawFrame(GraphicsContext graphics,
                           Image frame,
                           double centerX,
                           double centerY,
                           double width,
                           double alpha) {
        if (graphics == null || frame == null || frame.isError() || width <= 0.0 || alpha <= 0.0) {
            return;
        }
        double aspect = frame.getHeight() / Math.max(1.0, frame.getWidth());
        double height = width * aspect;
        graphics.save();
        graphics.setGlobalAlpha(alpha);
        graphics.drawImage(frame, centerX - width * 0.5, centerY - height * 0.58, width, height);
        graphics.restore();
    }

    private Image resolveFrame(Image[] frames, double progress) {
        if (frames == null || frames.length == 0) {
            return null;
        }
        int index = (int) Math.floor(Math.max(0.0, Math.min(0.999, progress)) * frames.length);
        index = Math.max(0, Math.min(frames.length - 1, index));
        return frames[index];
    }
}
