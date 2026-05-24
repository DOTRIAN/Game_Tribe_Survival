package drop;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DropSpriteLoader {
    private static final Map<String, DropAnimation> ANIMATION_CACHE = new LinkedHashMap<>();

    private DropSpriteLoader() {
    }

    public static synchronized DropAnimation loadFrameSequence(String directory, int frameCount, long frameDurationNs) {
        String cacheKey = "seq|" + directory + "|" + frameCount + "|" + frameDurationNs;
        DropAnimation cached = ANIMATION_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Image> frames = new ArrayList<>();
        for (int index = 1; index <= Math.max(0, frameCount); index++) {
            Image frame = loadPreparedImage(directory + "/frame_" + index + ".png");
            if (frame != null) {
                frames.add(frame);
            }
        }

        DropAnimation animation = new DropAnimation(normalizeFrames(frames), frameDurationNs);
        ANIMATION_CACHE.put(cacheKey, animation);
        return animation;
    }

    public static synchronized DropAnimation loadSpriteSheet(String path,
                                                             int frameWidth,
                                                             int frameHeight,
                                                             int frameCount,
                                                             long frameDurationNs) {
        String cacheKey = "sheet|" + path + "|" + frameWidth + "|" + frameHeight + "|" + frameCount + "|" + frameDurationNs;
        DropAnimation cached = ANIMATION_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Image spriteSheet = loadRawImage(path);
        if (spriteSheet == null || spriteSheet.getPixelReader() == null) {
            DropAnimation empty = new DropAnimation(List.of(), frameDurationNs);
            ANIMATION_CACHE.put(cacheKey, empty);
            return empty;
        }

        int safeFrameWidth = Math.max(1, frameWidth);
        int safeFrameHeight = Math.max(1, frameHeight);
        int maxFramesByWidth = Math.max(1, (int) (spriteSheet.getWidth() / safeFrameWidth));
        int resolvedFrameCount = Math.max(1, Math.min(frameCount, maxFramesByWidth));
        if (spriteSheet.getHeight() < safeFrameHeight) {
            DropAnimation empty = new DropAnimation(List.of(), frameDurationNs);
            ANIMATION_CACHE.put(cacheKey, empty);
            return empty;
        }

        List<Image> frames = new ArrayList<>(resolvedFrameCount);
        for (int index = 0; index < resolvedFrameCount; index++) {
            WritableImage rawFrame = new WritableImage(spriteSheet.getPixelReader(), index * safeFrameWidth, 0, safeFrameWidth, safeFrameHeight);
            Image preparedFrame = prepareDropFrame(rawFrame);
            if (preparedFrame != null) {
                frames.add(preparedFrame);
            }
        }

        DropAnimation animation = new DropAnimation(normalizeFrames(frames), frameDurationNs);
        ANIMATION_CACHE.put(cacheKey, animation);
        return animation;
    }

    static Image loadPreparedImage(String path) {
        Image raw = loadRawImage(path);
        return raw == null ? null : prepareDropFrame(raw);
    }

    static Image prepareInventoryIcon(Image raw) {
        if (raw == null || raw.isError() || raw.getPixelReader() == null) {
            return null;
        }
        return trimTransparentBounds(removeWhiteBackdrop(raw));
    }

    private static Image loadRawImage(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        Image image = new Image(file.toURI().toString(), false);
        if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0 || image.getPixelReader() == null) {
            return null;
        }
        return image;
    }

    private static Image prepareDropFrame(Image raw) {
        return trimTransparentBounds(removeWhiteBackdrop(raw));
    }

    private static List<Image> normalizeFrames(List<Image> frames) {
        if (frames == null || frames.isEmpty()) {
            return List.of();
        }

        int maxWidth = 1;
        int maxHeight = 1;
        for (Image frame : frames) {
            if (frame == null || frame.getPixelReader() == null) {
                continue;
            }
            maxWidth = Math.max(maxWidth, (int) Math.ceil(frame.getWidth()));
            maxHeight = Math.max(maxHeight, (int) Math.ceil(frame.getHeight()));
        }

        List<Image> normalized = new ArrayList<>(frames.size());
        for (Image frame : frames) {
            if (frame == null || frame.getPixelReader() == null) {
                continue;
            }
            normalized.add(centerOnTransparentCanvas(frame, maxWidth, maxHeight));
        }
        return List.copyOf(normalized);
    }

    private static Image centerOnTransparentCanvas(Image frame, int canvasWidth, int canvasHeight) {
        if (frame == null || frame.getPixelReader() == null) {
            return null;
        }
        int width = Math.max(1, canvasWidth);
        int height = Math.max(1, canvasHeight);
        WritableImage canvas = new WritableImage(width, height);
        PixelWriter writer = canvas.getPixelWriter();
        PixelReader reader = frame.getPixelReader();

        int drawX = Math.max(0, (width - (int) Math.ceil(frame.getWidth())) / 2);
        int drawY = Math.max(0, (height - (int) Math.ceil(frame.getHeight())) / 2);
        int frameWidth = (int) Math.ceil(frame.getWidth());
        int frameHeight = (int) Math.ceil(frame.getHeight());
        for (int y = 0; y < frameHeight; y++) {
            for (int x = 0; x < frameWidth; x++) {
                writer.setArgb(drawX + x, drawY + y, reader.getArgb(x, y));
            }
        }
        return canvas;
    }

    private static Image removeWhiteBackdrop(Image source) {
        if (source == null || source.getPixelReader() == null) {
            return source;
        }
        int width = (int) Math.ceil(source.getWidth());
        int height = (int) Math.ceil(source.getHeight());
        if (width <= 0 || height <= 0) {
            return source;
        }

        PixelReader reader = source.getPixelReader();
        boolean[] removeMask = new boolean[width * height];
        ArrayDeque<Integer> queue = new ArrayDeque<>();

        for (int x = 0; x < width; x++) {
            enqueueWhitePixel(reader, width, height, removeMask, queue, x, 0);
            enqueueWhitePixel(reader, width, height, removeMask, queue, x, height - 1);
        }
        for (int y = 0; y < height; y++) {
            enqueueWhitePixel(reader, width, height, removeMask, queue, 0, y);
            enqueueWhitePixel(reader, width, height, removeMask, queue, width - 1, y);
        }

        while (!queue.isEmpty()) {
            int index = queue.removeFirst();
            int x = index % width;
            int y = index / width;

            enqueueWhitePixel(reader, width, height, removeMask, queue, x - 1, y);
            enqueueWhitePixel(reader, width, height, removeMask, queue, x + 1, y);
            enqueueWhitePixel(reader, width, height, removeMask, queue, x, y - 1);
            enqueueWhitePixel(reader, width, height, removeMask, queue, x, y + 1);
        }

        boolean changed = false;
        WritableImage cleaned = new WritableImage(width, height);
        PixelWriter writer = cleaned.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (removeMask[index]) {
                    writer.setArgb(x, y, 0x00000000);
                    changed = true;
                } else {
                    writer.setArgb(x, y, reader.getArgb(x, y));
                }
            }
        }
        return changed ? cleaned : source;
    }

    private static void enqueueWhitePixel(PixelReader reader,
                                          int width,
                                          int height,
                                          boolean[] removeMask,
                                          ArrayDeque<Integer> queue,
                                          int x,
                                          int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }
        int index = y * width + x;
        if (removeMask[index]) {
            return;
        }
        int argb = reader.getArgb(x, y);
        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        if (alpha < 245) {
            return;
        }
        if (red < 245 || green < 245 || blue < 245) {
            return;
        }
        removeMask[index] = true;
        queue.addLast(index);
    }

    private static Image trimTransparentBounds(Image source) {
        if (source == null || source.getPixelReader() == null) {
            return source;
        }
        PixelReader reader = source.getPixelReader();
        int width = (int) Math.ceil(source.getWidth());
        int height = (int) Math.ceil(source.getHeight());
        if (width <= 0 || height <= 0) {
            return source;
        }

        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = (reader.getArgb(x, y) >>> 24) & 0xFF;
                if (alpha <= 0) {
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
        int croppedWidth = Math.max(1, maxX - minX + 1);
        int croppedHeight = Math.max(1, maxY - minY + 1);
        return new WritableImage(reader, minX, minY, croppedWidth, croppedHeight);
    }
}
