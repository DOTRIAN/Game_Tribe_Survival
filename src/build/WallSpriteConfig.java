package build;

import java.util.List;

/**
 * WallSpriteConfig:
 * - Cua so cau hinh DUY NHAT cho tat ca region crop tu tuong.jpg.
 * - Muon chinh crop khit hon thi sua toa do tai day, khong phai sua AssetManager/Renderer.
 *
 * Luu y:
 * - Cac toa do duoi day duoc dat theo kich thuoc sheet that 1376x768.
 * - Ten cu (stoneWall_*) duoc giu lai de hotbar va code cu van goi duoc.
 * - Ten moi (wall_*) duoc dung boi WallSpriteResolver top-down.
 */
public final class WallSpriteConfig {
    private WallSpriteConfig() {
    }

    public static final String SHEET_FILE_NAME = "tuong.jpg";

    // Tile map hien tai dang dung 16x16 theo map.tmx.
    // Wall render duoc giu khop 1 o tile de preview va collision khong lech nhau.
    public static final double WALL_RENDER_WIDTH = 16.0;
    public static final double WALL_RENDER_HEIGHT = 16.0;
    public static final double WALL_ANCHOR_X = 0.0;
    public static final double WALL_ANCHOR_Y = 0.0;

    /**
     * getRegions:
     * - Tra ve toan bo sprite can crop tu sprite sheet.
     * - Bao gom:
     *   1) Ten cu de tuong thich code cu.
     *   2) Ten moi cho resolver top-down.
     */
    public static List<SpriteRegion> getRegions() {
        return List.of(
                // ===== Alias ten cu de code cu van goi duoc =====
                // Chi dung 1 sprite goc cho wall thang.
                // Cac key E/S/W duoc map cung region de giu tuong thich code cu, nhung renderer se rotate.
                new SpriteRegion("stoneWall_N", 252, 8, 140, 78),
                new SpriteRegion("stoneWall_E", 252, 8, 140, 78),
                new SpriteRegion("stoneWall_S", 252, 8, 140, 78),
                new SpriteRegion("stoneWall_W", 252, 8, 140, 78),

                new SpriteRegion("stoneWallHalf_N", 252, 131, 140, 61),
                new SpriteRegion("stoneWallHalf_E", 481, 131, 140, 61),
                new SpriteRegion("stoneWallHalf_S", 734, 131, 140, 61),
                new SpriteRegion("stoneWallHalf_W", 989, 131, 140, 61),

                // Chi dung 1 sprite goc base roi xoay.
                new SpriteRegion("stoneWallRound_N", 733, 438, 136, 84),
                new SpriteRegion("stoneWallRound_E", 733, 438, 136, 84),
                new SpriteRegion("stoneWallRound_S", 733, 438, 136, 84),
                new SpriteRegion("stoneWallRound_W", 733, 438, 136, 84),

                // ===== Sprite top-down dung cho gameplay moi =====
                // Luu y quan trong:
                // - Ban truoc da dung cac sprite dai/thon o hang duoi, nen khi fit vao tile 16x16 thi bi co thanh "manh vo".
                // - O day uu tien sprite vuong/hop day dan hon de dat trong 1 o tile ma van nhin ro.
                // Cat bo 1 it phan tren de tranh vien den mo xuat hien khi render/rotate.
                new SpriteRegion("wall_straight_base", 252, 8, 140, 78),
                new SpriteRegion("wall_single", 252, 8, 140, 78),
                new SpriteRegion("wall_horizontal", 252, 8, 140, 78),
                new SpriteRegion("wall_vertical", 252, 8, 140, 78),

                // Corner:
                // - Dung nhom corner top-down o giua sheet.
                // - Dat ten don gian de resolver de doc hon.
                // Cat bo phan vien den o phia tren cua sprite goc.
                new SpriteRegion("corner_base", 733, 438, 136, 84),
                new SpriteRegion("corner_NE", 733, 438, 136, 84),
                new SpriteRegion("corner_ES", 733, 438, 136, 84),
                new SpriteRegion("corner_SW", 733, 438, 136, 84),
                new SpriteRegion("corner_WN", 733, 438, 136, 84),

                // T-junction va cross tam thoi alias ve sprite day de tranh vo hinh.
                // Khi co art rieng co the thay bang region moi ma khong can sua resolver nhieu.
                new SpriteRegion("wall_t_n", 252, 8, 140, 78),
                new SpriteRegion("wall_t_e", 252, 8, 140, 78),
                new SpriteRegion("wall_t_s", 252, 8, 140, 78),
                new SpriteRegion("wall_t_w", 252, 8, 140, 78),
                new SpriteRegion("wall_cross", 252, 8, 140, 78),

                // End pieces tam thoi cung dung sprite day dan hon de tranh hien thanh net mong.
                new SpriteRegion("wall_end_n", 252, 8, 140, 78),
                new SpriteRegion("wall_end_s", 252, 8, 140, 78),
                new SpriteRegion("wall_end_e", 252, 8, 140, 78),
                new SpriteRegion("wall_end_w", 252, 8, 140, 78),

                // Icon rieng cho hotbar: uu tien 1 sprite de nhin ro nhat trong o nho.
                new SpriteRegion("wall_icon", 252, 8, 140, 78)
        );
    }
}
