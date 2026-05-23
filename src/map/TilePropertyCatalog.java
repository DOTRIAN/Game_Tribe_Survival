package map;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * TilePropertyCatalog:
 * - Cung cap API de lay Tile Properties theo GID (gid tren layer map).
 * - Muc tieu: map team dat logic trong TSX thi game van doc duoc ma khong can Object Layer.
 *
 * Luu y:
 * - Tile properties nam theo localTileId trong TSX.
 * - Layer data dung GID, nen can buoc chuyen doi GID -> localTileId.
 */
public class TilePropertyCatalog {
    private final MapData mapData;

    public TilePropertyCatalog(MapData mapData) {
        this.mapData = mapData;
    }

    public Map<String, String> getPropertiesForGid(int gid) {
        TilesetData tileset = findTilesetForGid(gid);
        if (tileset == null) {
            return Collections.emptyMap();
        }
        int localId = gid - tileset.getFirstGid();
        return tileset.getPropertiesForLocalId(localId);
    }

    // True neu tile co key collision va value "on".
    public boolean isCollisionOn(int gid) {
        return hasOnValue(getPropertiesForGid(gid), "collision")
                || hasOnValue(getPropertiesForGid(gid), "Collision");
    }

    public boolean isTree(int gid) {
        Map<String, String> props = getPropertiesForGid(gid);
        // Tiled schema moi:
        // - Ho tro ca "tree:on" va "Tree:on"
        // - Ho tro nhan dien theo HP tile "Hp_tree > 0"
        if (hasOnValue(props, "tree") || hasOnValue(props, "Tree")) {
            return true;
        }
        int treeHp = parseInt(props.get("Hp_tree"), -1);
        return treeHp > 0;
    }

    public boolean isStone(int gid) {
        Map<String, String> props = getPropertiesForGid(gid);
        // Ho tro 2 kieu data:
        // 1) stone:on (contract cu/de xuat)
        // 2) stone_Hp/Hp_stone=... (data map team dang dung)
        if (hasOnValue(props, "stone") || hasOnValue(props, "Stone")) {
            return true;
        }
        int stoneHp = parseInt(props.get("Hp_stone"), -1);
        if (stoneHp <= 0) {
            stoneHp = parseInt(props.get("stone_Hp"), -1);
        }
        return stoneHp > 0;
    }

    public boolean isDa(int gid) {
        return hasOnValue(getPropertiesForGid(gid), "da");
    }

    public int getIntProperty(int gid, int fallback, String... keys) {
        Map<String, String> props = getPropertiesForGid(gid);
        for (String key : keys) {
            String raw = props.get(key);
            if (raw == null) {
                continue;
            }
            try {
                return Integer.parseInt(raw.trim());
            } catch (Exception ignored) {
                // try key tiep theo
            }
        }
        return fallback;
    }

    private boolean hasOnValue(Map<String, String> properties, String key) {
        String value = properties.get(key);
        return value != null && "on".equalsIgnoreCase(value.trim());
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private TilesetData findTilesetForGid(int gid) {
        if (gid <= 0 || mapData == null) {
            return null;
        }
        TilesetData chosen = null;
        for (TilesetData tileset : mapData.getTilesets()) {
            if (gid >= tileset.getFirstGid() && gid <= tileset.getLastGid()) {
                chosen = tileset;
            }
        }
        return chosen;
    }
}
