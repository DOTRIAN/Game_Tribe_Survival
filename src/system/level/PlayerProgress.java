package system.level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PlayerProgress:
 * - Du lieu save runtime cua nguoi choi: level da mo khoa + ket qua tung level.
 * - Duoc LevelManager load/save qua data/player_progress.json.
 *
 * Lien he voi hit/combat:
 * - Khong xu ly combat truc tiep.
 * - Nhan ket qua gian tiep sau combat (qua LevelResult) de cap nhat tien do.
 */
public class PlayerProgress {
    // unlockedLevels:
    // - tap cac level da mo khoa
    // - dung Set de tranh duplicate khi complete level nhieu lan
    private final Set<Integer> unlockedLevels;
    // levelResults:
    // - map levelId -> ket qua tot nhat/gan nhat cua level do
    // - duoc serialise xuong player_progress.json
    private final Map<Integer, LevelResult> levelResults;

    public PlayerProgress() {
        this.unlockedLevels = new LinkedHashSet<>();
        this.levelResults = new LinkedHashMap<>();
    }

    public static PlayerProgress fromMap(Map<String, Object> raw) {
        PlayerProgress progress = new PlayerProgress();

        // 1) Khoi phuc danh sach level da unlock
        Object unlocked = raw.get("unlockedLevels");
        if (unlocked instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Number number) {
                    progress.unlockLevel(number.intValue());
                }
            }
        }

        // 2) Khoi phuc ket qua tung level neu co
        Object results = raw.get("levelResults");
        if (results instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                int levelId;
                try {
                    levelId = Integer.parseInt(String.valueOf(entry.getKey()));
                } catch (Exception ignored) {
                    continue;
                }
                if (!(entry.getValue() instanceof Map<?, ?> valueMap)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) valueMap;
                progress.saveLevelResult(levelId, LevelResult.fromMap(levelId, typed));
            }
        }

        if (progress.unlockedLevels.isEmpty()) {
            // Save loi/khong day du -> van mo khoa level 1 de khong chan nguoi choi o menu.
            progress.unlockLevel(1);
        }

        return progress;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("unlockedLevels", new ArrayList<>(unlockedLevels));

        // completedLevels/currentLevel duoc ghi them de file save de doc bang mat thuong.
        // Runtime hien tai chu yeu doc tu levelResults + unlockedLevels.
        List<Integer> completedLevels = new ArrayList<>(levelResults.keySet());
        raw.put("completedLevels", completedLevels);
        raw.put("currentLevel", completedLevels.isEmpty() ? 1 : completedLevels.get(completedLevels.size() - 1));

        Map<String, Object> resultMaps = new LinkedHashMap<>();
        for (Map.Entry<Integer, LevelResult> entry : levelResults.entrySet()) {
            resultMaps.put(String.valueOf(entry.getKey()), entry.getValue().toMap());
        }
        raw.put("levelResults", resultMaps);
        return raw;
    }

    public void unlockLevel(int levelId) {
        if (levelId > 0) {
            unlockedLevels.add(levelId);
        }
    }

    public boolean isLevelUnlocked(int levelId) {
        return unlockedLevels.contains(levelId);
    }

    public void saveLevelResult(int levelId, LevelResult result) {
        if (result == null) {
            return;
        }
        // Khi co ket qua level thi level do chac chan phai la unlocked.
        levelResults.put(levelId, result);
        unlockLevel(levelId);
    }

    public int getLevelStars(int levelId) {
        LevelResult result = levelResults.get(levelId);
        return result == null ? 0 : result.getStars();
    }

    public int getTotalCompletedLevels() {
        return levelResults.size();
    }

    public int getTotalStars() {
        int total = 0;
        for (LevelResult result : levelResults.values()) {
            total += result.getStars();
        }
        return total;
    }

    public Set<Integer> getUnlockedLevels() {
        return new LinkedHashSet<>(unlockedLevels);
    }

    public Map<Integer, LevelResult> getLevelResults() {
        return new LinkedHashMap<>(levelResults);
    }
}
