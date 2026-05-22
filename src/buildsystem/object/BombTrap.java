package buildsystem.object;

import buildsystem.component.HealthComponent;
import buildsystem.component.RotationComponent;
import buildsystem.core.BuildDefinition;
import buildsystem.core.BuildObjectSeed;
import core.GameBalance;

public class BombTrap extends BuildObject {
    private final BombTrapType bombType;
    private BombTrapState state;
    private long stateStartedAtNs;
    private final long fuseDurationNs;
    private final double triggerRangePx;
    private final double explosionRadiusPx;
    private final int enemyDamage;

    public BombTrap(BuildDefinition definition, BuildObjectSeed seed) {
        super(seed.getObjectId(),
                definition.getType(),
                seed.getTileX(),
                seed.getTileY(),
                seed.getTileWidth(),
                seed.getTileHeight(),
                definition.getAutoTileGroup(),
                GameBalance.BOMB_TRAP_WORLD_WIDTH,
                GameBalance.BOMB_TRAP_WORLD_HEIGHT,
                (seed.getTileWidth() - GameBalance.BOMB_TRAP_WORLD_WIDTH) / 2.0,
                seed.getTileHeight() - GameBalance.BOMB_TRAP_WORLD_HEIGHT,
                seed.getSpriteKey(),
                seed.getRotationDegrees(),
                seed.getHealth());
        addComponent(new HealthComponent(seed.getHealth(), definition.getHealth()));
        addComponent(new RotationComponent(seed.getRotationDegrees()));
        this.bombType = BombTrapType.NORMAL;
        this.state = BombTrapState.IDLE;
        this.stateStartedAtNs = 0L;
        this.fuseDurationNs = GameBalance.BOMB_TRAP_FUSE_NS;
        this.triggerRangePx = GameBalance.BOMB_TRAP_TRIGGER_RANGE_TILES * seed.getTileWidth();
        this.explosionRadiusPx = GameBalance.BOMB_TRAP_EXPLOSION_RADIUS_TILES * seed.getTileWidth();
        this.enemyDamage = GameBalance.BOMB_TRAP_DAMAGE;
    }

    public BombTrapType getBombType() {
        return bombType;
    }

    public BombTrapState getState() {
        return state;
    }

    public long getStateStartedAtNs() {
        return stateStartedAtNs;
    }

    public long getFuseDurationNs() {
        return fuseDurationNs;
    }

    public double getTriggerRangePx() {
        return triggerRangePx;
    }

    public double getExplosionRadiusPx() {
        return explosionRadiusPx;
    }

    public int getEnemyDamage() {
        return enemyDamage;
    }

    public void arm(long nowNs) {
        if (state == BombTrapState.DESTROYED || state == BombTrapState.EXPLODING) {
            return;
        }
        state = BombTrapState.ARMED;
        stateStartedAtNs = nowNs;
    }

    public void startExploding(long nowNs) {
        if (state == BombTrapState.DESTROYED || state == BombTrapState.EXPLODING) {
            return;
        }
        state = BombTrapState.EXPLODING;
        stateStartedAtNs = nowNs;
    }

    public void markDestroyed(long nowNs) {
        state = BombTrapState.DESTROYED;
        stateStartedAtNs = nowNs;
    }

    public boolean isFuseFinished(long nowNs) {
        return state == BombTrapState.EXPLODING && nowNs - stateStartedAtNs >= fuseDurationNs;
    }

    public int resolveAnimationFrameIndex(long nowNs, int totalFrames) {
        if (totalFrames <= 0) {
            return 0;
        }
        if (state == BombTrapState.EXPLODING) {
            long elapsed = Math.max(0L, nowNs - stateStartedAtNs);
            double ratio = Math.min(1.0, elapsed / (double) fuseDurationNs);
            int warningFrames = Math.max(1, totalFrames - 1);
            int frame = Math.min(warningFrames - 1, (int) Math.floor(ratio * warningFrames));
            long remaining = Math.max(0L, fuseDurationNs - elapsed);
            if (remaining <= 300_000_000L) {
                return (elapsed / 110_000_000L) % 2 == 0 ? totalFrames - 1 : Math.max(0, warningFrames - 1);
            }
            return frame;
        }
        if (state == BombTrapState.ARMED) {
            return 0;
        }
        if (state == BombTrapState.DESTROYED) {
            return totalFrames - 1;
        }
        return 0;
    }
}
