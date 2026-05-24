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
    private final WolfEnemy.WorldQuery worldQuery;
    private final Random random;
    private final List<WolfEnemy> wolves;

    public WolfSpawnManager(CollisionManager collisionManager,
                            WolfEnemy.MovementValidator movementValidator,
                            WolfEnemy.WorldQuery worldQuery,
                            Random random) {
        this.collisionManager = collisionManager;
        this.movementValidator = movementValidator;
        this.worldQuery = worldQuery;
        this.random = random == null ? new Random() : random;
        this.wolves = new ArrayList<>();
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

    public void updateAll(long nowNs,
                          boolean isNight,
                          Player player,
                          BaseCamp baseCamp,
                          double worldWidth,
                          double worldHeight) {
        for (WolfEnemy wolf : wolves) {
            if (wolf == null) {
                continue;
            }
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
            WolfEnemy wolf = new WolfEnemy(x, y, width, height, movementValidator, worldQuery, random);
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
            return 120.0;
        }
        return Math.max(120.0, Math.max(collisionManager.getTileWidth(), collisionManager.getTileHeight()) * 3.4);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
