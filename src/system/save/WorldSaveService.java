package system.save;

import inventory.Inventory;
import system.level.SimpleJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WorldSaveService:
 * - Luu/nap trang thai world sinh ton de nguoi choi vao lai choi tiep.
 */
public class WorldSaveService {
    private final Path savePath;

    // Constructor:
    // - Input: duong dan file save.
    public WorldSaveService(String savePath) {
        this.savePath = Path.of(savePath);
    }

    // save:
    // - Input: map du lieu world.
    // - Output: true neu luu thanh cong.
    // - Tac dong: ghi file json de persist session hien tai.
    public boolean save(Map<String, Object> snapshot) {
        try {
            Files.createDirectories(savePath.getParent());
            String json = SimpleJson.stringify(snapshot);
            Files.writeString(savePath, json, StandardCharsets.UTF_8);
            return true;
        } catch (IOException exception) {
            System.err.println("[WorldSaveService] Save failed: " + exception.getMessage());
            return false;
        }
    }

    // load:
    // - Output: map snapshot hoac null neu chua co save.
    // - Tac dong: phuc hoi du lieu world cho lan choi tiep theo.
    public Map<String, Object> load() {
        if (!Files.exists(savePath)) {
            return null;
        }
        try {
            String json = Files.readString(savePath, StandardCharsets.UTF_8);
            Object parsed = SimpleJson.parse(json);
            if (parsed instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) map;
                return typed;
            }
        } catch (Exception exception) {
            System.err.println("[WorldSaveService] Load failed: " + exception.getMessage());
        }
        return null;
    }

    // deleteSave:
    // - Output: true neu file save duoc xoa hoac von khong ton tai.
    // - Tac dong: xoa snapshot world tren disk de session sau bat dau tu trang thai sach.
    public boolean deleteSave() {
        try {
            return Files.deleteIfExists(savePath);
        } catch (IOException exception) {
            System.err.println("[WorldSaveService] Delete failed: " + exception.getMessage());
            return false;
        }
    }

    // toNumber:
    // - Input: value bat ky tu map parse.
    // - Output: so double an toan co fallback.
    public static double toDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }

    public static int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    public static long toLong(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return fallback;
    }

    // parseInventory:
    // - Input: block inventory da parse JSON.
    // - Output: map item -> amount.
    public static Map<String, Integer> parseInventory(Object value) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (!(value instanceof Map<?, ?> map)) {
            return result;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            int amount = toInt(entry.getValue(), 0);
            if (amount > 0) {
                result.put(key, amount);
            }
        }
        return result;
    }

    // buildInventorySnapshot:
    // - Input: inventory runtime.
    // - Output: map de serialize JSON.
    public static Map<String, Object> buildInventorySnapshot(Inventory inventory) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : inventory.snapshot().entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
