package wallbuilder.model;

/**
 * HotbarItem:
 * - Dinh nghia cac vat pham co the dat trong hotbar.
 * - Muc tieu la giu du lieu don gian, de sinh vien de doc va de mo rong.
 */
public enum HotbarItem {
    EMPTY("Trong", ""),
    SMALL_STONE("Da nho", "\uD83E\uDEA8"),
    STONE_WALL("Tuong da", "\uD83E\uDDF1");

    private final String displayName;
    private final String iconText;

    HotbarItem(String displayName, String iconText) {
        this.displayName = displayName;
        this.iconText = iconText;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconText() {
        return iconText;
    }
}
