package system.resource;

/**
 * DropResult:
 * - Du lieu ket qua khi 1 resource bi pha.
 * - Chua itemId + so luong item roi ra.
 * - Class nho gon de sau nay de truyen cho InventorySystem/ItemEntitySystem.
 */
public class DropResult {
    private final String itemId;
    private final int amount;

    public DropResult(String itemId, int amount) {
        this.itemId = itemId == null ? "" : itemId;
        this.amount = Math.max(0, amount);
    }

    public String getItemId() {
        return itemId;
    }

    public int getAmount() {
        return amount;
    }
}

