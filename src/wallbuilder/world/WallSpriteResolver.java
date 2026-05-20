package wallbuilder.world;

import java.util.Map;

/**
 * WallSpriteResolver:
 * - Chon sprite tường dua tren hang xom N/S/E/W.
 * - He thong chua can auto-wall hoan hao, nhung da duoc thiet ke san de mo rong.
 */
public class WallSpriteResolver {
    /**
     * resolveAssetKey:
     * - Input: vi tri wall va map tat ca wall hien co.
     * - Output: asset key phu hop.
     * - Tac dong gameplay: tuong tu doi sprite so bo theo huong noi.
     */
    public String resolveAssetKey(int row, int column, Map<String, Wall> wallsByTileKey) {
        boolean north = hasWall(row - 1, column, wallsByTileKey);
        boolean south = hasWall(row + 1, column, wallsByTileKey);
        boolean east = hasWall(row, column + 1, wallsByTileKey);
        boolean west = hasWall(row, column - 1, wallsByTileKey);

        int neighborCount = count(north) + count(south) + count(east) + count(west);
        if (neighborCount == 0) {
            return "stoneWall_S";
        }
        if (neighborCount == 1) {
            if (east) {
                return "stoneWallHalf_E";
            }
            if (west) {
                return "stoneWallHalf_W";
            }
            if (north) {
                return "stoneWallHalf_N";
            }
            return "stoneWallHalf_S";
        }
        if ((east || west) && !(north || south)) {
            return "stoneWall_E";
        }
        if ((north || south) && !(east || west)) {
            return "stoneWall_N";
        }
        if (north && east && !south && !west) {
            return "stoneWallRound_N";
        }
        if (east && south && !north && !west) {
            return "stoneWallRound_E";
        }
        if (south && west && !north && !east) {
            return "stoneWallRound_S";
        }
        if (west && north && !south && !east) {
            return "stoneWallRound_W";
        }

        // Fallback cho cac truong hop 3-4 huong de game van render duoc.
        if (east || west) {
            return "stoneWall_E";
        }
        return "stoneWall_S";
    }

    private boolean hasWall(int row, int column, Map<String, Wall> wallsByTileKey) {
        return wallsByTileKey.containsKey(key(row, column));
    }

    private String key(int row, int column) {
        return row + ":" + column;
    }

    private int count(boolean value) {
        return value ? 1 : 0;
    }
}
