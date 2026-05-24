package entity;

import system.DamageSystem;
import system.MovementSlideSystem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class FriendlyArcherManager {
    public interface WorldQuery {
        boolean canOccupy(FriendlyArcher archer, double x, double y, double width, double height);
        int getTileWidth();
        int getTileHeight();
        double getWorldWidth();
        double getWorldHeight();
    }

    private static final double ARRIVE_DISTANCE = 4.0;
    private static final boolean DEBUG_ARCHER = true;

    private final List<FriendlyArcher> archers;
    private final Random random;

    public FriendlyArcherManager(Random random) {
        this.archers = new ArrayList<>();
        this.random = random == null ? new Random() : random;
    }

    public List<FriendlyArcher> getArchers() {
        return archers;
    }

    public void clear() {
        archers.clear();
    }

    public FriendlyArcher spawnAtTile(int gridX, int gridY, int tileWidth, int tileHeight) {
        double width = tileWidth * 0.58;
        double height = tileHeight * 0.74;
        double x = gridX * tileWidth + (tileWidth - width) * 0.5;
        double y = gridY * tileHeight + tileHeight - height;
        FriendlyArcher archer = new FriendlyArcher(x, y, width, height, 0.8, 3 + random.nextInt(3));
        archer.setIdleDuration(randomIdleDuration());
        archers.add(archer);
        return archer;
    }

    public void update(long nowNs,
                       Player player,
                       List<Enemy> enemies,
                       List<ArrowProjectile> arrowProjectiles,
                       WorldQuery worldQuery) {
        if (worldQuery == null) {
            return;
        }

        List<FriendlyArcher> removed = new ArrayList<>();
        for (FriendlyArcher archer : archers) {
            if (archer == null) {
                continue;
            }
            double deltaSeconds = archer.beginUpdate(nowNs);
            if (archer.isRemovalRequested()) {
                removed.add(archer);
                continue;
            }

            updateSingle(archer, nowNs, deltaSeconds, enemies, arrowProjectiles, worldQuery);
            archer.updateAnimation(deltaSeconds);

            if (archer.isRemovalRequested()) {
                removed.add(archer);
            }
        }

        if (!removed.isEmpty()) {
            archers.removeAll(removed);
        }
        archers.sort(Comparator.comparingDouble(FriendlyArcher::getFootY));
    }

    private void updateSingle(FriendlyArcher archer,
                              long nowNs,
                              double deltaSeconds,
                              List<Enemy> enemies,
                              List<ArrowProjectile> arrowProjectiles,
                              WorldQuery worldQuery) {
        if (archer.getState() == AllyUnit.AllyState.DEAD) {
            if (archer.hasFinishedNonLoopingAnimation()) {
                archer.requestRemoval();
            }
            return;
        }

        if (archer.getState() == AllyUnit.AllyState.HURT) {
            if (!archer.hasFinishedNonLoopingAnimation()) {
                return;
            }
            archer.setState(AllyUnit.AllyState.IDLE);
        }

        Enemy target = findNearestEnemy(archer, enemies);
        archer.setCurrentTargetEnemy(target);

        if (target != null) {
            archer.clearWanderTarget();
            if (DEBUG_ARCHER) {
                System.out.println("[Archer] target found distance=" + (int) distance(archer.getCenterX(), archer.getCenterY(), target.getCenterX(), target.getCenterY()));
            }
            handleCombat(archer, target, nowNs, deltaSeconds, arrowProjectiles, worldQuery);
            return;
        }

        archer.setCurrentTargetEnemy(null);
        if (DEBUG_ARCHER) {
            System.out.println("[Archer] wander");
        }
        if (archer.distanceToHome() > (archer.getWanderRadiusTiles() + 2.0) * Math.max(worldQuery.getTileWidth(), worldQuery.getTileHeight())) {
            archer.clearWanderTarget();
            returnHome(archer, deltaSeconds, worldQuery);
            return;
        }

        if (archer.getState() == AllyUnit.AllyState.RETURN_HOME && archer.distanceToHome() > ARRIVE_DISTANCE) {
            returnHome(archer, deltaSeconds, worldQuery);
            return;
        }

        updateWander(archer, deltaSeconds, worldQuery);
    }

    private void handleCombat(FriendlyArcher archer,
                              Enemy target,
                              long nowNs,
                              double deltaSeconds,
                              List<ArrowProjectile> arrowProjectiles,
                              WorldQuery worldQuery) {
        double distance = distance(archer.getCenterX(), archer.getCenterY(), target.getCenterX(), target.getCenterY());
        archer.faceTargetX(target.getCenterX());

        if (distance <= archer.getMeleeRange()) {
            if (archer.getState() != AllyUnit.AllyState.ATTACK) {
                archer.setState(AllyUnit.AllyState.ATTACK);
            } else if (archer.hasFinishedNonLoopingAnimation()) {
                archer.restartAnimationCycle();
            }
            if (!archer.isMeleeAppliedThisCycle() && archer.getFrameIndex() >= 2 && archer.canMelee(nowNs)) {
                DamageSystem.applyDamage(archer, target, archer.getMeleeDamage(), nowNs);
                archer.markMelee(nowNs);
                archer.setMeleeAppliedThisCycle(true);
                if (DEBUG_ARCHER) {
                    System.out.println("[Archer] melee attack");
                }
            }
            return;
        }

        if (distance <= archer.getShootRange()) {
            if (archer.getState() != AllyUnit.AllyState.SHOT) {
                archer.setState(AllyUnit.AllyState.SHOT);
            } else if (archer.hasFinishedNonLoopingAnimation()) {
                archer.restartAnimationCycle();
            }
            if (!archer.isShotReleasedThisCycle() && archer.getFrameIndex() >= 8 && archer.canShoot(nowNs)) {
                spawnArrow(archer, target, nowNs, arrowProjectiles);
                archer.markShot(nowNs);
                archer.setShotReleasedThisCycle(true);
                if (DEBUG_ARCHER) {
                    System.out.println("[Archer] shoot arrow");
                }
            }
            if (distance < archer.getMeleeRange() * 1.6) {
                moveAwayFromEnemy(archer, target, deltaSeconds, worldQuery);
            }
            return;
        }

        archer.setState(AllyUnit.AllyState.WALK);
        moveToward(archer, target.getX(), target.getY(), deltaSeconds, worldQuery);
    }

    private void updateWander(FriendlyArcher archer, double deltaSeconds, WorldQuery worldQuery) {
        if (!archer.hasWanderTarget()) {
            archer.setState(AllyUnit.AllyState.IDLE);
            archer.addIdleTime(deltaSeconds);
            if (archer.getIdleTimer() < archer.getIdleDuration()) {
                return;
            }
            if (!pickRandomWanderTarget(archer, worldQuery)) {
                archer.resetIdleTimer();
                archer.setIdleDuration(randomIdleDuration());
                return;
            }
            archer.setState(AllyUnit.AllyState.WALK);
            return;
        }

        archer.setState(AllyUnit.AllyState.WALK);
        archer.faceTargetX(archer.getWanderTargetX() + archer.getWidth() * 0.5);
        moveToward(archer, archer.getWanderTargetX(), archer.getWanderTargetY(), deltaSeconds, worldQuery);
        if (archer.distanceTo(archer.getWanderTargetX(), archer.getWanderTargetY()) < ARRIVE_DISTANCE) {
            archer.clearWanderTarget();
            archer.resetIdleTimer();
            archer.setIdleDuration(randomIdleDuration());
            archer.setState(AllyUnit.AllyState.IDLE);
        }
    }

    private void returnHome(FriendlyArcher archer, double deltaSeconds, WorldQuery worldQuery) {
        archer.setState(AllyUnit.AllyState.RETURN_HOME);
        archer.faceTargetX(archer.getHomeX() + archer.getWidth() * 0.5);
        moveToward(archer, archer.getHomeX(), archer.getHomeY(), deltaSeconds, worldQuery);
        if (archer.distanceToHome() < ARRIVE_DISTANCE) {
            archer.setPosition(archer.getHomeX(), archer.getHomeY());
            archer.resetIdleTimer();
            archer.setIdleDuration(randomIdleDuration());
            archer.setState(AllyUnit.AllyState.IDLE);
        }
    }

    private boolean pickRandomWanderTarget(FriendlyArcher archer, WorldQuery worldQuery) {
        int tileWidth = Math.max(1, worldQuery.getTileWidth());
        int tileHeight = Math.max(1, worldQuery.getTileHeight());
        for (int attempt = 0; attempt < 24; attempt++) {
            int offsetTileX = random.nextInt(archer.getWanderRadiusTiles() * 2 + 1) - archer.getWanderRadiusTiles();
            int offsetTileY = random.nextInt(archer.getWanderRadiusTiles() * 2 + 1) - archer.getWanderRadiusTiles();
            double candidateX = archer.getHomeX() + offsetTileX * tileWidth;
            double candidateY = archer.getHomeY() + offsetTileY * tileHeight;
            if (!worldQuery.canOccupy(archer, candidateX, candidateY, archer.getWidth(), archer.getHeight())) {
                continue;
            }
            archer.setWanderTarget(candidateX, candidateY);
            archer.resetIdleTimer();
            return true;
        }
        return false;
    }

    private void moveToward(FriendlyArcher archer,
                            double targetX,
                            double targetY,
                            double deltaSeconds,
                            WorldQuery worldQuery) {
        double dx = targetX - archer.getX();
        double dy = targetY - archer.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= 0.001) {
            return;
        }
        double step = archer.getSpeed() * Math.max(0.0, deltaSeconds) * 60.0;
        double moveX = archer.getX() + (dx / distance) * Math.min(step, distance);
        double moveY = archer.getY() + (dy / distance) * Math.min(step, distance);
        moveArcher(archer, moveX, moveY, worldQuery);
    }

    private void moveAwayFromEnemy(FriendlyArcher archer,
                                   Enemy enemy,
                                   double deltaSeconds,
                                   WorldQuery worldQuery) {
        double dx = archer.getX() - enemy.getX();
        double dy = archer.getY() - enemy.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= 0.001) {
            return;
        }
        double step = archer.getSpeed() * 0.70 * Math.max(0.0, deltaSeconds) * 60.0;
        double moveX = archer.getX() + (dx / distance) * step;
        double moveY = archer.getY() + (dy / distance) * step;
        moveArcher(archer, moveX, moveY, worldQuery);
    }

    private void moveArcher(FriendlyArcher archer, double targetX, double targetY, WorldQuery worldQuery) {
        double maxX = Math.max(0.0, worldQuery.getWorldWidth() - archer.getWidth());
        double maxY = Math.max(0.0, worldQuery.getWorldHeight() - archer.getHeight());
        double newX = clamp(targetX, 0.0, maxX);
        double newY = clamp(targetY, 0.0, maxY);
        MovementSlideSystem.MoveResult result = MovementSlideSystem.move(
                archer.getX(),
                archer.getY(),
                archer.getWidth(),
                archer.getHeight(),
                newX - archer.getX(),
                newY - archer.getY(),
                (x, y, width, height) -> worldQuery.canOccupy(archer, x, y, width, height)
        );
        archer.setPosition(result.x(), result.y());
    }

    private Enemy findNearestEnemy(FriendlyArcher archer, List<Enemy> enemies) {
        if (enemies == null || enemies.isEmpty()) {
            return null;
        }
        Enemy nearest = null;
        double nearestDistance = archer.getVisionRange();
        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive() || !enemy.isHostile()) {
                continue;
            }
            double distance = distance(archer.getCenterX(), archer.getCenterY(), enemy.getCenterX(), enemy.getCenterY());
            if (distance > archer.getVisionRange() || distance >= nearestDistance) {
                continue;
            }
            nearestDistance = distance;
            nearest = enemy;
        }
        return nearest;
    }

    private void spawnArrow(FriendlyArcher archer, Enemy target, long nowNs, List<ArrowProjectile> arrowProjectiles) {
        if (arrowProjectiles == null || target == null) {
            return;
        }
        double dx = target.getCenterX() - archer.getCenterX();
        double dy = target.getCenterY() - archer.getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= 0.001) {
            return;
        }
        double speed = archer.getArrowSpeed();
        arrowProjectiles.add(new ArrowProjectile(
                archer.getCenterX(),
                archer.getCenterY() - archer.getHeight() * 0.15,
                (dx / distance) * speed,
                (dy / distance) * speed,
                archer.getRangedDamage(),
                nowNs,
                core.GameBalance.FRIENDLY_ARCHER_ARROW_LIFETIME_NS,
                "friendly_archer_arrow"
        ));
    }

    private double randomIdleDuration() {
        return 1.0 + random.nextDouble();
    }

    private double distance(double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
