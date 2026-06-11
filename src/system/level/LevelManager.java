package system.level;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * LevelManager:
 * - Dieu phoi toan bo vong doi level: load level data, select level, complete level, save progress.
 * - Hoat dong dang singleton de Game, UI level select, va save system dung chung 1 state.
 *
 * Lien he voi hit/combat:
 * - Combat khong nam trong class nay.
 * - Ket qua cua combat/resource gathering se duoc Game tong hop thanh LevelResult,
 *   sau do truyen vao completeLevel(...) de unlock level tiep theo.
 */
public class LevelManager {
    // DEFAULT_PROGRESS_FILE:
    // - Day la file save duy nhat cua level system MVP.
    // - Muc tieu la de luong load/save de tim va debug khi co loi tien do.
    private static final String DEFAULT_PROGRESS_FILE = "data/player_progress.json";
    private static LevelManager instance;

    // levels:
    // - Cache runtime cua toan bo level da parse tu level_data.json
    // - Sau khi load xong, Game/Renderer chi doc danh sach nay, khong doc file JSON truc tiep nua
    private final List<Level> levels;
    private PlayerProgress playerProgress;
    private Level currentLevel;

    private LevelManager() {
        this.levels = new ArrayList<>();
        this.playerProgress = new PlayerProgress();
        // Save moi luon mo khoa level 1 de nguoi choi co diem vao game.
        this.playerProgress.unlockLevel(1);
    }

    public static LevelManager getInstance() {
        // Singleton de:
        // - UI level select, Game runtime va save/load cung nhin chung mot state
        // - tranh viec moi noi tu tao 1 bo progress rieng roi lech du lieu
        if (instance == null) {
            instance = new LevelManager();
        }
        return instance;
    }

    public void loadLevels(String filePath) {
        try {
            // level_data.json la "master list":
            // moi level trong game deu di qua file nay.
            String content = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
            Object parsed = SimpleJson.parse(content);
            if (!(parsed instanceof Map<?, ?> root)) {
                throw new IllegalArgumentException("Invalid level_data.json root");
            }

            // "levels" la array JSON chua tung level object.
            // Moi item trong array se duoc parse sang 1 Level runtime.
            Object rawLevels = root.get("levels");
            levels.clear();
            if (rawLevels instanceof List<?> list) {
                for (Object item : list) {
                    if (!(item instanceof Map<?, ?> map)) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typed = (Map<String, Object>) map;
                    levels.add(Level.fromMap(typed));
                }
            }

            // Sap xep theo ID de:
            // - UI hien thi level dung thu tu
            // - getNextLevel() don gian hon
            levels.sort((left, right) -> Integer.compare(left.getId(), right.getId()));
            if (playerProgress.getUnlockedLevels().isEmpty()) {
                playerProgress.unlockLevel(1);
            }
            System.out.println("[LevelManager] Loaded " + levels.size() + " levels");
        } catch (Exception exception) {
            System.err.println("[LevelManager] Cannot load levels: " + exception.getMessage());
            levels.clear();
        }
    }

    public void loadProgress() {
        Path path = Path.of(DEFAULT_PROGRESS_FILE);
        if (!Files.exists(path)) {
            // Chua co file save -> tao progress trang.
            playerProgress = new PlayerProgress();
            playerProgress.unlockLevel(1);
            return;
        }

        try {
            // player_progress.json duoc parse bang cung parser toi gian voi level_data.json
            // de giu cung mot cach xu ly du lieu trong toan bo level system.
            String content = Files.readString(path, StandardCharsets.UTF_8);
            Object parsed = SimpleJson.parse(content);
            if (parsed instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) map;
                playerProgress = PlayerProgress.fromMap(typed);
            }
        } catch (Exception exception) {
            System.err.println("[LevelManager] Cannot load progress: " + exception.getMessage());
            playerProgress = new PlayerProgress();
            playerProgress.unlockLevel(1);
        }
    }

    public void saveProgress() {
        try {
            // Save duoc viet theo JSON toi gian bang SimpleJson de khong phu thuoc lib ngoai.
            Path path = Path.of(DEFAULT_PROGRESS_FILE);
            Files.createDirectories(path.getParent());
            Files.writeString(path, SimpleJson.stringify(playerProgress.toMap()), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            System.err.println("[LevelManager] Cannot save progress: " + exception.getMessage());
        }
    }

    public boolean selectLevel(int levelId) {
        Level level = getLevelById(levelId);
        // Khong cho vao man chua unlock.
        if (level == null || !playerProgress.isLevelUnlocked(levelId)) {
            return false;
        }
        // currentLevel la "level dang choi" trong runtime, khac voi selectedLevelId o UI.
        currentLevel = level;
        return true;
    }

    public void completeLevel(LevelResult result) {
        if (currentLevel == null || result == null) {
            return;
        }

        // Luu ket qua level vua clear, sau do unlock level tiep theo.
        playerProgress.saveLevelResult(currentLevel.getId(), result);
        // uu tien unlock theo data JSON cua level hien tai.
        // neu JSON khong chi ro thi fallback sang level co ID + 1.
        if (currentLevel.getUnlocksLevel() > 0 && getLevelById(currentLevel.getUnlocksLevel()) != null) {
            playerProgress.unlockLevel(currentLevel.getUnlocksLevel());
        } else {
            Level next = getNextLevel();
            if (next != null) {
                playerProgress.unlockLevel(next.getId());
            }
        }
        saveProgress();
    }

    public void resetProgress() {
        playerProgress = new PlayerProgress();
        playerProgress.unlockLevel(1);
        currentLevel = null;
        saveProgress();
    }

    public List<Level> getAllLevels() {
        return Collections.unmodifiableList(levels);
    }

    public Level getLevelById(int levelId) {
        for (Level level : levels) {
            if (level.getId() == levelId) {
                return level;
            }
        }
        return null;
    }

    public Level getCurrentLevel() {
        return currentLevel;
    }

    public Level getNextLevel() {
        if (currentLevel == null) {
            return null;
        }
        return getLevelById(currentLevel.getId() + 1);
    }

    public int getFirstUnlockedLevelId() {
        for (Level level : levels) {
            if (playerProgress.isLevelUnlocked(level.getId())) {
                return level.getId();
            }
        }
        return levels.isEmpty() ? 1 : levels.get(0).getId();
    }

    public int getSuggestedNextLevelId() {
        // Ham nay phuc vu man LEVEL_COMPLETE:
        // - neu level ke tiep da mo khoa thi goi y cho nguoi choi vao thang level do
        // - neu khong, fallback level dau tien dang mo khoa de tranh null flow
        Level next = getNextLevel();
        if (next != null && playerProgress.isLevelUnlocked(next.getId())) {
            return next.getId();
        }
        return getFirstUnlockedLevelId();
    }

    public PlayerProgress getPlayerProgress() {
        return playerProgress;
    }

    public int getTotalCompletedLevels() {
        return playerProgress.getTotalCompletedLevels();
    }

    public int getTotalStars() {
        return playerProgress.getTotalStars();
    }
}
