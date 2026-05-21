package buildsystem.object;

import build.WallSpriteConfig;
import buildsystem.component.CollisionComponent;
import buildsystem.component.HealthComponent;
import buildsystem.component.RotationComponent;
import buildsystem.core.BuildDefinition;
import buildsystem.core.BuildObjectSeed;

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
                WallSpriteConfig.WALL_RENDER_WIDTH,
                WallSpriteConfig.WALL_RENDER_HEIGHT,
                WallSpriteConfig.WALL_ANCHOR_X,
                WallSpriteConfig.WALL_ANCHOR_Y,
                seed.getSpriteKey(),
                seed.getRotationDegrees(),
                seed.getHealth());
        addComponent(new CollisionComponent(WallSpriteConfig.WALL_RENDER_WIDTH, WallSpriteConfig.WALL_RENDER_HEIGHT));
        addComponent(new HealthComponent(seed.getHealth(), definition.getHealth()));
        addComponent(new RotationComponent(seed.getRotationDegrees()));
    }
}
