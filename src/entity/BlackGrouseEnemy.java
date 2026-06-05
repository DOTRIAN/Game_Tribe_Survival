package entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class BlackGrouseEnemy extends Enemy {
    @FunctionalInterface
    public interface MovementValidator {
        boolean canOccupy(BlackGrouseEnemy enemy, double x, double y, double width, double height);
    }

    private static final String IDLE_ASSET = "Black_grouse_Idle_with_shadow";
    private static final String WALK_ASSET = "Black_grouse_Walk_with_shadow";
    private static final String HURT_ASSET = "Black_grouse_Hurt_with_shadow";
    private static final String DEATH_ASSET = "Black_grouse_Death_with_shadow";
    private static final int DIRECTION_ROWS = 4;
    private static final long IDLE_FRAME_NS = 140_000_000L;
    private static final long WALK_FRAME_NS = 110_000_000L;
    private static final long HURT_FRAME_NS = 110_000_000L;
    private static final long DEATH_FRAME_NS = 125_000_000L;
    private static final double PATROL_SPEED_PX_PER_SEC = 26.0;
    private static final double ARRIVAL_THRESHOLD = 2.0;
    private static final int TARGET_PICK_ATTEMPTS = 24;

    private static final class SheetSpec {
        private final Image image;
        private final int columns;
        private final int frameWidth;
        private final int frameHeight;

        private SheetSpec(String assetName, int columns) {
            this.image = loadImage(assetName);
            this.columns = Math.max(1, columns);
            this.frameWidth = Math.max(1, (int) Math.round(image.getWidth() / this.columns));
            this.frameHeight = Math.max(1, (int) Math.round(image.getHeight() / DIRECTION_ROWS));
        }
    }

    private static final Map<EnemyState, SheetSpec> SHEETS = new EnumMap<>(EnemyState.class);
    static {
        SHEETS.put(EnemyState.IDLE, new SheetSpec(IDLE_ASSET, 4));
        SHEETS.put(EnemyState.WALK, new SheetSpec(WALK_ASSET, 6));
        SHEETS.put(EnemyState.HURT, new SheetSpec(HURT_ASSET, 4));
        SHEETS.put(EnemyState.DEATH, new SheetSpec(DEATH_ASSET, 6));
    }

    private final Random random;
    private final MovementValidator movementValidator;
    private final double spawnPointX;
    private final double spawnPointY;
    private final int tileWidth;
    private final int tileHeight;

    private EnemyState state;
    private Direction direction;
    private int frameIndex;
    private long lastFrameAtNs;
    private long lastUpdateAtNs;
    private long idleUntilNs;
    private boolean removeFromWorld;
    private boolean wasMovingBeforeHurt;
    private boolean hasTarget;
    private double targetX;
    private double targetY;

    public BlackGrouseEnemy(double x,
                            double y,
                            int tileWidth,
                            int tileHeight,
                            Random random,
                            MovementValidator movementValidator) {
        super(
                x,
                y,
                maxFrameWidth(),
                maxFrameHeight(),
                PATROL_SPEED_PX_PER_SEC,
                10,
                0,
                1L,
                resolveAssetPath(WALK_ASSET),
                SHEETS.get(EnemyState.WALK).columns,
                DIRECTION_ROWS,
                WALK_FRAME_NS,
                resolveAssetPath(IDLE_ASSET),
                SHEETS.get(EnemyState.IDLE).columns,
                DIRECTION_ROWS,
                IDLE_FRAME_NS
        );
        this.tileWidth = Math.max(1, tileWidth);
        this.tileHeight = Math.max(1, tileHeight);
        this.random = random == null ? new Random() : random;
        this.movementValidator = movementValidator;
        this.spawnPointX = x;
        this.spawnPointY = y;
        this.state = EnemyState.IDLE;
        this.direction = Direction.DOWN;
        this.frameIndex = 0;
        this.lastFrameAtNs = 0L;
        this.lastUpdateAtNs = -1L;
        this.idleUntilNs = 0L;
        this.removeFromWorld = false;
        this.wasMovingBeforeHurt = false;
        this.hasTarget = false;
        this.targetX = x;
        this.targetY = y;
    }

    @Override
    public void update(long now, Player player, double worldWidth, double worldHeight) {
        if (removeFromWorld) {
            return;
        }
        double deltaSeconds = 0.0;
        if (lastUpdateAtNs > 0L) {
            deltaSeconds = (now - lastUpdateAtNs) / 1_000_000_000.0;
        }
        lastUpdateAtNs = now;
        if (deltaSeconds < 0.0) {
            deltaSeconds = 0.0;
        }
        if (deltaSeconds > 0.20) {
            deltaSeconds = 0.20;
        }

        if (state == EnemyState.DEATH) {
            if (advanceNonLoopingAnimation(now, DEATH_FRAME_NS, frameCountFor(state))) {
                removeFromWorld = true;
            }
            return;
        }

        if (state == EnemyState.HURT) {
            if (advanceNonLoopingAnimation(now, HURT_FRAME_NS, frameCountFor(state))) {
                state = hasTarget && distanceTo(targetX, targetY) > ARRIVAL_THRESHOLD
                        ? EnemyState.WALK
                        : EnemyState.IDLE;
                frameIndex = 0;
                lastFrameAtNs = now;
            }
            return;
        }

        updatePatrol(now, deltaSeconds);
    }

    @Override
    public void tryAttackPlayer(Player player, long now) {
        // Black Grouse la animal vo hai, khong tan cong player.
    }

    @Override
    public void takeDamage(int amount) {
        if (amount <= 0 || removeFromWorld || state == EnemyState.DEATH) {
            return;
        }
        wasMovingBeforeHurt = state == EnemyState.WALK;
        super.takeDamage(amount);
        if (hp <= 0) {
            hp = 0;
            switchState(EnemyState.DEATH, System.nanoTime());
            return;
        }
        switchState(EnemyState.HURT, System.nanoTime());
    }

    @Override
    public void draw(GraphicsContext graphicsContext, double cameraX, double cameraY, long nowNs) {
        if (!shouldRender(nowNs)) {
            return;
        }

        SheetSpec sheet = SHEETS.get(state);
        if (sheet == null || sheet.image == null || sheet.image.isError()) {
            graphicsContext.setFill(Color.DARKSLATEGRAY);
            graphicsContext.fillRect(Math.round(x - cameraX), Math.round(y - cameraY), width, height);
            return;
        }

        int row = rowFor(direction);
        int column = Math.max(0, Math.min(frameIndex, sheet.columns - 1));
        double sourceX = column * sheet.frameWidth;
        double sourceY = row * sheet.frameHeight;
        double screenX = Math.round(x - cameraX);
        double screenY = Math.round(y - cameraY);

        graphicsContext.drawImage(
                sheet.image,
                sourceX,
                sourceY,
                sheet.frameWidth,
                sheet.frameHeight,
                screenX,
                screenY,
                width,
                height
        );

        if (isHitFlashActive(nowNs)) {
            Image overlay = extractFrame(sheet, row, column, getHitFlashColor());
            if (overlay != null) {
                graphicsContext.save();
                graphicsContext.setGlobalAlpha(0.45);
                graphicsContext.drawImage(overlay, screenX, screenY, width, height);
                graphicsContext.restore();
            }
        }
    }

    @Override
    public boolean isHostile() {
        return false;
    }

    @Override
    public boolean shouldRender(long nowNs) {
        return !removeFromWorld;
    }

    @Override
    public boolean shouldRemoveFromWorld() {
        return removeFromWorld;
    }

    @Override
    public String getEnemyType() {
        return "BLACK_GROUSE";
    }

    private void updatePatrol(long now, double deltaSeconds) {
        if (now < idleUntilNs) {
            switchState(EnemyState.IDLE, now);
            advanceLoopingAnimation(now, IDLE_FRAME_NS, frameCountFor(EnemyState.IDLE));
            return;
        }

        if (!hasTarget && !chooseNextTarget(now)) {
            switchState(EnemyState.IDLE, now);
            idleUntilNs = now + randomDurationNs(500_000_000L, 1_100_000_000L);
            advanceLoopingAnimation(now, IDLE_FRAME_NS, frameCountFor(EnemyState.IDLE));
            return;
        }

        double dx = targetX - x;
        double dy = targetY - y;
        if (Math.abs(dx) <= ARRIVAL_THRESHOLD && Math.abs(dy) <= ARRIVAL_THRESHOLD) {
            x = targetX;
            y = targetY;
            hasTarget = false;
            idleUntilNs = now + randomDurationNs(550_000_000L, 1_350_000_000L);
            switchState(EnemyState.IDLE, now);
            advanceLoopingAnimation(now, IDLE_FRAME_NS, frameCountFor(EnemyState.IDLE));
            return;
        }

        updateDirection(dx, dy);
        if (tryMoveTowardTarget(deltaSeconds)) {
            switchState(EnemyState.WALK, now);
            advanceLoopingAnimation(now, WALK_FRAME_NS, frameCountFor(EnemyState.WALK));
            return;
        }

        hasTarget = false;
        idleUntilNs = now + randomDurationNs(250_000_000L, 600_000_000L);
        switchState(EnemyState.IDLE, now);
        advanceLoopingAnimation(now, IDLE_FRAME_NS, frameCountFor(EnemyState.IDLE));
    }

    private boolean chooseNextTarget(long now) {
        for (int attempt = 0; attempt < TARGET_PICK_ATTEMPTS; attempt++) {
            int radiusTiles = 2 + random.nextInt(3);
            int tileOffsetX = random.nextInt(radiusTiles * 2 + 1) - radiusTiles;
            int tileOffsetY = random.nextInt(radiusTiles * 2 + 1) - radiusTiles;
            if (tileOffsetX == 0 && tileOffsetY == 0) {
                continue;
            }

            double candidateX = spawnPointX + tileOffsetX * tileWidth;
            double candidateY = spawnPointY + tileOffsetY * tileHeight;
            if (!canOccupy(candidateX, candidateY)) {
                continue;
            }

            targetX = candidateX;
            targetY = candidateY;
            hasTarget = true;
            idleUntilNs = now;
            return true;
        }
        return false;
    }

    private boolean tryMoveTowardTarget(double deltaSeconds) {
        if (deltaSeconds <= 0.0) {
            return false;
        }
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= 0.0001) {
            return false;
        }

        double step = Math.min(distance, speed * deltaSeconds);
        double moveX = (dx / distance) * step;
        double moveY = (dy / distance) * step;
        double oldX = x;
        double oldY = y;

        if (Math.abs(moveX) > 0.0001) {
            double nextX = x + moveX;
            if (canOccupy(nextX, y)) {
                x = nextX;
            }
        }
        if (Math.abs(moveY) > 0.0001) {
            double nextY = y + moveY;
            if (canOccupy(x, nextY)) {
                y = nextY;
            }
        }
        return Math.abs(oldX - x) > 0.0001 || Math.abs(oldY - y) > 0.0001;
    }

    private boolean canOccupy(double nextX, double nextY) {
        return movementValidator == null || movementValidator.canOccupy(this, nextX, nextY, width, height);
    }

    private void updateDirection(double dx, double dy) {
        if (Math.abs(dx) >= Math.abs(dy)) {
            if (Math.abs(dx) > 0.0001) {
                direction = dx < 0 ? Direction.LEFT : Direction.RIGHT;
            }
            return;
        }
        if (Math.abs(dy) > 0.0001) {
            direction = dy < 0 ? Direction.UP : Direction.DOWN;
        }
    }

    private void switchState(EnemyState nextState, long now) {
        if (state == nextState && nextState != EnemyState.HURT && nextState != EnemyState.DEATH) {
            return;
        }
        state = nextState;
        frameIndex = 0;
        lastFrameAtNs = now;
        if (nextState == EnemyState.HURT && !wasMovingBeforeHurt) {
            hasTarget = false;
        }
    }

    private void advanceLoopingAnimation(long now, long frameDurationNs, int frameCount) {
        if (frameCount <= 0) {
            frameIndex = 0;
            return;
        }
        if (lastFrameAtNs == 0L) {
            lastFrameAtNs = now;
            return;
        }
        if (now - lastFrameAtNs < frameDurationNs) {
            return;
        }
        long steps = Math.max(1L, (now - lastFrameAtNs) / frameDurationNs);
        frameIndex = (int) ((frameIndex + steps) % frameCount);
        lastFrameAtNs += steps * frameDurationNs;
    }

    private boolean advanceNonLoopingAnimation(long now, long frameDurationNs, int frameCount) {
        if (frameCount <= 0) {
            return true;
        }
        if (lastFrameAtNs == 0L) {
            lastFrameAtNs = now;
            return false;
        }
        if (now - lastFrameAtNs < frameDurationNs) {
            return false;
        }
        lastFrameAtNs = now;
        if (frameIndex < frameCount - 1) {
            frameIndex++;
            return false;
        }
        return true;
    }

    private int frameCountFor(EnemyState state) {
        SheetSpec spec = SHEETS.get(state);
        return spec == null ? 1 : spec.columns;
    }

    private int rowFor(Direction direction) {
        return switch (direction) {
            case UP -> 0;
            case DOWN -> 1;
            case LEFT -> 2;
            case RIGHT -> 3;
        };
    }

    private long randomDurationNs(long minNs, long maxNs) {
        if (maxNs <= minNs) {
            return minNs;
        }
        return minNs + (long) (random.nextDouble() * (maxNs - minNs));
    }

    private double distanceTo(double worldX, double worldY) {
        double dx = worldX - x;
        double dy = worldY - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private Image extractFrame(SheetSpec sheet, int row, int column, Color tint) {
        PixelReader reader = sheet.image.getPixelReader();
        if (reader == null) {
            return null;
        }
        WritableImage frame = new WritableImage(sheet.frameWidth, sheet.frameHeight);
        for (int py = 0; py < sheet.frameHeight; py++) {
            for (int px = 0; px < sheet.frameWidth; px++) {
                Color source = reader.getColor(column * sheet.frameWidth + px, row * sheet.frameHeight + py);
                double alpha = source.getOpacity();
                frame.getPixelWriter().setColor(
                        px,
                        py,
                        alpha <= 0.001
                                ? Color.TRANSPARENT
                                : Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), alpha)
                );
            }
        }
        return frame;
    }

    private static int maxFrameWidth() {
        int max = 1;
        for (SheetSpec spec : SHEETS.values()) {
            max = Math.max(max, spec.frameWidth);
        }
        return max;
    }

    private static int maxFrameHeight() {
        int max = 1;
        for (SheetSpec spec : SHEETS.values()) {
            max = Math.max(max, spec.frameHeight);
        }
        return max;
    }

    public static int defaultRenderWidth() {
        return maxFrameWidth();
    }

    public static int defaultRenderHeight() {
        return maxFrameHeight();
    }

    private static Image loadImage(String assetName) {
        return new Image(resolveAssetPath(assetName));
    }

    private static String resolveAssetPath(String assetName) {
        String fileName = assetName.endsWith(".png") ? assetName : assetName + ".png";
        Path relative = Paths.get("assets", "Black_grouse", fileName);
        if (Files.exists(relative)) {
            return relative.toUri().toString();
        }
        return Paths.get("assets", "Black_grouse", assetName).toUri().toString();
    }
}
