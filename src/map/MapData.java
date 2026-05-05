package map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapData {
    // Kich thuoc map theo tile.
    private final int widthInTiles;
    private final int heightInTiles;
    // Kich thuoc moi tile theo pixel.
    private final int tileWidth;
    private final int tileHeight;
    // Danh sach layer tile va object.
    private final List<TileLayerData> tileLayers;
    private final List<MapObjectData> collisionObjects;
    // Danh sach tileset da resolve xong path + image.
    private final List<TilesetData> tilesets;

    public MapData(int widthInTiles, int heightInTiles, int tileWidth, int tileHeight,
                   List<TileLayerData> tileLayers, List<MapObjectData> collisionObjects, List<TilesetData> tilesets) {
        this.widthInTiles = widthInTiles;
        this.heightInTiles = heightInTiles;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.tileLayers = new ArrayList<>(tileLayers);
        this.collisionObjects = new ArrayList<>(collisionObjects);
        this.tilesets = new ArrayList<>(tilesets);
    }

    public int getWidthInTiles() {
        return widthInTiles;
    }

    public int getHeightInTiles() {
        return heightInTiles;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getPixelWidth() {
        return widthInTiles * tileWidth;
    }

    public int getPixelHeight() {
        return heightInTiles * tileHeight;
    }

    public List<TileLayerData> getTileLayers() {
        return Collections.unmodifiableList(tileLayers);
    }

    public List<MapObjectData> getCollisionObjects() {
        return Collections.unmodifiableList(collisionObjects);
    }

    public List<TilesetData> getTilesets() {
        return Collections.unmodifiableList(tilesets);
    }

    public TileLayerData findLayerByName(String name) {
        for (TileLayerData layer : tileLayers) {
            if (layer.getName().equals(name)) {
                return layer;
            }
        }
        return null;
    }
}
