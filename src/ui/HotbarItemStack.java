package ui;

/**
 * HotbarItemStack:
 * - Du lieu 1 slot quickbar render o UI.
 * - Tach khoi BuildToolbar de hotbar co the hien item su dung chung, khong chi build item.
 */
public class HotbarItemStack {
    private final String itemId;
    private final int amount;
    private final ItemUiMeta meta;
    private final boolean buildItem;

    public HotbarItemStack(String itemId, int amount, ItemUiMeta meta, boolean buildItem) {
        this.itemId = itemId;
        this.amount = amount;
        this.meta = meta;
        this.buildItem = buildItem;
    }

    public String getItemId() {
        return itemId;
    }

    public int getAmount() {
        return amount;
    }

    public ItemUiMeta getMeta() {
        return meta;
    }

    public boolean isBuildItem() {
        return buildItem;
    }
}
