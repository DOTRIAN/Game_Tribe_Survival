package entity;

/**
 * ThrownBomb:
 * - Projectile bomb bay tu nguoi choi den diem muc tieu.
 * - Fire bomb no ngay khi cham dich.
 * - Bomb trap cham dat truoc, sau do moi bat fuse roi no.
 */
public class ThrownBomb {
    private static final String BOMB_TRAP_ITEM_ID = "bomb_trap";
    private static final String SAMURAI_THROW_ITEM_ID = "samurai_throw";

    private enum State {
        FLYING,
        LANDED,
        EXPLODED
    }

    private final String bombItemId;
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
    private final boolean fuseStartsOnLanding;

    private double x;
    private double y;
    private double progress;
    private long landedAtNs;
    private State state;

    public ThrownBomb(String bombItemId,
                      double startX,
                      double startY,
                      double targetX,
                      double targetY,
                      double speedPerTick,
                      double blastRadius,
                      int damage,
                      long createdAtNs,
                      long maxLifetimeNs,
                      double renderSize) {
        this.bombItemId = bombItemId == null ? "" : bombItemId;
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
        this.fuseStartsOnLanding = BOMB_TRAP_ITEM_ID.equals(this.bombItemId);
        this.x = startX;
        this.y = startY;
        this.progress = 0.0;
        this.landedAtNs = -1L;
        this.state = State.FLYING;
    }

    public void update(long nowNs) {
        if (state == State.EXPLODED) {
            return;
        }
        if (state == State.LANDED) {
            if (nowNs - landedAtNs >= maxLifetimeNs) {
                state = State.EXPLODED;
            }
            return;
        }
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= speedPerTick) {
            x = targetX;
            y = targetY;
            progress = 1.0;
            if (fuseStartsOnLanding) {
                landedAtNs = nowNs;
                state = State.LANDED;
            } else {
                state = State.EXPLODED;
            }
            return;
        }
        x += (dx / distance) * speedPerTick;
        y += (dy / distance) * speedPerTick;

        double fullDist = Math.max(1.0, Math.sqrt((targetX - startX) * (targetX - startX) + (targetY - startY) * (targetY - startY)));
        double traveled = Math.sqrt((x - startX) * (x - startX) + (y - startY) * (y - startY));
        progress = Math.min(1.0, traveled / fullDist);

        if (!fuseStartsOnLanding && nowNs - createdAtNs >= maxLifetimeNs) {
            state = State.EXPLODED;
        }
    }

    public boolean shouldExplode() {
        return state == State.EXPLODED;
    }

    public double getX() {
        return x;
    }

    public String getBombItemId() {
        return bombItemId;
    }

    public double getY() {
        return y;
    }

    public double getArcHeight() {
        if (SAMURAI_THROW_ITEM_ID.equalsIgnoreCase(bombItemId)) {
            return 0.0;
        }
        if (state != State.FLYING) {
            return 0.0;
        }
        double t = Math.max(0.0, Math.min(1.0, progress));
        return 18.0 * 4.0 * t * (1.0 - t);
    }

    public boolean isFlying() {
        return state == State.FLYING;
    }

    public boolean isLanded() {
        return state == State.LANDED;
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

    public double getRotationDegrees() {
        return Math.toDegrees(Math.atan2(targetY - startY, targetX - startX));
    }

    public int resolveBombTrapFrameIndex(long nowNs, int totalFrames) {
        if (totalFrames <= 0) {
            return 0;
        }
        if (state == State.LANDED && landedAtNs > 0L) {
            long elapsed = Math.max(0L, nowNs - landedAtNs);
            double ratio = Math.min(1.0, elapsed / (double) maxLifetimeNs);
            int warningFrames = Math.max(1, totalFrames - 1);
            int frame = Math.min(warningFrames - 1, (int) Math.floor(ratio * warningFrames));
            long remaining = Math.max(0L, maxLifetimeNs - elapsed);
            if (remaining <= 300_000_000L) {
                return (elapsed / 110_000_000L) % 2 == 0 ? totalFrames - 1 : Math.max(0, warningFrames - 1);
            }
            return frame;
        }
        return 0;
    }
}
