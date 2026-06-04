package entity;

import buildsystem.core.BuildManager;
import buildsystem.core.CollisionManager;
import buildsystem.object.BuildObject;
import map.MapData;

import java.util.Collection;
import java.util.List;
import java.util.Random;

public class WildlifeSpawnManager {
    private record SpawnPlan(WildlifeEnemy.Kind kind, int minCount, int maxCount) {}

    private static final SpawnPlan[] SPAWN_PLANS = {
            new SpawnPlan(WildlifeEnemy.Kind.BOAR, 3, 4),
            new SpawnPlan(WildlifeEnemy.Kind.DEER, 4, 6),
            new SpawnPlan(WildlifeEnemy.Kind.FOX, 4, 5),
            new SpawnPlan(WildlifeEnemy.Kind.HARE, 6, 8)
    };
    private static final int MAX_SPAWN_ATTEMPTS = 900;
    private static final int MIN_PLAYER_DISTANCE_TILES = 5;

    private final MapData mapData;
    private final CollisionManager collisionManager;
    private final BuildManager buildManager;
    private final Player player;
    private final Random random;

    public WildlifeSpawnManager(MapData mapData,
                                CollisionManager collisionManager,
                                BuildManager buildManager,
                                Player player,
                                Random random) {
        this.mapData = mapData;
        this.collisionManager = collisionManager;
        this.buildManager = buildManager;
        this.player = player;
        this.random = random == null ? new Random() : random;
    }

    public void spawnInitialWildlife(List<Enemy> enemies) {
        if (enemies == null) {
            return;
        }
        for (SpawnPlan plan : SPAWN_PLANS) {
            spawnByPlan(plan, enemies);
        }
    }

    private void spawnByPlan(SpawnPlan plan, List<Enemy> enemies) {
        int existing = 0;
        for (Enemy enemy : enemies) {
            if (enemy != null && enemy.isAlive() && plan.kind.enemyType().equalsIgnoreCase(enemy.getEnemyType()) && !enemy.shouldRemoveFromWorld()) {
                existing++;
            }
        }
        if (existing > 0) {
            return;
        }

        int targetCount = plan.minCount + random.nextInt(plan.maxCount - plan.minCount + 1);
        int attempts = 0;
        while (existing < targetCount && attempts < MAX_SPAWN_ATTEMPTS) {
            attempts++;
            double[] candidate = randomSpawnPosition(plan.kind);
            if (candidate == null) {
                continue;
            }
            WildlifeEnemy enemy = new WildlifeEnemy(
                    plan.kind,
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

    private double[] randomSpawnPosition(WildlifeEnemy.Kind kind) {
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
        double x = tileX * tileW + (tileW - WildlifeEnemy.defaultRenderWidth(kind)) * 0.5;
        double y = tileY * tileH + (tileH - WildlifeEnemy.defaultRenderHeight(kind)) * 0.5;
        return new double[]{x, y};
    }

    private boolean canOccupy(WildlifeEnemy enemy, double x, double y, double width, double height) {
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
