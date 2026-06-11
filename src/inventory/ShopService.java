package inventory;

import core.GameBalance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopService {
    private static final String WOOD_FENCE_ITEM_ID = "wood_fence";
    private static final String WALL_ITEM_ALIAS = "wall";
    private static final String COIN_ITEM_ID = "coin";
    private static final String WOOD_WALL_ITEM_ID = "wood_wall";
    private static final String POTION_ITEM_ID = "potion";
    private static final String TORCH_ITEM_ID = "torch";
    private static final String ARCHER_TOWER_ITEM_ID = "archer_tower";
    private static final String FRIENDLY_ARCHER_ITEM_ID = "friendly_archer";
    private static final String BOMB_TRAP_ITEM_ID = "bomb_trap";
    private static final String CHEST_ITEM_ID = "chest";
    private static final String FIRE_BOMB_ITEM_ID = "fire_bomb";
    private static final String BASIC_SWORD_ITEM_ID = "basic_sword";
    private static final String PICKAXE_ITEM_ID = "pickaxe";
    private static final String CARROT_ITEM_ID = "carrot";
    private static final String AXE_ITEM_ID = "axe";

    public String normalizeShopItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "";
        }
        return switch (itemId.trim().toLowerCase()) {
            case "axe", "riu", "rÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬u" -> AXE_ITEM_ID;
            case WALL_ITEM_ALIAS -> WOOD_FENCE_ITEM_ID;
            case "wood_fence", "wood fence" -> WOOD_FENCE_ITEM_ID;
            case "cung", "archer_tower" -> ARCHER_TOWER_ITEM_ID;
            case "archer", "friendly_archer", "friendly archer" -> FRIENDLY_ARCHER_ITEM_ID;
            case "chest", "ruong", "ruong_do" -> CHEST_ITEM_ID;
            case "bomb", "bomb_trap", "bomb trap", "bom" -> BOMB_TRAP_ITEM_ID;
            case "fire_bomb", "fire bomb", "firebomb", "bom_lua", "bomb_fire" -> FIRE_BOMB_ITEM_ID;
            default -> itemId.trim().toLowerCase();
        };
    }

    public Map<String, Integer> resolvePurchaseCosts(String resolvedItemId) {
        return switch (resolvedItemId) {
            case WOOD_FENCE_ITEM_ID -> Map.of(COIN_ITEM_ID, GameBalance.WOOD_FENCE_PRICE);
            case WOOD_WALL_ITEM_ID -> Map.of(COIN_ITEM_ID, GameBalance.WOOD_WALL_PRICE);
            case POTION_ITEM_ID -> Map.of(COIN_ITEM_ID, 12);
            case TORCH_ITEM_ID -> Map.of(COIN_ITEM_ID, GameBalance.TORCH_PRICE);
            case ARCHER_TOWER_ITEM_ID -> Map.of("wood", 10, "stone", 10);
            case FRIENDLY_ARCHER_ITEM_ID -> Map.of(COIN_ITEM_ID, GameBalance.FRIENDLY_ARCHER_PRICE);
            case CHEST_ITEM_ID -> Map.of(COIN_ITEM_ID, GameBalance.CHEST_PRICE);
            case BOMB_TRAP_ITEM_ID -> Map.of(COIN_ITEM_ID, GameBalance.BOMB_TRAP_PRICE);
            case FIRE_BOMB_ITEM_ID -> Map.of(COIN_ITEM_ID, GameBalance.FIRE_BOMB_PRICE);
            case BASIC_SWORD_ITEM_ID -> Map.of(COIN_ITEM_ID, 18);
            case PICKAXE_ITEM_ID -> Map.of(COIN_ITEM_ID, 14);
            case CARROT_ITEM_ID -> Map.of(COIN_ITEM_ID, 3);
            default -> Map.of();
        };
    }

    public boolean hasEnoughResources(Inventory inventory, Map<String, Integer> purchaseCosts) {
        if (inventory == null || purchaseCosts == null || purchaseCosts.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, Integer> entry : purchaseCosts.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            int required = entry.getValue() == null ? 0 : Math.max(0, entry.getValue());
            if (inventory.getAmount(entry.getKey()) < required) {
                return false;
            }
        }
        return true;
    }

    public boolean consumePurchaseCosts(Inventory inventory, Map<String, Integer> purchaseCosts) {
        if (inventory == null || !hasEnoughResources(inventory, purchaseCosts)) {
            return false;
        }
        List<Map.Entry<String, Integer>> consumed = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : purchaseCosts.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            int amount = entry.getValue() == null ? 0 : Math.max(0, entry.getValue());
            if (amount <= 0) {
                continue;
            }
            if (!inventory.consumeItem(entry.getKey(), amount)) {
                for (Map.Entry<String, Integer> rollback : consumed) {
                    inventory.addItem(rollback.getKey(), rollback.getValue());
                }
                return false;
            }
            consumed.add(Map.entry(entry.getKey(), amount));
        }
        return true;
    }

    public void refundPurchaseCosts(Inventory inventory, Map<String, Integer> purchaseCosts) {
        if (inventory == null || purchaseCosts == null || purchaseCosts.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : purchaseCosts.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            int amount = entry.getValue() == null ? 0 : Math.max(0, entry.getValue());
            if (amount > 0) {
                inventory.addItem(entry.getKey(), amount);
            }
        }
    }
}
