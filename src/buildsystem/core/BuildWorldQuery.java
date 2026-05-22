package buildsystem.core;

/**
 * BuildWorldQuery:
 * - Lop cau noi giua BuildSystem va world/runtime.
 * - PlacementValidator hoi interface nay de kiem tra bounds, terrain, water va static collision.
 */
public interface BuildWorldQuery {
    int getTileWidth();

    int getTileHeight();

    double getWorldWidth();

    double getWorldHeight();

    boolean isBlockedByTerrain(double x, double y, double width, double height);

    boolean isBlockedByWater(double x, double y, double width, double height);

    boolean isBlockedByStaticObjects(double x, double y, double width, double height);

    boolean isFlatTerrain(double x, double y, double width, double height);

    default boolean isBlockedByDynamicEntities(double x, double y, double width, double height) {
        return false;
    }
}
