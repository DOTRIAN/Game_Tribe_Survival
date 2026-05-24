package drop;

import javafx.scene.image.Image;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InventoryIconLoader {
    private static final Map<String, Image> ICON_CACHE = new LinkedHashMap<>();

    private InventoryIconLoader() {
    }

    public static synchronized Image loadMainIcon(String directory) {
        if (directory == null || directory.isBlank()) {
            return null;
        }
        if (ICON_CACHE.containsKey(directory)) {
            return ICON_CACHE.get(directory);
        }

        File file = new File(directory, "main.png");
        if (!file.exists() || !file.isFile()) {
            ICON_CACHE.put(directory, null);
            return null;
        }

        Image raw = new Image(file.toURI().toString(), false);
        if (raw.isError() || raw.getWidth() <= 0 || raw.getHeight() <= 0 || raw.getPixelReader() == null) {
            ICON_CACHE.put(directory, null);
            return null;
        }

        Image prepared = DropSpriteLoader.prepareInventoryIcon(raw);
        ICON_CACHE.put(directory, prepared);
        return prepared;
    }
}
