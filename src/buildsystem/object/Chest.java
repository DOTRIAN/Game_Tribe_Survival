package buildsystem.object;

import buildsystem.component.CollisionComponent;
import buildsystem.component.HealthComponent;
import buildsystem.component.InventoryComponent;
import buildsystem.core.BuildDefinition;
import buildsystem.core.BuildObjectSeed;

/** Chest/workbench-like object co inventory rieng hoac station state rieng. */
public class Chest extends BuildObject {
    public Chest(BuildDefinition definition, BuildObjectSeed seed) {
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
        addComponent(new HealthComponent(seed.getHealth(), definition.getHealth()));
        addComponent(new InventoryComponent());
    }
}
