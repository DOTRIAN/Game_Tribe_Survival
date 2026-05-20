package wallbuilder.item;

import javafx.scene.image.Image;

/**
 * BuildItem:
 * - Lop nen cho item co the dat xuong world.
 * - Tile footprint duoc luu o day de BuildManager dung cho preview va collision.
 */
public abstract class BuildItem implements Item {
    private final String id;
    private final String displayName;
    private final Image icon;
    private final int tileWidth;
    private final int tileHeight;

    protected BuildItem(String id, String displayName, Image icon, int tileWidth, int tileHeight) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Image getIcon() {
        return icon;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }
}
