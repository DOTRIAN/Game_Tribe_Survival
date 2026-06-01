package boss;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
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
