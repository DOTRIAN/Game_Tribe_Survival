package boss;

import boss.entity.FinalBoss;
import boss.projectile.FireOrbManager;
import entity.Enemy;
import entity.Player;

import java.util.List;

public final class BossFightController {
    private final BossArena arena;
    private final FireOrbManager fireOrbManager;
    private FinalBoss boss;
    private BossFightResult result;

    public BossFightController(BossArena arena, FireOrbManager fireOrbManager) {
        this.arena = arena;
        this.fireOrbManager = fireOrbManager;
        this.result = BossFightResult.ACTIVE;
    }

    public void ensureSpawned(List<Enemy> enemies) {
        if (boss == null) {
            boss = new FinalBoss(arena.getSpawnX(), arena.getSpawnY());
            if (fireOrbManager != null) {
                boss.setBarrageEmitter((originX, originY, dirX, dirY, orbCount, nowNs) ->
                        fireOrbManager.emitBarrage(originX, originY, dirX, dirY, orbCount, nowNs));
            }
        }
        if (enemies != null && !enemies.contains(boss)) {
            enemies.add(boss);
        }
    }

    public void update(long now, Player player, List<Enemy> enemies, double worldWidth, double worldHeight) {
        ensureSpawned(enemies);
        if (boss == null) {
            return;
        }
        if (player == null || player.isDead()) {
            result = BossFightResult.PLAYER_DEFEATED;
            return;
        }
        boss.update(now, player, worldWidth, worldHeight);
        if (boss.isEncounterFinished()) {
            result = BossFightResult.VICTORY;
        }
    }

    public FinalBoss getBoss() {
        return boss;
    }

    public BossFightResult getResult() {
        return result;
    }
}
