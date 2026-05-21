package buildsystem.placement;

/** Strategy cho cach snap/toa do dat object. */
public interface PlacementStrategy {
    int snapX(double worldX, int tileWidth);
    int snapY(double worldY, int tileHeight);
}
