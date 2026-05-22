package inventory;

import buildsystem.core.BuildInventory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inventory:
 * - Luu toan bo vat pham/tai nguyen nguoi choi dang so huu trong world sinh ton.
 * - Class nay duoc tach rieng de Game khong phai quan ly map item thu cong nua.
 */
public class Inventory implements BuildInventory {
    // items:
    // - key: itemId (wood, stone, ...)
    // - value: so luong dang co
    private final Map<String, Integer> items;

    // Constructor:
    // - Khoi tao kho rong de su dung ngay khi tao world moi.
    public Inventory() {
        this.items = new LinkedHashMap<>();
    }

    // addItem:
    // - Input: itemId, amount can cong.
    // - Output: khong tra ve, cap nhat truc tiep state inventory.
    // - Tac dong: tang tai nguyen cho craft/xay dung/sinh ton.
    public void addItem(String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) {
            return;
        }
        itemId = normalizeItemId(itemId);
        items.put(itemId, items.getOrDefault(itemId, 0) + amount);
    }

    // consumeItem:
    // - Input: itemId, amount muon tru.
    // - Output: true neu tru thanh cong, false neu khong du.
    // - Tac dong: dam bao khong co tinh trang xai qua so luong hien co.
    public boolean consumeItem(String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) {
            return false;
        }
        itemId = normalizeItemId(itemId);
        int current = items.getOrDefault(itemId, 0);
        if (current < amount) {
            return false;
        }
        int next = current - amount;
        if (next == 0) {
            items.remove(itemId);
        } else {
            items.put(itemId, next);
        }
        return true;
    }

    // getAmount:
    // - Input: itemId can tra cuu.
    // - Output: so luong hien co cua item, mac dinh 0.
    public int getAmount(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return 0;
        }
        itemId = normalizeItemId(itemId);
        return items.getOrDefault(itemId, 0);
    }

    // snapshot:
    // - Output: ban sao read-only de HUD/save system doc du lieu an toan.
    public Map<String, Integer> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(items));
    }

    // restore:
    // - Input: du lieu item tu file save.
    // - Tac dong: phuc hoi inventory cho lan choi tiep theo.
    public void restore(Map<String, Integer> restoredItems) {
        items.clear();
        if (restoredItems == null) {
            return;
        }
        for (Map.Entry<String, Integer> entry : restoredItems.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            String itemId = normalizeItemId(entry.getKey());
            int value = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            if (value > 0) {
                items.put(itemId, items.getOrDefault(itemId, 0) + value);
            }
        }
    }

    private String normalizeItemId(String itemId) {
        String normalized = itemId == null ? "" : itemId.trim().toLowerCase();
        if ("stone_wall".equals(normalized)) {
            return "wood_fence";
        }
        return normalized;
    }
}
