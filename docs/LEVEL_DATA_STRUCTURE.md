# 📁 Level Data Structure & JSON Configuration

## 📌 Mục Đích

Chi tiết về cấu trúc dữ liệu màn chơi và format file JSON cho level management.

---

## 1. 📊 Level Data Structure Overview

### 1.1 Folder Organization

```
resources/
├── levels/
│   ├── level_data.json              # Danh sách tất cả levels
│   ├── templates/
│   │   ├── difficulty_curves.json   # Difficulty progression curves
│   │   └── reward_templates.json    # Reward templates
│   │
│   ├── level_01/
│   │   ├── map.tmx                  # Tiled map file
│   │   ├── config.json              # Level-specific config
│   │   ├── enemies.json             # Enemy spawn data
│   │   ├── resources.json           # Resource placement
│   │   ├── objectives.json          # Level objectives
│   │   └── preview.png              # Level thumbnail
│   │
│   ├── level_02/
│   │   ├── (same structure)
│   │
│   └── level_50/
│       ├── (same structure)
│
└── progression/
    ├── player_progress.json         # Player's saved progress
    └── global_stats.json            # Global leaderboard data
```

---

## 2. 🎯 level_data.json (Main Level List)

### 2.1 Full Structure

```json
{
  "version": "1.0",
  "totalLevels": 50,
  "lastUpdated": "2026-05-13",

  "levels": [
    {
      "id": 1,
      "number": 1,
      "name": "The Beginning",
      "description": "Learn the basics of survival",
      "zone": "Tutorial",
      "chapter": "Chapter 1: First Steps",

      "difficulty": {
        "score": 1,
        "label": "Very Easy",
        "recommendation": "New players"
      },

      "map": {
        "file": "level_01/map.tmx",
        "width": 120,
        "height": 80,
        "tileSize": 32
      },

      "environment": {
        "dayLengthSeconds": 120,
        "nightLengthSeconds": 80,
        "startPhase": "dawn",
        "weather": "clear",
        "temperature": 20
      },

      "objectives": [
        {
          "id": "obj_survive_night",
          "title": "Survive the first night",
          "description": "Make it through to dawn",
          "type": "SURVIVAL",
          "target": 1,
          "unit": "nights",
          "required": true
        },
        {
          "id": "obj_gather_wood",
          "title": "Gather resources",
          "description": "Collect 10 wood",
          "type": "GATHER",
          "target": 10,
          "unit": "wood",
          "required": true
        },
        {
          "id": "obj_learn_attack",
          "title": "Combat Tutorial",
          "description": "Defeat 1 wolf",
          "type": "COMBAT",
          "target": 1,
          "unit": "enemies",
          "required": false
        }
      ],

      "content": {
        "enemies": [
          {
            "type": "wolf",
            "name": "Wolf",
            "count": 1,
            "spawnTrigger": "night",
            "difficulty": 1
          }
        ],

        "resources": [
          {
            "type": "tree",
            "count": 10,
            "spawnLocations": "distributed",
            "respawnable": false
          }
        ],

        "structures": [],

        "npcs": [],

        "loot": []
      },

      "scoring": {
        "baseScore": 1000,
        "targetScore": 800,
        "maxScore": 1500,
        "scoreBreakdown": {
          "completion": 500,
          "noDeaths": 300,
          "speed": 200
        }
      },

      "stars": {
        "requirements": [
          {
            "stars": 3,
            "minScore": 1000,
            "conditions": [
              "score >= 1000",
              "deaths == 0",
              "complete_all_objectives"
            ]
          },
          {
            "stars": 2,
            "minScore": 700,
            "conditions": ["score >= 700", "deaths <= 2"]
          },
          {
            "stars": 1,
            "minScore": 400,
            "conditions": ["score >= 400", "complete_main_objectives"]
          }
        ]
      },

      "rewards": {
        "experience": 100,
        "currency": 50,
        "items": [],
        "unlocks": []
      },

      "progression": {
        "unlockCondition": "START_GAME",
        "prerequisites": [],
        "nextLevels": [2],
        "alternativeLevels": []
      },

      "unlocks": {
        "levels": [2],
        "items": ["basic_axe"],
        "achievements": ["tutorial_complete"]
      },

      "metadata": {
        "estimatedTime": 300,
        "recommended": true,
        "newPlayer": true,
        "tutorial": true,
        "boss": false,
        "endless": false
      }
    },

    {
      "id": 2,
      "number": 2,
      "name": "First Hunt",
      "description": "Prove your combat skills",
      "zone": "Tutorial",
      "chapter": "Chapter 1: First Steps",

      "difficulty": {
        "score": 2,
        "label": "Easy",
        "recommendation": "New players"
      },

      "map": {
        "file": "level_02/map.tmx",
        "width": 140,
        "height": 100,
        "tileSize": 32
      },

      "environment": {
        "dayLengthSeconds": 120,
        "nightLengthSeconds": 100,
        "startPhase": "morning",
        "weather": "clear",
        "temperature": 22
      },

      "objectives": [
        {
          "id": "obj_survive_nights",
          "title": "Survive 2 nights",
          "type": "SURVIVAL",
          "target": 2,
          "unit": "nights",
          "required": true
        },
        {
          "id": "obj_kill_enemies",
          "title": "Defeat enemies",
          "type": "COMBAT",
          "target": 5,
          "unit": "enemies",
          "required": true
        },
        {
          "id": "obj_gather_resources",
          "title": "Gather 25 wood",
          "type": "GATHER",
          "target": 25,
          "unit": "wood",
          "required": false
        }
      ],

      "content": {
        "enemies": [
          {
            "type": "wolf",
            "count": 2,
            "spawnTrigger": "night",
            "difficulty": 1
          }
        ],
        "resources": [
          {
            "type": "tree",
            "count": 15
          },
          {
            "type": "rock",
            "count": 5
          }
        ]
      },

      "scoring": {
        "baseScore": 1200,
        "targetScore": 960,
        "maxScore": 1800
      },

      "rewards": {
        "experience": 150,
        "currency": 75,
        "items": ["stone_pickaxe"]
      },

      "progression": {
        "unlockCondition": "COMPLETE_LEVEL_1_WITH_1_STAR",
        "prerequisites": [1],
        "nextLevels": [3, 4]
      },

      "metadata": {
        "estimatedTime": 600,
        "newPlayer": true,
        "tutorial": false
      }
    },

    {
      "id": 5,
      "number": 5,
      "name": "The Camp",
      "description": "Build a defensive position",
      "zone": "Beginner",
      "chapter": "Chapter 2: Settlement",

      "difficulty": {
        "score": 5,
        "label": "Normal",
        "recommendation": "Players with basic skills"
      },

      "map": {
        "file": "level_05/map.tmx",
        "width": 160,
        "height": 120,
        "tileSize": 32
      },

      "environment": {
        "dayLengthSeconds": 120,
        "nightLengthSeconds": 100,
        "startPhase": "morning",
        "weather": "clear"
      },

      "objectives": [
        {
          "id": "obj_survive_nights",
          "title": "Survive 5 nights",
          "type": "SURVIVAL",
          "target": 5,
          "unit": "nights",
          "required": true
        },
        {
          "id": "obj_gather_wood",
          "title": "Gather 50 wood",
          "type": "GATHER",
          "target": 50,
          "unit": "wood",
          "required": true
        },
        {
          "id": "obj_build_shelters",
          "title": "Build 2 shelters",
          "type": "BUILD",
          "target": 2,
          "unit": "structures",
          "required": true
        },
        {
          "id": "obj_kill_enemies",
          "title": "Defeat 10 enemies",
          "type": "COMBAT",
          "target": 10,
          "unit": "enemies",
          "required": true
        },
        {
          "id": "obj_score",
          "title": "Reach 5000 score",
          "type": "SCORE",
          "target": 5000,
          "unit": "points",
          "required": false
        }
      ],

      "content": {
        "enemies": [
          {
            "type": "wolf",
            "count": 3,
            "spawnTrigger": "night",
            "wavePattern": "increasing"
          },
          {
            "type": "skeleton",
            "count": 1,
            "spawnTrigger": "night_4",
            "difficulty": 2
          }
        ],

        "resources": [
          { "type": "tree", "count": 20, "respawnable": false },
          { "type": "rock", "count": 8 },
          { "type": "grass", "count": 15 },
          { "type": "vegetable", "count": 5 }
        ]
      },

      "scoring": {
        "baseScore": 2000,
        "targetScore": 1600,
        "maxScore": 3000,
        "scoreBreakdown": {
          "completion": 1000,
          "resourceEfficiency": 500,
          "noDeaths": 500,
          "speedBonus": 300,
          "combatKills": 200
        }
      },

      "stars": {
        "requirements": [
          {
            "stars": 3,
            "minScore": 2500,
            "conditions": ["score >= 2500", "deaths == 0", "playTime < 600"]
          },
          {
            "stars": 2,
            "minScore": 1600,
            "conditions": ["score >= 1600", "deaths <= 3"]
          },
          {
            "stars": 1,
            "minScore": 900,
            "conditions": ["complete_survival_objective"]
          }
        ]
      },

      "rewards": {
        "experience": 500,
        "currency": 250,
        "items": ["iron_axe", "shelter_blueprint"],
        "unlocks": ["level_6", "level_7"]
      },

      "progression": {
        "unlockCondition": "COMPLETE_ZONE_Tutorial AND PLAYER_LEVEL >= 5",
        "prerequisites": [1, 2, 3, 4],
        "nextLevels": [6, 7],
        "alternativeLevels": [8]
      },

      "metadata": {
        "estimatedTime": 900,
        "newPlayer": false,
        "difficulty": "spike",
        "boss": false
      }
    },

    {
      "id": 20,
      "number": 20,
      "name": "The First Boss",
      "description": "Face the alpha wolf",
      "zone": "Advanced",
      "chapter": "Chapter 4: Trials",

      "difficulty": {
        "score": 15,
        "label": "Hard",
        "recommendation": "Experienced players"
      },

      "map": {
        "file": "level_20/map.tmx",
        "width": 180,
        "height": 140,
        "tileSize": 32
      },

      "environment": {
        "dayLengthSeconds": 90,
        "nightLengthSeconds": 150,
        "startPhase": "dusk",
        "weather": "dark"
      },

      "objectives": [
        {
          "id": "obj_survive_nights",
          "title": "Survive 3 nights",
          "type": "SURVIVAL",
          "target": 3,
          "unit": "nights",
          "required": true
        },
        {
          "id": "obj_defeat_boss",
          "title": "Defeat the Alpha Wolf",
          "type": "BOSS",
          "target": 1,
          "unit": "boss",
          "required": true
        },
        {
          "id": "obj_perfect_run",
          "title": "No deaths",
          "type": "CHALLENGE",
          "target": 0,
          "unit": "deaths",
          "required": false
        }
      ],

      "content": {
        "enemies": [
          {
            "type": "wolf",
            "count": 5,
            "spawnTrigger": "night",
            "difficulty": 2
          },
          {
            "type": "alpha_wolf",
            "count": 1,
            "spawnTrigger": "night_2",
            "isBoss": true,
            "difficulty": 5,
            "hp": 200,
            "abilities": ["charge", "howl", "pack_summon"]
          }
        ],

        "resources": [
          { "type": "tree", "count": 10 },
          { "type": "rock", "count": 8 },
          { "type": "powerup_health", "count": 3 },
          { "type": "powerup_armor", "count": 2 }
        ]
      },

      "scoring": {
        "baseScore": 5000,
        "targetScore": 4000,
        "maxScore": 8000
      },

      "rewards": {
        "experience": 1000,
        "currency": 500,
        "items": ["legendary_sword", "armor_helmet"],
        "unlocks": ["level_21", "endless_mode"]
      },

      "progression": {
        "unlockCondition": "COMPLETE_ZONE_Intermediate AND TOTAL_STARS >= 30",
        "prerequisites": [
          1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19
        ],
        "nextLevels": [21]
      },

      "metadata": {
        "estimatedTime": 1200,
        "boss": true,
        "difficulty_spike": true,
        "checkpoint": true
      }
    }
  ]
}
```

---

## 3. 📝 level_XX/config.json (Per-Level Config)

```json
{
  "levelId": 5,
  "name": "The Camp",

  "timing": {
    "dayLength": 120,
    "nightLength": 100,
    "duskLength": 20,
    "dawnLength": 25,
    "totalCycleDuration": 265,
    "expectedCompletionTime": 900
  },

  "difficulty": {
    "enemyHealthMultiplier": 1.0,
    "enemyDamageMultiplier": 1.0,
    "resourceAbundance": 1.0,
    "playerRegenRate": 1.0
  },

  "spawning": {
    "waveSpawning": true,
    "waveInterval": 60,
    "maxConcurrentEnemies": 5,
    "spawnLocations": [
      { "x": 10, "y": 10 },
      { "x": 310, "y": 10 },
      { "x": 310, "y": 310 }
    ]
  },

  "playerStarting": {
    "position": { "x": 160, "y": 150 },
    "health": 100,
    "inventory": [],
    "equipment": {}
  },

  "boundaries": {
    "minX": 0,
    "minY": 0,
    "maxX": 3840,
    "maxY": 2560
  }
}
```

---

## 4. 🎯 level_XX/enemies.json (Enemy Spawn Config)

```json
{
  "levelId": 5,
  "enemySpawns": [
    {
      "id": "spawn_1",
      "enemyType": "wolf",
      "count": 1,
      "triggerTime": "night",
      "triggerNight": 1,
      "spawnLocation": { "x": 50, "y": 50 },
      "behaviour": "pursuit",
      "difficulty": 1,
      "loot": {
        "experience": 50,
        "currency": 10,
        "items": []
      }
    },
    {
      "id": "spawn_2",
      "enemyType": "wolf",
      "count": 2,
      "triggerTime": "night",
      "triggerNight": 2,
      "spawnLocation": "random_perimeter",
      "behaviour": "pack_tactics",
      "difficulty": 1
    },
    {
      "id": "spawn_3",
      "enemyType": "skeleton",
      "count": 1,
      "triggerTime": "night",
      "triggerNight": 4,
      "spawnLocation": "opposite_player",
      "behaviour": "aggressive",
      "difficulty": 2,
      "specialAbilities": ["ranged_attack", "regenerate"]
    }
  ]
}
```

---

## 5. 📦 level_XX/resources.json (Resource Placement)

```json
{
  "levelId": 5,
  "resourceSpawns": [
    {
      "id": "res_tree_1",
      "resourceType": "tree",
      "count": 20,
      "positions": [
        { "x": 30, "y": 40 },
        { "x": 50, "y": 60 },
        { "x": 70, "y": 45 }
      ],
      "harvestTime": 2,
      "yieldPerHarvest": 5,
      "respawnable": false,
      "maxReturns": 1
    },
    {
      "id": "res_rock_1",
      "resourceType": "rock",
      "count": 8,
      "positions": "distributed",
      "distributionArea": { "x": 0, "y": 0, "w": 600, "h": 600 },
      "harvestTime": 3,
      "yieldPerHarvest": 3,
      "respawnable": true,
      "respawnTime": 120
    },
    {
      "id": "res_grass_1",
      "resourceType": "grass",
      "count": 15,
      "positions": "scattered",
      "harvestTime": 1,
      "yieldPerHarvest": 2,
      "respawnable": true,
      "respawnTime": 60
    },
    {
      "id": "res_vegetable_1",
      "resourceType": "vegetable",
      "count": 5,
      "positions": "clustered",
      "harvestTime": 1.5,
      "yieldPerHarvest": 1,
      "respawnable": false
    }
  ]
}
```

---

## 6. 🎯 level_XX/objectives.json (Detailed Objectives)

```json
{
  "levelId": 5,
  "objectives": [
    {
      "id": "primary_survive",
      "type": "SURVIVAL",
      "title": "Survive 5 nights",
      "description": "Make it through 5 complete night cycles",
      "required": true,
      "target": 5,
      "unit": "nights",
      "progress": 0,
      "reward": 1000
    },
    {
      "id": "primary_gather",
      "type": "GATHER",
      "title": "Gather 50 wood",
      "description": "Collect a total of 50 wood resources",
      "required": true,
      "target": 50,
      "unit": "wood",
      "progress": 0,
      "reward": 500,
      "bonus": {
        "extraReward": 250,
        "condition": "gather_all_without_death"
      }
    },
    {
      "id": "primary_build",
      "type": "BUILD",
      "title": "Build 2 shelters",
      "description": "Construct 2 defensive shelters",
      "required": true,
      "target": 2,
      "unit": "structures",
      "progress": 0,
      "reward": 1000,
      "details": {
        "structureType": "shelter",
        "materials": { "wood": 20, "stone": 10 }
      }
    },
    {
      "id": "primary_combat",
      "type": "COMBAT",
      "title": "Defeat 10 enemies",
      "description": "Eliminate 10 enemy creatures",
      "required": true,
      "target": 10,
      "unit": "enemies",
      "progress": 0,
      "reward": 1000
    },
    {
      "id": "secondary_score",
      "type": "SCORE",
      "title": "Reach 5000 score",
      "description": "Accumulate a high score through combat and efficiency",
      "required": false,
      "target": 5000,
      "unit": "points",
      "progress": 0,
      "reward": 500
    },
    {
      "id": "challenge_flawless",
      "type": "CHALLENGE",
      "title": "Flawless Victory",
      "description": "Complete level without dying",
      "required": false,
      "target": 0,
      "unit": "deaths",
      "progress": 0,
      "reward": 1000,
      "difficulty": "hard"
    }
  ]
}
```

---

## 7. 💾 player_progress.json (Player Save File)

```json
{
  "playerId": "player_abc123",
  "playerName": "Hero",
  "createdAt": "2026-05-10T10:00:00Z",
  "lastPlayedAt": "2026-05-13T15:30:45Z",

  "progression": {
    "currentLevel": 5,
    "completedLevels": [1, 2, 3, 4],
    "unlockedLevels": [1, 2, 3, 4, 5, 6, 7],
    "nextLevelToPlay": 5
  },

  "statistics": {
    "totalPlayTime": 14400,
    "totalSessions": 25,
    "totalDeaths": 8,
    "totalEnemiesKilled": 45,
    "totalResourcesGathered": 450
  },

  "levelResults": {
    "1": {
      "completed": true,
      "completedAt": "2026-05-10T10:15:00Z",
      "attempts": 1,
      "score": 950,
      "stars": 3,
      "playTime": 285,
      "deaths": 0,
      "kills": 1,
      "resourcesGathered": 15
    },
    "2": {
      "completed": true,
      "completedAt": "2026-05-10T10:45:00Z",
      "attempts": 3,
      "score": 820,
      "stars": 2,
      "playTime": 450,
      "deaths": 2,
      "kills": 5,
      "resourcesGathered": 40
    },
    "3": {
      "completed": true,
      "attempts": 1,
      "score": 1050,
      "stars": 3,
      "playTime": 320
    },
    "4": {
      "completed": true,
      "attempts": 5,
      "score": 500,
      "stars": 1,
      "playTime": 600
    },
    "5": {
      "completed": false,
      "inProgress": true,
      "attempts": 2,
      "bestScore": 300,
      "bestStars": 0,
      "playTime": 150
    }
  },

  "inventory": {
    "items": [
      { "id": "basic_axe", "quantity": 1 },
      { "id": "stone_pickaxe", "quantity": 1 },
      { "id": "wood", "quantity": 25 },
      { "id": "stone", "quantity": 10 }
    ],
    "capacity": 20,
    "used": 4
  },

  "stats": {
    "totalExperience": 400,
    "playerLevel": 5,
    "currency": 285,
    "totalStars": 9,
    "totalStarsPossible": 12
  },

  "achievements": ["tutorial_complete", "first_kill", "ten_levels_completed"]
}
```

---

## 8. 🎯 difficulty_curves.json (Template)

```json
{
  "difficultyProgression": [
    {
      "levelRange": [1, 5],
      "zone": "Tutorial",
      "enemyHealthMult": 0.8,
      "enemyDamageMult": 0.8,
      "resourceAbundance": 1.5,
      "description": "Learning phase"
    },
    {
      "levelRange": [6, 12],
      "zone": "Beginner",
      "enemyHealthMult": 1.0,
      "enemyDamageMult": 1.0,
      "resourceAbundance": 1.2,
      "description": "Foundation building"
    },
    {
      "levelRange": [13, 20],
      "zone": "Intermediate",
      "enemyHealthMult": 1.3,
      "enemyDamageMult": 1.2,
      "resourceAbundance": 1.0,
      "description": "Challenge increases"
    },
    {
      "levelRange": [21, 30],
      "zone": "Advanced",
      "enemyHealthMult": 1.6,
      "enemyDamageMult": 1.5,
      "resourceAbundance": 0.9,
      "description": "High difficulty"
    },
    {
      "levelRange": [31, 50],
      "zone": "Expert",
      "enemyHealthMult": 2.0,
      "enemyDamageMult": 1.8,
      "resourceAbundance": 0.8,
      "description": "Extreme challenge"
    }
  ]
}
```

---

## 9. ✅ Checklist

- [ ] Create level_data.json with all levels
- [ ] Create folder structure for levels
- [ ] Create config.json for each level
- [ ] Create enemies.json for each level
- [ ] Create resources.json for each level
- [ ] Create objectives.json for each level
- [ ] Create difficulty_curves.json
- [ ] Create player_progress.json template
- [ ] Write JSON schema validator
- [ ] Test JSON parsing in LevelLoader
- [ ] Add level preview images

---

## 10. 🎯 JSON Schema Validation

```java
public class LevelDataValidator {

    public static void validateLevelData(Level level) {
        // Validate basic fields
        assert level.getId() > 0 : "Level ID must be positive";
        assert level.getName() != null : "Level name required";
        assert level.getDifficulty() > 0 : "Difficulty must be positive";

        // Validate objectives
        assert !level.getObjectives().isEmpty() : "At least one objective required";

        // Validate map
        assert level.getMapFileName() != null : "Map file required";

        // Validate progression
        assert level.getRewards() != null : "Rewards required";
    }
}
```

---

## Tóm Tắt

Cấu trúc dữ liệu cung cấp:

- ✅ Tối ưu hóa performance (JSON loading)
- ✅ Dễ dàng tạo/sửa levels
- ✅ Hỗ trợ future extensions
- ✅ Clear separation of concerns
- ✅ Standardized format
