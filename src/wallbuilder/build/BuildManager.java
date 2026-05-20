package wallbuilder.build;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import wallbuilder.collision.CollisionManager;
import wallbuilder.item.BuildItem;
import wallbuilder.item.Item;
import wallbuilder.item.WallItem;
import wallbuilder.util.GridUtils;
import wallbuilder.world.GameWorld;
import wallbuilder.world.Player;

/**
 * BuildManager:
 * - Dieu khien build mode, preview, validate va dat wall that.
 */
public class BuildManager {
    private final GameWorld gameWorld;
    private final CollisionManager collisionManager;
    private final Rectangle previewRectangle;

    private BuildMode buildMode;
    private BuildItem selectedBuildItem;
    private int previewRow;
    private int previewColumn;
    private boolean previewValid;

    public BuildManager(GameWorld gameWorld, CollisionManager collisionManager) {
        this.gameWorld = gameWorld;
        this.collisionManager = collisionManager;
        this.previewRectangle = new Rectangle(GridUtils.TILE_SIZE, GridUtils.TILE_SIZE);
        this.buildMode = BuildMode.NONE;
        this.selectedBuildItem = null;
        this.previewRow = -1;
        this.previewColumn = -1;
        this.previewValid = false;

        previewRectangle.setVisible(false);
        previewRectangle.setMouseTransparent(true);
        previewRectangle.setStrokeWidth(2);
    }

    public Rectangle getPreviewRectangle() {
        return previewRectangle;
    }

    /**
     * selectItem:
     * - Input: item dang duoc chon trong hotbar.
     * - Tac dong: vao/ra BUILD_WALL_MODE tuy theo item.
     */
    public void selectItem(Item item) {
        if (item instanceof WallItem wallItem) {
            selectedBuildItem = wallItem;
            buildMode = BuildMode.BUILD_WALL_MODE;
            previewRectangle.setVisible(true);
            return;
        }
        cancelBuildMode();
    }

    public void cancelBuildMode() {
        buildMode = BuildMode.NONE;
        selectedBuildItem = null;
        previewRow = -1;
        previewColumn = -1;
        previewValid = false;
        previewRectangle.setVisible(false);
    }

    /**
     * updatePreview:
     * - Input: worldX/worldY cua chuot va player hien tai.
     * - Tac dong: snap chuot ve tile va doi mau preview theo do hop le.
     */
    public void updatePreview(double worldX, double worldY, Player player) {
        if (buildMode != BuildMode.BUILD_WALL_MODE || selectedBuildItem == null) {
            previewRectangle.setVisible(false);
            return;
        }

        previewColumn = GridUtils.snapToTile(worldX);
        previewRow = GridUtils.snapToTile(worldY);
        previewValid = collisionManager.canPlaceWall(previewRow, previewColumn)
                && !isPreviewOverPlayer(player);

        previewRectangle.setVisible(true);
        previewRectangle.setWidth(selectedBuildItem.getTileWidth() * GridUtils.TILE_SIZE);
        previewRectangle.setHeight(selectedBuildItem.getTileHeight() * GridUtils.TILE_SIZE);
        previewRectangle.setLayoutX(GridUtils.toPixel(previewColumn));
        previewRectangle.setLayoutY(GridUtils.toPixel(previewRow));
        previewRectangle.setFill(previewValid ? Color.color(0.1, 0.95, 0.2, 0.35) : Color.color(1.0, 0.12, 0.12, 0.35));
        previewRectangle.setStroke(previewValid ? Color.LIMEGREEN : Color.RED);
    }

    /**
     * tryPlaceSelectedItem:
     * - Output: true neu dat wall thanh cong.
     * - Tac dong gameplay: tao wall that va cap nhat collision trong world.
     */
    public boolean tryPlaceSelectedItem(Player player) {
        if (buildMode != BuildMode.BUILD_WALL_MODE || selectedBuildItem == null || !previewValid) {
            return false;
        }
        if (selectedBuildItem instanceof WallItem) {
            return gameWorld.placeWall(previewRow, previewColumn);
        }
        return false;
    }

    public BuildMode getBuildMode() {
        return buildMode;
    }

    private boolean isPreviewOverPlayer(Player player) {
        return player.getOccupiedTileRow() == previewRow && player.getOccupiedTileColumn() == previewColumn;
    }
}
