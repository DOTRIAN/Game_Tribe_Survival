package buildsystem.placement;

/**
 * GridPlacementStrategy:
 * - Snap object theo tile grid.
 * - Dung cho wall, chest, workbench, turret, machine...
 */
public class GridPlacementStrategy implements PlacementStrategy {
    @Override
    public int snapX(double worldX, int tileWidth) {
        return (int) Math.floor(worldX / Math.max(1, tileWidth));
    }

    @Override
    public int snapY(double worldY, int tileHeight) {
        return (int) Math.floor(worldY / Math.max(1, tileHeight));
    }
}
