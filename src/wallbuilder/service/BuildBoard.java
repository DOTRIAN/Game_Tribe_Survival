package wallbuilder.service;

import wallbuilder.model.HotbarItem;
import wallbuilder.model.TileType;

/**
 * BuildBoard:
 * - Quan ly du lieu o luoi trong man hinh xay dung.
 * - Class nay chi xu ly data, khong phu thuoc JavaFX, nen de test va de hoc OOP.
 */
public class BuildBoard {
    private final int rows;
    private final int columns;
    private final TileType[][] tiles;

    public BuildBoard(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.tiles = new TileType[rows][columns];
        clear();
    }

    /**
     * clear:
     * - Dua toan bo ban xay dung ve trang thai rong.
     * - Dung khi khoi tao hoac muon reset nhanh.
     */
    public void clear() {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                tiles[row][column] = TileType.EMPTY;
            }
        }
    }

    /**
     * placeItem:
     * - Input: vi tri o va item dang chon trong hotbar.
     * - Output: true neu dat thanh cong, false neu item rong hoac o ngoai bien.
     * - Tac dong: cap nhat tile tai vi tri nguoi choi click.
     */
    public boolean placeItem(int row, int column, HotbarItem hotbarItem) {
        if (!isInside(row, column) || hotbarItem == null || hotbarItem == HotbarItem.EMPTY) {
            return false;
        }
        tiles[row][column] = mapHotbarItemToTile(hotbarItem);
        return true;
    }

    /**
     * removeTile:
     * - Input: vi tri can xoa.
     * - Output: true neu xoa thanh cong.
     * - Tac dong: dua o ve trang thai rong.
     */
    public boolean removeTile(int row, int column) {
        if (!isInside(row, column)) {
            return false;
        }
        tiles[row][column] = TileType.EMPTY;
        return true;
    }

    public TileType getTile(int row, int column) {
        if (!isInside(row, column)) {
            return TileType.EMPTY;
        }
        return tiles[row][column];
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    private boolean isInside(int row, int column) {
        return row >= 0 && row < rows && column >= 0 && column < columns;
    }

    private TileType mapHotbarItemToTile(HotbarItem hotbarItem) {
        return switch (hotbarItem) {
            case SMALL_STONE -> TileType.SMALL_STONE;
            case STONE_WALL -> TileType.STONE_WALL;
            case EMPTY -> TileType.EMPTY;
        };
    }
}
