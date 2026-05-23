package buildsystem.fence;

import buildsystem.component.HealthComponent;
import buildsystem.core.BuildDefinition;
import buildsystem.core.BuildObjectSeed;
import buildsystem.core.BuildType;
import buildsystem.object.BuildObject;
import core.GameBalance;

public class FenceEntity extends BuildObject {
    private static final double SINGLE_WIDTH_SCALE = 0.76;
    private static final double SINGLE_HEIGHT_SCALE = 1.60;

    public FenceEntity(int tileX, int tileY, int tileWidth, int tileHeight) {
        super("map_fence_" + tileX + "_" + tileY,
                BuildType.STONE_WALL,
                tileX,
                tileY,
                tileWidth,
                tileHeight,
                "wood_fence",
                tileWidth,
                tileHeight,
                0.0,
                0.0,
                FenceRenderer.SINGLE_SPRITE_KEY,
                0.0,
                GameBalance.WOOD_FENCE_MAX_HP);
        addComponent(FenceCollision.create(tileWidth, tileHeight));
        addComponent(new HealthComponent(GameBalance.WOOD_FENCE_MAX_HP, GameBalance.WOOD_FENCE_MAX_HP));
    }

    public FenceEntity(BuildDefinition definition, BuildObjectSeed seed) {
        super(seed.getObjectId(),
                definition.getType(),
                seed.getTileX(),
                seed.getTileY(),
                seed.getTileWidth(),
                seed.getTileHeight(),
                definition.getAutoTileGroup(),
                seed.getTileWidth(),
                seed.getTileHeight(),
                0.0,
                0.0,
                seed.getSpriteKey(),
                0.0,
                seed.getHealth());
        addComponent(FenceCollision.create(seed.getTileWidth(), seed.getTileHeight()));
        addComponent(new HealthComponent(seed.getHealth(), definition.getHealth()));
    }

    @Override
    public double getRenderWidth() {
        return computeRenderWidth(getSpriteKey(), getTileWidth(), getTileHeight());
    }

    @Override
    public double getRenderHeight() {
        return computeRenderHeight(getSpriteKey(), getTileHeight());
    }

    @Override
    public double getRenderX() {
        return computeRenderX(getTileX(), getTileWidth(), getRenderWidth(), getSpriteKey());
    }

    @Override
    public double getRenderY() {
        return computeRenderY(getTileY(), getTileHeight(), getRenderHeight());
    }

    @Override
    public double getCenterX() {
        return getRenderX() + getRenderWidth() / 2.0;
    }

    @Override
    public double getCenterY() {
        return getRenderY() + getRenderHeight() / 2.0;
    }

    public static double computeRenderWidth(String spriteKey, int tileWidth, int tileHeight) {
        double baseWidth = Math.max(1.0, tileWidth);
        return baseWidth * SINGLE_WIDTH_SCALE;
    }

    public static double computeRenderHeight(String spriteKey, int tileHeight) {
        double baseHeight = Math.max(1.0, tileHeight);
        return baseHeight * SINGLE_HEIGHT_SCALE;
    }

    public static double computeRenderX(int tileX, int tileWidth, double renderWidth, String spriteKey) {
        double tileCenterX = tileX * tileWidth + tileWidth / 2.0;
        return tileCenterX - renderWidth / 2.0;
    }

    public static double computeRenderY(int tileY, int tileHeight, double renderHeight) {
        return tileY * tileHeight + tileHeight - renderHeight;
    }

    public double getFootY() {
        return getTileY() * getTileHeight() + getTileHeight();
    }
}
