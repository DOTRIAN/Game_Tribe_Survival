package entity;

import javafx.geometry.Point2D;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public class FlowFieldManager {
    public interface TileBlocker {
        boolean isBlocked(int tileX, int tileY);
    }

    private record Tile(int x, int y) {
    }

    private final int tileWidth;
    private final int tileHeight;
    private final int widthTiles;
    private final int heightTiles;
    private final TileBlocker blocker;
    private final EnemyAiDebug debug;
    private final int[][] distances;
    private boolean dirty;
    private long earliestRebuildAtNs;

    public FlowFieldManager(int tileWidth,
                            int tileHeight,
                            int widthTiles,
                            int heightTiles,
                            TileBlocker blocker,
                            EnemyAiDebug debug) {
        this.tileWidth = Math.max(1, tileWidth);
        this.tileHeight = Math.max(1, tileHeight);
        this.widthTiles = Math.max(1, widthTiles);
        this.heightTiles = Math.max(1, heightTiles);
        this.blocker = blocker;
        this.debug = debug;
        this.distances = new int[this.heightTiles][this.widthTiles];
        this.dirty = true;
        this.earliestRebuildAtNs = 0L;
        clearDistances();
    }

    public void markDirty(long nowNs, long debounceNs) {
        dirty = true;
        earliestRebuildAtNs = Math.max(earliestRebuildAtNs, nowNs + Math.max(0L, debounceNs));
    }

    public void update(long nowNs, BaseCamp baseCamp) {
        if (!dirty || baseCamp == null || nowNs < earliestRebuildAtNs) {
            return;
        }
        long startedAtNs = System.nanoTime();
        rebuild(baseCamp);
        dirty = false;
        earliestRebuildAtNs = 0L;
        if (debug != null) {
            debug.recordFlowRebuild(System.nanoTime() - startedAtNs);
        }
    }

    public Point2D getWaypoint(double worldX, double worldY) {
        int tileX = (int) Math.floor(worldX / tileWidth);
        int tileY = (int) Math.floor(worldY / tileHeight);
        if (!isInside(tileX, tileY)) {
            return null;
        }
        int currentDistance = distances[tileY][tileX];
        if (currentDistance <= 0) {
            return null;
        }
        Tile best = null;
        int bestDistance = currentDistance;
        for (Tile neighbor : neighbors(tileX, tileY)) {
            if (!isInside(neighbor.x(), neighbor.y())) {
                continue;
            }
            int candidateDistance = distances[neighbor.y()][neighbor.x()];
            if (candidateDistance >= 0 && candidateDistance < bestDistance) {
                best = neighbor;
                bestDistance = candidateDistance;
            }
        }
        if (best == null) {
            return null;
        }
        return new Point2D(best.x() * tileWidth + tileWidth * 0.5, best.y() * tileHeight + tileHeight * 0.5);
    }

    private void rebuild(BaseCamp baseCamp) {
        clearDistances();
        Queue<Tile> queue = new ArrayDeque<>();
        seedBaseApproachTiles(baseCamp, queue);
        while (!queue.isEmpty()) {
            Tile current = queue.poll();
            int nextDistance = distances[current.y()][current.x()] + 1;
            for (Tile neighbor : neighbors(current.x(), current.y())) {
                if (!isInside(neighbor.x(), neighbor.y())
                        || distances[neighbor.y()][neighbor.x()] != -1
                        || blocker != null && blocker.isBlocked(neighbor.x(), neighbor.y())) {
                    continue;
                }
                distances[neighbor.y()][neighbor.x()] = nextDistance;
                queue.offer(neighbor);
            }
        }
    }

    private void seedBaseApproachTiles(BaseCamp baseCamp, Queue<Tile> queue) {
        int minTileX = (int) Math.floor(baseCamp.getCollisionX() / tileWidth);
        int maxTileX = (int) Math.floor((baseCamp.getCollisionX() + baseCamp.getCollisionWidth()) / tileWidth);
        int minTileY = (int) Math.floor(baseCamp.getCollisionY() / tileHeight);
        int maxTileY = (int) Math.floor((baseCamp.getCollisionY() + baseCamp.getCollisionHeight()) / tileHeight);
        for (int tileY = minTileY - 1; tileY <= maxTileY + 1; tileY++) {
            for (int tileX = minTileX - 1; tileX <= maxTileX + 1; tileX++) {
                if (!isInside(tileX, tileY)) {
                    continue;
                }
                boolean insideCamp = tileX >= minTileX && tileX <= maxTileX && tileY >= minTileY && tileY <= maxTileY;
                if (insideCamp || blocker != null && blocker.isBlocked(tileX, tileY)) {
                    continue;
                }
                if (distances[tileY][tileX] != -1) {
                    continue;
                }
                distances[tileY][tileX] = 0;
                queue.offer(new Tile(tileX, tileY));
            }
        }
    }

    private Tile[] neighbors(int tileX, int tileY) {
        return new Tile[] {
                new Tile(tileX + 1, tileY),
                new Tile(tileX - 1, tileY),
                new Tile(tileX, tileY + 1),
                new Tile(tileX, tileY - 1)
        };
    }

    private boolean isInside(int tileX, int tileY) {
        return tileX >= 0 && tileY >= 0 && tileX < widthTiles && tileY < heightTiles;
    }

    private void clearDistances() {
        for (int[] row : distances) {
            Arrays.fill(row, -1);
        }
    }
}
