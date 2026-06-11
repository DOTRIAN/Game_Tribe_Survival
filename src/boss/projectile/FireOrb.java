package boss.projectile;

import entity.Entity;
import javafx.scene.paint.Color;

public final class FireOrb extends Entity {
    public static final double RENDER_WIDTH = 64.0;
    public static final double RENDER_HEIGHT = 32.0;
    public static final double HIT_RADIUS = 16.0;

    private boolean active;
    private double velocityXPerSecond;
    private double velocityYPerSecond;
    private int damage;
    private long expireAtNs;
    private long lastUpdateAtNs;
    private long lastFrameAtNs;
    private int frameIndex;
    private double rotationDegrees;

    public FireOrb() {
        super(-1000.0, -1000.0, RENDER_WIDTH, RENDER_HEIGHT, 0, 1);
        this.active = false;
        this.velocityXPerSecond = 0.0;
        this.velocityYPerSecond = 0.0;
        this.damage = 0;
        this.expireAtNs = 0L;
        this.lastUpdateAtNs = 0L;
        this.lastFrameAtNs = 0L;
        this.frameIndex = 0;
        this.rotationDegrees = 0.0;
        hp = 0;
    }

    public void activate(double centerX,
                         double centerY,
                         double velocityXPerSecond,
                         double velocityYPerSecond,
                         int damage,
                         long nowNs,
                         long lifetimeNs) {
        this.x = centerX - width * 0.5;
        this.y = centerY - height * 0.5;
        this.velocityXPerSecond = velocityXPerSecond;
        this.velocityYPerSecond = velocityYPerSecond;
        this.damage = Math.max(1, damage);
        this.expireAtNs = nowNs + Math.max(1L, lifetimeNs);
        this.lastUpdateAtNs = nowNs;
        this.lastFrameAtNs = nowNs;
        this.frameIndex = 0;
        this.rotationDegrees = Math.toDegrees(Math.atan2(velocityYPerSecond, velocityXPerSecond));
        this.active = true;
        this.hp = 1;
    }

    public void update(long nowNs, int frameCount, long frameDurationNs) {
        if (!active) {
            return;
        }
        long elapsedNs = Math.max(0L, nowNs - lastUpdateAtNs);
        double deltaSeconds = elapsedNs / 1_000_000_000.0;
        x += velocityXPerSecond * deltaSeconds;
        y += velocityYPerSecond * deltaSeconds;
        lastUpdateAtNs = nowNs;

        if (frameCount > 0 && frameDurationNs > 0L) {
            long animationElapsed = Math.max(0L, nowNs - lastFrameAtNs);
            if (animationElapsed >= frameDurationNs) {
                long advance = animationElapsed / frameDurationNs;
                frameIndex = (int) ((frameIndex + advance) % frameCount);
                lastFrameAtNs += advance * frameDurationNs;
            }
        }
    }

    public void deactivate() {
        active = false;
        hp = 0;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isExpired(long nowNs) {
        return !active || nowNs >= expireAtNs;
    }

    public int getDamage() {
        return damage;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public double getRotationDegrees() {
        return rotationDegrees;
    }

    public double getHitRadius() {
        return HIT_RADIUS;
    }

    public double getCenterX() {
        return x + width * 0.5;
    }

    public double getCenterY() {
        return y + height * 0.5;
    }

    @Override
    public void triggerHitFlash(long nowNs, long durationNs, Color color) {
        // Fire orb render khong dung hit flash.
    }
}
