package boss;

import entity.Enemy;
import entity.Player;
import map.MapType;

import java.util.List;

public final class BossManager {
    public static final String BOSS_MAP_IMAGE_PATH = "boss2.jpg";

    private BossFightController activeFight;
    private String pendingToast;

    public void onMapChanged(MapType mapType, double worldWidth, double worldHeight, List<Enemy> enemies) {
        if (mapType != MapType.BOSS_MAP) {
            activeFight = null;
            pendingToast = null;
            return;
        }

        BossArena arena = BossArena.fromWorldBounds(BOSS_MAP_IMAGE_PATH, worldWidth, worldHeight);
        activeFight = new BossFightController(arena);
        activeFight.ensureSpawned(enemies);
        pendingToast = "Boss room: Chuot trai de danh, F/J/K de dung ky nang da mo khoa, tranh don cleave.";
    }

    public void update(long now, Player player, List<Enemy> enemies, double worldWidth, double worldHeight) {
        if (activeFight == null) {
            return;
        }
        activeFight.update(now, player, enemies, worldWidth, worldHeight);
    }

    public String buildObjectiveStatus(Player player) {
        if (activeFight == null) {
            return "";
        }
        if (activeFight.getResult() == BossFightResult.VICTORY) {
            return "Final Boss da guc!\n"
                    + "• Hoan tat man boss hien tai\n"
                    + "• Co the noi them cutscene / phan thuong sau";
        }
        if (activeFight.getResult() == BossFightResult.PLAYER_DEFEATED) {
            return "Final Boss:\n"
                    + "• Nhan vat da guc nga\n"
                    + "• Thu lai va canh don cleave";
        }

        var boss = activeFight.getBoss();
        if (boss == null) {
            return "Final Boss:\n• Dang trieu hoi...";
        }

        String skillHint = player != null && player.getLevel() >= 4
                ? "F/J/K: dung combo ky nang"
                : player != null && player.getLevel() >= 3
                ? "F/J: ky nang da san sang, K mo o level 4"
                : player != null && player.getLevel() >= 2
                ? "F: ky nang da san sang, J/K mo o level cao hon"
                : "Chuot trai danh thuong, F/J/K mo khoa theo level";

        return "Final Boss:\n"
                + "• HP " + boss.getHp() + "/" + boss.getMaxHp() + "\n"
                + "• " + skillHint + "\n"
                + "• Lui ra khi boss chuan bi vung cleave";
    }

    public String consumePendingToast() {
        String toast = pendingToast;
        pendingToast = null;
        return toast;
    }
}
