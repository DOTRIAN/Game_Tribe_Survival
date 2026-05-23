package buildsystem.fence;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FenceRenderer {
    public static final String SINGLE_SPRITE_KEY = "wood_fence_single";
    public static final String DOUBLE_SPRITE_KEY = "wood_fence_double";
    public static final String CONNECTOR_SPRITE_KEY = "wood_fence_connector";
    public static final String FLAG_RESERVED_SPRITE_KEY = "wood_fence_flag_reserved";
    public static final String ICON_SPRITE_KEY = "wood_fence_icon";

    private static final Path SHEET_PATH = Path.of("assets", "wall.png");

    // Bounding boxes x1,y1,x2,y2 duoc lay tu sprite thuc te trong wall.png.
    private static final int[] SINGLE_BOUNDS = {88, 451, 215, 858};
    private static final int[] DOUBLE_BOUNDS = {292, 468, 529, 849};
    private static final int[] FLAG_BOUNDS = {598, 410, 746, 889};
    private FenceRenderer() {
    }

    public static Map<String, Image> loadSprites() {
        Map<String, Image> sprites = new LinkedHashMap<>();
        if (!Files.isRegularFile(SHEET_PATH)) {
            System.out.println("Failed to load fence sheet: " + SHEET_PATH);
            return sprites;
        }

        Image sheet = new Image(SHEET_PATH.toUri().toString());
        if (sheet.isError()) {
            System.out.println("Failed to load fence sheet: " + SHEET_PATH);
            return sprites;
        }

        Image single = cropSprite(sheet, SINGLE_BOUNDS);
        Image doubled = cropSprite(sheet, DOUBLE_BOUNDS);
        Image flag = cropSprite(sheet, FLAG_BOUNDS);
        if (single == null || doubled == null || flag == null) {
            System.out.println("Fence sprite crop failed: " + SHEET_PATH);
            return sprites;
        }

        sprites.put(SINGLE_SPRITE_KEY, single);
        sprites.put(DOUBLE_SPRITE_KEY, doubled);
        sprites.put(CONNECTOR_SPRITE_KEY, createConnectorSprite(doubled));
        sprites.put(FLAG_RESERVED_SPRITE_KEY, flag);
        sprites.put(ICON_SPRITE_KEY, createShopIcon(single));
        return sprites;
    }

    private static Image cropSprite(Image sheet, int[] bounds) {
        PixelReader reader = sheet.getPixelReader();
        if (reader == null || bounds == null || bounds.length != 4) {
            return null;
        }
        int minX = Math.max(0, bounds[0]);
        int minY = Math.max(0, bounds[1]);
        int maxX = Math.min((int) Math.round(sheet.getWidth()) - 1, bounds[2]);
        int maxY = Math.min((int) Math.round(sheet.getHeight()) - 1, bounds[3]);
        if (maxX < minX || maxY < minY) {
            return null;
        }

        WritableImage cropped = new WritableImage(reader, minX, minY, maxX - minX + 1, maxY - minY + 1);
        return whitenToTransparent(cropped);
    }

    private static Image whitenToTransparent(Image source) {
        PixelReader reader = source.getPixelReader();
        if (reader == null) {
            return source;
        }
        int width = (int) Math.round(source.getWidth());
        int height = (int) Math.round(source.getHeight());
        WritableImage output = new WritableImage(width, height);
        PixelWriter writer = output.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                writer.setColor(x, y, isBackground(color) ? Color.TRANSPARENT : color);
            }
        }
        return trimTransparentBounds(output);
    }

    private static Image trimTransparentBounds(Image source) {
        PixelReader reader = source.getPixelReader();
        if (reader == null) {
            return source;
        }
        int width = (int) Math.round(source.getWidth());
        int height = (int) Math.round(source.getHeight());
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (reader.getColor(x, y).getOpacity() <= 0.001) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        if (maxX < minX || maxY < minY) {
            return source;
        }
        return new WritableImage(reader, minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static Image createShopIcon(Image single) {
        double canvasSize = 96.0;
        double targetHeight = 74.0;
        double scale = targetHeight / Math.max(1.0, single.getHeight());
        double targetWidth = single.getWidth() * scale;

        Canvas canvas = new Canvas(canvasSize, canvasSize);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setImageSmoothing(false);
        double drawX = (canvasSize - targetWidth) / 2.0;
        double drawY = canvasSize - 10.0 - targetHeight;
        graphics.drawImage(single, drawX, drawY, targetWidth, targetHeight);

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        WritableImage image = new WritableImage((int) canvasSize, (int) canvasSize);
        canvas.snapshot(parameters, image);
        return image;
    }

    private static Image createConnectorSprite(Image doubled) {
        PixelReader reader = doubled == null ? null : doubled.getPixelReader();
        if (reader == null) {
            return doubled;
        }
        int width = (int) Math.round(doubled.getWidth());
        int height = (int) Math.round(doubled.getHeight());
        if (width <= 0 || height <= 0) {
            return doubled;
        }

        int minY = Math.max(0, (int) Math.round(height * 0.34));
        int maxY = Math.min(height - 1, (int) Math.round(height * 0.55));
        int minX = Math.max(0, (int) Math.round(width * 0.18));
        int maxX = Math.min(width - 1, (int) Math.round(width * 0.82));

        WritableImage slice = new WritableImage(maxX - minX + 1, maxY - minY + 1);
        PixelWriter writer = slice.getPixelWriter();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                writer.setColor(x - minX, y - minY, reader.getColor(x, y));
            }
        }
        return trimTransparentBounds(slice);
    }

    private static boolean isBackground(Color color) {
        if (color == null || color.getOpacity() <= 0.001) {
            return true;
        }
        return color.getRed() >= 0.975
                && color.getGreen() >= 0.975
                && color.getBlue() >= 0.975;
    }
}
