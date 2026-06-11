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
        addComponent(new CollisionComponent(WallSpriteConfig.WALL_RENDER_WIDTH, WallSpriteConfig.WALL_RENDER_HEIGHT));
        addComponent(new HealthComponent(seed.getHealth(), definition.getHealth()));
        addComponent(new RotationComponent(seed.getRotationDegrees()));
    }

    private static double renderWidth(BuildDefinition definition) {
        return WallSpriteConfig.WALL_RENDER_WIDTH;
    }

    private static double renderHeight(BuildDefinition definition) {
        return WallSpriteConfig.WALL_RENDER_HEIGHT;
    }

    private static double anchorX(BuildDefinition definition) {
        return WallSpriteConfig.WALL_ANCHOR_X;
    }

    private static double anchorY(BuildDefinition definition) {
        return WallSpriteConfig.WALL_ANCHOR_Y;
    }
}
