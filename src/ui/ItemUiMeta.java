package ui;

import javafx.scene.image.Image;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ItemUiMeta:
 * - Metadata UI cho item trong shop/inventory/hotbar.
 * - Tach rieng de ca Game va overlay deu co the dung cung thong tin hien thi.
 */
public class ItemUiMeta {
    private final String itemId;
    private final String displayName;
    private final String description;
    private final int price;
    private final String placeholderIconText;
    private final Image imageIcon;
    private final Map<String, Integer> purchaseCosts;

    public ItemUiMeta(String itemId,
                      String displayName,
                      String description,
                      int price,
                      String placeholderIconText,
                      Image imageIcon) {
        this(itemId, displayName, description, price, placeholderIconText, imageIcon, null);
    }

    public ItemUiMeta(String itemId,
                      String displayName,
                      String description,
                      int price,
                      String placeholderIconText,
                      Image imageIcon,
                      Map<String, Integer> purchaseCosts) {
        this.itemId = itemId;
        this.displayName = displayName;
        this.description = description;
        this.price = price;
        this.placeholderIconText = placeholderIconText;
        this.imageIcon = imageIcon;
        this.purchaseCosts = normalizePurchaseCosts(purchaseCosts, price);
    }

    public String getItemId() {
        return itemId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public String getPlaceholderIconText() {
        return placeholderIconText;
    }

    public Image getImageIcon() {
        return imageIcon;
    }

    public Map<String, Integer> getPurchaseCosts() {
        return purchaseCosts;
    }

    private Map<String, Integer> normalizePurchaseCosts(Map<String, Integer> costs, int fallbackPrice) {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        if (costs != null) {
            for (Map.Entry<String, Integer> entry : costs.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                int amount = entry.getValue() == null ? 0 : Math.max(0, entry.getValue());
                if (amount <= 0) {
                    continue;
                }
                normalized.put(entry.getKey().trim().toLowerCase(), amount);
            }
        }
        if (normalized.isEmpty() && fallbackPrice > 0) {
            normalized.put("coin", fallbackPrice);
        }
        return Collections.unmodifiableMap(normalized);
    }
}
