package ui;

import javafx.scene.image.Image;

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

    public ItemUiMeta(String itemId,
                      String displayName,
                      String description,
                      int price,
                      String placeholderIconText,
                      Image imageIcon) {
        this.itemId = itemId;
        this.displayName = displayName;
        this.description = description;
        this.price = price;
        this.placeholderIconText = placeholderIconText;
        this.imageIcon = imageIcon;
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
}
