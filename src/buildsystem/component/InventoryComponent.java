package buildsystem.component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * InventoryComponent:
 * - Dung cho chest/furnace/workbench co kho item rieng.
 */
public class InventoryComponent implements BuildComponent {
    private final Map<String, Integer> items;

    public InventoryComponent() {
        this.items = new LinkedHashMap<>();
    }

    public Map<String, Integer> snapshot() {
        return new LinkedHashMap<>(items);
    }
}
