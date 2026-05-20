package animation;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

public final class SpriteSheetLoader {
    private SpriteSheetLoader() {
    }

    public static Image[] loadGrid(String imagePath, int columns, int rows) {
        Image spriteSheet = new Image(imagePath);
        int frameWidth = (int) spriteSheet.getWidth() / columns;
        int frameHeight = (int) spriteSheet.getHeight() / rows;
        return loadGrid(imagePath, columns, rows, frameWidth, frameHeight);
    }

    public static Image[] loadGrid(String imagePath, int columns, int rows, int frameWidth, int frameHeight) {
        Image spriteSheet = new Image(imagePath);
        PixelReader pixelReader = spriteSheet.getPixelReader();

        Image[] frames = new Image[columns * rows];
        int index = 0;

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int x = column * frameWidth;
                int y = row * frameHeight;
                frames[index] = new WritableImage(pixelReader, x, y, frameWidth, frameHeight);
                index++;
            }
        }

        return frames;
    }

    // Cat theo region bat dau tu (startX,startY) de lay dung row mong muon trong spritesheet.
    public static Image[] loadGridRegion(String imagePath,
                                         int startX,
                                         int startY,
                                         int columns,
                                         int rows,
                                         int frameWidth,
                                         int frameHeight) {
        Image spriteSheet = new Image(imagePath);
        PixelReader pixelReader = spriteSheet.getPixelReader();

        Image[] frames = new Image[columns * rows];
        int index = 0;

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int x = startX + column * frameWidth;
                int y = startY + row * frameHeight;
                frames[index] = new WritableImage(pixelReader, x, y, frameWidth, frameHeight);
                index++;
            }
        }

        return frames;
    }
}
