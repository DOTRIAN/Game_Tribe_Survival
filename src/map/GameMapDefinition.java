package map;

import javafx.geometry.Rectangle2D;

public record GameMapDefinition(
        MapType type,
        String tiledMapPath,
        String backgroundImagePath,
        double spawnX,
        double spawnY,
        boolean useBaseCampSpawn,
        boolean showBaseCamp,
        boolean allowEnemySpawns,
        boolean allowChunkResources,
        boolean allowBuilding,
        Rectangle2D transitionTrigger,
        MapType triggerTarget
) {
    public boolean hasFixedSpawn() {
        return !useBaseCampSpawn;
    }
}
