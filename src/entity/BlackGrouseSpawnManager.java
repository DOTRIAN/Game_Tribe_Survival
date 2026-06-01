package entity;

import buildsystem.core.BuildManager;
import buildsystem.core.CollisionManager;
import buildsystem.object.BuildObject;
import map.MapData;

import java.util.Collection;
import java.util.List;
import java.util.Random;

public class BlackGrouseSpawnManager {
    private static final int MIN_SPAWN_COUNT = 9;
    private static final int MAX_SPAWN_COUNT = 12;
    private static final int MAX_SPAWN_ATTEMPTS = 600;
    private static final int MIN_PLAYER_DISTANCE_TILES = 5;

    private final MapData mapData;
    private final CollisionManager collisionManager;
    private final BuildManager buildManager;
    private final Player player;
    private final Random random;
    private List<Enemy> activeEnemies;

    public BlackGrouseSpawnManager(MapData mapData,
                                   CollisionManager collisionManager,
                                   BuildManager buildManager,
                                   Player player,
                                   Random random) {
        this.mapData = mapData;
        this.collisionManager = collisionManager;
        this.buildManager = buildManager;
        this.player = player;
        this.random = random == null ? new Random() : random;
        this.activeEnemies = List.of();
    }

    public void spawnInitialFlock(List<Enemy> enemies) {
        if (enemies == null) {
            return;
        }
        this.activeEnemies = enemies;
        int existing = 0;
        for (Enemy enemy : enemies) {
            if (enemy != null && "BLACK_GROUSE".equalsIgnoreCase(enemy.getEnemyType()) && !enemy.shouldRemoveFromWorld()) {
                existing++;
            }
        }
        if (existing > 0) {
            return;
        }

        int targetCount = MIN_SPAWN_COUNT + random.nextInt(MAX_SPAWN_COUNT - MIN_SPAWN_COUNT + 1);
        int attempts = 0;
        while (existing < targetCount && attempts < MAX_SPAWN_ATTEMPTS) {
            attempts++;
            double[] candidate = randomSpawnPosition();
            if (candidate == null) {
                continue;
            }
            BlackGrouseEnemy enemy = new BlackGrouseEnemy(
                    candidate[0],
                    candidate[1],
                    tileWidth(),
                    tileHeight(),
                    random,
                    this::canOccupy
            );
            if (!canOccupy(enemy, enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight())) {
                continue;
            }
            enemies.add(enemy);
            existing++;
        }
    }

    private double[] randomSpawnPosition() {
        int tileW = tileWidth();
        int tileH = tileHeight();
        int tilesX = mapData == null ? Math.max(1, collisionManager.getTileWidth()) : Math.max(1, mapData.getWidthInTiles());
        int tilesY = mapData == null ? Math.max(1, collisionManager.getTileHeight()) : Math.max(1, mapData.getHeightInTiles());
        if (mapData == null) {
            tilesX = Math.max(1, (int) Math.floor(collisionManager.getWorldWidth() / tileW));
            tilesY = Math.max(1, (int) Math.floor(collisionManager.getWorldHeight() / tileH));
        }

        int tileX = random.nextInt(tilesX);
        int tileY = random.nextInt(tilesY);
        double x = tileX * tileW + (tileW - BlackGrouseEnemy.defaultRenderWidth()) * 0.5;
        double y = tileY * tileH + (tileH - BlackGrouseEnemy.defaultRenderHeight()) * 0.5;
        return new double[]{x, y};
    }

    private boolean canOccupy(BlackGrouseEnemy enemy, double x, double y, double width, double height) {
        if (collisionManager == null || player == null) {
            return false;
        }
        if (collisionManager.isBlockedByStaticObjects(x, y, width, height)
                || collisionManager.isBlockedByTerrain(x, y, width, height)
                || collisionManager.isBlockedByWater(x, y, width, height)) {
            return false;
        }

        Collection<BuildObject> placedObjects = buildManager == null ? List.of() : buildManager.getPlacedObjects();
        if (collisionManager.intersectsPlacedBuildObject(x, y, width, height, placedObjects)) {
            return false;
        }

        double minDistance = Math.max(tileWidth(), tileHeight()) * MIN_PLAYER_DISTANCE_TILES;
        double dx = (x + width * 0.5) - player.getCenterX();
        double dy = (y + height * 0.5) - player.getCenterY();
        if (Math.sqrt(dx * dx + dy * dy) < minDistance) {
            return false;
        }

        return true;
    }

    private int tileWidth() {
        return mapData == null ? collisionManager.getTileWidth() : Math.max(1, mapData.getTileWidth());
    }

    private int tileHeight() {
        return mapData == null ? collisionManager.getTileHeight() : Math.max(1, mapData.getTileHeight());
    }
}
