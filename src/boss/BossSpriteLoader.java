package boss;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BossSpriteLoader {
    private static final Pattern TRAILING_NUMBER_PATTERN = Pattern.compile("(\\d+)(?=\\.[^.]+$)");

    private BossSpriteLoader() {
    }

    public static Image[] loadSequence(String folderPath) {
        if (folderPath == null || folderPath.isBlank()) {
            return new Image[0];
        }
        Path folder = Path.of(folderPath);
        if (!Files.isDirectory(folder)) {
            return new Image[0];
        }

        List<Image> frames = new ArrayList<>();
        try (var children = Files.list(folder)) {
            children
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.comparingInt(BossSpriteLoader::extractFrameOrder))
                    .forEach(path -> frames.add(new Image(path.toUri().toString(), false)));
        } catch (IOException ignored) {
            return new Image[0];
        }
        return frames.toArray(Image[]::new);
    }

    public static int findMaxWidth(Iterable<Image[]> sequences) {
        int max = 0;
        if (sequences == null) {
            return max;
        }
        for (Image[] sequence : sequences) {
            if (sequence == null) {
                continue;
            }
            for (Image frame : sequence) {
                if (frame == null || frame.isError()) {
                    continue;
                }
                max = Math.max(max, (int) Math.round(frame.getWidth()));
            }
        }
        return max;
    }

    public static int findMaxHeight(Iterable<Image[]> sequences) {
        int max = 0;
        if (sequences == null) {
            return max;
        }
        for (Image[] sequence : sequences) {
            if (sequence == null) {
                continue;
            }
            for (Image frame : sequence) {
                if (frame == null || frame.isError()) {
                    continue;
                }
                max = Math.max(max, (int) Math.round(frame.getHeight()));
            }
        }
        return max;
    }

    public static Image[] normalizeFrames(Image[] frames, int targetWidth, int targetHeight) {
        if (frames == null || frames.length == 0 || targetWidth <= 0 || targetHeight <= 0) {
            return frames == null ? new Image[0] : frames;
        }
        Image[] normalized = new Image[frames.length];
        for (int index = 0; index < frames.length; index++) {
            Image frame = frames[index];
            if (frame == null || frame.isError()) {
                normalized[index] = frame;
                continue;
            }
            int frameWidth = (int) Math.round(frame.getWidth());
            int frameHeight = (int) Math.round(frame.getHeight());
            if (frameWidth == targetWidth && frameHeight == targetHeight) {
                normalized[index] = frame;
                continue;
            }

            PixelReader reader = frame.getPixelReader();
            if (reader == null) {
                normalized[index] = frame;
                continue;
            }

            WritableImage padded = new WritableImage(targetWidth, targetHeight);
            PixelWriter writer = padded.getPixelWriter();
            int offsetX = Math.max(0, (targetWidth - frameWidth) / 2);
            int offsetY = Math.max(0, targetHeight - frameHeight);
            for (int y = 0; y < frameHeight; y++) {
                for (int x = 0; x < frameWidth; x++) {
                    writer.setColor(offsetX + x, offsetY + y, reader.getColor(x, y));
                }
            }
            normalized[index] = padded;
        }
        return normalized;
    }

    public static Image[] trimNearBlackFrames(Image[] frames) {
        if (frames == null || frames.length == 0) {
            return frames == null ? new Image[0] : frames;
        }
        Image[] trimmed = new Image[frames.length];
        for (int index = 0; index < frames.length; index++) {
            trimmed[index] = trimNearBlackFrame(removeNearBlackBackground(frames[index]));
        }
        return trimmed;
    }

    public static Image[] trimTransparentVerticalKeepWidth(Image[] frames) {
        if (frames == null || frames.length == 0) {
            return frames == null ? new Image[0] : frames;
        }
        Image[] trimmed = new Image[frames.length];
        for (int index = 0; index < frames.length; index++) {
            trimmed[index] = trimTransparentVerticalKeepWidth(frames[index]);
        }
        return trimmed;
    }

    public static Image[] removeNearBlackBackground(Image[] frames) {
        if (frames == null || frames.length == 0) {
            return frames == null ? new Image[0] : frames;
        }
        Image[] transparent = new Image[frames.length];
        for (int index = 0; index < frames.length; index++) {
            transparent[index] = removeNearBlackBackground(frames[index]);
        }
        return transparent;
    }

    public static Image removeNearBlackBackground(Image frame) {
        if (frame == null || frame.isError()) {
            return frame;
        }
        PixelReader reader = frame.getPixelReader();
        if (reader == null) {
            return frame;
        }
        int width = (int) Math.round(frame.getWidth());
        int height = (int) Math.round(frame.getHeight());
        WritableImage transparent = new WritableImage(width, height);
        PixelWriter writer = transparent.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                writer.setColor(x, y, isNearBlackBackground(color) ? Color.TRANSPARENT : color);
            }
        }
        return transparent;
    }

    public static Image[] removeEdgeBackground(Image[] frames, double tolerance) {
        if (frames == null || frames.length == 0) {
            return frames == null ? new Image[0] : frames;
        }
        Image[] cleaned = new Image[frames.length];
        for (int index = 0; index < frames.length; index++) {
            cleaned[index] = removeEdgeBackground(frames[index], tolerance);
        }
        return cleaned;
    }

    public static Image removeEdgeBackground(Image frame, double tolerance) {
        if (frame == null || frame.isError()) {
            return frame;
        }
        PixelReader reader = frame.getPixelReader();
        if (reader == null) {
            return frame;
        }
        int width = (int) Math.round(frame.getWidth());
        int height = (int) Math.round(frame.getHeight());
        WritableImage result = new WritableImage(width, height);
        PixelWriter writer = result.getPixelWriter();
        boolean[] visited = new boolean[width * height];
        Deque<int[]> queue = new ArrayDeque<>();

        for (int x = 0; x < width; x++) {
            seedEdgePixel(reader, queue, visited, width, x, 0, tolerance);
            seedEdgePixel(reader, queue, visited, width, x, height - 1, tolerance);
        }
        for (int y = 0; y < height; y++) {
            seedEdgePixel(reader, queue, visited, width, 0, y, tolerance);
            seedEdgePixel(reader, queue, visited, width, width - 1, y, tolerance);
        }

        boolean[] background = new boolean[width * height];
        while (!queue.isEmpty()) {
            int[] point = queue.removeFirst();
            int x = point[0];
            int y = point[1];
            int index = y * width + x;
            background[index] = true;
            Color base = reader.getColor(x, y);
            visitNeighbor(reader, queue, visited, width, height, x + 1, y, base, tolerance);
            visitNeighbor(reader, queue, visited, width, height, x - 1, y, base, tolerance);
            visitNeighbor(reader, queue, visited, width, height, x, y + 1, base, tolerance);
            visitNeighbor(reader, queue, visited, width, height, x, y - 1, base, tolerance);
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                Color color = reader.getColor(x, y);
                writer.setColor(x, y, background[index] ? Color.TRANSPARENT : color);
            }
        }
        return result;
    }

    public static Image trimTransparentVerticalKeepWidth(Image frame) {
        if (frame == null || frame.isError()) {
            return frame;
        }
        PixelReader reader = frame.getPixelReader();
        if (reader == null) {
            return frame;
        }
        int width = (int) Math.round(frame.getWidth());
        int height = (int) Math.round(frame.getHeight());
        int minY = height;
        int maxY = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                if (color.getOpacity() < 0.02) {
                    continue;
                }
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxY < minY) {
            return frame;
        }
        return new WritableImage(reader, 0, minY, width, maxY - minY + 1);
    }

    private static Image trimNearBlackFrame(Image frame) {
        if (frame == null || frame.isError()) {
            return frame;
        }
        PixelReader reader = frame.getPixelReader();
        if (reader == null) {
            return frame;
        }

        int width = (int) Math.round(frame.getWidth());
        int height = (int) Math.round(frame.getHeight());
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                if (isNearBlackBackground(color)) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        if (maxX < minX || maxY < minY) {
            return frame;
        }
        return new WritableImage(reader, minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static boolean isNearBlackBackground(Color color) {
        if (color == null) {
            return true;
        }
        return color.getOpacity() < 0.02
                || (color.getRed() <= 0.04 && color.getGreen() <= 0.04 && color.getBlue() <= 0.04);
    }

    private static void seedEdgePixel(PixelReader reader,
                                      Deque<int[]> queue,
                                      boolean[] visited,
                                      int width,
                                      int x,
                                      int y,
                                      double tolerance) {
        Color color = reader.getColor(x, y);
        if (color.getOpacity() < 0.01) {
            return;
        }
        int index = y * width + x;
        if (visited[index]) {
            return;
        }
        visited[index] = true;
        queue.addLast(new int[]{x, y});
    }

    private static void visitNeighbor(PixelReader reader,
                                      Deque<int[]> queue,
                                      boolean[] visited,
                                      int width,
                                      int height,
                                      int x,
                                      int y,
                                      Color base,
                                      double tolerance) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }
        int index = y * width + x;
        if (visited[index]) {
            return;
        }
        Color color = reader.getColor(x, y);
        if (color.getOpacity() < 0.01) {
            visited[index] = true;
            return;
        }
        if (colorDistance(base, color) > tolerance) {
            return;
        }
        visited[index] = true;
        queue.addLast(new int[]{x, y});
    }

    private static double colorDistance(Color a, Color b) {
        double dr = a.getRed() - b.getRed();
        double dg = a.getGreen() - b.getGreen();
        double db = a.getBlue() - b.getBlue();
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    private static int extractFrameOrder(Path path) {
        String fileName = path == null ? "" : path.getFileName().toString();
        Matcher matcher = TRAILING_NUMBER_PATTERN.matcher(fileName);
        if (!matcher.find()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }
}
