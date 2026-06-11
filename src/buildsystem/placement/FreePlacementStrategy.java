package buildsystem.placement;

/**
 * FreePlacementStrategy:
 * - Van dua ve tile index de tai su dung collision/tile query, nhung world cursor khong bat buoc nam dung tam tile.
 * - Dung cho torch/campfire/decor va cac object can cam giac dat "tu do" hon.
 */
public class FreePlacementStrategy implements PlacementStrategy {
    @Override
    public int snapX(double worldX, int tileWidth) {
        double half = Math.max(1, tileWidth) * 0.5;
        return (int) Math.floor((worldX - half) / Math.max(1, tileWidth));
    }

    @Override
    public int snapY(double worldY, int tileHeight) {
        double half = Math.max(1, tileHeight) * 0.5;
        return (int) Math.floor((worldY - half) / Math.max(1, tileHeight));
    }
}
