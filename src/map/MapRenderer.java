package map;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import system.resource.ResourceNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapRenderer {
    private final MapData mapData;
    private final TilePropertyCatalog tilePropertyCatalog;
    private List<ResourceNode> resources;
    // Cache tile tint de tranh tao lai tung pixel moi frame.
    private final Map<String, Image> tintedTileCache;

    public MapRenderer(MapData mapData) {
        this.mapData = mapData;
        this.tilePropertyCatalog = new TilePropertyCatalog(mapData);
        this.resources = new ArrayList<>();
        this.tintedTileCache = new HashMap<>();
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
                    if (intersectsDestroyedResource(x, y, gid, tileWorldX, tileWorldY, tileW, tileH)) {
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

                // Neu tile nam trong resource vua bi danh, phu them lop trang mo de tao "nhay trang".
                if (hideDestroyedResourceTiles) {
                    double tileWorldX = x * tileW;
                    double tileWorldY = y * tileH;
                    ResourceNode flashingResource = findFlashingAliveResource(x, y, gid, tileWorldX, tileWorldY, tileW, tileH, now);
                    if (flashingResource != null) {
                        Image tintedTile = buildTintedTileImage(tileset, sourceX, sourceY, flashingResource.getHitFlashColor());
                        if (tintedTile != null) {
                            gc.save();
                            // Giam do dam flash trang cho nhe mat hon.
                            gc.setGlobalAlpha(0.58);
                            gc.drawImage(
                                    tintedTile,
                                    tileWorldX - cameraX, tileWorldY - cameraY, tileW, tileH
                            );
                            gc.restore();
                        }
                    }
                }
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

    private boolean intersectsDestroyedResource(int tileGridX, int tileGridY, int gid, double tileX, double tileY, double tileW, double tileH) {
        for (ResourceNode resource : resources) {
            if (resource == null || resource.isAlive()) {
                continue;
            }
            if (resource.hasVisualTiles()) {
                if (resource.containsVisualTile(tileGridX, tileGridY) && isMatchingResourceTile(resource, gid)) {
                    return true;
                }
                continue;
            }
            if (!isMatchingResourceTile(resource, gid)) {
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

    private ResourceNode findFlashingAliveResource(int tileGridX, int tileGridY, int gid, double tileX, double tileY, double tileW, double tileH, long nowNs) {
        for (ResourceNode resource : resources) {
            if (resource == null || !resource.isAlive() || !resource.isHitFlashActive(nowNs)) {
                continue;
            }
            if (resource.hasVisualTiles()) {
                if (resource.containsVisualTile(tileGridX, tileGridY) && isMatchingResourceTile(resource, gid)) {
                    return resource;
                }
                continue;
            }
            if (!isMatchingResourceTile(resource, gid)) {
                continue;
            }
            boolean hit = resource.getX() < tileX + tileW
                    && resource.getX() + resource.getWidth() > tileX
                    && resource.getY() < tileY + tileH
                    && resource.getY() + resource.getHeight() > tileY;
            if (hit) {
                return resource;
            }
        }
        return null;
    }

    private boolean isMatchingResourceTile(ResourceNode resource, int gid) {
        if (resource == null || gid <= 0) {
            return false;
        }
        if (resource.getResourceType() == system.resource.ResourceType.TREE) {
            return tilePropertyCatalog.isTree(gid);
        }
        if (resource.getResourceType() == system.resource.ResourceType.ROCK) {
            return tilePropertyCatalog.isStone(gid);
        }
        return true;
    }

    // Cat tile goc theo source rect roi tao ban tint theo alpha cua tile.
    // Cach nay giup flash "om shape" cua cay/da thay vi phu hinh vuong.
    private Image buildTintedTileImage(TilesetData tileset, int sourceX, int sourceY, Color tint) {
        if (tileset == null || tint == null) {
            return null;
        }
        Image srcImage = tileset.getImage();
        if (srcImage == null || srcImage.isError()) {
            return null;
        }

        int sw = tileset.getTileWidth();
        int sh = tileset.getTileHeight();
        if (sw <= 0 || sh <= 0) {
            return null;
        }

        int tintR = (int) Math.round(tint.getRed() * 255.0);
        int tintG = (int) Math.round(tint.getGreen() * 255.0);
        int tintB = (int) Math.round(tint.getBlue() * 255.0);
        String key = System.identityHashCode(srcImage) + ":" + sourceX + ":" + sourceY + ":" + tintR + ":" + tintG + ":" + tintB;
        Image cached = tintedTileCache.get(key);
        if (cached != null) {
            return cached;
        }

        PixelReader reader = srcImage.getPixelReader();
        if (reader == null) {
            return null;
        }
        int imageW = (int) srcImage.getWidth();
        int imageH = (int) srcImage.getHeight();
        WritableImage tinted = new WritableImage(sw, sh);
        PixelWriter writer = tinted.getPixelWriter();
        for (int py = 0; py < sh; py++) {
            for (int px = 0; px < sw; px++) {
                int sx = sourceX + px;
                int sy = sourceY + py;
                if (sx < 0 || sy < 0 || sx >= imageW || sy >= imageH) {
                    writer.setColor(px, py, Color.TRANSPARENT);
                    continue;
                }
                Color src = reader.getColor(sx, sy);
                double a = src.getOpacity();
                if (a <= 0.001) {
                    writer.setColor(px, py, Color.TRANSPARENT);
                    continue;
                }
                writer.setColor(px, py, Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), a));
            }
        }
        tintedTileCache.put(key, tinted);
        return tinted;
    }
}
