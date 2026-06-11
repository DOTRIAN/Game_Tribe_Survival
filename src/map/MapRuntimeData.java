package map;

import java.util.List;

public record MapRuntimeData(
        MapData mapData,
        List<MapObjectData> runtimeObjects,
        TileCollisionResolver tileCollisionResolver,
        double worldWidth,
        double worldHeight
) {
}
