package entity;

import buildsystem.object.BuildObject;
import javafx.geometry.Point2D;

import java.util.List;

public interface EnemyNavigationContext {
    int getTileWidth();

    int getTileHeight();

    void requestPath(Enemy requester,
                     double startX,
                     double startY,
                     double targetX,
                     double targetY,
                     PathfindingManager.WalkValidator validator,
                     long nowNs);

    PathfindingManager.PathResult consumePathResult(Enemy requester);

    boolean canPathOccupy(Enemy requester, double x, double y, double width, double height);

    Point2D getFlowFieldWaypoint(double worldX, double worldY);

    BuildObject findBlockingObstacle(Enemy enemy, double targetX, double targetY, int maxRayTiles, int maxNearbyRadiusTiles);

    boolean damageWall(Enemy enemy, BuildObject wall, long nowNs);

    boolean damageBase(Enemy enemy, BaseCamp baseCamp, long nowNs);

    void onObstacleDestroyed(BuildObject obstacle, long nowNs);

    void recordFenceAttack(String enemyType);

    void recordStuck(String enemyType);
}
