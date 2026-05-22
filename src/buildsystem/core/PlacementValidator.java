package buildsystem.core;

import buildsystem.component.CollisionComponent;
import buildsystem.object.BuildObject;
import buildsystem.placement.PlacementContext;
import entity.Player;

import java.util.Collection;
import java.util.Map;

/**
 * PlacementValidator:
 * - Cua vao duy nhat de validate placement.
 * - Placement flow:
 *   1) Kiem tra UI overlap.
 *   2) Tinh world bounds cua footprint.
 *   3) Terrain / water / flat ground.
 *   4) Static collision voi map/resource/object.
 *   5) Blocking entity (player).
 *   6) Near-object rule cho trap/machine/farm...
 */
public class PlacementValidator {
    private final BuildWorldQuery worldQuery;

    public PlacementValidator(BuildWorldQuery worldQuery) {
        this.worldQuery = worldQuery;
    }

    public PlacementResult validate(BuildDefinition definition,
                                    PlacementContext context,
                                    Collection<BuildObject> placedObjects) {
        if (definition == null || context == null) {
            return PlacementResult.invalid("missing-definition");
        }
        if (context.isMouseOverUi()) {
            return PlacementResult.invalid("mouse-over-ui");
        }

        double x = context.getTileX() * worldQuery.getTileWidth();
        double y = context.getTileY() * worldQuery.getTileHeight();
        double width = definition.getFootprintWidthTiles() * worldQuery.getTileWidth();
        double height = definition.getFootprintHeightTiles() * worldQuery.getTileHeight();

        if (x < 0 || y < 0 || x + width > worldQuery.getWorldWidth() || y + height > worldQuery.getWorldHeight()) {
            return PlacementResult.invalid("out-of-world");
        }
        if (definition.isRequiresFlatTerrain() && !worldQuery.isFlatTerrain(x, y, width, height)) {
            return PlacementResult.invalid("terrain-not-flat");
        }
        if (worldQuery.isBlockedByTerrain(x, y, width, height)) {
            return PlacementResult.invalid("terrain-blocked");
        }
        if (definition.isWaterRestricted() && worldQuery.isBlockedByWater(x, y, width, height)) {
            return PlacementResult.invalid("water-blocked");
        }
        if (worldQuery.isBlockedByStaticObjects(x, y, width, height)) {
            return PlacementResult.invalid("blocked-by-world-object");
        }
        if (worldQuery.isBlockedByDynamicEntities(x, y, width, height)) {
            return PlacementResult.invalid("blocked-by-entity");
        }
        if (intersectsPlayer(context.getPlayer(), x, y, width, height)) {
            return PlacementResult.invalid("blocked-by-player");
        }
        if (definition.isBlocksPlacementOverlap() && intersectsPlacedObjects(x, y, width, height, placedObjects)) {
            return PlacementResult.invalid("blocked-by-build-object");
        }
        PlacementResult nearObjectRule = validateNearObjectRules(definition, context, placedObjects);
        if (!nearObjectRule.isValid()) {
            return nearObjectRule;
        }
        return PlacementResult.valid();
    }

    private PlacementResult validateNearObjectRules(BuildDefinition definition,
                                                    PlacementContext context,
                                                    Collection<BuildObject> placedObjects) {
        Map<BuildType, Integer> rules = definition.getMinDistanceByType();
        if (rules.isEmpty() || placedObjects == null) {
            return PlacementResult.valid();
        }
        for (BuildObject object : placedObjects) {
            if (object == null) {
                continue;
            }
            Integer minDistance = rules.get(object.getType());
            if (minDistance == null || minDistance <= 0) {
                continue;
            }
            int dx = Math.abs(object.getTileX() - context.getTileX());
            int dy = Math.abs(object.getTileY() - context.getTileY());
            if (Math.max(dx, dy) <= minDistance) {
                return PlacementResult.invalid("too-close-to-" + object.getType().name().toLowerCase());
            }
        }
        return PlacementResult.valid();
    }

    private boolean intersectsPlacedObjects(double x,
                                            double y,
                                            double width,
                                            double height,
                                            Collection<BuildObject> placedObjects) {
        if (placedObjects == null) {
            return false;
        }
        for (BuildObject object : placedObjects) {
            if (object == null) {
                continue;
            }
            if (object.getComponent(CollisionComponent.class) == null) {
                continue;
            }
            if (intersectsRect(
                    x,
                    y,
                    width,
                    height,
                    object.getRenderX(),
                    object.getRenderY(),
                    object.getCollisionWidth(),
                    object.getCollisionHeight())) {
                return true;
            }
        }
        return false;
    }

    private boolean intersectsPlayer(Player player, double x, double y, double width, double height) {
        if (player == null) {
            return false;
        }
        double px = player.getX() + player.getWidth() * 0.22;
        double py = player.getY() + player.getHeight() * 0.30;
        double pw = player.getWidth() * 0.56;
        double ph = player.getHeight() * 0.62;
        return intersectsRect(px, py, pw, ph, x, y, width, height);
    }

    private boolean intersectsRect(double ax, double ay, double aw, double ah,
                                   double bx, double by, double bw, double bh) {
        return ax < bx + bw
                && ax + aw > bx
                && ay < by + bh
                && ay + ah > by;
    }
}
