package wallbuilder.world;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import wallbuilder.asset.AssetManager;
import wallbuilder.collision.CollisionManager;
import wallbuilder.util.GridUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GameWorld:
 * - Chua nen map, wall layer, obstacle layer, preview layer va player.
 * - Day la noi wall that duoc dat vao va xuat hien trong world.
 */
public class GameWorld extends Pane {
    private static final int WORLD_ROWS = 12;
    private static final int WORLD_COLUMNS = 18;

    private final AssetManager assetManager;
    private final Canvas groundCanvas;
    private final Pane obstacleLayer;
    private final Pane wallLayer;
    private final Pane overlayLayer;
    private final Player player;
    private final Map<String, Wall> wallsByTileKey;
    private final List<Rectangle2D> staticObstacles;
    private final CollisionManager collisionManager;
    private final WallSpriteResolver wallSpriteResolver;

    public GameWorld(AssetManager assetManager) {
        this.assetManager = assetManager;
        this.groundCanvas = new Canvas(WORLD_COLUMNS * GridUtils.TILE_SIZE, WORLD_ROWS * GridUtils.TILE_SIZE);
        this.obstacleLayer = new Pane();
        this.wallLayer = new Pane();
        this.overlayLayer = new Pane();
        this.player = new Player(72, 72);
        this.wallsByTileKey = new LinkedHashMap<>();
        this.staticObstacles = new ArrayList<>();
        this.collisionManager = new CollisionManager(WORLD_ROWS, WORLD_COLUMNS, staticObstacles, wallsByTileKey);
        this.wallSpriteResolver = new WallSpriteResolver();

        setPrefSize(groundCanvas.getWidth(), groundCanvas.getHeight());
        getChildren().addAll(groundCanvas, obstacleLayer, wallLayer, overlayLayer, player.getBody());

        buildSampleObstacles();
        drawGround();
    }

    /**
     * placeWall:
     * - Input: tile row/column muon dat.
     * - Output: true neu dat thanh cong.
     * - Tac dong gameplay: wall that duoc them vao world va co collision ngay.
     */
    public boolean placeWall(int row, int column) {
        if (!collisionManager.canPlaceWall(row, column)) {
            return false;
        }

        ImageView imageView = new ImageView();
        imageView.setFitWidth(GridUtils.TILE_SIZE);
        imageView.setFitHeight(GridUtils.TILE_SIZE);
        imageView.setPreserveRatio(false);
        imageView.setLayoutX(GridUtils.toPixel(column));
        imageView.setLayoutY(GridUtils.toPixel(row));

        Wall wall = new Wall(row, column, imageView);
        wallsByTileKey.put(tileKey(row, column), wall);
        wallLayer.getChildren().add(imageView);

        refreshWallSprite(row, column);
        refreshWallSprite(row - 1, column);
        refreshWallSprite(row + 1, column);
        refreshWallSprite(row, column - 1);
        refreshWallSprite(row, column + 1);
        return true;
    }

    public void setBuildPreview(Rectangle previewRectangle) {
        overlayLayer.getChildren().setAll(previewRectangle);
    }

    public CollisionManager getCollisionManager() {
        return collisionManager;
    }

    public Player getPlayer() {
        return player;
    }

    private void buildSampleObstacles() {
        // Obstacles mau de test:
        // - Khong dat wall chong len.
        // - Player khong di xuyen qua.
        addObstacleTile(2, 4, 2, 2, "#546e2f");
        addObstacleTile(7, 9, 3, 1, "#795548");
        addObstacleTile(8, 2, 1, 2, "#6d4c41");
    }

    private void addObstacleTile(int row, int column, int widthInTiles, int heightInTiles, String color) {
        double pixelX = GridUtils.toPixel(column);
        double pixelY = GridUtils.toPixel(row);
        double pixelWidth = widthInTiles * GridUtils.TILE_SIZE;
        double pixelHeight = heightInTiles * GridUtils.TILE_SIZE;

        Rectangle obstacleView = new Rectangle(pixelWidth, pixelHeight, Color.web(color));
        obstacleView.setLayoutX(pixelX);
        obstacleView.setLayoutY(pixelY);
        obstacleView.setArcWidth(12);
        obstacleView.setArcHeight(12);
        obstacleView.setStroke(Color.color(0, 0, 0, 0.35));
        obstacleLayer.getChildren().add(obstacleView);

        staticObstacles.add(new Rectangle2D(pixelX, pixelY, pixelWidth, pixelHeight));
    }

    private void drawGround() {
        GraphicsContext graphicsContext = groundCanvas.getGraphicsContext2D();
        graphicsContext.setFill(Color.web("#8cc36f"));
        graphicsContext.fillRect(0, 0, groundCanvas.getWidth(), groundCanvas.getHeight());

        for (int row = 0; row < WORLD_ROWS; row++) {
            for (int column = 0; column < WORLD_COLUMNS; column++) {
                double x = GridUtils.toPixel(column);
                double y = GridUtils.toPixel(row);
                graphicsContext.setFill((row + column) % 2 == 0 ? Color.web("#88bb67") : Color.web("#7fb05f"));
                graphicsContext.fillRect(x, y, GridUtils.TILE_SIZE, GridUtils.TILE_SIZE);
            }
        }

        graphicsContext.setStroke(Color.color(0, 0, 0, 0.15));
        for (int row = 0; row <= WORLD_ROWS; row++) {
            double y = GridUtils.toPixel(row);
            graphicsContext.strokeLine(0, y, groundCanvas.getWidth(), y);
        }
        for (int column = 0; column <= WORLD_COLUMNS; column++) {
            double x = GridUtils.toPixel(column);
            graphicsContext.strokeLine(x, 0, x, groundCanvas.getHeight());
        }
    }

    private void refreshWallSprite(int row, int column) {
        Wall wall = wallsByTileKey.get(tileKey(row, column));
        if (wall == null) {
            return;
        }
        String assetKey = wallSpriteResolver.resolveAssetKey(row, column, wallsByTileKey);
        wall.getImageView().setImage(assetManager.getWallImage(assetKey));
    }

    private String tileKey(int row, int column) {
        return row + ":" + column;
    }
}
