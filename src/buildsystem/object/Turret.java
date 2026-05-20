package buildsystem.object;

import buildsystem.component.CollisionComponent;
import buildsystem.component.DamageComponent;
import buildsystem.component.HealthComponent;
import buildsystem.component.RotationComponent;
import buildsystem.core.BuildDefinition;
import buildsystem.core.BuildObjectSeed;

/** Turret/cannon/defensive structure object. */
public class Turret extends BuildObject {
    public Turret(BuildDefinition definition, BuildObjectSeed seed) {
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
        addComponent(new CollisionComponent(seed.getTileWidth(), seed.getTileHeight()));
        addComponent(new DamageComponent(10));
        addComponent(new HealthComponent(seed.getHealth(), definition.getHealth()));
        addComponent(new RotationComponent(seed.getRotationDegrees()));
    }
}
