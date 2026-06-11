package buildsystem.sprite;

import buildsystem.core.BuildDefinition;
import buildsystem.fence.FenceConnectionSystem;
import buildsystem.object.BuildObject;

import java.util.Collection;

/**
 * OrthogonalAutoTileResolver:
 * - Resolver N,E,S,W cho object ket noi 4 huong.
 * - Co the tai su dung cho wall/fence/pipe/cable/road vi dieu kien duy nhat la cung autoTileGroup.
 */
public class OrthogonalAutoTileResolver implements AutoTileResolver {
    private final FenceConnectionSystem fenceConnectionSystem;

    public OrthogonalAutoTileResolver() {
        this.fenceConnectionSystem = new FenceConnectionSystem();
    }

    @Override
    public SpriteSelection resolve(BuildDefinition definition,
                                   int tileX,
                                   int tileY,
                                   Collection<BuildObject> placedObjects,
                                   double preferredRotationDegrees) {
        if (definition != null && "wood_fence".equalsIgnoreCase(definition.getAutoTileGroup())) {
            return fenceConnectionSystem.resolve(definition, tileX, tileY, placedObjects);
        }
        boolean north = hasNeighbor(definition, tileX, tileY - 1, placedObjects);
        boolean east = hasNeighbor(definition, tileX + 1, tileY, placedObjects);
        boolean south = hasNeighbor(definition, tileX, tileY + 1, placedObjects);
        boolean west = hasNeighbor(definition, tileX - 1, tileY, placedObjects);

        int mask = (north ? 1 : 0) | (east ? 2 : 0) | (south ? 4 : 0) | (west ? 8 : 0);
        // Rotation cua object duoc xem la input tu nguoi choi (Q rotate).
        // Auto-tile chi doi sprite key theo hang xom, khong tu y xoay object.
        return switch (mask) {
            case 0 -> new SpriteSelection("wall_single", preferredRotationDegrees, mask);
            case 1 -> new SpriteSelection("wall_end_n", preferredRotationDegrees, mask);
            case 2 -> new SpriteSelection("wall_end_e", preferredRotationDegrees, mask);
            case 4 -> new SpriteSelection("wall_end_s", preferredRotationDegrees, mask);
            case 8 -> new SpriteSelection("wall_end_w", preferredRotationDegrees, mask);
            case 3 -> new SpriteSelection("corner_base", preferredRotationDegrees, mask);
            case 6 -> new SpriteSelection("corner_base", preferredRotationDegrees, mask);
            case 12 -> new SpriteSelection("corner_base", preferredRotationDegrees, mask);
            case 9 -> new SpriteSelection("corner_base", preferredRotationDegrees, mask);
            case 5 -> new SpriteSelection("wall_vertical", preferredRotationDegrees, mask);
            case 10 -> new SpriteSelection("wall_horizontal", preferredRotationDegrees, mask);
            case 7 -> new SpriteSelection("wall_t_n", preferredRotationDegrees, mask);
            case 11 -> new SpriteSelection("wall_t_e", preferredRotationDegrees, mask);
            case 13 -> new SpriteSelection("wall_t_s", preferredRotationDegrees, mask);
            case 14 -> new SpriteSelection("wall_t_w", preferredRotationDegrees, mask);
            case 15 -> new SpriteSelection("wall_cross", preferredRotationDegrees, mask);
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
