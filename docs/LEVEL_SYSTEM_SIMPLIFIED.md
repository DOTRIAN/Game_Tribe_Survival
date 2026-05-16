# 🎮 Hệ Thống Màn Chơi - Phiên Bản Đơn Giản

## 📋 Mục Đích

Thiết kế hệ thống quản lý **6-9 màn chơi** cho Tribe Survival Game (Đồ án môn học).

---

## 1. 📊 Cấu Trúc Màn Chơi

### 1.1 Tổng Quan Số Màn (6-9 màn)

```
Tiến độ Campaign:
├── Tutorial (Màn 1-2)
│   ├── Level 1: Học cơ bản (thu thập gỗ, sống sót)
│   └── Level 2: Hướng dẫn chiến đấu (đánh quái)
├── Main Campaign (Màn 3-6)
│   ├── Level 3: Xây dựng nhà (học crafting)
│   ├── Level 4: Quái mạnh hơn
│   ├── Level 5: Thử thách hỗn hợp
│   └── Level 6: Boss cuối (tùy chọn)
└── Mở rộng (nếu còn thời gian)
    └── Level 7-9: Các màn bổ sung
```

---

## 2. 📝 Thông Tin Mỗi Màn

```json
{
  "levelId": 1,
  "name": "Bắt đầu",
  "description": "Học cơ bản sinh tồn",
  "difficulty": 1,

  "map": {
    "file": "level_01/map.tmx",
    "width": 120,
    "height": 80
  },

  "objectives": ["Thu thập 10 gỗ", "Sống sót 1 đêm"],

  "rewards": {
    "experience": 100,
    "items": ["basic_axe"]
  },

  "enemies": [{ "type": "wolf", "count": 1, "spawnAt": "night" }],

  "resources": [
    { "type": "tree", "count": 8 },
    { "type": "rock", "count": 3 }
  ],

  "environment": {
    "dayLength": 120,
    "nightLength": 80
  }
}
```

---

## 3. 🎯 LevelManager - Core Methods

```java
// Hệ thống quản lý màn chơi
LevelManager manager = LevelManager.getInstance();

// Tải tất cả màn từ file JSON
manager.loadLevels("resources/levels/level_data.json");

// Lấy màn hiện tại
Level current = manager.getCurrentLevel();

// Lấy màn kế tiếp
Level next = manager.getNextLevel();

// Chọn màn để chơi
manager.selectLevel(1);

// Hoàn thành màn
LevelResult result = new LevelResult(current);
result.setScore(800);
manager.completeLevel(result);

// Lưu tiến độ
manager.saveProgress();

// Lấy tiến độ người chơi
PlayerProgress progress = manager.getPlayerProgress();
int completedLevels = progress.getTotalCompletedLevels();
```

---

## 4. 🏆 Hệ Thống Sao - Đơn Giản

```
⭐⭐ Hoàn hảo (2 sao)
├─ Hoàn thành tất cả mục tiêu
└─ Không chết

⭐ Tốt (1 sao)
├─ Hoàn thành mục tiêu chính
└─ Có thể chết được
```

---

## 5. 💾 Cấu Trúc Thư Mục

```
resources/
└── levels/
    ├── level_data.json          # Danh sách 6-9 màn
    ├── level_01/
    │   ├── map.tmx
    │   └── config.json
    ├── level_02/
    ├── ...
    └── level_09/

data/
└── player_progress.json         # Lưu tiến độ người chơi
```

---

## 6. 🔄 Trạng Thái Màn

```
LOCKED (Khóa)
    ↓ (người chơi hoàn thành màn trước)
AVAILABLE (Có sẵn)
    ↓ (người chơi chọn màn)
IN_PROGRESS (Đang chơi)
    ├─ ✓ Hoàn thành mục tiêu
    │   ↓
    │ COMPLETED (Hoàn thành)
    │
    └─ ✗ Người chơi chết
        ↓
        IN_PROGRESS (Có thể thử lại)
```

---

## 7. 📊 Ví Dụ 6 Màn Chính

### Level 1: Bắt đầu

- **Mục tiêu**: Thu thập 10 gỗ, sống sót 1 đêm
- **Quái**: 1 con sói (dễ)
- **Thời gian**: ~5 phút
- **Sơ cấp**: ✓ Dễ học

### Level 2: Chiến đấu

- **Mục tiêu**: Đánh 3 con quái
- **Quái**: 2-3 con sói
- **Thời gian**: ~7 phút
- **Sơ cấp**: ✓ Học combat

### Level 3: Xây dựng

- **Mục tiêu**: Thu thập 20 gỗ, xây nhà
- **Quái**: 3 con sói
- **Thời gian**: ~10 phút
- **Sơ cấp**: ✓ Học crafting

### Level 4: Thử thách

- **Mục tiêu**: Sống 3 đêm, đánh 5 quái
- **Quái**: 2-4 con sói mỗi đêm
- **Thời gian**: ~15 phút
- **Sơ cấp**: Tương đương

### Level 5: Nâng cao

- **Mục tiêu**: Sống 4 đêm, thu 30 gỗ, đánh 8 quái
- **Quái**: 3-5 con sói, thêm skeleton
- **Thời gian**: ~18 phút
- **Sơ cấp**: Khó

### Level 6: Boss cuối

- **Mục tiêu**: Đánh Boss sói
- **Quái**: 1 Boss sói mạnh + 2 con sói bình thường
- **Thời gian**: ~20 phút
- **Sơ cấp**: Rất khó

---

## 8. ✅ Checklist Triển Khai

**Giai đoạn 1: Cơ sở**

- [ ] Tạo LevelManager class
- [ ] Tạo Level data class
- [ ] Tạo PlayerProgress class
- [ ] Tạo level_data.json với 6 màn

**Giai đoạn 2: Tích hợp**

- [ ] Thêm LEVEL_SELECT state vào Game
- [ ] Tạo LevelSelectScreen UI
- [ ] Tạo ResultsScreen UI
- [ ] Thêm logic hoàn thành màn

**Giai đoạn 3: Polish**

- [ ] Lưu/tải tiến độ
- [ ] Test tất cả màn
- [ ] Thêm chuyển màn animation
- [ ] Kiểm tra difficulty curve

---

## 9. 💾 Ví Dụ level_data.json (6 màn)

```json
{
  "version": "1.0",
  "totalLevels": 6,
  "levels": [
    {
      "id": 1,
      "name": "Bắt đầu",
      "difficulty": 1,
      "map": "level_01/map.tmx",
      "objectives": ["Thu thập 10 gỗ", "Sống sót 1 đêm"],
      "enemies": [{ "type": "wolf", "count": 1 }],
      "resources": [{ "type": "tree", "count": 8 }],
      "unlockNext": true
    },
    {
      "id": 2,
      "name": "Chiến đấu",
      "difficulty": 2,
      "map": "level_02/map.tmx",
      "objectives": ["Đánh 3 quái"],
      "enemies": [{ "type": "wolf", "count": 3 }],
      "unlockNext": true,
      "requiresLevel": 1
    },
    {
      "id": 3,
      "name": "Xây dựng",
      "difficulty": 3,
      "map": "level_03/map.tmx",
      "objectives": ["Thu 20 gỗ", "Xây nhà"],
      "enemies": [{ "type": "wolf", "count": 2 }],
      "resources": [{ "type": "tree", "count": 12 }],
      "unlockNext": true,
      "requiresLevel": 2
    },
    {
      "id": 4,
      "name": "Thử thách",
      "difficulty": 4,
      "map": "level_04/map.tmx",
      "objectives": ["Sống 3 đêm", "Đánh 5 quái"],
      "enemies": [{ "type": "wolf", "count": 4 }],
      "unlockNext": true,
      "requiresLevel": 3
    },
    {
      "id": 5,
      "name": "Nâng cao",
      "difficulty": 5,
      "map": "level_05/map.tmx",
      "objectives": ["Sống 4 đêm", "Thu 30 gỗ", "Đánh 8 quái"],
      "enemies": [
        { "type": "wolf", "count": 4 },
        { "type": "skeleton", "count": 1 }
      ],
      "unlockNext": true,
      "requiresLevel": 4
    },
    {
      "id": 6,
      "name": "Boss cuối",
      "difficulty": 6,
      "map": "level_06/map.tmx",
      "objectives": ["Đánh Boss sói"],
      "enemies": [
        { "type": "alpha_wolf", "count": 1, "isBoss": true },
        { "type": "wolf", "count": 2 }
      ],
      "requiresLevel": 5
    }
  ]
}
```

---

## 10. 📌 Lưu Ý Thiết Kế

✅ **Đơn giản** - Chỉ cần 6 màn, không cần endless mode  
✅ **Dễ mở rộng** - Có thể thêm 3 màn nữa nếu còn thời gian  
✅ **Có progression** - Difficulty tăng dần từ 1 → 6  
✅ **Không phức tạp** - Quản lý JSON, không cần database  
✅ **Dễ test** - Từng màn độc lập

---

## 11. ⏱️ Ước Tính Thời Gian Code

- **LevelManager**: 2-3 giờ
- **Level data + JSON**: 1 giờ
- **LevelSelectScreen UI**: 2-3 giờ
- **ResultsScreen UI**: 1-2 giờ
- **Integration + Testing**: 2-3 giờ
- **Total**: ~10-12 giờ

---

## Tóm Tắt

Đây là thiết kế **nhẹ nhàng, đơn giản** phù hợp với đồ án học tập:

- 6 màn chính (có thể mở rộng đến 9)
- Hệ thống JSON đơn giản
- LevelManager quản lý mọi thứ
- 2-star system (không cần 3 sao)
- Dễ code, dễ test, dễ mở rộng
