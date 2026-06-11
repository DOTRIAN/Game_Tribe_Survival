package buildsystem.object;

import buildsystem.component.HealthComponent;
import buildsystem.component.LightComponent;
import buildsystem.core.BuildDefinition;
import buildsystem.core.BuildObjectSeed;

/** Campfire object vua la decor vua la light source. */
public class Campfire extends BuildObject {
    public Campfire(BuildDefinition definition, BuildObjectSeed seed) {
        super(seed.getObjectId(),
                definition.getType(),
                seed.getTileX(),
                seed.getTileY(),
                seed.getTileWidth(),
                seed.getTileHeight(),
                definition.getAutoTileGroup(),
                seed.getTileWidth(),
                seed.getTileHeight(),
                0,
                0,
                seed.getSpriteKey(),
                seed.getRotationDegrees(),
                seed.getHealth());
        addComponent(new LightComponent(
                core.GameBalance.CAMPFIRE_LIGHT_CORE_RADIUS,
                core.GameBalance.CAMPFIRE_LIGHT_INNER_RADIUS,
                core.GameBalance.CAMPFIRE_LIGHT_OUTER_RADIUS,
                core.GameBalance.CAMPFIRE_LIGHT_FADE_RADIUS,
                core.GameBalance.CAMPFIRE_LIGHT_INTENSITY,
                true,
                core.GameBalance.TORCH_LIGHT_RADIUS_FLICKER_PERCENT,
                core.GameBalance.TORCH_LIGHT_ALPHA_FLICKER_PERCENT,
                1.8
        ));
        addComponent(new HealthComponent(seed.getHealth(), definition.getHealth()));
    }
}
