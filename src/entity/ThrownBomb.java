package entity;

/**
 * ThrownBomb:
 * - Projectile bomb bay tu nguoi choi den diem muc tieu.
 * - Khi het fuse hoac cham dich se no.
 */
public class ThrownBomb {
    private final double startX;
    private final double startY;
    private final double targetX;
    private final double targetY;
    private final double speedPerTick;
    private final double blastRadius;
    private final int damage;
    private final long createdAtNs;
    private final long maxLifetimeNs;
    private final double renderSize;

    private double x;
    private double y;
    private double progress;
    private boolean exploded;

    public ThrownBomb(double startX,
                      double startY,
                      double targetX,
                      double targetY,
                      double speedPerTick,
                      double blastRadius,
                      int damage,
                      long createdAtNs,
                      long maxLifetimeNs,
                      double renderSize) {
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.speedPerTick = Math.max(0.1, speedPerTick);
        this.blastRadius = Math.max(8.0, blastRadius);
        this.damage = Math.max(1, damage);
        this.createdAtNs = createdAtNs;
        this.maxLifetimeNs = Math.max(120_000_000L, maxLifetimeNs);
        this.renderSize = Math.max(8.0, renderSize);
        this.x = startX;
        this.y = startY;
        this.progress = 0.0;
        this.exploded = false;
    }

    public void update(long nowNs) {
        if (exploded) {
            return;
        }
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= speedPerTick) {
            x = targetX;
            y = targetY;
            progress = 1.0;
            exploded = true;
            return;
        }
        x += (dx / distance) * speedPerTick;
        y += (dy / distance) * speedPerTick;

        double fullDist = Math.max(1.0, Math.sqrt((targetX - startX) * (targetX - startX) + (targetY - startY) * (targetY - startY)));
        double traveled = Math.sqrt((x - startX) * (x - startX) + (y - startY) * (y - startY));
        progress = Math.min(1.0, traveled / fullDist);

        if (nowNs - createdAtNs >= maxLifetimeNs) {
            exploded = true;
        }
    }

    public boolean shouldExplode() {
        return exploded;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getArcHeight() {
        double t = Math.max(0.0, Math.min(1.0, progress));
        return 18.0 * 4.0 * t * (1.0 - t);
    }

    public double getBlastRadius() {
        return blastRadius;
    }

    public int getDamage() {
        return damage;
    }

    public double getRenderSize() {
        return renderSize;
    }
}
