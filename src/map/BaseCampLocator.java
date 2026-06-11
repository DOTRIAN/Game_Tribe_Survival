package map;

import system.CollisionSystem;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BaseCampLocator {
    private record TileCoord(int x, int y) {}
    private record TileBounds(int minX, int minY, int maxX, int maxY, int tileCount) {}

    @FunctionalInterface
    public interface SpawnResolver {
        double[] resolve(double preferredX, double preferredY);
    }

    public MapObjectData resolveBaseCampMarker(MapData mapData,
                                               List<MapObjectData> mapCollisions,
                                               int defaultBaseCampHp) {
        MapObjectData objectMarker = findBaseCampObjectMarker(mapCollisions);
        if (objectMarker != null) {
            return objectMarker;
        }
        return detectBaseCampTileBounds(mapData, defaultBaseCampHp);
    }

    public double[] findPlayerSpawnNearBaseCamp(MapObjectData marker,
                                                double playerWidth,
                                                double playerHeight,
                                                double tileWidth,
                                                double tileHeight,
                                                SpawnResolver spawnResolver) {
        double gap = Math.max(tileWidth, tileHeight);
        double markerCenterX = marker.getX() + marker.getWidth() * 0.5;
        double markerCenterY = marker.getY() + marker.getHeight() * 0.5;
        double[][] candidates = {
                {markerCenterX - playerWidth * 0.5, marker.getY() + marker.getHeight() + gap},
                {markerCenterX - playerWidth * 0.5, marker.getY() - playerHeight - gap},
                {marker.getX() - playerWidth - gap, markerCenterY - playerHeight * 0.5},
                {marker.getX() + marker.getWidth() + gap, markerCenterY - playerHeight * 0.5}
        };
        for (double[] candidate : candidates) {
            double[] safeSpawn = spawnResolver.resolve(candidate[0], candidate[1]);
            if (!CollisionSystem.intersects(
                    marker.getX(),
                    marker.getY(),
                    marker.getWidth(),
                    marker.getHeight(),
                    safeSpawn[0],
                    safeSpawn[1],
                    playerWidth,
                    playerHeight)) {
                return safeSpawn;
            }
        }
        return spawnResolver.resolve(markerCenterX - playerWidth * 0.5, marker.getY() + marker.getHeight() + gap);
    }

    private MapObjectData findBaseCampObjectMarker(List<MapObjectData> mapCollisions) {
        if (mapCollisions == null) {
            return null;
        }
        for (MapObjectData object : mapCollisions) {
            if (object == null) {
                continue;
            }
            if (isBaseCampMarker(object.getName(), object.getType(), object.getProperties())) {
                return object;
            }
        }
        return null;
    }

    private MapObjectData detectBaseCampTileBounds(MapData mapData, int defaultBaseCampHp) {
        if (mapData == null) {
            return null;
        }
        TilePropertyCatalog catalog = new TilePropertyCatalog(mapData);
        int tileW = Math.max(1, mapData.getTileWidth());
        int tileH = Math.max(1, mapData.getTileHeight());
        Set<TileCoord> baseCampTiles = new LinkedHashSet<>();

        for (TileLayerData layer : mapData.getTileLayers()) {
            if (layer == null) {
                continue;
            }
            for (int y = 0; y < layer.getHeight(); y++) {
                for (int x = 0; x < layer.getWidth(); x++) {
                    int gid = layer.getGidAt(x, y);
                    if (gid <= 0) {
                        continue;
                    }
                    if (!isBaseCampTile(catalog.getPropertiesForGid(gid))) {
                        continue;
                    }
                    baseCampTiles.add(new TileCoord(x, y));
                }
            }
        }

        TileBounds bounds = selectBaseCampBounds(baseCampTiles, mapData);
        if (bounds == null) {
            return null;
        }

        double x = bounds.minX() * tileW;
        double y = bounds.minY() * tileH;
        double width = (bounds.maxX() - bounds.minX() + 1) * tileW;
        double height = (bounds.maxY() - bounds.minY() + 1) * tileH;
        return new MapObjectData(-999, "BaseCamp", "BaseCamp", x, y, width, height, Map.of("Hp_tent", String.valueOf(defaultBaseCampHp)));
    }

    private TileBounds selectBaseCampBounds(Set<TileCoord> baseCampTiles, MapData mapData) {
        if (baseCampTiles == null || baseCampTiles.isEmpty() || mapData == null) {
            return null;
        }

        Set<TileCoord> remaining = new LinkedHashSet<>(baseCampTiles);
        TileBounds bestBounds = null;
        double mapCenterX = mapData.getWidthInTiles() * 0.5;
        double mapCenterY = mapData.getHeightInTiles() * 0.5;

        while (!remaining.isEmpty()) {
            TileCoord start = remaining.iterator().next();
            TileBounds candidate = extractBaseCampClusterBounds(start, remaining);
            if (candidate == null) {
                continue;
            }
            if (bestBounds == null || isPreferredBaseCampBounds(candidate, bestBounds, mapCenterX, mapCenterY)) {
                bestBounds = candidate;
            }
        }
        return bestBounds;
    }

    private TileBounds extractBaseCampClusterBounds(TileCoord start, Set<TileCoord> remaining) {
        if (start == null || remaining == null || !remaining.remove(start)) {
            return null;
        }

        List<TileCoord> frontier = new ArrayList<>();
        frontier.add(start);
        int minX = start.x();
        int minY = start.y();
        int maxX = start.x();
        int maxY = start.y();
        int count = 0;

        for (int index = 0; index < frontier.size(); index++) {
            TileCoord current = frontier.get(index);
            count++;
            minX = Math.min(minX, current.x());
            minY = Math.min(minY, current.y());
            maxX = Math.max(maxX, current.x());
            maxY = Math.max(maxY, current.y());

            collectAdjacentBaseCampTile(new TileCoord(current.x() + 1, current.y()), remaining, frontier);
            collectAdjacentBaseCampTile(new TileCoord(current.x() - 1, current.y()), remaining, frontier);
            collectAdjacentBaseCampTile(new TileCoord(current.x(), current.y() + 1), remaining, frontier);
            collectAdjacentBaseCampTile(new TileCoord(current.x(), current.y() - 1), remaining, frontier);
        }

        return new TileBounds(minX, minY, maxX, maxY, count);
    }

    private void collectAdjacentBaseCampTile(TileCoord candidate, Set<TileCoord> remaining, List<TileCoord> frontier) {
        if (candidate == null || remaining == null || frontier == null) {
            return;
        }
        if (remaining.remove(candidate)) {
            frontier.add(candidate);
        }
    }

    private boolean isPreferredBaseCampBounds(TileBounds candidate,
                                              TileBounds currentBest,
                                              double mapCenterX,
                                              double mapCenterY) {
        if (candidate.tileCount() != currentBest.tileCount()) {
            return candidate.tileCount() > currentBest.tileCount();
        }
        return distanceToMapCenter(candidate, mapCenterX, mapCenterY) < distanceToMapCenter(currentBest, mapCenterX, mapCenterY);
    }

    private double distanceToMapCenter(TileBounds bounds, double mapCenterX, double mapCenterY) {
        double centerX = (bounds.minX() + bounds.maxX()) * 0.5;
        double centerY = (bounds.minY() + bounds.maxY()) * 0.5;
        double dx = centerX - mapCenterX;
        double dy = centerY - mapCenterY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private boolean isBaseCampTile(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return false;
        }
        if (hasTruthyProperty(properties, "isBaseCamp", "baseCamp", "BaseCamp")) {
            return true;
        }
        return parsePositiveInt(-1, properties.get("Hp_tent")) > 0;
    }

    private boolean isBaseCampMarker(String name, String type, Map<String, String> properties) {
        if (hasTruthyProperty(properties, "isBaseCamp", "baseCamp", "BaseCamp")) {
            return true;
        }
        if (parsePositiveInt(-1, properties == null ? null : properties.get("Hp_tent")) > 0) {
            return true;
        }
        return isBaseCampToken(name) || isBaseCampToken(type);
    }

    private boolean isBaseCampToken(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.equals("basecamp")
                || normalized.equals("mainhouse")
                || normalized.equals("main_house")
                || normalized.equals("main-house");
    }

    private boolean hasTruthyProperty(Map<String, String> properties, String... keys) {
        if (properties == null || properties.isEmpty() || keys == null) {
            return false;
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            String value = properties.get(key);
            if (value == null) {
                continue;
            }
            String normalized = value.trim().toLowerCase();
            if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private int parsePositiveInt(int fallback, String raw) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
