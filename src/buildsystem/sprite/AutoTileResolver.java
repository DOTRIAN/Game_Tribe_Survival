package buildsystem.sprite;

import buildsystem.core.BuildDefinition;
import buildsystem.object.BuildObject;

import java.util.Collection;

/**
 * AutoTileResolver:
 * - Resolver chung cho cac object can auto connect: wall, fence, pipe, cable, road...
 */
public interface AutoTileResolver {
    SpriteSelection resolve(BuildDefinition definition,
                            int tileX,
                            int tileY,
                            Collection<BuildObject> placedObjects,
                            double preferredRotationDegrees);
}
