package build;

import entity.Player;
import map.MapData;
import map.MapObjectData;
import map.TileCollisionResolver;
import system.resource.ResourceManager;

import java.util.Collection;
import java.util.List;

/**
 * CollisionManager:
 * - Gom toan bo rule kiem tra dat wall va va cham voi wall runtime.
 * - Tiep nhan thong tin map/resource/player/wall de BuildManager chi tap trung vao flow build mode.
 */
public class CollisionManager {
    private final MapData mapData;
    private final List<MapObjectData> mapCollisions;
    private final TileCollisionResolver tileCollisionResolver;
    private final ResourceManager resourceManager;
    private final double worldWidth;
    private final double worldHeight;

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
    }

    /**
     * canPlaceWall:
     * - Input: tile muon dat wall, player hien tai va danh sach wall da dat.
     * - Output: true neu tile hop le.
     * - Tac dong gameplay: dieu khien preview xanh/do va chan dat tuong de len vat can.
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

        if (tileCollisionResolver != null && tileCollisionResolver.isBlocked(wallX, wallY, wallWidth, wallHeight)) {
            return false;
        }

        if (resourceManager != null && resourceManager.isBlockedByAliveResource(wallX, wallY, wallWidth, wallHeight)) {
            return false;
        }

        if (mapCollisions != null) {
            for (MapObjectData object : mapCollisions) {
                if (object == null) {
                    continue;
                }
                if ("Collision".equalsIgnoreCase(object.getType())
                        && object.intersects(wallX, wallY, wallWidth, wallHeight)) {
                    return false;
                }
            }
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

    /**
     * intersectsPlacedWall:
     * - Input: hitbox world-space cua player/quai va danh sach wall.
     * - Output: true neu hitbox cham wall.
     * - Tac dong gameplay: ngan player/quai di xuyen qua tuong vua dat.
     */
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

    public int getTileWidth() {
        return mapData == null ? 32 : Math.max(1, mapData.getTileWidth());
    }

    public int getTileHeight() {
        return mapData == null ? 32 : Math.max(1, mapData.getTileHeight());
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
