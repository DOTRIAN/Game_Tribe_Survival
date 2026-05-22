package entity;

import javafx.scene.paint.Color;

/** Projectile fired by archer towers. */
public class ArrowProjectile extends Entity {
    private final double velocityX;
    private final double velocityY;
    private final int damage;
    private final long expireAtNs;
    private final String spriteKey;
    private final double rotationDegrees;

    public ArrowProjectile(double x,
                           double y,
                           double velocityX,
                           double velocityY,
                           int damage,
                           long spawnAtNs,
                           long lifeTimeNs,
                           String spriteKey) {
        super(x - 10.0, y - 10.0, 20.0, 20.0, 0, 1);
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.damage = Math.max(1, damage);
        this.expireAtNs = spawnAtNs + Math.max(1L, lifeTimeNs);
        this.spriteKey = spriteKey == null ? "" : spriteKey;
        this.rotationDegrees = Math.toDegrees(Math.atan2(velocityY, velocityX));
    }

    public void update() {
        x += velocityX;
        y += velocityY;
    }

    public int getDamage() {
        return damage;
    }

    public boolean isExpired(long nowNs) {
        return nowNs > expireAtNs;
    }

    public String getSpriteKey() {
        return spriteKey;
    }

    public double getRotationDegrees() {
        return rotationDegrees;
    }

    @Override
    public void triggerHitFlash(long nowNs, long durationNs, Color color) {
        // Projectile does not use hit flash.
    }
}
