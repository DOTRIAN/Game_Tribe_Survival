package buildsystem.sprite;

import buildsystem.core.BuildDefinition;
import buildsystem.object.BuildObject;

import java.util.Collection;

/**
 * OrthogonalAutoTileResolver:
 * - Resolver N,E,S,W cho object ket noi 4 huong.
 * - Co the tai su dung cho wall/fence/pipe/cable/road vi dieu kien duy nhat la cung autoTileGroup.
 */
public class OrthogonalAutoTileResolver implements AutoTileResolver {
    @Override
    public SpriteSelection resolve(BuildDefinition definition,
                                   int tileX,
                                   int tileY,
                                   Collection<BuildObject> placedObjects,
                                   double preferredRotationDegrees) {
        boolean north = hasNeighbor(definition, tileX, tileY - 1, placedObjects);
        boolean east = hasNeighbor(definition, tileX + 1, tileY, placedObjects);
        boolean south = hasNeighbor(definition, tileX, tileY + 1, placedObjects);
        boolean west = hasNeighbor(definition, tileX - 1, tileY, placedObjects);

        int mask = (north ? 1 : 0) | (east ? 2 : 0) | (south ? 4 : 0) | (west ? 8 : 0);
        return switch (mask) {
            case 0 -> new SpriteSelection("wall_single", preferredRotationDegrees, mask);
            case 1 -> new SpriteSelection("wall_end_n", 0.0, mask);
            case 2 -> new SpriteSelection("wall_end_e", 90.0, mask);
            case 4 -> new SpriteSelection("wall_end_s", 180.0, mask);
            case 8 -> new SpriteSelection("wall_end_w", 270.0, mask);
            case 3 -> new SpriteSelection("corner_base", 270.0, mask);
            case 6 -> new SpriteSelection("corner_base", 0.0, mask);
            case 12 -> new SpriteSelection("corner_base", 90.0, mask);
            case 9 -> new SpriteSelection("corner_base", 180.0, mask);
            case 5 -> new SpriteSelection("wall_vertical", 0.0, mask);
            case 10 -> new SpriteSelection("wall_horizontal", 0.0, mask);
            case 7 -> new SpriteSelection("wall_t_n", 0.0, mask);
            case 11 -> new SpriteSelection("wall_t_e", 90.0, mask);
            case 13 -> new SpriteSelection("wall_t_s", 180.0, mask);
            case 14 -> new SpriteSelection("wall_t_w", 270.0, mask);
            case 15 -> new SpriteSelection("wall_cross", 0.0, mask);
            default -> new SpriteSelection(definition.getDefaultSpriteKey(), preferredRotationDegrees, mask);
        };
    }

    private boolean hasNeighbor(BuildDefinition definition, int tileX, int tileY, Collection<BuildObject> objects) {
        if (definition == null || objects == null) {
            return false;
        }
        for (BuildObject object : objects) {
            if (object == null) {
                continue;
            }
            if (object.getTileX() == tileX
                    && object.getTileY() == tileY
                    && sameAutoTileGroup(definition, object)) {
                return true;
            }
        }
        return false;
    }

    private boolean sameAutoTileGroup(BuildDefinition definition, BuildObject object) {
        if (object == null || definition == null || definition.getAutoTileGroup() == null) {
            return false;
        }
        return definition.getAutoTileGroup().equalsIgnoreCase(object.getAutoTileGroup());
    }
}
