package wallbuilder.collision;

import javafx.geometry.Rectangle2D;
import wallbuilder.util.GridUtils;
import wallbuilder.world.Wall;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CollisionManager:
 * - Gom toan bo rule collision cho build va movement.
 * - Quan ly obstacle tinh va wall runtime.
 */
public class CollisionManager {
    private final int worldRows;
    private final int worldColumns;
    private final List<Rectangle2D> staticObstacles;
    private final Map<String, Wall> wallsByTileKey;

    public CollisionManager(int worldRows, int worldColumns, List<Rectangle2D> staticObstacles, Map<String, Wall> wallsByTileKey) {
        this.worldRows = worldRows;
        this.worldColumns = worldColumns;
        this.staticObstacles = new ArrayList<>(staticObstacles);
        this.wallsByTileKey = wallsByTileKey;
    }

    /**
     * canPlaceWall:
     * - Input: tile row/column can dat wall.
     * - Output: true neu hop le.
     * - Tac dong gameplay: dieu khien preview xanh/do va chan dat tuong sai.
     */
    public boolean canPlaceWall(int row, int column) {
        if (!isInsideTile(row, column)) {
            return false;
        }
        if (wallsByTileKey.containsKey(tileKey(row, column))) {
            return false;
        }

        Rectangle2D tileBounds = tileBounds(row, column);
        for (Rectangle2D obstacle : staticObstacles) {
            if (obstacle.intersects(tileBounds)) {
                return false;
            }
        }
        return true;
    }

    /**
     * collides:
     * - Input: rectangle theo pixel cua entity.
     * - Output: true neu cham bien, obstacle hoac wall.
     * - Tac dong gameplay: player/quai se khong di xuyen qua wall.
     */
    public boolean collides(double x, double y, double width, double height) {
        Rectangle2D bounds = new Rectangle2D(x, y, width, height);
        if (x < 0 || y < 0 || x + width > worldColumns * GridUtils.TILE_SIZE || y + height > worldRows * GridUtils.TILE_SIZE) {
            return true;
        }

        for (Rectangle2D obstacle : staticObstacles) {
            if (obstacle.intersects(bounds)) {
                return true;
            }
        }

        for (Wall wall : wallsByTileKey.values()) {
            if (tileBounds(wall.getRow(), wall.getColumn()).intersects(bounds)) {
                return true;
            }
        }
        return false;
    }

    public List<Rectangle2D> getStaticObstacles() {
        return staticObstacles;
    }

    private boolean isInsideTile(int row, int column) {
        return row >= 0 && row < worldRows && column >= 0 && column < worldColumns;
    }

    private Rectangle2D tileBounds(int row, int column) {
        return new Rectangle2D(
                GridUtils.toPixel(column),
                GridUtils.toPixel(row),
                GridUtils.TILE_SIZE,
                GridUtils.TILE_SIZE
        );
    }

    private String tileKey(int row, int column) {
        return row + ":" + column;
    }
}
