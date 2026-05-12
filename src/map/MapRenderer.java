package map;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import system.resource.ResourceNode;

import java.util.ArrayList;
import java.util.List;

public class MapRenderer {
    private final MapData mapData;
    private List<ResourceNode> resources;

    public MapRenderer(MapData mapData) {
        this.mapData = mapData;
        this.resources = new ArrayList<>();
    }

    // Nhan danh sach resource runtime tu Renderer/Game moi frame.
    public void setResources(List<ResourceNode> resources) {
        if (resources == null) {
            this.resources = new ArrayList<>();
            return;
        }
        this.resources = resources;
    }

    // Render cac layer nam DUOI entity.
    // Ham nay duoc goi truoc khi ve player/wolf de tao "mat dat + vat the chan duoi".
    // Cac layer o day thuong la:
    // - Grounds: nen dat/co/duong
    // - Objects: than cay/da/props o tam thap
    public void renderBelowEntities(GraphicsContext gc, double cameraX, double cameraY, long now) {
        renderLayerByName(gc, "Grounds", cameraX, cameraY, now, false);
        renderLayerByName(gc, "Objects", cameraX, cameraY, now, true);
    }

    // Render cac layer nam TREN entity.
    // Ham nay duoc goi sau khi ve player/wolf de tao hieu ung bi che boi tan cay/mai nha.
    // Layer duoc dat o day la:
    // - Foreground: tan cay, mai, phan decor o cao do lon hon nhan vat
    public void renderAboveEntities(GraphicsContext gc, double cameraX, double cameraY, long now) {
        renderLayerByName(gc, "Foreground", cameraX, cameraY, now, true);
    }

    private void renderLayerByName(GraphicsContext gc, String layerName, double cameraX, double cameraY, long now, boolean hideDestroyedResourceTiles) {
        TileLayerData layer = mapData.findLayerByName(layerName);
        if (layer == null) {
            return;
        }

        int tileW = mapData.getTileWidth();
        int tileH = mapData.getTileHeight();

        // Duyet tung tile theo thu tu tren-xuong, trai-qua phai de giu logic ban do orthogonal.
        for (int y = 0; y < layer.getHeight(); y++) {
            for (int x = 0; x < layer.getWidth(); x++) {
                int gid = layer.getGidAt(x, y);
                if (gid == 0) {
                    // gid = 0 nghia la o trong, bo qua de tiet kiem draw call.
                    continue;
                }

                // Khi resource bi pha, an tile o layer Objects/Foreground giao voi hitbox cua resource do.
                // Muc tieu: "bien mat" tam thoi resource de thay duoc ket qua chat/dao.
                if (hideDestroyedResourceTiles) {
                    double tileWorldX = x * tileW;
                    double tileWorldY = y * tileH;
                    if (intersectsDestroyedResource(tileWorldX, tileWorldY, tileW, tileH)) {
                        continue;
                    }
                }

                TilesetData tileset = findTilesetForGid(gid);
                if (tileset == null) {
                    continue;
                }

                Image image = tileset.getImage();
                if (image == null || image.isError()) {
                    // Fallback debug mau neu file image bi loi.
                    gc.setFill(Color.DARKMAGENTA);
                    gc.fillRect(x * tileW - cameraX, y * tileH - cameraY, tileW, tileH);
                    continue;
                }

                int localId = gid - tileset.getFirstGid();
                localId = tileset.resolveLocalIdForTime(localId, now);
                int sourceX = (localId % tileset.getColumns()) * tileset.getTileWidth();
                int sourceY = (localId / tileset.getColumns()) * tileset.getTileHeight();
                // sourceX/sourceY: vi tri tile trong sprite sheet (toa do cat trong image nguon).
                // x*tileW, y*tileH: vi tri tile tren world map.
                // tru cameraX/cameraY de chuyen tu world space sang screen space.

                gc.drawImage(
                        image,
                        sourceX, sourceY, tileset.getTileWidth(), tileset.getTileHeight(),
                        x * tileW - cameraX, y * tileH - cameraY, tileW, tileH
                );
            }
        }
    }

    private TilesetData findTilesetForGid(int gid) {
        List<TilesetData> tilesets = mapData.getTilesets();
        TilesetData chosen = null;
        for (TilesetData tileset : tilesets) {
            if (gid >= tileset.getFirstGid() && gid <= tileset.getLastGid()) {
                chosen = tileset;
            }
        }
        return chosen;
    }

    private boolean intersectsDestroyedResource(double tileX, double tileY, double tileW, double tileH) {
        for (ResourceNode resource : resources) {
            if (resource == null || resource.isAlive()) {
                continue;
            }
            boolean hit = resource.getX() < tileX + tileW
                    && resource.getX() + resource.getWidth() > tileX
                    && resource.getY() < tileY + tileH
                    && resource.getY() + resource.getHeight() > tileY;
            if (hit) {
                return true;
            }
        }
        return false;
    }
}
