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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WildlifeEnemy extends Enemy {
    @FunctionalInterface
    public interface MovementValidator {
        boolean canOccupy(WildlifeEnemy enemy, double x, double y, double width, double height);
    }

    public enum Kind {
        BOAR(
                "BOAR",
                "Boar",
                new AnimationSet(
                        new SheetDefinition("Boar_Idle_with_shadow", 4),
                        new SheetDefinition("Boar_Walk_with_shadow", 6),
                        new SheetDefinition("Boar_Hurt_with_shadow", 4),
                        new SheetDefinition("Boar_Death_with_shadow", 6)
                ),
                20.0,
                24,
                3,
                true,
                72.0,
                18.0,
                900_000_000L,
                140_000_000L,
                110_000_000L,
                110_000_000L,
                125_000_000L
        ),
        DEER(
                "DEER",
                "Deer",
                new AnimationSet(
                        new SheetDefinition("Deer_Idle_with_shadow", 4),
                        new SheetDefinition("Deer_Walk_with_shadow", 6),
                        new SheetDefinition("Deer_Hurt_with_shadow", 4),
                        new SheetDefinition("Deer_Death_with_shadow", 7)
                ),
                28.0,
                18,
                0,
                false,
                0.0,
                0.0,
                1L,
                140_000_000L,
                110_000_000L,
                110_000_000L,
                125_000_000L
        ),
        FOX(
                "FOX",
                "Fox",
                new AnimationSet(
                        new SheetDefinition("Fox_Idle_with_shadow", 4),
                        new SheetDefinition("Fox_walk_with_shadow", 6),
                        new SheetDefinition("Fox_Hurt_with_shadow", 4),
                        new SheetDefinition("Fox_Death_with_shadow", 6)
                ),
                30.0,
                16,
                0,
                false,
                0.0,
                0.0,
                1L,
                140_000_000L,
                110_000_000L,
                110_000_000L,
                125_000_000L
        ),
        HARE(
                "HARE",
                "Hare",
                new AnimationSet(
                        new SheetDefinition("Hare_Idle_with_shadow", 4),
                        new SheetDefinition("Hare_Walk_with_shadow", 5),
                        new SheetDefinition("Hare_Hurt_with_shadow", 4),
                        new SheetDefinition("Hare_Death_with_shadow", 6)
                ),
                34.0,
                12,
                0,
                false,
                0.0,
                0.0,
                1L,
                140_000_000L,
                110_000_000L,
                110_000_000L,
                125_000_000L
        );

        private final String enemyType;
        private final String assetFolder;
        private final AnimationSet animations;
        private final double patrolSpeed;
        private final int maxHp;
        private final int contactDamage;
        private final boolean attacksPlayer;
        private final double aggroRange;
        private final double attackRange;
        private final long attackCooldownNs;
        private final long idleFrameNs;
        private final long walkFrameNs;
        private final long hurtFrameNs;
        private final long deathFrameNs;

        Kind(String enemyType,
             String assetFolder,
             AnimationSet animations,
             double patrolSpeed,
             int maxHp,
             int contactDamage,
             boolean attacksPlayer,
             double aggroRange,
             double attackRange,
             long attackCooldownNs,
             long idleFrameNs,
             long walkFrameNs,
             long hurtFrameNs,
             long deathFrameNs) {
            this.enemyType = enemyType;
            this.assetFolder = assetFolder;
            this.animations = animations;
            this.patrolSpeed = patrolSpeed;
            this.maxHp = maxHp;
            this.contactDamage = contactDamage;
            this.attacksPlayer = attacksPlayer;
            this.aggroRange = aggroRange;
            this.attackRange = attackRange;
            this.attackCooldownNs = attackCooldownNs;
            this.idleFrameNs = idleFrameNs;
            this.walkFrameNs = walkFrameNs;
            this.hurtFrameNs = hurtFrameNs;
            this.deathFrameNs = deathFrameNs;
        }

        public String enemyType() {
            return enemyType;
        }
    }

    private static final int DIRECTION_ROWS = 4;
    private static final double ARRIVAL_THRESHOLD = 2.0;
    private static final int TARGET_PICK_ATTEMPTS = 24;
    private static final double MAX_DELTA_SECONDS = 0.20;

    private static final class SheetDefinition {
        private final String fileName;
        private final int columns;

        private SheetDefinition(String fileName, int columns) {
            this.fileName = fileName;
            this.columns = Math.max(1, columns);
        }
    }

    private record AnimationSet(SheetDefinition idle, SheetDefinition walk, SheetDefinition hurt, SheetDefinition death) {}

    private static final class SheetSpec {
        private final Image image;
        private final int columns;
        private final int frameWidth;
        private final int frameHeight;

        private SheetSpec(Kind kind, SheetDefinition definition) {
            this.image = loadImage(kind.assetFolder, definition.fileName);
            this.columns = definition.columns;
            this.frameWidth = Math.max(1, (int) Math.round(image.getWidth() / this.columns));
            this.frameHeight = Math.max(1, (int) Math.round(image.getHeight() / DIRECTION_ROWS));
        }
    }

    private static final Map<Kind, Map<EnemyState, SheetSpec>> SHEETS = new EnumMap<>(Kind.class);
    static {
        for (Kind kind : Kind.values()) {
            Map<EnemyState, SheetSpec> specs = new EnumMap<>(EnemyState.class);
            specs.put(EnemyState.IDLE, new SheetSpec(kind, kind.animations.idle()));
            specs.put(EnemyState.WALK, new SheetSpec(kind, kind.animations.walk()));
            specs.put(EnemyState.HURT, new SheetSpec(kind, kind.animations.hurt()));
            specs.put(EnemyState.DEATH, new SheetSpec(kind, kind.animations.death()));
            SHEETS.put(kind, specs);
        }
    }

    private final Kind kind;
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

    public WildlifeEnemy(Kind kind,
                         double x,
                         double y,
                         int tileWidth,
                         int tileHeight,
                         Random random,
                         MovementValidator movementValidator) {
        super(
                x,
                y,
                maxFrameWidth(kind),
                maxFrameHeight(kind),
                kind.patrolSpeed,
                kind.maxHp,
                Math.max(1, kind.contactDamage),
                Math.max(1L, kind.attackCooldownNs),
                resolveAssetPath(kind.assetFolder, kind.animations.walk().fileName),
                sheetFor(kind, EnemyState.WALK).columns,
                DIRECTION_ROWS,
                kind.walkFrameNs,
                resolveAssetPath(kind.assetFolder, kind.animations.idle().fileName),
                sheetFor(kind, EnemyState.IDLE).columns,
                DIRECTION_ROWS,
                kind.idleFrameNs
        );
        this.kind = kind;
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
        if (deltaSeconds > MAX_DELTA_SECONDS) {
            deltaSeconds = MAX_DELTA_SECONDS;
        }

        if (state == EnemyState.DEATH) {
            if (advanceNonLoopingAnimation(now, kind.deathFrameNs, frameCountFor(EnemyState.DEATH))) {
                removeFromWorld = true;
            }
            return;
        }

        if (state == EnemyState.HURT) {
            if (advanceNonLoopingAnimation(now, kind.hurtFrameNs, frameCountFor(EnemyState.HURT))) {
                state = hasTarget && distanceTo(targetX, targetY) > ARRIVAL_THRESHOLD
                        ? EnemyState.WALK
                        : EnemyState.IDLE;
                frameIndex = 0;
                lastFrameAtNs = now;
            }
            return;
        }

        if (kind.attacksPlayer && player != null && shouldChasePlayer(player)) {
            updateAggro(now, deltaSeconds, player);
            return;
        }

        updatePatrol(now, deltaSeconds);
    }

    @Override
    public void tryAttackPlayer(Player player, long now) {
        if (!kind.attacksPlayer) {
            return;
        }
        super.tryAttackPlayer(player, now);
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

        SheetSpec sheet = sheetFor(kind, state);
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
        return kind.enemyType;
    }

    public Kind getKind() {
        return kind;
    }

    public static int defaultRenderWidth(Kind kind) {
        return maxFrameWidth(kind);
    }

    public static int defaultRenderHeight(Kind kind) {
        return maxFrameHeight(kind);
    }

    private void updatePatrol(long now, double deltaSeconds) {
        if (now < idleUntilNs) {
            switchState(EnemyState.IDLE, now);
            advanceLoopingAnimation(now, kind.idleFrameNs, frameCountFor(EnemyState.IDLE));
            return;
        }

        if (!hasTarget && !chooseNextTarget(now, 2, 4)) {
            switchState(EnemyState.IDLE, now);
            idleUntilNs = now + randomDurationNs(500_000_000L, 1_100_000_000L);
            advanceLoopingAnimation(now, kind.idleFrameNs, frameCountFor(EnemyState.IDLE));
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
            advanceLoopingAnimation(now, kind.idleFrameNs, frameCountFor(EnemyState.IDLE));
            return;
        }

        updateDirection(dx, dy);
        if (tryMoveTowardTarget(deltaSeconds)) {
            switchState(EnemyState.WALK, now);
            advanceLoopingAnimation(now, kind.walkFrameNs, frameCountFor(EnemyState.WALK));
            return;
        }

        hasTarget = false;
        idleUntilNs = now + randomDurationNs(250_000_000L, 600_000_000L);
        switchState(EnemyState.IDLE, now);
        advanceLoopingAnimation(now, kind.idleFrameNs, frameCountFor(EnemyState.IDLE));
    }

    private void updateAggro(long now, double deltaSeconds, Player player) {
        targetX = player.getCenterX() - width * 0.5;
        targetY = player.getCenterY() - height * 0.5;
        hasTarget = true;

        double dx = targetX - x;
        double dy = targetY - y;
        updateDirection(dx, dy);
        double distance = distanceTo(targetX, targetY);
        if (distance <= kind.attackRange) {
            switchState(EnemyState.IDLE, now);
            advanceLoopingAnimation(now, kind.idleFrameNs, frameCountFor(EnemyState.IDLE));
            super.tryAttackPlayer(player, now);
            return;
        }

        if (tryMoveTowardTarget(deltaSeconds)) {
            switchState(EnemyState.WALK, now);
            advanceLoopingAnimation(now, kind.walkFrameNs, frameCountFor(EnemyState.WALK));
            return;
        }

        switchState(EnemyState.IDLE, now);
        advanceLoopingAnimation(now, kind.idleFrameNs, frameCountFor(EnemyState.IDLE));
    }

    private boolean shouldChasePlayer(Player player) {
        return distanceTo(player.getCenterX() - width * 0.5, player.getCenterY() - height * 0.5) <= kind.aggroRange;
    }

    private boolean chooseNextTarget(long now, int minRadiusTiles, int maxRadiusTiles) {
        int radiusSpan = Math.max(0, maxRadiusTiles - minRadiusTiles);
        for (int attempt = 0; attempt < TARGET_PICK_ATTEMPTS; attempt++) {
            int radiusTiles = minRadiusTiles + (radiusSpan <= 0 ? 0 : random.nextInt(radiusSpan + 1));
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
        SheetSpec spec = sheetFor(kind, state);
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

    private static SheetSpec sheetFor(Kind kind, EnemyState state) {
        Map<EnemyState, SheetSpec> specs = SHEETS.get(kind);
        return specs == null ? null : specs.get(state);
    }

    private static int maxFrameWidth(Kind kind) {
        int max = 1;
        for (SheetSpec spec : SHEETS.get(kind).values()) {
            max = Math.max(max, spec.frameWidth);
        }
        return max;
    }

    private static int maxFrameHeight(Kind kind) {
        int max = 1;
        for (SheetSpec spec : SHEETS.get(kind).values()) {
            max = Math.max(max, spec.frameHeight);
        }
        return max;
    }

    private static Image loadImage(String assetFolder, String assetName) {
        return new Image(resolveAssetPath(assetFolder, assetName));
    }

    private static String resolveAssetPath(String assetFolder, String assetName) {
        String fileName = assetName.endsWith(".png") ? assetName : assetName + ".png";
        Path relative = Paths.get("assets", assetFolder, fileName);
        if (Files.exists(relative)) {
            return relative.toUri().toString();
        }
        return Paths.get("assets", assetFolder, assetName).toUri().toString();
    }
}
