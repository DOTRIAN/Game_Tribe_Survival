package wallbuilder.world;

import javafx.scene.image.ImageView;

/**
 * Wall:
 * - 1 bức tường thật da duoc dat vao world.
 * - Luu vi tri theo tile va ImageView de render sprite.
 */
public class Wall {
    private final int row;
    private final int column;
    private final ImageView imageView;

    public Wall(int row, int column, ImageView imageView) {
        this.row = row;
        this.column = column;
        this.imageView = imageView;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public ImageView getImageView() {
        return imageView;
    }
}
