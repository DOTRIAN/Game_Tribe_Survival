package boss;

import boss.projectile.FireOrb;
import boss.projectile.FireOrbManager;
import entity.Enemy;
import entity.Player;
import map.MapType;

import java.util.List;

public final class BossManager {
    public static final String BOSS_MAP_IMAGE_PATH = "boss2.jpg";

    private final FireOrbManager fireOrbManager;
    private BossFightController activeFight;
    private String pendingToast;

    public BossManager() {
        this.fireOrbManager = new FireOrbManager();
    }

    public interface FireOrbWorldQuery extends FireOrbManager.WorldQuery {
    }

    public void onMapChanged(MapType mapType, double worldWidth, double worldHeight, List<Enemy> enemies) {
        if (mapType != MapType.BOSS_MAP) {
            activeFight = null;
            pendingToast = null;
            fireOrbManager.clear();
            return;
        }

        BossArena arena = BossArena.fromWorldBounds(BOSS_MAP_IMAGE_PATH, worldWidth, worldHeight);
        fireOrbManager.clear();
        activeFight = new BossFightController(arena, fireOrbManager);
        activeFight.ensureSpawned(enemies);
        pendingToast = "Boss room: Tieu diet Boss.";
    }

    public void update(long now,
                       Player player,
                       List<Enemy> enemies,
                       double worldWidth,
                       double worldHeight,
                       FireOrbWorldQuery worldQuery) {
        fireOrbManager.update(now, player, worldQuery, worldWidth, worldHeight);
        if (activeFight == null) {
            return;
        }
        activeFight.update(now, player, enemies, worldWidth, worldHeight);
    }

    public List<FireOrb> getActiveFireOrbs() {
        return fireOrbManager.getActiveOrbs();
    }

    public FireOrbManager getFireOrbManager() {
        return fireOrbManager;
    }

    public String buildObjectiveStatus(Player player) {
        if (activeFight == null) {
            return "";
        }
        if (activeFight.getResult() == BossFightResult.VICTORY) {
            return "Nhiem vu hoan thanh";
        }
        return "Tieu diet Boss";
    }

    public boolean isVictory() {
        return activeFight != null && activeFight.getResult() == BossFightResult.VICTORY;
    }

    public String consumePendingToast() {
        String toast = pendingToast;
        pendingToast = null;
        return toast;
    }
}
