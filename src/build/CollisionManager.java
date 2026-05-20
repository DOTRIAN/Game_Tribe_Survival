package build;

import buildsystem.core.BuildWorldQuery;
import buildsystem.object.BuildObject;
import entity.Player;
import map.MapData;
import map.MapObjectData;
import map.TileCollisionResolver;
import map.TilePropertyCatalog;
import system.resource.ResourceManager;

import java.util.Collection;
import java.util.List;

/**
 * CollisionManager:
 * - Adapter runtime cua game hien tai cho BuildSystem moi.
 * - PlacementValidator goi BuildWorldQuery thay vi biet chi tiet map/resource/tile collision.
 */
public class CollisionManager implements BuildWorldQuery {
    private final MapData mapData;
    private final List<MapObjectData> mapCollisions;
    private final TileCollisionResolver tileCollisionResolver;
    private final ResourceManager resourceManager;
    private final double worldWidth;
    private final double worldHeight;
    private final TilePropertyCatalog tilePropertyCatalog;

    public CollisionManager(MapData mapData,
                            List<MapObjectData> mapCollisions,
                            TileCollisionResolver tileCollisionResolver,
                            ResourceManager resourceManager,
                            double worldWidth,
                            double worldHeight) {
        this.mapData = mapData;
        this.mapCollisions = mapCollisions;
        this.tileCollisionResolver = tileCollisionResolver;
        this.resourceManager = resourceManager;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.tilePropertyCatalog = mapData == null ? null : new TilePropertyCatalog(mapData);
    }

    /**
     * intersectsPlacedBuildObject:
     * - Input: hitbox world-space cua player/quai va danh sach build object.
     * - Output: true neu hitbox cham vao object co collision.
     */
    public boolean intersectsPlacedBuildObject(double x,
                                               double y,
                                               double width,
                                               double height,
                                               Collection<BuildObject> placedObjects) {
        if (placedObjects == null || placedObjects.isEmpty()) {
            return false;
        }
        for (BuildObject object : placedObjects) {
            if (object == null) {
                continue;
            }
            if (intersectsRect(
                    x,
                    y,
                    width,
                    height,
                    object.getRenderX(),
                    object.getRenderY(),
                    object.getCollisionWidth(),
                    object.getCollisionHeight())) {
                return true;
            }
        }
        return false;
    }

    /**
     * API cu de giu tuong thich cho nhung noi chua refactor xong.
     */
    public boolean canPlaceWall(int tileX,
                                int tileY,
                                Player player,
                                Collection<Wall> placedWalls) {
        int tileWidth = getTileWidth();
        int tileHeight = getTileHeight();
        double wallX = GridUtils.toWorldPixel(tileX, tileWidth);
        double wallY = GridUtils.toWorldPixel(tileY, tileHeight);
        double wallWidth = tileWidth;
        double wallHeight = tileHeight;

        if (wallX < 0 || wallY < 0 || wallX + wallWidth > worldWidth || wallY + wallHeight > worldHeight) {
            return false;
        }
        if (isBlockedByTerrain(wallX, wallY, wallWidth, wallHeight)) {
            return false;
        }
        if (isBlockedByWater(wallX, wallY, wallWidth, wallHeight)) {
            return false;
        }
        if (isBlockedByStaticObjects(wallX, wallY, wallWidth, wallHeight)) {
            return false;
        }
        if (player != null && intersectsPlayer(player, wallX, wallY, wallWidth, wallHeight)) {
            return false;
        }

        if (placedWalls != null) {
            for (Wall wall : placedWalls) {
                if (wall == null) {
                    continue;
                }
                if (intersectsRect(
                        wall.getRenderX(),
                        wall.getRenderY(),
                        wall.getWidth(),
                        wall.getHeight(),
                        wallX,
                        wallY,
                        wallWidth,
                        wallHeight)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean intersectsPlacedWall(double x, double y, double width, double height, Collection<Wall> placedWalls) {
        if (placedWalls == null || placedWalls.isEmpty()) {
            return false;
        }
        for (Wall wall : placedWalls) {
            if (wall == null) {
                continue;
            }
            if (intersectsRect(x, y, width, height, wall.getRenderX(), wall.getRenderY(), wall.getWidth(), wall.getHeight())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getTileWidth() {
        return mapData == null ? 32 : Math.max(1, mapData.getTileWidth());
    }

    @Override
    public int getTileHeight() {
        return mapData == null ? 32 : Math.max(1, mapData.getTileHeight());
    }

    @Override
    public double getWorldWidth() {
        return worldWidth;
    }

    @Override
    public double getWorldHeight() {
        return worldHeight;
    }

    @Override
    public boolean isBlockedByTerrain(double x, double y, double width, double height) {
        if (tileCollisionResolver != null && tileCollisionResolver.isBlocked(x, y, width, height)) {
            return true;
        }

        if (resourceManager != null && resourceManager.isBlockedByAliveResource(x, y, width, height)) {
            return true;
        }

        if (mapCollisions != null) {
            for (MapObjectData object : mapCollisions) {
                if (object == null) {
                    continue;
                }
                if ("Collision".equalsIgnoreCase(object.getType())
                        && object.intersects(x, y, width, height)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isBlockedByWater(double x, double y, double width, double height) {
        if (mapData == null || tilePropertyCatalog == null) {
            return false;
        }
        int tileW = getTileWidth();
        int tileH = getTileHeight();
        int minTileX = (int) Math.floor(x / tileW);
        int maxTileX = (int) Math.floor((x + width - 0.001) / tileW);
        int minTileY = (int) Math.floor(y / tileH);
        int maxTileY = (int) Math.floor((y + height - 0.001) / tileH);
        for (int ty = minTileY; ty <= maxTileY; ty++) {
            for (int tx = minTileX; tx <= maxTileX; tx++) {
                int gid = getTopVisualGidAt(tx, ty);
                if (gid <= 0) {
                    continue;
                }
                String water = tilePropertyCatalog.getPropertiesForGid(gid).get("water");
                if (water != null && "on".equalsIgnoreCase(water.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isBlockedByStaticObjects(double x, double y, double width, double height) {
        if (x < 0 || y < 0 || x + width > worldWidth || y + height > worldHeight) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isFlatTerrain(double x, double y, double width, double height) {
        // Runtime hien tai chua co data do doc cua tile.
        // Hook nay ton tai san de machine/workbench/farm co the bo sung rule "nen phang" sau nay.
        return !isBlockedByTerrain(x, y, width, height);
    }

    private int getTopVisualGidAt(int tileX, int tileY) {
        if (mapData == null) {
            return 0;
        }
        if (mapData.findLayerByName("Foreground") != null) {
            int gid = mapData.findLayerByName("Foreground").getGidAt(tileX, tileY);
            if (gid > 0) {
                return gid;
            }
        }
        if (mapData.findLayerByName("Objects") != null) {
            int gid = mapData.findLayerByName("Objects").getGidAt(tileX, tileY);
            if (gid > 0) {
                return gid;
            }
        }
        if (mapData.findLayerByName("Grounds") != null) {
            return mapData.findLayerByName("Grounds").getGidAt(tileX, tileY);
        }
        return 0;
    }

    private boolean intersectsPlayer(Player player, double wallX, double wallY, double wallWidth, double wallHeight) {
        double px = player.getX() + player.getWidth() * 0.22;
        double py = player.getY() + player.getHeight() * 0.30;
        double pw = player.getWidth() * 0.56;
        double ph = player.getHeight() * 0.62;
        return intersectsRect(px, py, pw, ph, wallX, wallY, wallWidth, wallHeight);
    }

    private boolean intersectsRect(double ax, double ay, double aw, double ah,
                                   double bx, double by, double bw, double bh) {
        return ax < bx + bw
                && ax + aw > bx
                && ay < by + bh
                && ay + ah > by;
    }
}
