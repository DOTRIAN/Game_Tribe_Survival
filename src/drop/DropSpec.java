package drop;

public class DropSpec {
    private final DropItemType type;
    private final int quantity;

    public DropSpec(DropItemType type, int quantity) {
        this.type = type == null ? DropItemType.UNKNOWN : type;
        this.quantity = Math.max(0, quantity);
    }

    public DropItemType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }
}
