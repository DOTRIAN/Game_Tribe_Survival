package entity;

import animation.SpriteSheetLoader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OrcBerserkEnemy extends Enemy {
    public static final double RENDER_WIDTH = 88.0;
    public static final double RENDER_HEIGHT = 88.0;

    private static final double SPEED = 0.38;
    private static final int MAX_HP = 34;
    private static final int DAMAGE = 1;
    private static final long ATTACK_COOLDOWN_NS = 700_000_000L;
    private static final long RUN_FRAME_NS = 90_000_000L;
    private static final long IDLE_FRAME_NS = 130_000_000L;

    public OrcBerserkEnemy(double x, double y) {
        super(
                x,
                y,
                RENDER_WIDTH,
                RENDER_HEIGHT,
                SPEED,
                MAX_HP,
                DAMAGE,
                ATTACK_COOLDOWN_NS,
                resolveAsset("Run"),
                6,
                1,
                RUN_FRAME_NS,
                resolveAsset("Idle"),
                5,
                1,
                IDLE_FRAME_NS
        );
    }

    @Override
    protected double collisionInsetLeft(double width, double height) {
        return width * 0.24;
    }

    @Override
    protected double collisionInsetRight(double width, double height) {
        return width * 0.24;
    }

    @Override
    protected double collisionInsetTop(double width, double height) {
        return height * 0.46;
    }

    @Override
    protected double collisionInsetBottom(double width, double height) {
        return height * 0.06;
    }

    @Override
    public String getEnemyType() {
        return "ORC_BERSERK";
    }

    public static void preloadAssets() {
        SpriteSheetLoader.loadGrid(resolveAsset("Idle"), 5, 1);
        SpriteSheetLoader.loadGrid(resolveAsset("Run"), 6, 1);
        SpriteSheetLoader.loadGrid(resolveAsset("Walk"), 7, 1);
        SpriteSheetLoader.loadGrid(resolveAsset("Attack_1"), 4, 1);
        SpriteSheetLoader.loadGrid(resolveAsset("Attack_2"), 5, 1);
        SpriteSheetLoader.loadGrid(resolveAsset("Attack_3"), 2, 1);
        SpriteSheetLoader.loadGrid(resolveAsset("Dead"), 4, 1);
        SpriteSheetLoader.loadGrid(resolveAsset("Hurt"), 2, 1);
    }

    private static String resolveAsset(String baseName) {
        Path folder = Paths.get("assets", "Orc_Berserk");
        Path exact = folder.resolve(baseName + ".png");
        if (Files.exists(exact)) {
            return exact.toUri().toString();
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.png")) {
            for (Path candidate : stream) {
                String fileName = candidate.getFileName().toString();
                if (fileName.equalsIgnoreCase(baseName + ".png")) {
                    return candidate.toUri().toString();
                }
            }
        } catch (IOException ignored) {
        }
        return exact.toUri().toString();
    }
}
