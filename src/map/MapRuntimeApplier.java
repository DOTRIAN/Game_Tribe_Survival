package map;

import system.resource.ResourceManager;
import system.resource.TileResourceAdapter;

import java.util.ArrayList;
import java.util.List;

public final class MapRuntimeApplier {
    private MapRuntimeApplier() {
    }

    public static MapRuntimeData prepare(GameMapDefinition definition, ResourceManager resourceManager) {
        if (definition == null) {
            return new MapRuntimeData(null, List.of(), null, 0.0, 0.0);
        }

        MapData loadedMap = GameMapFactory.loadMapData(definition.tiledMapPath());
        List<MapObjectData> runtimeObjects = new ArrayList<>();
        double worldWidth;
        double worldHeight;

        if (loadedMap != null) {
            runtimeObjects.addAll(loadedMap.getCollisionObjects());
            runtimeObjects.addAll(new TileResourceAdapter().buildResourceObjects(loadedMap));
            worldWidth = loadedMap.getPixelWidth();
            worldHeight = loadedMap.getPixelHeight();
        } else {
            double[] imageSize = GameMapFactory.readImageSize(definition.backgroundImagePath());
            worldWidth = imageSize[0];
            worldHeight = imageSize[1];
        }

        TileCollisionResolver resolver = loadedMap == null ? null : new TileCollisionResolver(loadedMap, resourceManager);
        return new MapRuntimeData(loadedMap, runtimeObjects, resolver, worldWidth, worldHeight);
    }
}
