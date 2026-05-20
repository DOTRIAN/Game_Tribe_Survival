package build;

/**
 * GridUtils:
 * - Gom cac ham quy doi screen/world pixel sang o tile.
 * - Dat rieng de BuildManager va CollisionManager dung cung mot cong thuc snap.
 */
public final class GridUtils {
    private GridUtils() {
    }

    /**
     * snapToTile:
     * - Input: toa do pixel theo 1 truc va tile size cua map.
     * - Output: index tile theo truc do.
     * - Tac dong gameplay: dam bao preview va tuong that nam khit tren luoi.
     */
    public static int snapToTile(double pixel, int tileSize) {
        if (tileSize <= 0) {
            return 0;
        }
        return (int) Math.floor(pixel / tileSize);
    }

    /**
     * toWorldPixel:
     * - Input: index tile va tile size.
     * - Output: toa do pixel world da snap theo luoi.
     */
    public static double toWorldPixel(int tileIndex, int tileSize) {
        return tileIndex * tileSize;
    }
}
