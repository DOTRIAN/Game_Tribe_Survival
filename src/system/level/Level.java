package system.level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Level:
 * - DTO/runtime model cho 1 level (doc tu resources/levels/level_data.json).
 * - Chiua metadata map + objective de Game va Renderer doc theo huong data-driven.
 *
 * Lien he voi hit/combat:
 * - Khong tinh sat thuong truc tiep.
 * - Sau moi lan combat/resource hit, Game cap nhat collectedResources va elapsedNs,
 *   roi goi Level.areObjectivesCompleted(...) de check win condition.
 */
public class Level {
    // Day la model toi gian cho 1 level trong file JSON.
    // Muc tieu la de data-driven:
    // sua objective/map trong JSON ma khong can hard-code them trong Game.
    private final int id;
    private final String name;
    private final String description;
    private final int difficulty;
    private final String mapFile;
    private final int requiredLevel;
    private final int unlocksLevel;
    private final List<LevelObjective> objectives;

    public Level(int id, String name, String description, int difficulty, String mapFile,
                 int requiredLevel, int unlocksLevel, List<LevelObjective> objectives) {
        this.id = id;
        this.name = name == null ? "Level " + id : name;
        this.description = description == null ? "" : description;
        this.difficulty = Math.max(1, difficulty);
        this.mapFile = mapFile == null ? "" : mapFile;
        this.requiredLevel = Math.max(0, requiredLevel);
        this.unlocksLevel = Math.max(0, unlocksLevel);
        this.objectives = objectives == null ? new ArrayList<>() : new ArrayList<>(objectives);
    }

    public static Level fromMap(Map<String, Object> raw) {
        // Parse 1 object JSON -> 1 Level runtime.
        int id = readInt(raw.get("id"), 0);
        String name = readString(raw.get("name"), "Level " + id);
        String description = readString(raw.get("description"), "");
        int difficulty = readInt(raw.get("difficulty"), 1);
        String mapFile = readString(raw.get("mapFile"), "assets/maps/mapdemo.tmx");
        int requiredLevel = readInt(raw.get("requiredLevel"), 0);
        int unlocksLevel = readInt(raw.get("unlocksLevel"), id + 1);

        // objectives trong JSON duoc parse rieng thay vi de raw object:
        // khi vao runtime thi Game chi lam viec voi LevelObjective typed object.
        List<LevelObjective> objectives = new ArrayList<>();
        Object rawObjectives = raw.get("objectives");
        if (rawObjectives instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> objectiveMap)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) objectiveMap;
                objectives.add(new LevelObjective(
                        readString(typed.get("type"), "COLLECT"),
                        readString(typed.get("item"), ""),
                        readInt(typed.get("amount"), 0),
                        readInt(typed.get("seconds"), 0),
                        readString(typed.get("description"), "")
                ));
            }
        }

        return new Level(id, name, description, difficulty, mapFile, requiredLevel, unlocksLevel, objectives);
    }

    public Map<String, Object> toMap() {
        List<Map<String, Object>> objectiveMaps = new ArrayList<>();
        for (LevelObjective objective : objectives) {
            objectiveMaps.add(Map.of(
                    "type", objective.getType(),
                    "item", objective.getItem(),
                    "amount", objective.getAmount(),
                    "seconds", objective.getSeconds(),
                    "description", objective.getDescription()
            ));
        }
        return Map.of(
                "id", id,
                "name", name,
                "description", description,
                "difficulty", difficulty,
                "mapFile", mapFile,
                "requiredLevel", requiredLevel,
                "unlocksLevel", unlocksLevel,
                "objectives", objectiveMaps
        );
    }

    public boolean areObjectivesCompleted(Map<String, Integer> collectedResources, long elapsedNs) {
        // Tat ca objective deu phai xong moi clear level.
        if (objectives.isEmpty()) {
            return false;
        }
        for (LevelObjective objective : objectives) {
            if (!objective.isCompleted(collectedResources, elapsedNs)) {
                return false;
            }
        }
        return true;
    }

    public String buildObjectiveStatus(Map<String, Integer> collectedResources, long elapsedNs) {
        if (objectives.isEmpty()) {
            return "No objectives";
        }

        // Chuoi status nay duoc dua len HUD/man header.
        // Ly do ghep thanh 1 dong:
        // - Renderer hien tai don gian, khong can them widget objective list rieng.
        List<String> lines = new ArrayList<>();
        for (LevelObjective objective : objectives) {
            String prefix = objective.isCompleted(collectedResources, elapsedNs) ? "[x] " : "[ ] ";
            lines.add(prefix + objective.getDisplayLabel() + " (" + objective.buildProgressText(collectedResources, elapsedNs) + ")");
        }
        return String.join(" | ", lines);
    }

    public String getPrimaryObjectiveLabel() {
        if (objectives.isEmpty()) {
            return "No objectives";
        }
        return objectives.get(0).getDisplayLabel();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public String getMapFile() {
        return mapFile;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public int getUnlocksLevel() {
        return unlocksLevel;
    }

    public List<LevelObjective> getObjectives() {
        return Collections.unmodifiableList(objectives);
    }

    private static int readInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue.trim());
            } catch (Exception ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String readString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }
}
