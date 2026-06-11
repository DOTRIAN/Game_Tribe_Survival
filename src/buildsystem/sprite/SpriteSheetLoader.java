package buildsystem.sprite;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SpriteSheetLoader:
 * - Load 1 sprite sheet va cat nhieu sprite con tu sheet do.
 * - Class nay duoc tao rieng vi game hien tai khong con load tung file wall_*.png nua.
 */
public final class SpriteSheetLoader {
    private SpriteSheetLoader() {
    }

    /**
     * crop:
     * - Input: image goc + toa do region trong sprite sheet.
     * - Output: 1 Image moi chua dung sprite da cat.
     * - Tac dong gameplay: cho phep 1 file tuong.jpg phuc vu ca hotbar va world wall.
     */
    public static Image crop(Image sheet, int x, int y, int width, int height) {
        if (sheet == null || sheet.isError()) {
            throw new IllegalArgumentException("sheet must be a valid image");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("crop size must be positive");
        }

        PixelReader pixelReader = sheet.getPixelReader();
        if (pixelReader == null) {
            throw new IllegalStateException("sheet pixel reader is null");
        }
        return new WritableImage(pixelReader, x, y, width, height);
    }

    /**
     * cropAll:
     * - Input: sheet goc va danh sach region can cat.
     * - Output: Map key -> Image da cat.
     * - Tac dong gameplay: AssetManager goi 1 lan, sau do cache lai cho moi he thong.
     */
    public static Map<String, Image> cropAll(Image sheet, List<SpriteRegion> regions) {
        Map<String, Image> sprites = new LinkedHashMap<>();
        if (sheet == null || regions == null) {
            return sprites;
        }
        for (SpriteRegion region : regions) {
            if (region == null || region.getName() == null || region.getName().isBlank()) {
                continue;
            }
            Image image = crop(sheet, region.getX(), region.getY(), region.getWidth(), region.getHeight());
            sprites.put(region.getName(), image);
            System.out.println(
                    "Loaded sprite: " + region.getName()
                            + " x=" + region.getX()
                            + " y=" + region.getY()
                            + " w=" + region.getWidth()
                            + " h=" + region.getHeight()
            );
        }
        return sprites;
    }
}
