# 🏗️ LevelManager - Triển Khai Đơn Giản

## 📌 Mục Đích

Code chi tiết LevelManager cho hệ thống 6-9 màn chơi (với chú thích Việt đầy đủ).

---

## 1. 🎯 LevelManager.java (Phiên Bản Đơn Giản)

```java
/**
 * Quản lý hệ thống màn chơi (Singleton Pattern)
 * Quản lý: Tải màn, theo dõi tiến độ, lưu/tải game
 */
public class LevelManager {
    private static LevelManager instance;

    // Danh sách tất cả các màn
    private List<Level> levels;

    // Màn hiện tại đang chơi
    private Level currentLevel;

    // Tiến độ của người chơi
    private PlayerProgress playerProgress;

    // ============ Singleton Pattern ============
    private LevelManager() {
        this.levels = new ArrayList<>();
    }

    /**
     * Lấy instance duy nhất của LevelManager
     */
    public static LevelManager getInstance() {
        if (instance == null) {
            instance = new LevelManager();
        }
        return instance;
    }

    // ============ Tải và Khởi Tạo ============

    /**
     * Tải tất cả các màn từ file JSON
     * @param filePath đường dẫn đến level_data.json
     */
    public void loadLevels(String filePath) {
        try {
            // Dùng Gson hoặc Jackson để parse JSON
            String content = Files.readString(Paths.get(filePath));
            Gson gson = new Gson();

            // Parse JSON → List<Level>
            LevelData data = gson.fromJson(content, LevelData.class);
            this.levels = data.levels;

            System.out.println("[LevelManager] ✓ Tải được " + levels.size() + " màn");

            // Khởi tạo tiến độ người chơi
            if (this.playerProgress == null) {
                this.playerProgress = new PlayerProgress();
            }

        } catch (IOException e) {
            System.err.println("[LevelManager] ✗ Lỗi tải levels: " + e.getMessage());
        }
    }

    // ============ Truy Vấn Màn ============

    /**
     * Lấy tất cả các màn
     */
    public List<Level> getAllLevels() {
        return new ArrayList<>(levels);
    }

    /**
     * Lấy màn theo ID
     * @param levelId ID của màn (1-9)
     */
    public Level getLevelById(int levelId) {
        for (Level level : levels) {
            if (level.getId() == levelId) {
                return level;
            }
        }
        return null;
    }

    /**
     * Lấy màn hiện tại đang chơi
     */
    public Level getCurrentLevel() {
        return currentLevel;
    }

    /**
     * Lấy màn kế tiếp
     */
    public Level getNextLevel() {
        if (currentLevel == null) return null;
        int nextId = currentLevel.getId() + 1;
        return getLevelById(nextId);
    }

    // ============ Chọn và Quản Lý Màn ============

    /**
     * Chọn một màn để chơi
     * @param levelId ID của màn cần chọn
     */
    public void selectLevel(int levelId) {
        Level level = getLevelById(levelId);

        // Kiểm tra xem màn này có được mở khóa không
        if (level != null && playerProgress.isLevelUnlocked(levelId)) {
            this.currentLevel = level;
            System.out.println("[LevelManager] ✓ Đã chọn: " + level.getName());
        } else {
            System.err.println("[LevelManager] ✗ Không thể chọn màn này");
        }
    }

    /**
     * Hoàn thành một màn
     * @param result kết quả hoàn thành (điểm số, sao...)
     */
    public void completeLevel(LevelResult result) {
        if (currentLevel == null) {
            System.err.println("[LevelManager] ✗ Không có màn nào đang chơi");
            return;
        }

        int levelId = currentLevel.getId();

        // Lưu kết quả
        playerProgress.saveLevelResult(levelId, result);

        // Mở khóa màn tiếp theo
        int nextId = levelId + 1;
        if (getLevelById(nextId) != null) {
            playerProgress.unlockLevel(nextId);
            System.out.println("[LevelManager] ✓ Đã mở khóa: Màn " + nextId);
        }

        // Lưu tiến độ vào file
        saveProgress();

        System.out.println("[LevelManager] ✓ Hoàn thành: " + currentLevel.getName());
        System.out.println("    Điểm: " + result.getScore() + " | Sao: " + result.getStars());
    }

    /**
     * Quay lại khi chơi mà chưa hoàn thành
     */
    public void quitLevel() {
        if (currentLevel != null) {
            System.out.println("[LevelManager] ← Thoát khỏi: " + currentLevel.getName());
            currentLevel = null;
        }
    }

    // ============ Tiến Độ & Thống Kê ============

    /**
     * Lấy đối tượng tiến độ người chơi
     */
    public PlayerProgress getPlayerProgress() {
        return playerProgress;
    }

    /**
     * Lấy tổng số màn hoàn thành
     */
    public int getTotalCompletedLevels() {
        return playerProgress.getTotalCompletedLevels();
    }

    /**
     * Lấy tổng số sao đạt được
     */
    public int getTotalStars() {
        int total = 0;
        for (Level level : levels) {
            int stars = playerProgress.getLevelStars(level.getId());
            total += stars;
        }
        return total;
    }

    /**
     * Tính phần trăm hoàn thành
     */
    public float getCompletionPercentage() {
        int completed = getTotalCompletedLevels();
        return (float) completed / levels.size() * 100f;
    }

    // ============ Lưu & Tải ============

    /**
     * Lưu tiến độ vào file player_progress.json
     */
    public void saveProgress() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(playerProgress);

            // Tạo thư mục nếu chưa có
            Files.createDirectories(Paths.get("data"));

            // Ghi vào file
            Files.writeString(Paths.get("data/player_progress.json"), json);
            System.out.println("[LevelManager] 💾 Đã lưu tiến độ");

        } catch (IOException e) {
            System.err.println("[LevelManager] ✗ Lỗi lưu: " + e.getMessage());
        }
    }

    /**
     * Tải tiến độ từ file
     */
    public void loadProgress() {
        try {
            String content = Files.readString(Paths.get("data/player_progress.json"));
            Gson gson = new Gson();
            this.playerProgress = gson.fromJson(content, PlayerProgress.class);
            System.out.println("[LevelManager] 📂 Đã tải tiến độ");

        } catch (IOException e) {
            // Nếu file không tồn tại, tạo tiến độ mới
            System.out.println("[LevelManager] → Tạo tiến độ mới");
            this.playerProgress = new PlayerProgress();
            playerProgress.unlockLevel(1); // Mở khóa màn 1
        }
    }

    /**
     * Reset toàn bộ tiến độ (dùng để test)
     */
    public void resetAllProgress() {
        playerProgress = new PlayerProgress();
        playerProgress.unlockLevel(1); // Chỉ mở màn 1
        saveProgress();
        System.out.println("[LevelManager] 🔄 Đã reset toàn bộ tiến độ");
    }
}

// Helper class để parse JSON
private static class LevelData {
    List<Level> levels;
}
```

---

## 2. 📊 Level.java (Data Class)

```java
/**
 * Lớp đại diện cho một màn chơi
 */
public class Level {
    private int id;              // ID màn (1-9)
    private String name;         // Tên màn ("Bắt đầu", "Boss cuối"...)
    private int difficulty;      // Độ khó (1-6)
    private String mapFile;      // File map (.tmx)
    private List<String> objectives;  // Mục tiêu
    private List<EnemySpawn> enemies; // Danh sách quái
    private int requiredLevel;   // Màn yêu cầu trước

    // Constructor
    public Level() {}

    // ============ Getters & Setters ============
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    public String getMapFile() { return mapFile; }
    public void setMapFile(String mapFile) { this.mapFile = mapFile; }

    public List<String> getObjectives() { return objectives; }
    public void setObjectives(List<String> objectives) { this.objectives = objectives; }

    public List<EnemySpawn> getEnemies() { return enemies; }
    public void setEnemies(List<EnemySpawn> enemies) { this.enemies = enemies; }

    public int getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(int requiredLevel) { this.requiredLevel = requiredLevel; }

    @Override
    public String toString() {
        return "Màn " + id + ": " + name + " (Độ khó: " + difficulty + ")";
    }
}
```

---

## 3. 📈 PlayerProgress.java (Theo Dõi Tiến Độ)

```java
/**
 * Lớp theo dõi tiến độ của người chơi
 * Lưu: Màn hoàn thành, điểm, sao...
 */
public class PlayerProgress {
    // Các màn đã mở khóa
    private Set<Integer> unlockedLevels;

    // Kết quả của mỗi màn
    private Map<Integer, LevelResult> levelResults;

    // Constructor
    public PlayerProgress() {
        this.unlockedLevels = new HashSet<>();
        this.levelResults = new HashMap<>();
    }

    // ============ Mở Khóa ============

    /**
     * Mở khóa một màn
     */
    public void unlockLevel(int levelId) {
        unlockedLevels.add(levelId);
    }

    /**
     * Kiểm tra xem màn có được mở khóa không
     */
    public boolean isLevelUnlocked(int levelId) {
        return unlockedLevels.contains(levelId);
    }

    // ============ Kết Quả ============

    /**
     * Lưu kết quả hoàn thành một màn
     */
    public void saveLevelResult(int levelId, LevelResult result) {
        levelResults.put(levelId, result);
    }

    /**
     * Lấy số sao của một màn
     */
    public int getLevelStars(int levelId) {
        LevelResult result = levelResults.get(levelId);
        return result != null ? result.getStars() : 0;
    }

    /**
     * Kiểm tra xem màn đã hoàn thành chưa
     */
    public boolean isLevelCompleted(int levelId) {
        return levelResults.containsKey(levelId);
    }

    // ============ Thống Kê ============

    /**
     * Tổng số màn hoàn thành
     */
    public int getTotalCompletedLevels() {
        return levelResults.size();
    }

    /**
     * Tổng số sao từ tất cả màn
     */
    public int getTotalStars() {
        int total = 0;
        for (LevelResult result : levelResults.values()) {
            total += result.getStars();
        }
        return total;
    }

    /**
     * Lấy tất cả các màn đã mở khóa
     */
    public Set<Integer> getUnlockedLevels() {
        return new HashSet<>(unlockedLevels);
    }
}
```

---

## 4. 🏆 LevelResult.java (Kết Quả Hoàn Thành)

```java
/**
 * Lưu kết quả hoàn thành một màn
 */
public class LevelResult {
    private int levelId;
    private int score;        // Điểm số
    private int stars;        // Sao (0-2)
    private long playTime;    // Thời gian chơi
    private int deaths;       // Số lần chết

    // Constructor
    public LevelResult() {}

    public LevelResult(int levelId) {
        this.levelId = levelId;
        this.stars = 0;
    }

    // ============ Getters & Setters ============
    public int getLevelId() { return levelId; }
    public void setLevelId(int levelId) { this.levelId = levelId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = Math.min(2, stars); }

    public long getPlayTime() { return playTime; }
    public void setPlayTime(long playTime) { this.playTime = playTime; }

    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }

    /**
     * Tính số sao dựa trên số lần chết
     * 2 sao: không chết
     * 1 sao: có chết
     */
    public void calculateStars() {
        if (deaths == 0) {
            setStars(2);  // 2 sao - hoàn hảo
        } else {
            setStars(1);  // 1 sao - tốt
        }
    }

    @Override
    public String toString() {
        return "Màn " + levelId + ": " + score + " điểm, " + stars + " sao (" + deaths + " lần chết)";
    }
}
```

---

## 5. 🔌 Tích Hợp vào Game.java

```java
public class Game {
    private LevelManager levelManager;
    private Level currentLevel;

    // Constructor
    public Game() {
        // Khởi tạo LevelManager
        levelManager = LevelManager.getInstance();

        // Tải tất cả các màn từ JSON
        levelManager.loadLevels("resources/levels/level_data.json");

        // Tải tiến độ trước đó nếu có
        levelManager.loadProgress();
    }

    /**
     * Chọn màn để chơi
     */
    public void selectLevel(int levelId) {
        levelManager.selectLevel(levelId);
        currentLevel = levelManager.getCurrentLevel();

        if (currentLevel != null) {
            // Tải bản đồ của màn
            loadLevelMap(currentLevel.getMapFile());

            // Chuyển sang state PLAYING
            this.gameState = GameState.PLAYING;
        }
    }

    /**
     * Kiểm tra khi màn hoàn thành
     */
    private void checkLevelCompletion() {
        // Kiểm tra xem tất cả mục tiêu đã hoàn thành chưa
        if (allObjectivesCompleted()) {

            // Tính thời gian chơi
            long playTime = System.currentTimeMillis() - levelStartTime;

            // Tạo kết quả
            LevelResult result = new LevelResult(currentLevel.getId());
            result.setScore(calculateScore());
            result.setPlayTime(playTime);
            result.setDeaths(playerDeathCount);
            result.calculateStars();  // Tính sao

            // Hoàn thành màn
            levelManager.completeLevel(result);

            // Chuyển sang Results Screen
            this.gameState = GameState.RESULTS_SCREEN;
        }
    }

    /**
     * Khi người chơi chết
     */
    public void onPlayerDeath() {
        playerDeathCount++;

        // Có thể thử lại hoặc quay lại chọn màn
        System.out.println("Bạn đã chết! (" + playerDeathCount + " lần)");

        // Sau đó người chơi có thể:
        // 1. Thử lại (reload level)
        // 2. Quay lại chọn màn (selectLevel screen)
    }
}
```

---

## 6. ✅ Checklist Triển Khai

- [ ] Tạo LevelManager.java
- [ ] Tạo Level.java
- [ ] Tạo PlayerProgress.java
- [ ] Tạo LevelResult.java
- [ ] Tạo level_data.json với 6 màn
- [ ] Tích hợp vào Game.java
- [ ] Tạo LevelSelectScreen UI
- [ ] Tạo ResultsScreen UI
- [ ] Test tất cả 6 màn
- [ ] Test save/load

---

## 7. 📝 Ví Dụ Sử Dụng

```java
// Trong main hoặc Game class
public void start() {
    // 1. Khởi tạo
    LevelManager manager = LevelManager.getInstance();
    manager.loadLevels("resources/levels/level_data.json");
    manager.loadProgress();

    // 2. Xem tất cả màn
    List<Level> allLevels = manager.getAllLevels();
    System.out.println("Tổng: " + allLevels.size() + " màn");

    // 3. Chọn một màn
    manager.selectLevel(1);
    Level current = manager.getCurrentLevel();
    System.out.println("Đã chọn: " + current);

    // 4. Chơi game... (thời gian sẽ trôi)
    simulateGameplay();

    // 5. Hoàn thành
    LevelResult result = new LevelResult(1);
    result.setScore(800);
    result.setDeaths(0);
    result.calculateStars();  // 2 sao vì không chết

    manager.completeLevel(result);

    // 6. Kiểm tra tiến độ
    System.out.println("Đã hoàn thành: " + manager.getTotalCompletedLevels() + " màn");
    System.out.println("Tổng sao: " + manager.getTotalStars());
    System.out.println("Hoàn thành: " + manager.getCompletionPercentage() + "%");

    // 7. Lưu tự động
    manager.saveProgress();
}
```

---

## Tóm Tắt

- **LevelManager**: Quản lý mọi thứ (load, track, save)
- **Level**: Thông tin của một màn
- **PlayerProgress**: Tiến độ người chơi
- **LevelResult**: Kết quả hoàn thành
- **Chỉ 6 màn**: Đơn giản, dễ quản lý
- **Chú thích Việt đầy đủ**: Dễ hiểu
