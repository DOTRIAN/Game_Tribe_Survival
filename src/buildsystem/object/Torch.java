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
        addComponent(new LightComponent(
                GameBalance.TORCH_LIGHT_CORE_RADIUS,
                GameBalance.TORCH_LIGHT_INNER_RADIUS,
                GameBalance.TORCH_LIGHT_OUTER_RADIUS,
                GameBalance.TORCH_LIGHT_FADE_RADIUS,
                GameBalance.TORCH_LIGHT_INTENSITY,
                true,
                GameBalance.TORCH_LIGHT_RADIUS_FLICKER_PERCENT,
                GameBalance.TORCH_LIGHT_ALPHA_FLICKER_PERCENT,
                GameBalance.TORCH_LIGHT_FLICKER_SPEED
        ));
        addComponent(new HealthComponent(seed.getHealth(), definition.getHealth()));
        addComponent(new RotationComponent(seed.getRotationDegrees()));
    }
}
