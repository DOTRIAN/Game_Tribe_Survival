package buildsystem.core;

import buildsystem.component.CollisionComponent;
import buildsystem.object.BuildObject;
import entity.Entity;
import map.MapData;
import map.MapObjectData;
import map.TileCollisionResolver;
import map.TilePropertyCatalog;
import system.resource.ResourceManager;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

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
    private Supplier<? extends Collection<? extends Entity>> dynamicEntitySupplier;

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
        this.dynamicEntitySupplier = List::of;
    }

    public void setDynamicEntitySupplier(Supplier<? extends Collection<? extends Entity>> dynamicEntitySupplier) {
        this.dynamicEntitySupplier = dynamicEntitySupplier == null ? List::of : dynamicEntitySupplier;
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
            if (object.getComponent(CollisionComponent.class) == null) {
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

    @Override
    public boolean isBlockedByDynamicEntities(double x, double y, double width, double height) {
        Collection<? extends Entity> dynamicEntities = dynamicEntitySupplier.get();
        if (dynamicEntities == null || dynamicEntities.isEmpty()) {
            return false;
        }
        for (Entity entity : dynamicEntities) {
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            if (intersectsRect(x, y, width, height, entity.getX(), entity.getY(), entity.getWidth(), entity.getHeight())) {
                return true;
            }
        }
        return false;
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

    private boolean intersectsRect(double ax, double ay, double aw, double ah,
                                   double bx, double by, double bw, double bh) {
        return ax < bx + bw
                && ax + aw > bx
                && ay < by + bh
                && ay + ah > by;
    }
}
