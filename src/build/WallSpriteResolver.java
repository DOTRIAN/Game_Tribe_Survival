package build;

import java.util.Map;

/**
 * WallSpriteResolver:
 * - Resolver moi:
 *   1) Chi dung 1 sprite thang goc `wall_straight_base`
 *   2) Chi dung 1 sprite goc goc `corner_base`
 * - Moi huong con lai se duoc renderer xoay anh, khong can cat `E/S/W` rieng nua.
 */
public class WallSpriteResolver {

    /**
     * resolve:
     * - Input: tile hien tai va danh sach wall da dat.
     * - Output: sprite key + rotation degrees + mask hang xom.
     * - Tac dong gameplay: auto-connect dung voi asset thuc te hien chi co 1 sprite thang va 1 sprite goc.
     */
    public WallSpriteSelection resolve(int tileX, int tileY, Map<String, Wall> wallsByTileKey, double preferredRotationDegrees) {
        boolean north = hasWall(tileX, tileY - 1, wallsByTileKey);
        boolean east = hasWall(tileX + 1, tileY, wallsByTileKey);
        boolean south = hasWall(tileX, tileY + 1, wallsByTileKey);
        boolean west = hasWall(tileX - 1, tileY, wallsByTileKey);

        int mask = (north ? 1 : 0)
                | (east ? 2 : 0)
                | (south ? 4 : 0)
                | (west ? 8 : 0);

        WallSpriteSelection selection = resolveSimple(north, east, south, west, mask, preferredRotationDegrees);
        System.out.println(
                "tileX=" + tileX
                        + " tileY=" + tileY
                        + " north=" + north
                        + " east=" + east
                        + " south=" + south
                        + " west=" + west
                        + " mask=" + mask
                        + " spriteKey=" + selection.getSpriteKey()
        );
        return selection;
    }

    private WallSpriteSelection resolveSimple(boolean north, boolean east, boolean south, boolean west, int mask, double preferredRotationDegrees) {
        // Corner:
        // - Sprite goc trong sheet duoc xem la truong hop East + South.
        // - Cac truong hop con lai se xoay tu base nay.
        // - Muc tieu la sua loi bo goc bi nguoc chieu so voi world hien tai.
        if (north && east && !south && !west) {
            return new WallSpriteSelection("corner_base", 270.0, mask);
        }
        if (!north && east && south && !west) {
            return new WallSpriteSelection("corner_base", 0.0, mask);
        }
        if (!north && !east && south && west) {
            return new WallSpriteSelection("corner_base", 90.0, mask);
        }
        if (north && !east && !south && west) {
            return new WallSpriteSelection("corner_base", 180.0, mask);
        }

        // Tường thẳng và tường đơn:
        // - KHONG auto-rotate theo hàng xóm.
        // - Chỉ dùng góc xoay mà player đã chọn bằng Q, hoặc góc xoay đang có của wall cũ.
        // - Điều này tránh tình trạng "chưa bấm Q mà tường đã tự xoay".
        // Tuong thang va tuong don:
        // - KHONG auto-rotate theo hang xom.
        // - Chi dung goc xoay ma player da chon bang Q, hoac goc xoay hien co cua wall cu.
        // - Muc tieu la tranh tinh trang "chua bam Q ma tuong da tu xoay".
        if (east || west || north || south) {
            return new WallSpriteSelection("wall_straight_base", preferredRotationDegrees, mask);
        }

        return new WallSpriteSelection("wall_straight_base", preferredRotationDegrees, mask);
    }

    private boolean hasWall(int tileX, int tileY, Map<String, Wall> wallsByTileKey) {
        return wallsByTileKey.containsKey(key(tileX, tileY));
    }

    private String key(int tileX, int tileY) {
        return tileX + ":" + tileY;
    }
}
