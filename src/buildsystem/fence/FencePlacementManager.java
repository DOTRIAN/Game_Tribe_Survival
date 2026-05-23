package buildsystem.fence;

import buildsystem.core.BuildDefinition;
import buildsystem.core.PlacementResult;
import buildsystem.core.PlacementValidator;
import buildsystem.object.BuildObject;
import buildsystem.placement.PlacementContext;

import java.util.Collection;

public class FencePlacementManager {
    public PlacementResult validate(BuildDefinition definition,
                                    PlacementContext context,
                                    PlacementValidator placementValidator,
                                    Collection<BuildObject> placedObjects) {
        return placementValidator.validate(definition, context, placedObjects);
    }
}
