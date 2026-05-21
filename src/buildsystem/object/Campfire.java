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
        addComponent(new LightComponent(180, 1.0));
        addComponent(new HealthComponent(seed.getHealth(), definition.getHealth()));
    }
}
