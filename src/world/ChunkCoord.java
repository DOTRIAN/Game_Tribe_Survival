package world;

import java.util.Objects;

/**
 * ChunkCoord:
 * - Toa do chunk trong world vo han theo he truc chunk (khong phai pixel).
 */
public class ChunkCoord {
    public final int x;
    public final int y;

    // Constructor:
    // - Input: chunkX, chunkY.
    public ChunkCoord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChunkCoord that)) {
            return false;
        }
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
