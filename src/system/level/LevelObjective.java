package system.level;

import java.util.Map;

/**
 * LevelObjective:
 * - Dinh nghia 1 objective don le trong level (COLLECT, SURVIVE).
 * - Chua logic isCompleted/progress de UI render trang thai objective.
 *
 * Lien he voi hit/combat:
 * - Khi nguoi choi danh trung resource (cay/da), Game tang collectedResources.
 * - isCompleted(...) se dung chinh so lieu nay de tinh objective COLLECT.
 */
public class LevelObjective {
    // Ho tro objective MVP:
    // - COLLECT: thu thap item
    // - SURVIVE: song sot theo thoi gian
    private final String type;
    private final String item;
    private final int amount;
    private final int seconds;
    private final String description;

    public LevelObjective(String type, String item, int amount, int seconds, String description) {
        this.type = type == null ? "COLLECT" : type.trim().toUpperCase();
        this.item = item == null ? "" : item.trim().toLowerCase();
        this.amount = Math.max(0, amount);
        this.seconds = Math.max(0, seconds);
        this.description = description == null ? "" : description.trim();
    }

    public String getType() {
        return type;
    }

    public String getItem() {
        return item;
    }

    public int getAmount() {
        return amount;
    }

    public int getSeconds() {
        return seconds;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted(Map<String, Integer> collectedResources, long elapsedNs) {
        if ("SURVIVE".equals(type)) {
            return elapsedNs >= seconds * 1_000_000_000L;
        }

        if ("COLLECT".equals(type)) {
            int current = collectedResources == null ? 0 : collectedResources.getOrDefault(item, 0);
            return current >= amount;
        }

        return false;
    }

    public String buildProgressText(Map<String, Integer> collectedResources, long elapsedNs) {
        // Chuoi nay duoc dua len UI de nguoi choi biet man da tien trien toi dau.
        if ("SURVIVE".equals(type)) {
            long elapsedSeconds = Math.max(0L, elapsedNs / 1_000_000_000L);
            long shown = Math.min(elapsedSeconds, seconds);
            return shown + "/" + seconds + "s";
        }

        if ("COLLECT".equals(type)) {
            int current = collectedResources == null ? 0 : collectedResources.getOrDefault(item, 0);
            return current + "/" + amount + " " + item;
        }

        return "";
    }

    public String getDisplayLabel() {
        // Neu JSON da co description thi uu tien xai description do
        // de level designer viet objective theo ngon ngu hien thi mong muon.
        if (!description.isEmpty()) {
            return description;
        }
        if ("SURVIVE".equals(type)) {
            return "Survive " + seconds + " seconds";
        }
        if ("COLLECT".equals(type)) {
            return "Collect " + amount + " " + item;
        }
        return type;
    }
}
