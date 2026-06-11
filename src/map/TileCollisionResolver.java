package map;

import system.resource.ResourceManager;
import system.resource.ResourceNode;
import system.resource.ResourceType;

import java.util.List;

/**
 * TileCollisionResolver:
 * - Xu ly collision theo Tile Properties (khong can Object Layer).
 * - Hoat dong doc lap voi Game, de Game chi can hoi "co bi block khong".
 *
 * Rule collision (cap nhat theo contract map team):
 * - CHI tile co collision:on moi block movement.
 * - tree/stone/da chi la nhan phan loai, khong tu dong block.
 * - Neu tile collision thuoc resource va resource da bi pha (dead) => cho phep di qua.
 */
public class TileCollisionResolver {
    private final MapData mapData;
    private final TilePropertyCatalog catalog;
    private final ResourceManager resourceManager;

    public TileCollisionResolver(MapData mapData, ResourceManager resourceManager) {
        this.mapData = mapData;
        this.catalog = new TilePropertyCatalog(mapData);
        this.resourceManager = resourceManager;
    }

    public boolean isBlocked(double x, double y, double w, double h) {
        if (mapData == null) {
            return false;
        }

        int tileW = mapData.getTileWidth();
        int tileH = mapData.getTileHeight();
        if (tileW <= 0 || tileH <= 0) {
            return false;
        }

        int minTileX = (int) Math.floor(x / tileW);
        int maxTileX = (int) Math.floor((x + w - 0.001) / tileW);
        int minTileY = (int) Math.floor(y / tileH);
        int maxTileY = (int) Math.floor((y + h - 0.001) / tileH);

        for (int ty = minTileY; ty <= maxTileY; ty++) {
            for (int tx = minTileX; tx <= maxTileX; tx++) {
                int gid = getTopVisualGidAt(tx, ty);
                if (gid <= 0) {
                    continue;
                }

                boolean collision = catalog.isCollisionOn(gid);
                if (!collision) {
                    continue;
                }

                boolean tree = catalog.isTree(gid);
                boolean stone = catalog.isStone(gid);

                // Neu tile collision nay thuoc resource va resource da bi pha => tile nay khong chan nua.
                if ((tree || stone) && isTileFreedByDestroyedResource(tx, ty, tileW, tileH, tree ? ResourceType.TREE : ResourceType.ROCK)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private int getTopVisualGidAt(int tileX, int tileY) {
        TileLayerData foreground = mapData.findLayerByName("Foreground");
        if (foreground == null) {
            // Fallback cho schema map moi dat ten "Foregrounds".
            foreground = mapData.findLayerByName("Foregrounds");
        }
        if (foreground != null) {
            int gid = foreground.getGidAt(tileX, tileY);
            if (gid > 0) {
                return gid;
            }
        }

        TileLayerData objects = mapData.findLayerByName("Objects");
        if (objects != null) {
            int gid = objects.getGidAt(tileX, tileY);
            if (gid > 0) {
                return gid;
            }
        }

        TileLayerData grounds = mapData.findLayerByName("Grounds");
        if (grounds != null) {
            return grounds.getGidAt(tileX, tileY);
        }
        return 0;
    }

    private boolean isTileFreedByDestroyedResource(int tileX, int tileY, int tileW, int tileH, ResourceType expectedType) {
        if (resourceManager == null) {
            return false;
        }

        double worldX = tileX * tileW;
        double worldY = tileY * tileH;
        List<ResourceNode> resources = resourceManager.getAllResources();
        for (ResourceNode node : resources) {
            if (node == null || node.isAlive()) {
                continue;
            }
            if (expectedType != null && node.getResourceType() != expectedType) {
                continue;
            }

            boolean intersects = node.getX() < worldX + tileW
                    && node.getX() + node.getWidth() > worldX
                    && node.getY() < worldY + tileH
                    && node.getY() + node.getHeight() > worldY;
            if (intersects) {
                return true;
            }
        }
        return false;
    }
}
