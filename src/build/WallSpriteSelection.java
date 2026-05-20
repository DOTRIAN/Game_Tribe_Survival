package build;

/**
 * WallSpriteSelection:
 * - Ket qua resolver tra ve cho 1 wall/preivew.
 * - Gom sprite key + goc xoay de renderer co the dung 1 anh goc roi rotate dung huong.
 */
public class WallSpriteSelection {
    private final String spriteKey;
    private final double rotationDegrees;
    private final int neighborMask;

    public WallSpriteSelection(String spriteKey, double rotationDegrees, int neighborMask) {
        this.spriteKey = spriteKey;
        this.rotationDegrees = rotationDegrees;
        this.neighborMask = neighborMask;
    }

    public String getSpriteKey() {
        return spriteKey;
    }

    public double getRotationDegrees() {
        return rotationDegrees;
    }

    public int getNeighborMask() {
        return neighborMask;
    }
}
