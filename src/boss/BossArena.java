package boss;

public final class BossArena {
    private final String mapImagePath;
    private final double spawnX;
    private final double spawnY;

    public BossArena(String mapImagePath, double spawnX, double spawnY) {
        this.mapImagePath = mapImagePath == null ? "" : mapImagePath;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
    }

    public String getMapImagePath() {
        return mapImagePath;
    }

    public double getSpawnX() {
        return spawnX;
    }

    public double getSpawnY() {
        return spawnY;
    }

    public static BossArena fromWorldBounds(String mapImagePath, double worldWidth, double worldHeight) {
        double safeWorldWidth = Math.max(640.0, worldWidth);
        double safeWorldHeight = Math.max(480.0, worldHeight);
        double spawnX = safeWorldWidth * 0.5 - 110.0;
        double spawnY = Math.max(48.0, safeWorldHeight * 0.26);
        return new BossArena(mapImagePath, spawnX, spawnY);
    }
}
