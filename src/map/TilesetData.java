package map;

import javafx.scene.image.Image;

public class TilesetData {
    // GID bat dau cua tileset trong map TMX.
    private final int firstGid;
    // So cot tile trong sprite sheet.
    private final int columns;
    // Kich thuoc tile (pixel) cua tileset.
    private final int tileWidth;
    private final int tileHeight;
    // So tile trong tileset de tinh range gid.
    private final int tileCount;
    // Anh tileset da load san de render nhanh.
    private final Image image;

    public TilesetData(int firstGid, int columns, int tileWidth, int tileHeight, int tileCount, Image image) {
        this.firstGid = firstGid;
        this.columns = columns;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.tileCount = tileCount;
        this.image = image;
    }

    public int getFirstGid() {
        return firstGid;
    }

    public int getLastGid() {
        return firstGid + tileCount - 1;
    }

    public int getColumns() {
        return columns;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public Image getImage() {
        return image;
    }
}
