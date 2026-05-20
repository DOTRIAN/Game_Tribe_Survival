package build;

import entity.Player;
import javafx.scene.image.Image;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BuildManager:
 * - Dieu khien chon item build, BUILD_WALL_MODE, preview sprite va dat tuong that.
 * - Ban nay bo Rectangle xanh, thay bang preview dung CHINH sprite tuong.
 */
public class BuildManager {
    private static final String STONE_WALL_ITEM_ID = "stone_wall";
    private static final boolean DEBUG_WALL = true;
    private static final String[] ROTATION_LABELS = {"N", "E", "S", "W"};
    private static final double[] ROTATION_DEGREES = {0.0, 90.0, 180.0, 270.0};

    private final AssetManager assetManager;
    private final CollisionManager collisionManager;
    private final WallSpriteResolver wallSpriteResolver;
    private final WallPreview wallPreview;
    private final Map<String, Wall> wallsByTileKey;

    private String selectedItemId;
    private BuildMode buildMode;
    private int previewTileX;
    private int previewTileY;
    private boolean previewValid;
    private int rotationIndex;
    private String lastLoggedPreviewKey;

    public BuildManager(AssetManager assetManager, CollisionManager collisionManager) {
        this.assetManager = assetManager;
        this.collisionManager = collisionManager;
        this.wallSpriteResolver = new WallSpriteResolver();
        this.wallPreview = new WallPreview();
        this.wallsByTileKey = new LinkedHashMap<>();
        this.selectedItemId = null;
        this.buildMode = BuildMode.NONE;
        this.previewTileX = -1;
        this.previewTileY = -1;
        this.previewValid = false;
        this.rotationIndex = 0;
        this.lastLoggedPreviewKey = "";
    }

    /**
     * selectItem:
     * - Input: item id dang duoc chon tu hotbar.
     * - Tac dong: chi khi chon stone_wall thi moi bat BUILD_WALL_MODE.
     */
    public void selectItem(String itemId) {
        selectedItemId = itemId;
        if (STONE_WALL_ITEM_ID.equals(itemId)) {
            System.out.println("Selected item: stone_wall");
            if (buildMode != BuildMode.BUILD_WALL_MODE) {
                buildMode = BuildMode.BUILD_WALL_MODE;
                wallPreview.setVisible(true);
                System.out.println("Enter BUILD_WALL_MODE");
            }
            return;
        }
        cancelBuildMode();
    }

    /**
     * rotateWall:
     * - Input: khong co, goi khi user bam Q.
     * - Output: doi rotation N -> E -> S -> W -> N.
     * - Tac dong gameplay: preview sprite doi ngay, va wall don le dat xuong se dung huong nay.
     */
    public void rotateWall() {
        if (buildMode != BuildMode.BUILD_WALL_MODE) {
            return;
        }
        rotationIndex = (rotationIndex + 1) % ROTATION_LABELS.length;
        System.out.println("Current rotation: " + getCurrentRotationLabel());
    }

    public void cancelBuildMode() {
        if (buildMode == BuildMode.BUILD_WALL_MODE) {
            System.out.println("Exit BUILD_WALL_MODE");
        }
        buildMode = BuildMode.NONE;
        selectedItemId = null;
        previewTileX = -1;
        previewTileY = -1;
        previewValid = false;
        wallPreview.setVisible(false);
        lastLoggedPreviewKey = "";
    }

    /**
     * updatePreview:
     * - Chuyen chuot screen-space -> world-space -> tile.
     * - Preview dung chinh sprite wall, cung scale/anchor voi wall that.
     */
    public void updatePreview(double mouseScreenX,
                              double mouseScreenY,
                              double cameraX,
                              double cameraY,
                              double cameraZoom,
                              boolean mouseOverUi,
                              Player player,
                              int availableStoneWalls) {
        if (buildMode != BuildMode.BUILD_WALL_MODE || !STONE_WALL_ITEM_ID.equals(selectedItemId)) {
            wallPreview.setVisible(false);
            previewValid = false;
            return;
        }

        if (mouseOverUi) {
            wallPreview.setVisible(false);
            previewValid = false;
            return;
        }

        double worldX = cameraX + mouseScreenX / cameraZoom;
        double worldY = cameraY + mouseScreenY / cameraZoom;
        int tileWidth = collisionManager.getTileWidth();
        int tileHeight = collisionManager.getTileHeight();
        int tileX = GridUtils.snapToTile(worldX, tileWidth);
        int tileY = GridUtils.snapToTile(worldY, tileHeight);

        previewTileX = tileX;
        previewTileY = tileY;
        previewValid = availableStoneWalls > 0
                && collisionManager.canPlaceWall(tileX, tileY, player, wallsByTileKey.values());

        WallSpriteSelection previewSelection = resolvePreviewSprite(tileX, tileY);
        String spriteKey = previewSelection.getSpriteKey();
        Image previewImage = assetManager.getWallImage(spriteKey);
        double renderX = tileX * tileWidth + (tileWidth - Wall.WALL_RENDER_WIDTH) / 2.0 + Wall.WALL_ANCHOR_X;
        double renderY = tileY * tileHeight + (tileHeight - Wall.WALL_RENDER_HEIGHT) / 2.0 + Wall.WALL_ANCHOR_Y;
        int neighborMask = previewSelection.getNeighborMask();

        wallPreview.setVisible(true);
        wallPreview.setValid(previewValid);
        wallPreview.setTileX(tileX);
        wallPreview.setTileY(tileY);
        wallPreview.setRenderX(renderX);
        wallPreview.setRenderY(renderY);
        wallPreview.setWidth(Wall.WALL_RENDER_WIDTH);
        wallPreview.setHeight(Wall.WALL_RENDER_HEIGHT);
        wallPreview.setSpriteKey(spriteKey);
        wallPreview.setRotationLabel(getCurrentRotationLabel());
        wallPreview.setRotationDegrees(previewSelection.getRotationDegrees());
        wallPreview.setNeighborMask(neighborMask);
        wallPreview.setImage(previewImage);
        wallPreview.setOpacity(0.5);

        String previewKey = tileX + ":" + tileY + ":" + spriteKey + ":" + getCurrentRotationLabel() + ":" + previewValid;
        if (DEBUG_WALL && !previewKey.equals(lastLoggedPreviewKey)) {
            System.out.println("Mouse world position: " + (int) worldX + ", " + (int) worldY);
            System.out.println("Preview grid position: " + tileX + ", " + tileY);
            System.out.println("Current rotation: " + getCurrentRotationLabel());
            System.out.println("previewSprite=" + spriteKey);
            System.out.println("neighborMask=" + neighborMask);
            lastLoggedPreviewKey = previewKey;
        }
    }

    /**
     * tryPlaceSelectedItem:
     * - Dat wall that vao world neu preview dang hop le.
     * - Wall don le nhan huong xoay hien tai, nhung sau do van duoc auto-connect voi hang xom can cap nhat.
     */
    public boolean tryPlaceSelectedItem(Player player, int availableStoneWalls) {
        System.out.println("stone_wall count: " + availableStoneWalls);
        System.out.println("Try place wall at " + previewTileX + ", " + previewTileY);

        if (buildMode != BuildMode.BUILD_WALL_MODE || !STONE_WALL_ITEM_ID.equals(selectedItemId)) {
            System.out.println("Place wall failed: invalid position");
            return false;
        }
        if (availableStoneWalls <= 0 || !previewValid) {
            System.out.println("Place wall failed: invalid position");
            return false;
        }
        if (!collisionManager.canPlaceWall(previewTileX, previewTileY, player, wallsByTileKey.values())) {
            previewValid = false;
            System.out.println("Place wall failed: invalid position");
            return false;
        }

        String initialSpriteKey = wallPreview.getSpriteKey();
        Wall wall = new Wall(
                previewTileX,
                previewTileY,
                collisionManager.getTileWidth(),
                collisionManager.getTileHeight(),
                initialSpriteKey,
                wallPreview.getRotationDegrees(),
                assetManager.getWallImage(initialSpriteKey)
        );
        wallsByTileKey.put(key(previewTileX, previewTileY), wall);
        refreshWallAndNeighbors(previewTileX, previewTileY);
        System.out.println("Place wall tileX=" + previewTileX + " tileY=" + previewTileY + " sprite=" + wall.getAssetKey());
        System.out.println("Place wall success");
        return true;
    }

    public Collection<Wall> getWalls() {
        return Collections.unmodifiableCollection(wallsByTileKey.values());
    }

    public BuildMode getBuildMode() {
        return buildMode;
    }

    public WallPreview getWallPreview() {
        return wallPreview;
    }

    public boolean isPreviewVisible() {
        return wallPreview.isVisible();
    }

    public boolean hasWallAt(int tileX, int tileY) {
        return wallsByTileKey.containsKey(key(tileX, tileY));
    }

    /**
     * clearWalls:
     * - Xoa toan bo wall runtime khi tao world moi.
     * - Vi sao can: neu khong reset thi PLAY world moi van keo theo tuong cua session cu.
     */
    public void clearWalls() {
        wallsByTileKey.clear();
        previewTileX = -1;
        previewTileY = -1;
        previewValid = false;
        wallPreview.setVisible(false);
    }

    public String getCurrentRotationLabel() {
        return ROTATION_LABELS[rotationIndex];
    }

    private void refreshWallAndNeighbors(int tileX, int tileY) {
        refreshWallSprite(tileX, tileY);
        refreshWallSprite(tileX, tileY - 1);
        refreshWallSprite(tileX + 1, tileY);
        refreshWallSprite(tileX, tileY + 1);
        refreshWallSprite(tileX - 1, tileY);
    }

    private void refreshWallSprite(int tileX, int tileY) {
        Wall wall = wallsByTileKey.get(key(tileX, tileY));
        if (wall == null) {
            return;
        }
        // Refresh sprite cua wall da dat:
        // - Giu nguyen goc xoay hien co cua wall thang.
        // - Chi de resolver auto xoay trong truong hop thuc su la wall goc.
        WallSpriteSelection selection = wallSpriteResolver.resolve(
                tileX,
                tileY,
                wallsByTileKey,
                wall.getRotationDegrees()
        );
        wall.updateSprite(
                selection.getSpriteKey(),
                selection.getRotationDegrees(),
                assetManager.getWallImage(selection.getSpriteKey())
        );
    }

    private WallSpriteSelection resolvePreviewSprite(int tileX, int tileY) {
        boolean north = hasWall(tileX, tileY - 1);
        boolean east = hasWall(tileX + 1, tileY);
        boolean south = hasWall(tileX, tileY + 1);
        boolean west = hasWall(tileX - 1, tileY);
        int mask = computeNeighborMask(tileX, tileY);

        // Khong co hang xom:
        // - Preview phai the hien huong xoay hien tai cua nguoi choi.
        if (!north && !east && !south && !west) {
            return new WallSpriteSelection("wall_straight_base", ROTATION_DEGREES[rotationIndex], mask);
        }

        // Co hang xom:
        // - Chi auto xoay neu roi vao truong hop corner.
        // - Neu chi la wall thang/noi don gian thi van giu goc xoay player dang chon bang Q.
        return wallSpriteResolver.resolve(tileX, tileY, wallsByTileKey, ROTATION_DEGREES[rotationIndex]);
    }

    private int computeNeighborMask(int tileX, int tileY) {
        boolean north = hasWall(tileX, tileY - 1);
        boolean east = hasWall(tileX + 1, tileY);
        boolean south = hasWall(tileX, tileY + 1);
        boolean west = hasWall(tileX - 1, tileY);
        return (north ? 1 : 0)
                | (east ? 2 : 0)
                | (south ? 4 : 0)
                | (west ? 8 : 0);
    }

    private boolean hasWall(int tileX, int tileY) {
        return wallsByTileKey.containsKey(key(tileX, tileY));
    }

    private String key(int tileX, int tileY) {
        return tileX + ":" + tileY;
    }
}
