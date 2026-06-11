package buildsystem.core;

/**
 * BuildObjectSeed:
 * - Dong goi toan bo du lieu khoi tao object that sau khi placement hop le.
 * - Factory nhan seed nay de tranh new object bang tham so roi rac khap codebase.
 */
public class BuildObjectSeed {
    private final String objectId;
    private final BuildDefinition definition;
    private final int tileX;
    private final int tileY;
    private final int tileWidth;
    private final int tileHeight;
    private final String spriteKey;
    private final double rotationDegrees;
    private final int health;

    public BuildObjectSeed(String objectId,
                           BuildDefinition definition,
                           int tileX,
                           int tileY,
                           int tileWidth,
                           int tileHeight,
                           String spriteKey,
                           double rotationDegrees,
                           int health) {
        this.objectId = objectId;
        this.definition = definition;
        this.tileX = tileX;
        this.tileY = tileY;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.spriteKey = spriteKey;
        this.rotationDegrees = rotationDegrees;
        this.health = health;
    }

    public String getObjectId() {
        return objectId;
    }

    public BuildDefinition getDefinition() {
        return definition;
    }

    public int getTileX() {
        return tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public String getSpriteKey() {
        return spriteKey;
    }

    public double getRotationDegrees() {
        return rotationDegrees;
    }

    public int getHealth() {
        return health;
    }
}
