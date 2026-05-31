package entity;

import buildsystem.core.CollisionManager;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WolfSpawnManager {
    private static final int WOLF_COUNT = 4;
    private static final double CORNER_MARGIN = 320.0;
    private static final int SPAWN_ATTEMPTS_PER_WOLF = 12;

    private final CollisionManager collisionManager;
    private final WolfEnemy.MovementValidator movementValidator;
    private final EnemyNavigationContext navigationContext;
    private final Random random;
    private final List<WolfEnemy> wolves;
    private int edgeSpawnCursor;

    public WolfSpawnManager(CollisionManager collisionManager,
                            WolfEnemy.MovementValidator movementValidator,
                            EnemyNavigationContext navigationContext,
                            Random random) {
        this.collisionManager = collisionManager;
        this.movementValidator = movementValidator;
        this.navigationContext = navigationContext;
        this.random = random == null ? new Random() : random;
        this.wolves = new ArrayList<>();
        this.edgeSpawnCursor = 0;
    }

    public void spawnAtMapCorners(List<Enemy> masterEnemies, double worldWidth, double worldHeight) {
        if (masterEnemies == null || wolves.size() >= WOLF_COUNT) {
            return;
        }
        double renderWidth = defaultWolfSize();
        double renderHeight = defaultWolfSize();
        double[][] corners = {
                {CORNER_MARGIN, CORNER_MARGIN},
                {Math.max(CORNER_MARGIN, worldWidth - renderWidth - CORNER_MARGIN), CORNER_MARGIN},
                {CORNER_MARGIN, Math.max(CORNER_MARGIN, worldHeight - renderHeight - CORNER_MARGIN)},
                {Math.max(CORNER_MARGIN, worldWidth - renderWidth - CORNER_MARGIN), Math.max(CORNER_MARGIN, worldHeight - renderHeight - CORNER_MARGIN)}
        };
        for (int index = wolves.size(); index < Math.min(WOLF_COUNT, corners.length); index++) {
            WolfEnemy wolf = spawnNearCorner(corners[index][0], corners[index][1], renderWidth, renderHeight, worldWidth, worldHeight);
            if (wolf == null) {
                continue;
            }
            wolves.add(wolf);
            masterEnemies.add(wolf);
        }
    }

    public void spawnAtMapEdges(List<Enemy> masterEnemies, int count, double worldWidth, double worldHeight) {
        if (masterEnemies == null || count <= 0) {
            return;
        }
        double renderWidth = defaultWolfSize();
        double renderHeight = defaultWolfSize();
        double margin = Math.max(CORNER_MARGIN, Math.max(renderWidth, renderHeight) * 3.0);
        for (int i = 0; i < count; i++) {
            double[] point = edgeSpawnPoint(edgeSpawnCursor++, renderWidth, renderHeight, margin, worldWidth, worldHeight);
            WolfEnemy wolf = spawnNearCorner(point[0], point[1], renderWidth, renderHeight, worldWidth, worldHeight);
            if (wolf == null) {
                continue;
            }
            wolves.add(wolf);
            masterEnemies.add(wolf);
        }
    }

    public void updateAll(long nowNs,
                          boolean isNight,
                          Player player,
                          BaseCamp baseCamp,
                          boolean debugEnabled,
                          double worldWidth,
                          double worldHeight) {
        for (WolfEnemy wolf : wolves) {
            if (wolf == null) {
                continue;
            }
            wolf.setDebugEnabled(debugEnabled);
            wolf.updateBehavior(nowNs, isNight, player, baseCamp, worldWidth, worldHeight);
        }
    }

    public void renderAll(GraphicsContext graphicsContext, double cameraX, double cameraY, long nowNs) {
        for (WolfEnemy wolf : wolves) {
            if (wolf == null || !wolf.shouldRender(nowNs)) {
                continue;
            }
            wolf.draw(graphicsContext, cameraX, cameraY, nowNs);
        }
    }

    public void cleanupRemoved() {
        wolves.removeIf(wolf -> wolf == null || wolf.shouldRemoveFromWorld());
    }

    public void clear() {
        wolves.clear();
        edgeSpawnCursor = 0;
    }

    public List<WolfEnemy> getWolves() {
        return List.copyOf(wolves);
    }

    private WolfEnemy spawnNearCorner(double baseX,
                                      double baseY,
                                      double width,
                                      double height,
                                      double worldWidth,
                                      double worldHeight) {
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS_PER_WOLF; attempt++) {
            double x = clamp(baseX + randomJitter(), 0.0, Math.max(0.0, worldWidth - width));
            double y = clamp(baseY + randomJitter(), 0.0, Math.max(0.0, worldHeight - height));
            WolfEnemy wolf = new WolfEnemy(x, y, width, height, movementValidator, navigationContext, random);
            wolf.setInitialPathDelayNs((long) (random.nextDouble() * 1_000_000_000L));
            wolf.setHome(x + width * 0.5, y + height * 0.5);
            if (movementValidator == null || movementValidator.canOccupy(wolf, x, y, width, height)) {
                return wolf;
            }
        }
        return null;
    }

    private double randomJitter() {
        double tile = collisionManager == null ? 28.0 : Math.max(collisionManager.getTileWidth(), collisionManager.getTileHeight());
        return (random.nextDouble() - 0.5) * tile * 3.0;
    }

    private double defaultWolfSize() {
        if (collisionManager == null) {
            return 76.0;
        }
        double tile = Math.max(collisionManager.getTileWidth(), collisionManager.getTileHeight());
        return Math.max(68.0, tile * 2.15);
    }

    private double[] edgeSpawnPoint(int index, double width, double height, double margin, double worldWidth, double worldHeight) {
        double maxX = Math.max(0.0, worldWidth - width);
        double maxY = Math.max(0.0, worldHeight - height);
        double sideOffset = 0.18 + 0.64 * random.nextDouble();
        return switch (index % 4) {
            case 0 -> new double[]{clamp(margin * 0.25, 0.0, maxX), clamp(worldHeight * sideOffset, 0.0, maxY)};
            case 1 -> new double[]{clamp(worldWidth - width - margin * 0.25, 0.0, maxX), clamp(worldHeight * sideOffset, 0.0, maxY)};
            case 2 -> new double[]{clamp(worldWidth * sideOffset, 0.0, maxX), clamp(margin * 0.25, 0.0, maxY)};
            default -> new double[]{clamp(worldWidth * sideOffset, 0.0, maxX), clamp(worldHeight - height - margin * 0.25, 0.0, maxY)};
        };
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
