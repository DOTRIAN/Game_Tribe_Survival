package system.level;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LevelResult:
 * - Snapshot ket qua mot lan clear level: score, sao, deaths, playTime.
 * - Duoc dung de hien man LEVEL_COMPLETE va luu vao player_progress.json.
 *
 * Lien he voi hit/combat:
 * - So luong tai nguyen nhat duoc (he qua tu viec hit resource) di vao createForCompletion(...)
 *   qua tham so collectedCount de tinh diem.
 */
public class LevelResult {
    // LevelResult la snapshot ket qua cua 1 lan clear level.
    // Hien tai du lieu nay duoc dung cho:
    // - unlock level tiep theo
    // - hien overlay LEVEL_COMPLETE
    // - save/load tien do
    private final int levelId;
    private final int score;
    private final int stars;
    private final int deaths;
    private final long playTimeSeconds;
    private final String completedAt;

    public LevelResult(int levelId, int score, int stars, int deaths, long playTimeSeconds, String completedAt) {
        this.levelId = levelId;
        this.score = Math.max(0, score);
        this.stars = Math.max(0, Math.min(3, stars));
        this.deaths = Math.max(0, deaths);
        this.playTimeSeconds = Math.max(0L, playTimeSeconds);
        this.completedAt = completedAt == null || completedAt.isBlank() ? LocalDateTime.now().toString() : completedAt;
    }

    public static LevelResult createForCompletion(Level level, long playTimeSeconds, int deaths, int collectedCount) {
        // Scoring hien tai la MVP:
        // - thu thap nhieu hon -> diem nen cao hon
        // - clear nhanh -> co speed bonus
        // - khong chet -> co bonus them
        int baseScore = Math.max(100, collectedCount * 100);
        int speedBonus = (int) Math.max(0L, 300L - playTimeSeconds * 3L);
        int score = baseScore + speedBonus + (deaths == 0 ? 150 : 0);
        int stars = playTimeSeconds <= 45 ? 3 : playTimeSeconds <= 90 ? 2 : 1;
        if (deaths > 0) {
            stars = Math.max(1, stars - 1);
        }
        return new LevelResult(level.getId(), score, stars, deaths, playTimeSeconds, LocalDateTime.now().toString());
    }

    public static LevelResult fromMap(int levelId, Map<String, Object> raw) {
        // Ho tro load tu JSON save.
        return new LevelResult(
                levelId,
                readInt(raw.get("score"), 0),
                readInt(raw.get("stars"), 0),
                readInt(raw.get("deaths"), 0),
                readLong(raw.get("playTimeSeconds"), readLong(raw.get("playTime"), 0L)),
                readString(raw.get("completedAt"), LocalDateTime.now().toString())
        );
    }

    public Map<String, Object> toMap() {
        // JSON shape duoc giu phang, de de inspect va debug file save.
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("completed", true);
        data.put("score", score);
        data.put("stars", stars);
        data.put("deaths", deaths);
        data.put("playTimeSeconds", playTimeSeconds);
        data.put("completedAt", completedAt);
        return data;
    }

    public int getLevelId() {
        return levelId;
    }

    public int getScore() {
        return score;
    }

    public int getStars() {
        return stars;
    }

    public int getDeaths() {
        return deaths;
    }

    public long getPlayTimeSeconds() {
        return playTimeSeconds;
    }

    public String getCompletedAt() {
        return completedAt;
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

    private static long readLong(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue.trim());
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
