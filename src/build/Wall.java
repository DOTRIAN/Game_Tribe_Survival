package build;

import javafx.scene.image.Image;

/**
 * Wall:
 * - Runtime data cua 1 buc tuong da dat trong world sinh ton.
 * - Ngoai tileX/tileY, class nay con tinh san vi tri render theo TILE_SIZE de Renderer khong phai lap lai cong thuc.
 */
public class Wall {
    // Cac hang so render:
    // - Giu wall khop voi tile hien tai 16x16.
    // - Neu sau nay doi tile size hoac muon cho tuong cao/thap hon, chi can sua tai day.
    public static final double WALL_RENDER_WIDTH = WallSpriteConfig.WALL_RENDER_WIDTH;
    public static final double WALL_RENDER_HEIGHT = WallSpriteConfig.WALL_RENDER_HEIGHT;
    public static final double WALL_ANCHOR_X = WallSpriteConfig.WALL_ANCHOR_X;
    public static final double WALL_ANCHOR_Y = WallSpriteConfig.WALL_ANCHOR_Y;

    private final int tileX;
    private final int tileY;
    private final int tileWidth;
    private final int tileHeight;

    // assetKey/image:
    // - assetKey giup debug va refresh sprite theo hang xom.
    // - image la sprite that de Renderer draw len world layer.
    private String assetKey;
    private double rotationDegrees;
    private Image image;

    public Wall(int tileX, int tileY, int tileWidth, int tileHeight, String assetKey, double rotationDegrees, Image image) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.assetKey = assetKey;
        this.rotationDegrees = rotationDegrees;
        this.image = image;
    }

    public int getTileX() {
        return tileX;
    }

    public int getTileY() {
        return tileY;
    }

    /**
     * getRenderX:
     * - Output: vi tri X world-space de ve tuong.
     * - Cong thuc nay giu tuong nam dung trong o tile, khong bi lech sang trai/phai.
     */
    public double getRenderX() {
        return tileX * tileWidth + (tileWidth - WALL_RENDER_WIDTH) / 2.0 + WALL_ANCHOR_X;
    }

    /**
     * getRenderY:
     * - Output: vi tri Y world-space de ve tuong.
     * - Cong thuc canh day sprite vao day o tile de wall top-down khong bi bay len tren.
     */
    public double getRenderY() {
        return tileY * tileHeight + (tileHeight - WALL_RENDER_HEIGHT) / 2.0 + WALL_ANCHOR_Y;
    }

    public double getWidth() {
        return WALL_RENDER_WIDTH;
    }

    public double getHeight() {
        return WALL_RENDER_HEIGHT;
    }

    public String getAssetKey() {
        return assetKey;
    }

    public double getRotationDegrees() {
        return rotationDegrees;
    }

    public Image getImage() {
        return image;
    }

    /**
     * updateSprite:
     * - Input: asset key moi va image moi sau khi wall co them/bot hang xom.
     * - Output: cap nhat sprite dang render.
     * - Tac dong gameplay: day la nen tang de auto-wall tu doi hinh theo huong.
     */
    public void updateSprite(String nextAssetKey, double nextRotationDegrees, Image nextImage) {
        this.assetKey = nextAssetKey;
        this.rotationDegrees = nextRotationDegrees;
        this.image = nextImage;
    }
}
