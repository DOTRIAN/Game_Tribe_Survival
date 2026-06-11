package boss.projectile;

import boss.BossSpriteLoader;
import entity.Player;
import system.DamageSystem;

import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FireOrbManager {
    public interface WorldQuery {
        boolean blocksFireOrb(double centerX, double centerY, double radius);
    }

    private static final String FIRE_ORB_ASSET_FOLDER = "assets/FB00";
    private static final double FIRE_ORB_SPEED = 300.0;
    private static final int FIRE_ORB_DAMAGE = 10;
    private static final long FIRE_ORB_LIFETIME_NS = 4_000_000_000L;
    private static final long FIRE_ORB_FRAME_NS = 75_000_000L;
    private static final int INITIAL_POOL_SIZE = 20;
    private static final int POOL_GROWTH_SIZE = 8;

    private final List<FireOrb> activeOrbs;
    private final List<FireOrb> pooledOrbs;
    private final Image[] frames;

    public FireOrbManager() {
        this.activeOrbs = new ArrayList<>();
        this.pooledOrbs = new ArrayList<>();
        this.frames = BossSpriteLoader.loadSequence(FIRE_ORB_ASSET_FOLDER);
        expandPool(INITIAL_POOL_SIZE);
    }

    public void emitBarrage(double originX,
                            double originY,
                            double forwardDirX,
                            double forwardDirY,
                            int orbCount,
                            long nowNs) {
        int safeCount = Math.max(1, orbCount);
        double baseAngle = Math.atan2(forwardDirY, forwardDirX);
        double spreadDegrees = safeCount > 9 ? 70.0 : 60.0;
        if (safeCount > pooledOrbs.size()) {
            expandPool(safeCount - pooledOrbs.size());
        }

        for (int index = 0; index < safeCount; index++) {
            double offsetDegrees = safeCount == 1
                    ? 0.0
                    : -spreadDegrees + ((spreadDegrees * 2.0) * index / (safeCount - 1.0));
            double angle = baseAngle + Math.toRadians(offsetDegrees);
            double velocityX = Math.cos(angle) * FIRE_ORB_SPEED;
            double velocityY = Math.sin(angle) * FIRE_ORB_SPEED;
            FireOrb orb = obtainOrb();
            orb.activate(originX, originY, velocityX, velocityY, FIRE_ORB_DAMAGE, nowNs, FIRE_ORB_LIFETIME_NS);
            activeOrbs.add(orb);
        }
    }

    public void update(long nowNs, Player player, WorldQuery worldQuery, double worldWidth, double worldHeight) {
        for (int index = activeOrbs.size() - 1; index >= 0; index--) {
            FireOrb orb = activeOrbs.get(index);
            if (orb == null || !orb.isActive()) {
                deactivate(index);
                continue;
            }

            orb.update(nowNs, frames.length, FIRE_ORB_FRAME_NS);

            if (orb.isExpired(nowNs)
                    || orb.getCenterX() < -FireOrb.HIT_RADIUS
                    || orb.getCenterY() < -FireOrb.HIT_RADIUS
                    || orb.getCenterX() > worldWidth + FireOrb.HIT_RADIUS
                    || orb.getCenterY() > worldHeight + FireOrb.HIT_RADIUS) {
                deactivate(index);
                continue;
            }

            if (worldQuery != null && worldQuery.blocksFireOrb(orb.getCenterX(), orb.getCenterY(), orb.getHitRadius())) {
                deactivate(index);
                continue;
            }

            if (player != null
                    && player.isAlive()
                    && intersectsCircleAndRect(
                    orb.getCenterX(),
                    orb.getCenterY(),
                    orb.getHitRadius(),
                    player.getCollisionX(),
                    player.getCollisionY(),
                    player.getCollisionWidth(),
                    player.getCollisionHeight())) {
                DamageSystem.applyDamage(null, player, orb.getDamage(), nowNs);
                deactivate(index);
            }
        }
    }

    public List<FireOrb> getActiveOrbs() {
        return Collections.unmodifiableList(activeOrbs);
    }

    public Image[] getFrames() {
        return frames;
    }

    public void clear() {
        for (int index = activeOrbs.size() - 1; index >= 0; index--) {
            deactivate(index);
        }
    }

    private FireOrb obtainOrb() {
        if (pooledOrbs.isEmpty()) {
            expandPool(POOL_GROWTH_SIZE);
        }
        return pooledOrbs.remove(pooledOrbs.size() - 1);
    }

    private void deactivate(int index) {
        FireOrb orb = activeOrbs.remove(index);
        if (orb == null) {
            return;
        }
        orb.deactivate();
        pooledOrbs.add(orb);
    }

    private void expandPool(int amount) {
        for (int index = 0; index < Math.max(1, amount); index++) {
            pooledOrbs.add(new FireOrb());
        }
    }

    private boolean intersectsCircleAndRect(double circleX,
                                            double circleY,
                                            double radius,
                                            double rectX,
                                            double rectY,
                                            double rectWidth,
                                            double rectHeight) {
        double nearestX = Math.max(rectX, Math.min(circleX, rectX + rectWidth));
        double nearestY = Math.max(rectY, Math.min(circleY, rectY + rectHeight));
        double dx = circleX - nearestX;
        double dy = circleY - nearestY;
        return dx * dx + dy * dy <= radius * radius;
    }
}
