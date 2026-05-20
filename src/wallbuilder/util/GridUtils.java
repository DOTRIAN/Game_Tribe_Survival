package wallbuilder.util;

/**
 * GridUtils:
 * - Utility quy doi pixel <-> tile.
 * - Dat 1 cho de tat ca he thong deu snap cung mot luoi.
 */
public final class GridUtils {
    public static final int TILE_SIZE = 48;

    private GridUtils() {
    }

    public static int snapToTile(double pixel) {
        return (int) Math.floor(pixel / TILE_SIZE);
    }

    public static double toPixel(int tileIndex) {
        return tileIndex * TILE_SIZE;
    }
}
