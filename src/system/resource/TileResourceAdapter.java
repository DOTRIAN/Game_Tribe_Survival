package system.resource;

import core.GameBalance;
import map.MapData;
import map.MapObjectData;
import map.TileLayerData;
import map.TilePropertyCatalog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TileResourceAdapter:
 * - Adapter map team "Tile Properties" -> danh sach MapObjectData runtime.
 * - Muc tieu: tai su dung ResourceManager hien co ma khong can viet lai he thong resource.
 *
 * Cach lam:
 * 1) Duyet tile layers co object visuals (Objects + Foreground/Foregrounds)
 * 2) Tim cac tile co nhan tree/stone
 * 3) Gom tile lien ke thanh cluster (4 huong)
 * 4) Moi cluster -> 1 MapObjectData gia lap (co x,y,w,h + properties gameplay)
 */
public class TileResourceAdapter {
    private static final int SYNTHETIC_ID_START = -100_000;

    public List<MapObjectData> buildResourceObjects(MapData mapData) {
        List<MapObjectData> result = new ArrayList<>();
        if (mapData == null) {
            return result;
        }

        TileLayerData objectsLayer = mapData.findLayerByName("Objects");
        TileLayerData foregroundLayer = findForegroundLayer(mapData);
        if (objectsLayer == null && foregroundLayer == null) {
            return result;
        }

        int width = mapData.getWidthInTiles();
        int height = mapData.getHeightInTiles();
        int tileW = mapData.getTileWidth();
        int tileH = mapData.getTileHeight();
        TilePropertyCatalog catalog = new TilePropertyCatalog(mapData);

        boolean[][] treeVisited = new boolean[height][width];
        boolean[][] stoneVisited = new boolean[height][width];
        int nextId = SYNTHETIC_ID_START;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gid = getTopVisualGidAt(x, y, objectsLayer, foregroundLayer);
                if (gid <= 0) {
                    continue;
                }

                if (!treeVisited[y][x] && catalog.isTree(gid)) {
                    Cluster cluster = floodFillCluster(x, y, "tree", mapData, objectsLayer, foregroundLayer, treeVisited, catalog);
                    if (!cluster.cells.isEmpty()) {
                        result.add(buildTreeObject(cluster, mapData, catalog, nextId--, tileW, tileH));
                    }
                }

                if (!stoneVisited[y][x] && catalog.isStone(gid)) {
                    Cluster cluster = floodFillCluster(x, y, "stone", mapData, objectsLayer, foregroundLayer, stoneVisited, catalog);
                    if (!cluster.cells.isEmpty()) {
                        result.add(buildStoneObject(cluster, mapData, catalog, nextId--, tileW, tileH));
                    }
                }
            }
        }
        return result;
    }

    private MapObjectData buildTreeObject(Cluster cluster, MapData mapData, TilePropertyCatalog catalog, int objectId, int tileW, int tileH) {
        int maxHp = 5;
        TileLayerData objectsLayer = mapData.findLayerByName("Objects");
        TileLayerData foregroundLayer = findForegroundLayer(mapData);
        for (Cell cell : cluster.cells) {
            int gid = getTopVisualGidAt(cell.x, cell.y, objectsLayer, foregroundLayer);
            // Uu tien schema Tiled moi, fallback schema cu.
            maxHp = Math.max(maxHp, catalog.getIntProperty(gid, 5, "Hp_tree", "Hp", "hp", "maxHp"));
        }

        // Drop mac dinh cho tree, su dung contract dang co cua ResourceManager.
        Map<String, String> props = new HashMap<>();
        props.put("kind", "tree_oak");
        props.put("maxHp", String.valueOf(maxHp));
        props.put("dropItem", "wood");
        props.put("dropMin", "1");
        props.put("dropMax", "3");
        props.put("visualTiles", toVisualTilesValue(cluster));

        return new MapObjectData(
                objectId,
                "TileTreeCluster",
                // Khong danh dau Collision o object adapter:
                // movement collision se do TileCollisionResolver (collision:on) quyet dinh.
                "Resource",
                cluster.minX * tileW,
                cluster.minY * tileH,
                (cluster.maxX - cluster.minX + 1) * tileW,
                (cluster.maxY - cluster.minY + 1) * tileH,
                props
        );
    }

    private MapObjectData buildStoneObject(Cluster cluster, MapData mapData, TilePropertyCatalog catalog, int objectId, int tileW, int tileH) {
        int maxHp = GameBalance.ROCK_HITS_TO_BREAK;
        TileLayerData objectsLayer = mapData.findLayerByName("Objects");
        TileLayerData foregroundLayer = findForegroundLayer(mapData);
        for (Cell cell : cluster.cells) {
            int gid = getTopVisualGidAt(cell.x, cell.y, objectsLayer, foregroundLayer);
            // Uu tien schema Tiled moi, fallback schema cu.
            maxHp = Math.max(maxHp, catalog.getIntProperty(gid, GameBalance.ROCK_HITS_TO_BREAK, "Hp_stone", "stone_Hp", "maxHp"));
        }

        Map<String, String> props = new HashMap<>();
        props.put("kind", "rock_small");
        props.put("maxHp", String.valueOf(maxHp));
        props.put("dropItem", "stone");
        props.put("dropMin", "1");
        props.put("dropMax", "2");
        props.put("visualTiles", toVisualTilesValue(cluster));

        return new MapObjectData(
                objectId,
                "TileStoneCluster",
                "Resource",
                cluster.minX * tileW,
                cluster.minY * tileH,
                (cluster.maxX - cluster.minX + 1) * tileW,
                (cluster.maxY - cluster.minY + 1) * tileH,
                props
        );
    }

    private Cluster floodFillCluster(int startX,
                                     int startY,
                                     String kind,
                                     MapData mapData,
                                     TileLayerData objectsLayer,
                                     TileLayerData foregroundLayer,
                                     boolean[][] visited,
                                     TilePropertyCatalog catalog) {
        Cluster cluster = new Cluster();
        int width = mapData.getWidthInTiles();
        int height = mapData.getHeightInTiles();

        ArrayDeque<Cell> queue = new ArrayDeque<>();
        queue.add(new Cell(startX, startY));
        visited[startY][startX] = true;

        while (!queue.isEmpty()) {
            Cell current = queue.pollFirst();
            int gid = getTopVisualGidAt(current.x, current.y, objectsLayer, foregroundLayer);
            if (gid <= 0) {
                continue;
            }

            boolean match = "tree".equals(kind) ? catalog.isTree(gid) : catalog.isStone(gid);
            if (!match) {
                continue;
            }

            cluster.add(current.x, current.y);

            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : dirs) {
                int nx = current.x + d[0];
                int ny = current.y + d[1];
                if (nx < 0 || ny < 0 || nx >= width || ny >= height || visited[ny][nx]) {
                    continue;
                }
                visited[ny][nx] = true;
                queue.add(new Cell(nx, ny));
            }
        }
        return cluster;
    }

    private int getTopVisualGidAt(int x, int y, TileLayerData objectsLayer, TileLayerData foregroundLayer) {
        int foregroundGid = foregroundLayer == null ? 0 : foregroundLayer.getGidAt(x, y);
        if (foregroundGid > 0) {
            return foregroundGid;
        }
        int objectGid = objectsLayer == null ? 0 : objectsLayer.getGidAt(x, y);
        if (objectGid > 0) {
            return objectGid;
        }
        return 0;
    }

    private TileLayerData findForegroundLayer(MapData mapData) {
        if (mapData == null) {
            return null;
        }
        TileLayerData layer = mapData.findLayerByName("Foreground");
        if (layer != null) {
            return layer;
        }
        return mapData.findLayerByName("Foregrounds");
    }

    private static final class Cell {
        private final int x;
        private final int y;

        private Cell(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class Cluster {
        private final List<Cell> cells;
        private int minX;
        private int minY;
        private int maxX;
        private int maxY;

        private Cluster() {
            this.cells = new ArrayList<>();
            this.minX = Integer.MAX_VALUE;
            this.minY = Integer.MAX_VALUE;
            this.maxX = Integer.MIN_VALUE;
            this.maxY = Integer.MIN_VALUE;
        }

        private void add(int x, int y) {
            cells.add(new Cell(x, y));
            if (x < minX) {
                minX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y > maxY) {
                maxY = y;
            }
        }
    }

    private String toVisualTilesValue(Cluster cluster) {
        if (cluster == null || cluster.cells.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cluster.cells.size(); i++) {
            Cell cell = cluster.cells.get(i);
            if (i > 0) {
                builder.append(';');
            }
            builder.append(cell.x).append(':').append(cell.y);
        }
        return builder.toString();
    }
}
