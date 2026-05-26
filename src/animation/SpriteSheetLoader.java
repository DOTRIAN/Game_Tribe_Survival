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

    public static Image[] loadHorizontalStrip(String imagePath, int frameCount) {
        Image spriteSheet = new Image(imagePath);
        PixelReader pixelReader = spriteSheet.getPixelReader();
        if (pixelReader == null || frameCount <= 0) {
            return new Image[0];
        }

        int sheetWidth = (int) Math.round(spriteSheet.getWidth());
        int sheetHeight = (int) Math.round(spriteSheet.getHeight());
        Image[] frames = new Image[frameCount];
        for (int index = 0; index < frameCount; index++) {
            int startX = (int) Math.round(index * sheetWidth / (double) frameCount);
            int endX = (int) Math.round((index + 1) * sheetWidth / (double) frameCount);
            int frameWidth = Math.max(1, endX - startX);
            if (startX + frameWidth > sheetWidth) {
                frameWidth = Math.max(1, sheetWidth - startX);
            }
            frames[index] = new WritableImage(pixelReader, startX, 0, frameWidth, sheetHeight);
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
