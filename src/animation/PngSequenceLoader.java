package animation;

import javafx.scene.image.Image;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public final class PngSequenceLoader {
    private static final Map<String, Image[]> CACHE = new HashMap<>();

    private PngSequenceLoader() {
    }

    public static synchronized Image[] loadPngSequence(String folderPath, String prefix, int frameCount) {
        int safeCount = Math.max(0, frameCount);
        String cacheKey = folderPath + "|" + prefix + "|" + safeCount;
        Image[] cached = CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Image[] frames = new Image[safeCount];
        for (int index = 0; index < safeCount; index++) {
            String fileName = prefix + String.format("%03d", index) + ".png";
            Path filePath = Paths.get(folderPath, fileName);
            if (!Files.exists(filePath)) {
                frames[index] = null;
                continue;
            }
            frames[index] = new Image(filePath.toUri().toString());
        }
        CACHE.put(cacheKey, frames);
        return frames;
    }
}
