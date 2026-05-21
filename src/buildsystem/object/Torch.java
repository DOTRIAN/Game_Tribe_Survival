package buildsystem.object;

import buildsystem.component.HealthComponent;
import buildsystem.component.LightComponent;
import buildsystem.component.RotationComponent;
import buildsystem.core.BuildDefinition;
import buildsystem.core.BuildObjectSeed;
import core.GameBalance;

/** Torch object mau cho lighting system dat xuong world. */
public class Torch extends BuildObject {
    public Torch(BuildDefinition definition, BuildObjectSeed seed) {
        super(seed.getObjectId(),
                definition.getType(),
                seed.getTileX(),
                seed.getTileY(),
                seed.getTileWidth(),
                seed.getTileHeight(),
                definition.getAutoTileGroup(),
                GameBalance.TORCH_WORLD_WIDTH,
                GameBalance.TORCH_WORLD_HEIGHT,
                0,
                (seed.getTileHeight() - GameBalance.TORCH_WORLD_HEIGHT) / 2.0,
                seed.getSpriteKey(),
                seed.getRotationDegrees(),
                seed.getHealth());
        addComponent(new LightComponent(GameBalance.TORCH_LIGHT_RADIUS, GameBalance.TORCH_LIGHT_INTENSITY));
        addComponent(new HealthComponent(seed.getHealth(), definition.getHealth()));
        addComponent(new RotationComponent(seed.getRotationDegrees()));
    }
}
