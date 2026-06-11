package system.save;

import drop.DroppedItem;
import inventory.Inventory;
import map.MapType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorldSnapshotHelper {
    private WorldSnapshotHelper() {
    }

    public static Map<String, Object> createSnapshot(MapType mapType,
                                                     double playerX,
                                                     double playerY,
                                                     int playerHp,
                                                     double playerEnergy,
                                                     int baseCampHp,
                                                     long elapsedNs,
                                                     Inventory inventory,
                                                     List<Map<String, Object>> buildObjects,
                                                     List<DroppedItem> droppedItems) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("mapType", mapType == null ? MapType.MAIN_MAP.name() : mapType.name());
        snapshot.put("playerX", playerX);
        snapshot.put("playerY", playerY);
        snapshot.put("playerHp", playerHp);
        snapshot.put("playerEnergy", playerEnergy);
        snapshot.put("baseCampHp", baseCampHp);
        snapshot.put("elapsedNs", Math.max(0L, elapsedNs));
        snapshot.put("inventory", WorldSaveService.buildInventorySnapshot(inventory));
        snapshot.put("buildObjects", buildObjects == null ? List.of() : buildObjects);
        snapshot.put("droppedItems", exportDroppedItems(droppedItems));
        return snapshot;
    }

    public static List<Map<String, Object>> exportDroppedItems(List<DroppedItem> droppedItems) {
        List<Map<String, Object>> snapshot = new ArrayList<>();
        if (droppedItems == null) {
            return snapshot;
        }
        for (DroppedItem droppedItem : droppedItems) {
            if (droppedItem != null) {
                snapshot.add(droppedItem.toSaveMap());
            }
        }
        return snapshot;
    }

    public static List<DroppedItem> restoreDroppedItems(Object rawValue) {
        List<DroppedItem> restored = new ArrayList<>();
        if (!(rawValue instanceof List<?> list)) {
            return restored;
        }
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            DroppedItem droppedItem = DroppedItem.fromSaveMap(map);
            if (droppedItem != null) {
                restored.add(droppedItem);
            }
        }
        return restored;
    }
}
