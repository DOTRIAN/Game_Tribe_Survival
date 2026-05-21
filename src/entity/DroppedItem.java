package entity;

import core.GameBalance;
import javafx.scene.paint.Color;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DroppedItem:
 * - Item roi ngoai world khi build object bi pha.
 * - Player di qua se nhat vao inventory, khong can them inventory rieng tren map.
 */
public class DroppedItem extends Entity {
    private final String itemId;
    private final String spriteKey;
    private final int amount;
    private final double renderWidth;
    private final double renderHeight;

    public DroppedItem(String itemId, String spriteKey, int amount, double x, double y) {
        this(itemId, spriteKey, amount, x, y, GameBalance.DROPPED_ITEM_SIZE, GameBalance.DROPPED_ITEM_SIZE);
    }

    public DroppedItem(String itemId, String spriteKey, int amount, double x, double y, double width, double height) {
        super(x, y, Math.max(4.0, width), Math.max(4.0, height), 0, 1);
        this.itemId = itemId == null ? "" : itemId;
        this.spriteKey = spriteKey == null ? "" : spriteKey;
        this.amount = Math.max(1, amount);
        this.renderWidth = Math.max(4.0, width);
        this.renderHeight = Math.max(4.0, height);
    }

    public String getItemId() {
        return itemId;
    }

    public String getSpriteKey() {
        return spriteKey;
    }

    public int getAmount() {
        return amount;
    }

    public double getRenderWidth() {
        return renderWidth;
    }

    public double getRenderHeight() {
        return renderHeight;
    }

    public Map<String, Object> toSaveMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("itemId", itemId);
        map.put("spriteKey", spriteKey);
        map.put("amount", amount);
        map.put("x", x);
        map.put("y", y);
        map.put("width", renderWidth);
        map.put("height", renderHeight);
        return map;
    }

    public static DroppedItem fromSaveMap(Map<?, ?> map) {
        if (map == null) {
            return null;
        }
        Object itemId = map.get("itemId");
        Object spriteKey = map.get("spriteKey");
        if (!(itemId instanceof String itemIdString) || itemIdString.isBlank()) {
            return null;
        }
        String spriteKeyString = spriteKey instanceof String sprite ? sprite : "";
        Object amountValue = map.get("amount");
        Object xValue = map.get("x");
        Object yValue = map.get("y");
        Object widthValue = map.get("width");
        Object heightValue = map.get("height");
        int amount = amountValue instanceof Number amountNumber ? amountNumber.intValue() : 1;
        double x = xValue instanceof Number xNumber ? xNumber.doubleValue() : 0.0;
        double y = yValue instanceof Number yNumber ? yNumber.doubleValue() : 0.0;
        double width = widthValue instanceof Number widthNumber ? widthNumber.doubleValue() : GameBalance.DROPPED_ITEM_SIZE;
        double height = heightValue instanceof Number heightNumber ? heightNumber.doubleValue() : GameBalance.DROPPED_ITEM_SIZE;
        return new DroppedItem(itemIdString, spriteKeyString, amount, x, y, width, height);
    }

    @Override
    public void triggerHitFlash(long nowNs, long durationNs, Color color) {
        // Dropped item khong can flash hit.
    }
}
