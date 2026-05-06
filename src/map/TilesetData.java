package map;

import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

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
    // localTileId -> du lieu animation (neu tile do co animation trong TSX).
    private final Map<Integer, TileAnimationData> animations;

    public TilesetData(int firstGid, int columns, int tileWidth, int tileHeight, int tileCount, Image image,
                       Map<Integer, TileAnimationData> animations) {
        this.firstGid = firstGid;
        this.columns = columns;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.tileCount = tileCount;
        this.image = image;
        this.animations = new HashMap<>(animations);
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

    public int resolveLocalIdForTime(int localId, long nowNs) {
        TileAnimationData animationData = animations.get(localId);
        if (animationData == null) {
            return localId;
        }
        return animationData.resolveLocalId(nowNs);
    }

    public static final class TileAnimationData {
        private final int[] frameLocalIds;
        private final long[] frameDurationsNs;
        private final long totalDurationNs;

        public TileAnimationData(int[] frameLocalIds, long[] frameDurationsNs) {
            this.frameLocalIds = frameLocalIds;
            this.frameDurationsNs = frameDurationsNs;
            long total = 0L;
            for (long duration : frameDurationsNs) {
                total += duration;
            }
            this.totalDurationNs = Math.max(total, 1L);
        }

        private int resolveLocalId(long nowNs) {
            long t = nowNs % totalDurationNs;
            long acc = 0L;
            for (int i = 0; i < frameDurationsNs.length; i++) {
                acc += frameDurationsNs[i];
                if (t < acc) {
                    return frameLocalIds[i];
                }
            }
            return frameLocalIds[frameLocalIds.length - 1];
        }
    }
}
