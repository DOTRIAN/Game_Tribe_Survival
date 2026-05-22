package buildsystem.object;

import buildsystem.component.CollisionComponent;
import buildsystem.component.HealthComponent;
import buildsystem.component.RotationComponent;
import buildsystem.core.BuildDefinition;
import buildsystem.core.BuildObjectSeed;
import buildsystem.sprite.WallSpriteConfig;

/**
 * Wall:
 * - BuildObject dung cho wall/fence/road-like object co auto connect.
 * - Auto-connect khong nam trong class nay; no nam o sprite resolver de co the tai su dung cho nhieu type khac.
 */
public class Wall extends BuildObject {
    public Wall(BuildDefinition definition, BuildObjectSeed seed) {
        super(seed.getObjectId(),
                definition.getType(),
                seed.getTileX(),
                seed.getTileY(),
                seed.getTileWidth(),
                seed.getTileHeight(),
                definition.getAutoTileGroup(),
                renderWidth(definition),
                renderHeight(definition),
                anchorX(definition),
                anchorY(definition),
                seed.getSpriteKey(),
                seed.getRotationDegrees(),
                seed.getHealth());
        addComponent(new CollisionComponent(renderWidth(definition), renderHeight(definition)));
        addComponent(new HealthComponent(seed.getHealth(), definition.getHealth()));
        addComponent(new RotationComponent(seed.getRotationDegrees()));
    }

    private static boolean isWoodFence(BuildDefinition definition) {
        return definition != null && "wood_fence".equalsIgnoreCase(definition.getAutoTileGroup());
    }

    private static double renderWidth(BuildDefinition definition) {
        return isWoodFence(definition) ? WallSpriteConfig.WOOD_FENCE_RENDER_WIDTH : WallSpriteConfig.WALL_RENDER_WIDTH;
    }

    private static double renderHeight(BuildDefinition definition) {
        return isWoodFence(definition) ? WallSpriteConfig.WOOD_FENCE_RENDER_HEIGHT : WallSpriteConfig.WALL_RENDER_HEIGHT;
    }

    private static double anchorX(BuildDefinition definition) {
        return isWoodFence(definition) ? WallSpriteConfig.WOOD_FENCE_ANCHOR_X : WallSpriteConfig.WALL_ANCHOR_X;
    }

    private static double anchorY(BuildDefinition definition) {
        return isWoodFence(definition) ? WallSpriteConfig.WOOD_FENCE_ANCHOR_Y : WallSpriteConfig.WALL_ANCHOR_Y;
    }
}
