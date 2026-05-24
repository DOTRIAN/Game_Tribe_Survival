package entity;

import animation.SpriteAnimation;
import animation.SpriteSheetLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import system.DamageSystem;
import system.MovementSlideSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WallJumperEnemy extends Enemy {
    public enum State {
        SPAWN,
        MOVE_TO_BASE,
        ATTACK_BASE,
        DEATH
    }

    @FunctionalInterface
    public interface MovementValidator {
        boolean canOccupy(WallJumperEnemy enemy, double x, double y, double width, double height);
    }

    private static final int FRAME_COUNT = 6;
    private static final long WALK_FRAME_NS = 95_000_000L;
    private static final long DEATH_FRAME_NS = 110_000_000L;
    private static final long SPAWN_DELAY_NS = 80_000_000L;
    private static final long ATTACK_COOLDOWN_NS = 1_000_000_000L;
    private static final double MOVE_SPEED = 0.78;
    private static final double ATTACK_RANGE_PADDING = 18.0;
    private static final int MAX_HP = 14;
    private static final int BASE_DAMAGE = 2;
    private static final double[] ATTACK_BOUNCE = {0.0, 5.5, 9.5, 6.0, 2.5, 0.0};

    private final SpriteAnimation walkAnimation;
    private final SpriteAnimation deathAnimation;
    private final MovementValidator movementValidator;

    private State state;
    private long stateStartedAtNs;
    private long lastBaseAttackAtNs;
    private boolean removeFromWorld;

    public WallJumperEnemy(double x, double y, int tileSize, MovementValidator ignored) {
        super(
                x,
                y,
                renderSize(tileSize),
                renderSize(tileSize),
                MOVE_SPEED,
                MAX_HP,
                BASE_DAMAGE,
                ATTACK_COOLDOWN_NS,
                asset("D_Walk"),
                FRAME_COUNT,
                1,
                WALK_FRAME_NS,
                asset("D_Walk"), FRAME_COUNT, 1, WALK_FRAME_NS
        );
        this.walkAnimation = animation("D_Walk", WALK_FRAME_NS);
        this.deathAnimation = animation("D_Death", DEATH_FRAME_NS);
        this.movementValidator = ignored;
        this.state = State.SPAWN;
        this.stateStartedAtNs = 0L;
        this.lastBaseAttackAtNs = -ATTACK_COOLDOWN_NS;
        this.removeFromWorld = false;
    }

    public void updateTowardBase(long nowNs, BaseCamp baseCamp, double worldWidth, double worldHeight) {
        if (removeFromWorld) {
            return;
        }
        if (stateStartedAtNs == 0L) {
            stateStartedAtNs = nowNs;
        }
        if (state == State.DEATH) {
            if (deathAnimation.updateOnce(nowNs)) {
                removeFromWorld = true;
            }
            return;
        }
        if (state == State.SPAWN) {
            walkAnimation.update(nowNs, true);
            if (nowNs - stateStartedAtNs >= SPAWN_DELAY_NS) {
                switchState(State.MOVE_TO_BASE, nowNs);
            }
            return;
        }
        if (baseCamp == null || !baseCamp.isAlive()) {
            walkAnimation.update(nowNs, true);
            return;
        }

        double dx = baseCamp.getCenterX() - getCenterX();
        double dy = baseCamp.getCenterY() - getCenterY();

        if (isWithinAttackRange(baseCamp)) {
            if (state != State.ATTACK_BASE) {
                switchState(State.ATTACK_BASE, nowNs);
            }
            walkAnimation.update(nowNs, true);
            return;
        }

        if (state != State.MOVE_TO_BASE) {
            switchState(State.MOVE_TO_BASE, nowNs);
        }
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > 0.0001) {
            double moveX = (dx / distance) * speed;
            double moveY = (dy / distance) * speed;
            if (movementValidator == null) {
                x += moveX;
                y += moveY;
            } else {
                MovementSlideSystem.MoveResult result = MovementSlideSystem.steerToward(
                        x,
                        y,
                        width,
                        height,
                        moveX,
                        moveY,
                        (nextX, nextY, nextWidth, nextHeight) -> movementValidator.canOccupy(this, nextX, nextY, nextWidth, nextHeight)
                );
                x = result.x();
                y = result.y();
            }
            clampPosition(0, 0, worldWidth, worldHeight);
        }
        walkAnimation.update(nowNs, true);
    }

    public boolean tryAttackBase(BaseCamp baseCamp, long nowNs) {
        if (state != State.ATTACK_BASE || baseCamp == null || !baseCamp.isAlive()) {
            return false;
        }
        if (!isWithinAttackRange(baseCamp)) {
            return false;
        }
        if (nowNs - lastBaseAttackAtNs < ATTACK_COOLDOWN_NS) {
            return false;
        }
        DamageSystem.applyDamage(this, baseCamp, damage, nowNs);
        lastBaseAttackAtNs = nowNs;
        return true;
    }

    @Override
    public void update(long now, Player player, double worldWidth, double worldHeight) {
        if (state == State.DEATH && !removeFromWorld) {
            updateTowardBase(now, null, worldWidth, worldHeight);
        }
    }

    @Override
    public void takeDamage(int amount) {
        if (amount <= 0 || removeFromWorld || state == State.DEATH) {
            return;
        }
        super.takeDamage(amount);
        if (hp <= 0) {
            hp = 0;
            switchState(State.DEATH, System.nanoTime());
        }
    }

    @Override
    public void draw(GraphicsContext graphicsContext, double cameraX, double cameraY, long nowNs) {
        if (removeFromWorld) {
            return;
        }
        Image frame = state == State.DEATH ? deathAnimation.getCurrentFrame() : walkAnimation.getCurrentFrame();
        double screenX = Math.round(x - cameraX);
        double screenY = Math.round(y - cameraY - jumpOffset());
        if (frame == null || frame.isError()) {
            graphicsContext.setFill(Color.DARKSEAGREEN);
            graphicsContext.fillOval(screenX, screenY, width, height);
            return;
        }
        graphicsContext.drawImage(frame, screenX, screenY, width, height);
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
        return "WALL_JUMPER";
    }

    public static double defaultRenderWidth(int tileSize) {
        return renderSize(tileSize);
    }

    public static double defaultRenderHeight(int tileSize) {
        return renderSize(tileSize);
    }

    private void switchState(State nextState, long nowNs) {
        state = nextState;
        stateStartedAtNs = nowNs;
        walkAnimation.reset();
        if (nextState == State.DEATH) {
            deathAnimation.reset();
        }
    }

    private boolean isWithinAttackRange(BaseCamp baseCamp) {
        double x1 = baseCamp.getX() - ATTACK_RANGE_PADDING;
        double y1 = baseCamp.getY() - ATTACK_RANGE_PADDING;
        double w1 = baseCamp.getWidth() + ATTACK_RANGE_PADDING * 2.0;
        double h1 = baseCamp.getHeight() + ATTACK_RANGE_PADDING * 2.0;
        return x < x1 + w1 && x + width > x1 && y < y1 + h1 && y + height > y1;
    }

    private double jumpOffset() {
        if (state != State.ATTACK_BASE) {
            return 0.0;
        }
        int index = walkAnimation.getCurrentFrameIndex();
        return ATTACK_BOUNCE[Math.max(0, Math.min(index, ATTACK_BOUNCE.length - 1))];
    }

    private static SpriteAnimation animation(String name, long frameNs) {
        return new SpriteAnimation(SpriteSheetLoader.loadGrid(asset(name), FRAME_COUNT, 1), frameNs);
    }

    private static String asset(String name) {
        String fileName = name.endsWith(".png") ? name : name + ".png";
        Path local = Paths.get("assets", "SlimeEnemy", fileName);
        if (Files.exists(local)) {
            return local.toUri().toString();
        }
        return Paths.get("assets", "SlimeEnemy", fileName).toUri().toString();
    }

    private static double renderSize(int tileSize) {
        return Math.max(48.0, Math.max(16, tileSize) * 1.5);
    }
}
