package map;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class GameMapFactory {
    private GameMapFactory() {
    }

    public static MapData tryLoadMainMap() {
        String preferredPath = resolveMainMapPath();
        if (preferredPath != null && !preferredPath.isBlank()) {
            try {
                return new TiledMapLoader().load(preferredPath);
            } catch (Exception error) {
                System.out.println("Cannot load main map " + preferredPath + ": " + error.getMessage());
            }
        }
        return null;
    }

    public static String resolveMainMapPath() {
        List<String> candidates = List.of(
                "assets/Map_Game/mapdep.tmx",
                "assets/Map_Game/map.tmx",
                "assets/maps/mapdemo.tmx"
        );
        for (String candidate : candidates) {
            if (candidate != null && Files.exists(Path.of(candidate))) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    public static MapManager createMapManager(MapData loadedMainMap,
                                              double playerWidth,
                                              double playerHeight,
                                              String bossMapImagePath) {
        int tileWidth = loadedMainMap == null ? 16 : Math.max(1, loadedMainMap.getTileWidth());
        int triggerX = tileWidth * 112;
        int triggerY = tileWidth * 8;
        Rectangle2D bossEntranceTrigger = new Rectangle2D(triggerX, triggerY, tileWidth * 3.0, tileWidth * 6.0);
        double[] bossSize = readImageSize(bossMapImagePath);
        double bossSpawnX = Math.max(32.0, bossSize[0] * 0.5 - playerWidth * 0.5);
        double bossSpawnY = Math.max(32.0, bossSize[1] - playerHeight - 96.0);
        return new MapManager(
                MapType.MAIN_MAP,
                List.of(
                        new GameMapDefinition(
                                MapType.MAIN_MAP,
                                resolveMainMapPath(),
                                null,
                                0,
                                0,
                                true,
                                true,
                                true,
                                true,
                                true,
                                bossEntranceTrigger,
                                MapType.BOSS_MAP
                        ),
                        new GameMapDefinition(
                                MapType.BOSS_MAP,
                                null,
                                bossMapImagePath,
                                bossSpawnX,
                                bossSpawnY,
                                false,
                                false,
                                false,
                                false,
                                false,
                                null,
                                null
                        )
                )
        );
    }

    public static MapData loadMapData(String tmxPath) {
        if (tmxPath == null || tmxPath.isBlank()) {
            return null;
        }
        try {
            return new TiledMapLoader().load(tmxPath);
        } catch (Exception error) {
            System.out.println("Cannot load map " + tmxPath + ": " + error.getMessage());
            return null;
        }
    }

    public static double[] readImageSize(String imagePath) {
        try {
            Image image = new Image(Path.of(imagePath).toUri().toString(), false);
            if (!image.isError() && image.getWidth() > 0 && image.getHeight() > 0) {
                return new double[]{image.getWidth(), image.getHeight()};
            }
        } catch (Exception ignored) {
        }
        return new double[]{1280.0, 720.0};
    }
}
