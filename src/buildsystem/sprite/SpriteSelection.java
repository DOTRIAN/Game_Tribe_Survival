package buildsystem.sprite;

/**
 * SpriteSelection:
 * - Ket qua resolver: sprite key + rotation + neighbor mask.
 */
public class SpriteSelection {
    private final String spriteKey;
    private final double rotationDegrees;
    private final int neighborMask;

    public SpriteSelection(String spriteKey, double rotationDegrees, int neighborMask) {
        this.spriteKey = spriteKey;
        this.rotationDegrees = rotationDegrees;
        this.neighborMask = neighborMask;
    }

    public String getSpriteKey() { return spriteKey; }
    public double getRotationDegrees() { return rotationDegrees; }
    public int getNeighborMask() { return neighborMask; }
}
