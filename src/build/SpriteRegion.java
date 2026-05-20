package build;

/**
 * SpriteRegion:
 * - Mo ta 1 vung crop ben trong sprite sheet tuong.jpg.
 * - Tach rieng class nay de WallSpriteConfig chi can khai bao danh sach region de chinh toa do crop.
 */
public class SpriteRegion {
    private final String name;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public SpriteRegion(String name, int x, int y, int width, int height) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
