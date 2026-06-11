package drop;

import core.GameBalance;
import entity.Entity;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DroppedItem:
 * - Item roi ngoai world khi build object bi pha.
 * - Player di qua se nhat vao inventory, khong can them inventory rieng tren map.
 */
public abstract class DroppedItem extends Entity {
    protected final DropItemType type;
    protected final int amount;
    protected final double renderWidth;
    protected final double renderHeight;

    public DroppedItem(DropItemType type, int amount, double x, double y) {
        this(type, amount, x, y, type == null ? GameBalance.DROPPED_ITEM_SIZE : type.getRenderWidth(), type == null ? GameBalance.DROPPED_ITEM_SIZE : type.getRenderHeight());
    }

    public DroppedItem(DropItemType type, int amount, double x, double y, double width, double height) {
        super(x, y, Math.max(4.0, width), Math.max(4.0, height), 0, 1);
        this.type = type == null ? DropItemType.UNKNOWN : type;
        this.amount = Math.max(1, amount);
        this.renderWidth = Math.max(4.0, width);
        this.renderHeight = Math.max(4.0, height);
    }

    public DropItemType getType() {
        return type;
    }

    public String getItemId() {
        return type.getInventoryItemId();
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

    public abstract Image getCurrentImage(long nowNs);

    public Map<String, Object> toSaveMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type.getId());
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
        Object typeValue = map.get("type");
        Object legacyItemIdValue = map.get("itemId");
        DropItemType type = typeValue instanceof String typeString
                ? DropItemType.fromId(typeString)
                : legacyItemIdValue instanceof String itemIdString
                ? DropItemType.fromId(itemIdString)
                : DropItemType.UNKNOWN;
        if (type == DropItemType.UNKNOWN) {
            return null;
        }
        Object amountValue = map.get("amount");
        Object xValue = map.get("x");
        Object yValue = map.get("y");
        Object widthValue = map.get("width");
        Object heightValue = map.get("height");
        int amount = amountValue instanceof Number amountNumber ? amountNumber.intValue() : 1;
        double x = xValue instanceof Number xNumber ? xNumber.doubleValue() : 0.0;
        double y = yValue instanceof Number yNumber ? yNumber.doubleValue() : 0.0;
        double width = widthValue instanceof Number widthNumber ? Math.max(widthNumber.doubleValue(), type.getRenderWidth()) : type.getRenderWidth();
        double height = heightValue instanceof Number heightNumber ? Math.max(heightNumber.doubleValue(), type.getRenderHeight()) : type.getRenderHeight();
        if (type == DropItemType.BOMB_TRAP) {
            return new BombDropItem(amount, x + width * 0.5, y + height * 0.5);
        }
        return new AnimatedDropItem(type, amount, x, y, width, height);
    }

    @Override
    public void triggerHitFlash(long nowNs, long durationNs, Color color) {
        // Dropped item khong can flash hit.
    }
}
