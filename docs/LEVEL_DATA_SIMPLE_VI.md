# 📁 Level Data Structure - Đơn Giản

## 📌 Mục Đích

Cấu trúc thư mục và file JSON cho hệ thống 6-9 màn chơi.

---

## 1. 📂 Cấu Trúc Thư Mục

```
Game_TEST/
├── src/
│   ├── core/
│   │   ├── Game.java
│   │   └── GameState.java
│   ├── system/
│   │   └── level/
│   │       ├── LevelManager.java      ⭐ Quản lý màn
│   │       ├── Level.java             (Dữ liệu màn)
│   │       ├── PlayerProgress.java    (Tiến độ người chơi)
│   │       └── LevelResult.java       (Kết quả hoàn thành)
│   └── ui/
│       ├── LevelSelectScreen.java     (Chọn màn)
│       └── ResultsScreen.java         (Kết quả)
│
├── resources/
│   └── levels/
│       ├── level_data.json            📋 Danh sách 6 màn
│       ├── level_01/
│       │   ├── map.tmx
│       │   └── config.json
│       ├── level_02/
│       │   ├── map.tmx
│       │   └── config.json
│       ├── ... (level 3-6)
│       └── level_06/
│           ├── map.tmx
│           └── config.json
│
└── data/
    └── player_progress.json           💾 Lưu tiến độ
```

---

## 2. 📋 level_data.json (Master List)

```json
{
  "version": "1.0",
  "totalLevels": 6,
  "description": "Danh sách 6 màn chơi chính",

  "levels": [
    {
      "id": 1,
      "name": "Bắt đầu",
      "description": "Học cơ bản sinh tồn",
      "difficulty": 1,
      "mapFile": "level_01/map.tmx",

      "objectives": ["Thu thập 10 gỗ", "Sống sót 1 đêm"],

      "enemies": [
        {
          "type": "wolf",
          "count": 1,
          "spawnAt": "night"
        }
      ],

      "resources": [
        { "type": "tree", "count": 8, "respawnable": false },
        { "type": "rock", "count": 3, "respawnable": false }
      ],

      "environment": {
        "dayLength": 120,
        "nightLength": 80,
        "startPhase": "dawn"
      },

      "progression": {
        "requiresLevel": 0,
        "unlocksLevel": 2
      }
    },

    {
      "id": 2,
      "name": "Chiến đấu",
      "description": "Học kỹ năng chiến đấu",
      "difficulty": 2,
      "mapFile": "level_02/map.tmx",

      "objectives": ["Đánh 3 con sói"],

      "enemies": [
        {
          "type": "wolf",
          "count": 3,
          "spawnAt": "night"
        }
      ],

      "resources": [
        { "type": "tree", "count": 6 },
        { "type": "rock", "count": 4 }
      ],

      "progression": {
        "requiresLevel": 1,
        "unlocksLevel": 3
      }
    },

    {
      "id": 3,
      "name": "Xây dựng",
      "description": "Xây nhà để phòng thủ",
      "difficulty": 3,
      "mapFile": "level_03/map.tmx",

      "objectives": ["Thu thập 20 gỗ", "Xây 1 nhà"],

      "enemies": [{ "type": "wolf", "count": 2, "spawnAt": "night" }],

      "resources": [
        { "type": "tree", "count": 12 },
        { "type": "rock", "count": 5 }
      ],

      "progression": {
        "requiresLevel": 2,
        "unlocksLevel": 4
      }
    },

    {
      "id": 4,
      "name": "Thử thách",
      "description": "Sống sót 3 đêm với quái mạnh",
      "difficulty": 4,
      "mapFile": "level_04/map.tmx",

      "objectives": ["Sống sót 3 đêm", "Đánh 5 quái"],

      "enemies": [{ "type": "wolf", "count": 4, "spawnAt": "night" }],

      "resources": [
        { "type": "tree", "count": 10 },
        { "type": "rock", "count": 6 }
      ],

      "progression": {
        "requiresLevel": 3,
        "unlocksLevel": 5
      }
    },

    {
      "id": 5,
      "name": "Nâng cao",
      "description": "Thử thách cực kỳ khó",
      "difficulty": 5,
      "mapFile": "level_05/map.tmx",

      "objectives": ["Sống sót 4 đêm", "Thu 30 gỗ", "Đánh 8 quái"],

      "enemies": [
        { "type": "wolf", "count": 5, "spawnAt": "night" },
        { "type": "skeleton", "count": 1, "spawnAt": "night_3" }
      ],

      "resources": [
        { "type": "tree", "count": 15 },
        { "type": "rock", "count": 7 }
      ],

      "progression": {
        "requiresLevel": 4,
        "unlocksLevel": 6
      }
    },

    {
      "id": 6,
      "name": "Boss cuối",
      "description": "Đánh bại Boss sói alpha",
      "difficulty": 6,
      "mapFile": "level_06/map.tmx",

      "objectives": ["Đánh bại Boss sói"],

      "enemies": [
        {
          "type": "alpha_wolf",
          "count": 1,
          "spawnAt": "night_2",
          "isBoss": true,
          "hp": 200
        },
        { "type": "wolf", "count": 2, "spawnAt": "night_2" }
      ],

      "resources": [
        { "type": "tree", "count": 8 },
        { "type": "rock", "count": 5 }
      ],

      "progression": {
        "requiresLevel": 5,
        "unlocksLevel": 0
      }
    }
  ]
}
```

---

## 3. ⚙️ level_XX/config.json (Cấu Hình Chi Tiết Mỗi Màn)

### level_01/config.json:

```json
{
  "levelId": 1,
  "name": "Bắt đầu",

  "timing": {
    "dayLength": 120,
    "nightLength": 80,
    "totalDuration": 200
  },

  "difficulty": {
    "enemyHealthMultiplier": 0.8,
    "enemyDamageMultiplier": 0.8,
    "resourceAbundance": 1.5
  },

  "playerStart": {
    "posX": 1920,
    "posY": 1600,
    "health": 100
  },

  "worldBounds": {
    "minX": 0,
    "minY": 0,
    "maxX": 3840,
    "maxY": 2560
  }
}
```

---

## 4. 💾 player_progress.json (Lưu Tiến Độ)

```json
{
  "playerName": "Hero",
  "createdAt": "2026-05-13",

  "progression": {
    "currentLevel": 3,
    "completedLevels": [1, 2],
    "unlockedLevels": [1, 2, 3, 4],
    "nextLevel": 3
  },

  "statistics": {
    "totalPlayTime": 1200,
    "totalDeaths": 5,
    "totalEnemiesKilled": 12,
    "totalResourcesGathered": 125
  },

  "levelResults": {
    "1": {
      "completed": true,
      "score": 850,
      "stars": 2,
      "playTime": 245,
      "deaths": 0,
      "kills": 1,
      "resourcesGathered": 15,
      "completedAt": "2026-05-13T10:15:00"
    },
    "2": {
      "completed": true,
      "score": 720,
      "stars": 1,
      "playTime": 380,
      "deaths": 2,
      "kills": 3,
      "resourcesGathered": 18,
      "completedAt": "2026-05-13T10:45:00"
    },
    "3": {
      "completed": false,
      "attempts": 1,
      "bestScore": 300
    }
  }
}
```

---

## 5. 🔄 Luồng Tạo File

### Bước 1: Tạo level_data.json

```java
// Trong class hay trong main
public static void createLevelData() {
    // Nếu file chưa tồn tại, tạo mới từ JSON ở trên
    String levelDataJson = """
    {
      "version": "1.0",
      "totalLevels": 6,
      ...
    }
    """;

    Files.writeString(
        Paths.get("resources/levels/level_data.json"),
        levelDataJson
    );
}
```

### Bước 2: Tạo Thư Mục Màn

```bash
# Từ command line
mkdir -p resources/levels/level_01
mkdir -p resources/levels/level_02
...
mkdir -p resources/levels/level_06
```

### Bước 3: Copy Map Files

```bash
# Copy file map.tmx vào mỗi thư mục
cp assets/maps/mapdemo.tmx resources/levels/level_01/map.tmx
# Hoặc tạo map mới cho mỗi màn
```

### Bước 4: Tạo Thư Mục Data

```bash
mkdir -p data
```

---

## 6. 📖 Ví Dụ Đọc JSON

```java
// Đọc level_data.json
public static void readLevels() throws IOException {
    String content = Files.readString(
        Paths.get("resources/levels/level_data.json")
    );

    // Parse JSON → List<Level>
    Gson gson = new Gson();
    JsonObject json = gson.fromJson(content, JsonObject.class);

    System.out.println("Tổng màn: " + json.get("totalLevels"));

    // Duyệt qua các màn
    JsonArray levels = json.getAsJsonArray("levels");
    for (int i = 0; i < levels.size(); i++) {
        JsonObject level = levels.get(i).getAsJsonObject();
        System.out.println(
            "Màn " + level.get("id") + ": " + level.get("name")
        );
    }
}
```

---

## 7. ✅ Checklist Tạo File

- [ ] Tạo thư mục resources/levels/
- [ ] Tạo level_data.json với 6 màn
- [ ] Tạo thư mục level_01/ đến level_06/
- [ ] Copy hoặc tạo map.tmx cho mỗi màn
- [ ] Tạo config.json cho mỗi màn
- [ ] Tạo thư mục data/
- [ ] Test đọc JSON trong LevelManager

---

## 8. 📝 Template Nhanh Mỗi Màn

```json
{
  "id": X,
  "name": "Tên màn",
  "description": "Mô tả",
  "difficulty": X,
  "mapFile": "level_0X/map.tmx",
  "objectives": ["Mục tiêu 1", "Mục tiêu 2"],
  "enemies": [
    { "type": "wolf", "count": Y, "spawnAt": "night" }
  ],
  "resources": [
    { "type": "tree", "count": Y },
    { "type": "rock", "count": Z }
  ],
  "progression": {z
    "requiresLevel": X-1,
    "unlocksLevel": X+1
  }
}
```

---

## 9. 🚀 Cách Sử Dụng

```java
// 1. Load levels
LevelManager manager = LevelManager.getInstance();
manager.loadLevels("resources/levels/level_data.json");

// 2. Xem mọi màn
for (Level level : manager.getAllLevels()) {
    System.out.println(level);
}

// 3. Chọn màn
manager.selectLevel(1);

// 4. Chơi game...
// ... tương tác trong game ...

// 5. Hoàn thành
LevelResult result = new LevelResult(1);
result.setScore(800);
result.setDeaths(0);
result.calculateStars();
manager.completeLevel(result);

// 6. Lưu
manager.saveProgress();

// 7. Xem tiến độ
System.out.println("Hoàn thành: " + manager.getTotalCompletedLevels() + "/6");
```

---

## Tóm Tắt

✅ **Đơn giản**: Chỉ 6 màn, dễ quản lý  
✅ **JSON-based**: Dễ chỉnh sửa, không cần code  
✅ **Mở rộng được**: Thêm Màn 7, 8, 9 dễ dàng  
✅ **Tự động lưu**: player_progress.json tracking  
✅ **Có chú thích**: Code dễ hiểu
