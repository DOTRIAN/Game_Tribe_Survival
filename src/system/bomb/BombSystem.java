package system.bomb;

import buildsystem.object.BombTrap;
import buildsystem.object.BombTrapState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BombSystem {
    public BombUpdateResult update(long nowNs, Collection<BombTrap> bombs) {
        List<BombExplosionEvent> explosions = new ArrayList<>();
        if (bombs == null || bombs.isEmpty()) {
            return new BombUpdateResult(explosions);
        }
        for (BombTrap bomb : bombs) {
            if (bomb == null || !bomb.isAlive()) {
                continue;
            }
            if (bomb.getState() == BombTrapState.IDLE) {
                bomb.startExploding(nowNs);
            }
            if (bomb.isFuseFinished(nowNs)) {
                bomb.markDestroyed(nowNs);
                explosions.add(new BombExplosionEvent(
                        bomb,
                        bomb.getCenterX(),
                        bomb.getCenterY(),
                        bomb.getExplosionRadiusPx(),
                        bomb.getEnemyDamage()
                ));
            }
        }
        return new BombUpdateResult(explosions);
    }

    public static final class BombUpdateResult {
        private final List<BombExplosionEvent> explosionEvents;

        public BombUpdateResult(List<BombExplosionEvent> explosionEvents) {
            this.explosionEvents = explosionEvents == null ? List.of() : explosionEvents;
        }

        public List<BombExplosionEvent> getExplosionEvents() {
            return explosionEvents;
        }
    }

    public static final class BombExplosionEvent {
        private final BombTrap bomb;
        private final double worldX;
        private final double worldY;
        private final double radius;
        private final int enemyDamage;

        public BombExplosionEvent(BombTrap bomb, double worldX, double worldY, double radius, int enemyDamage) {
            this.bomb = bomb;
            this.worldX = worldX;
            this.worldY = worldY;
            this.radius = radius;
            this.enemyDamage = enemyDamage;
        }

        public BombTrap getBomb() {
            return bomb;
        }

        public double getWorldX() {
            return worldX;
        }

        public double getWorldY() {
            return worldY;
        }

        public double getRadius() {
            return radius;
        }

        public int getEnemyDamage() {
            return enemyDamage;
        }
    }
}

