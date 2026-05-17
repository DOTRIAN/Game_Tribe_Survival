# 🚀 Quick Start Guide - Bắt Đầu Ngay

## 📌 Dành Cho Ai?

Guide này cho những bạn muốn **triển khai ngay** hệ thống 6 màn chơi mà không cần đọc cả đống tài liệu phức tạp.

---

## ⏱️ Thời Gian: ~2-3 ngày code

---

## 1️⃣ Tạo Classes Cơ Bản (1 giờ)

### File 1: LevelManager.java

```java
// Đặt vào: src/system/level/LevelManager.java

public class LevelManager {
    private static LevelManager instance;
    private List<Level> levels;
    private Level currentLevel;
    private PlayerProgress playerProgress;

    private LevelManager() {
        this.levels = new ArrayList<>();
        this.playerProgress = new PlayerProgress();
    }

    public static LevelManager getInstance() {
        if (instance == null) {
            instance = new LevelManager();
        }
        return instance;
    }

    // Tải levels từ JSON
    public void loadLevels(String filePath) {
        try {
            String json = Files.readString(Paths.get(filePath));
            Gson gson = new Gson();
            // Parse và lưu vào this.levels
            System.out.println("✓ Đã load " + levels.size() + " màn");
        } catch (IOException e) {
            System.err.println("✗ Lỗi: " + e.getMessage());
        }
    }

    // Chọn màn
    public void selectLevel(int id) {
        for (Level level : levels) {
            if (level.getId() == id) {
                this.currentLevel = level;
                System.out.println("✓ Chọn: " + level.getName());
                return;
            }
        }
    }

    // Hoàn thành màn
    public void completeLevel(LevelResult result) {
        int nextId = currentLevel.getId() + 1;
        playerProgress.saveLevelResult(currentLevel.getId(), result);
        playerProgress.unlockLevel(nextId);
        saveProgress();
        System.out.println("✓ Hoàn thành: " + result.getStars() + " sao");
    }

    // Lưu tiến độ
    public void saveProgress() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(playerProgress);
            Files.createDirectories(Paths.get("data"));
            Files.writeString(Paths.get("data/player_progress.json"), json);
        } catch (IOException e) {
            System.err.println("✗ Lỗi lưu: " + e.getMessage());
        }
    }

    // Getters
    public Level getCurrentLevel() { return currentLevel; }
    public Level getLevelById(int id) {
        for (Level l : levels) if (l.getId() == id) return l;
        return null;
    }
    public List<Level> getAllLevels() { return new ArrayList<>(levels); }
    public int getTotalCompletedLevels() {
        return playerProgress.getTotalCompletedLevels();
    }
}
```

### File 2: Level.java

```java
// Đặt vào: src/system/level/Level.java

public class Level {
    private int id;
    private String name;
    private int difficulty;
    private String mapFile;
    private List<String> objectives;
    private List<EnemySpawn> enemies;

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public int getDifficulty() { return difficulty; }
    public String getMapFile() { return mapFile; }
    public List<String> getObjectives() { return objectives; }
    public List<EnemySpawn> getEnemies() { return enemies; }

    @Override
    public String toString() {
        return "Màn " + id + ": " + name + " (Độ khó: " + difficulty + ")";
    }
}
```

### File 3: PlayerProgress.java

```java
// Đặt vào: src/system/level/PlayerProgress.java

public class PlayerProgress {
    private Set<Integer> unlockedLevels = new HashSet<>();
    private Map<Integer, LevelResult> results = new HashMap<>();

    public void unlockLevel(int id) { unlockedLevels.add(id); }
    public void saveLevelResult(int id, LevelResult r) { results.put(id, r); }
    public int getTotalCompletedLevels() { return results.size(); }
    public boolean isLevelUnlocked(int id) { return unlockedLevels.contains(id); }
}
```

### File 4: LevelResult.java

```java
// Đặt vào: src/system/level/LevelResult.java

public class LevelResult {
    private int levelId;
    private int score;
    private int stars;
    private int deaths;

    public LevelResult(int levelId) { this.levelId = levelId; }

    public void setScore(int s) { this.score = s; }
    public void setStars(int s) { this.stars = Math.min(2, s); }
    public void setDeaths(int d) { this.deaths = d; }

    public int getScore() { return score; }
    public int getStars() { return stars; }
    public int getDeaths() { return deaths; }

    // Tính sao: 2 sao nếu không chết, 1 sao nếu chết
    public void calculateStars() {
        setStars(deaths == 0 ? 2 : 1);
    }
}
```

---

## 2️⃣ Tạo File JSON (30 phút)

### File: resources/levels/level_data.json

```json
{
  "levels": [
    {
      "id": 1,
      "name": "Bắt đầu",
      "difficulty": 1,
      "mapFile": "level_01/map.tmx",
      "objectives": ["Thu 10 gỗ", "Sống 1 đêm"],
      "enemies": [{ "type": "wolf", "count": 1 }],
      "resources": [{ "type": "tree", "count": 8 }]
    },
    {
      "id": 2,
      "name": "Chiến đấu",
      "difficulty": 2,
      "mapFile": "level_02/map.tmx",
      "objectives": ["Đánh 3 quái"],
      "enemies": [{ "type": "wolf", "count": 3 }],
      "resources": [{ "type": "tree", "count": 6 }]
    },
    {
      "id": 3,
      "name": "Xây dựng",
      "difficulty": 3,
      "mapFile": "level_03/map.tmx",
      "objectives": ["Thu 20 gỗ", "Xây nhà"],
      "enemies": [{ "type": "wolf", "count": 2 }],
      "resources": [{ "type": "tree", "count": 12 }]
    },
    {
      "id": 4,
      "name": "Thử thách",
      "difficulty": 4,
      "mapFile": "level_04/map.tmx",
      "objectives": ["Sống 3 đêm", "Đánh 5 quái"],
      "enemies": [{ "type": "wolf", "count": 4 }],
      "resources": [{ "type": "tree", "count": 10 }]
    },
    {
      "id": 5,
      "name": "Nâng cao",
      "difficulty": 5,
      "mapFile": "level_05/map.tmx",
      "objectives": ["Sống 4 đêm", "Thu 30 gỗ"],
      "enemies": [{ "type": "wolf", "count": 5 }],
      "resources": [{ "type": "tree", "count": 15 }]
    },
    {
      "id": 6,
      "name": "Boss cuối",
      "difficulty": 6,
      "mapFile": "level_06/map.tmx",
      "objectives": ["Đánh Boss"],
      "enemies": [{ "type": "alpha_wolf", "count": 1, "isBoss": true }],
      "resources": [{ "type": "tree", "count": 8 }]
    }
  ]
}
```

---

## 3️⃣ Tích Hợp vào Game.java (2 giờ)

### Thêm vào Game Constructor:

```java
public Game() {
    // ... existing code ...

    // Khởi tạo LevelManager
    LevelManager levelManager = LevelManager.getInstance();
    levelManager.loadLevels("resources/levels/level_data.json");
    levelManager.loadProgress();  // Load tiến độ cũ nếu có
}
```

### Thêm method chọn màn:

```java
public void selectLevel(int levelId) {
    LevelManager.getInstance().selectLevel(levelId);
    currentLevel = LevelManager.getInstance().getCurrentLevel();

    if (currentLevel != null) {
        // Tải bản đồ của màn
        // gameState = GameState.PLAYING;
    }
}
```

### Thêm logic hoàn thành màn:

```java
private void checkLevelComplete() {
    // Kiểm tra tất cả mục tiêu hoàn thành
    if (objectivesComplete()) {
        LevelResult result = new LevelResult(currentLevel.getId());
        result.setScore(calculateScore());
        result.setDeaths(playerDeaths);
        result.calculateStars();

        LevelManager.getInstance().completeLevel(result);

        // Hiển thị results screen
        // gameState = GameState.RESULTS_SCREEN;
    }
}
```

---

## 4️⃣ Test Cơ Bản (1 giờ)

```java
// Chạy test này
public void testLevelSystem() {
    LevelManager mgr = LevelManager.getInstance();

    // 1. Load levels
    mgr.loadLevels("resources/levels/level_data.json");
    List<Level> all = mgr.getAllLevels();
    System.out.println("Tổng: " + all.size() + " màn");
    assert all.size() == 6;

    // 2. Chọn màn 1
    mgr.selectLevel(1);
    Level current = mgr.getCurrentLevel();
    System.out.println("Chọn: " + current);
    assert current.getId() == 1;

    // 3. Hoàn thành màn
    LevelResult result = new LevelResult(1);
    result.setScore(800);
    result.setDeaths(0);
    result.calculateStars();
    mgr.completeLevel(result);

    // 4. Kiểm tra
    assert mgr.getTotalCompletedLevels() == 1;

    System.out.println("✓ Tất cả test pass!");
}
```

---

## 5️⃣ UI Screens (2-3 giờ)

### LevelSelectScreen.java

```java
// Hiển thị grid các màn
public class LevelSelectScreen {

    public void render(GraphicsContext gc) {
        // Vẽ 6 nút (button) cho 6 màn
        LevelManager mgr = LevelManager.getInstance();

        int x = 100, y = 100;
        for (Level level : mgr.getAllLevels()) {
            // Vẽ nút
            gc.fillRect(x, y, 150, 100);
            gc.fillText(level.getName(), x + 10, y + 30);
            gc.fillText("Độ khó: " + level.getDifficulty(), x + 10, y + 60);

            // Hiển thị sao nếu đã hoàn thành
            int stars = mgr.getPlayerProgress()
                          .getLevelStars(level.getId());
            if (stars > 0) {
                gc.fillText("⭐".repeat(stars), x + 10, y + 80);
            }

            x += 160;
            if (x > 600) { x = 100; y += 120; }
        }
    }

    public void handleClick(double mx, double my) {
        // Kiểm tra nút nào được nhấn và chọn màn đó
    }
}
```

### ResultsScreen.java

```java
public class ResultsScreen {

    public void render(GraphicsContext gc) {
        LevelResult result = lastLevelResult;

        // Hiển thị kết quả
        gc.fillText("HOÀN THÀNH", 300, 100);
        gc.fillText("Điểm: " + result.getScore(), 300, 150);
        gc.fillText("Sao: " + "⭐".repeat(result.getStars()), 300, 200);
        gc.fillText("Chết: " + result.getDeaths() + " lần", 300, 250);

        // Nút
        gc.fillRect(250, 350, 100, 50);
        gc.fillText("Tiếp", 270, 380);

        gc.fillRect(400, 350, 100, 50);
        gc.fillText("Menu", 420, 380);
    }

    public void handleClick(double mx, double my) {
        if (mx > 250 && mx < 350 && my > 350 && my < 400) {
            // Next level
        } else if (mx > 400 && mx < 500 && my > 350 && my < 400) {
            // Back to menu
        }
    }
}
```

---

## 6️⃣ Checklist Hoàn Thành

- [ ] Tạo 4 classes (LevelManager, Level, PlayerProgress, LevelResult)
- [ ] Tạo level_data.json với 6 màn
- [ ] Tích hợp vào Game.java
- [ ] Tạo LevelSelectScreen
- [ ] Tạo ResultsScreen
- [ ] Test đầy đủ
- [ ] Lưu/tải tiến độ hoạt động
- [ ] Difficulty curve đúng (1→6)

---

## 7️⃣ Khoảng Thời Gian

```
Thứ 2: Classes + JSON (3-4 giờ)
Thứ 3: Tích hợp vào Game (2-3 giờ)
Thứ 4: UI Screens (3-4 giờ)
Thứ 5: Test + Fix bugs (2-3 giờ)
---
Tổng: ~12-14 giờ code (2-3 ngày)
```

---

## 8️⃣ Nếu Bị Lỗi

### Lỗi 1: "Cannot find symbol Gson"

```
→ Thêm dependency:
  - Intellij: File > Project Structure > Libraries > Add
  - Maven: <dependency><groupId>com.google.code.gson</groupId>...
```

### Lỗi 2: "File not found: level_data.json"

```
→ Tạo thư mục: resources/levels/
→ Đặt level_data.json vào đó
→ Chạy từ root project directory
```

### Lỗi 3: "NullPointerException in LevelManager"

```
→ Gọi loadProgress() sau loadLevels()
→ Kiểm tra path file đúng
→ Xem console log
```

---

## 9️⃣ Tiếp Theo (Nếu Có Thời Gian)

- [ ] Thêm Màn 7, 8, 9
- [ ] Sound effects khi hoàn thành
- [ ] Achievements/badges
- [ ] Leaderboard (local)
- [ ] Settings menu

---

## 🎯 Tóm Tắt

| Bước      | Việc               | Thời Gian  |
| --------- | ------------------ | ---------- |
| 1         | 4 Classes          | 1h         |
| 2         | level_data.json    | 30m        |
| 3         | Tích hợp Game.java | 2h         |
| 4         | LevelSelectScreen  | 1.5h       |
| 5         | ResultsScreen      | 1.5h       |
| 6         | Test + Fix         | 2-3h       |
| **TOTAL** |                    | **10-12h** |

---

## ✅ OK, Ready to Code!

Bắt đầu từ Bước 1️⃣ ngay bây giờ! 🚀

Nếu cần help, xem file:

- `LEVEL_SYSTEM_SIMPLIFIED.md` - Design overview
- `LEVELMANAGER_SIMPLE_VI.md` - Code chi tiết
- `LEVEL_DATA_SIMPLE_VI.md` - JSON structure
