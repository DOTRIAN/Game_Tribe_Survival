package entity;

import animation.PngSequenceLoader;
import animation.SpriteAnimation;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import system.DamageSystem;
import system.MovementSlideSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GolemEnemy extends Enemy {
    public enum State {
        IDLE,
        WALKING_TO_BASE,
        CHASE_TARGET,
        ATTACKING,
        HURT,
        DYING
    }

    @FunctionalInterface
    public interface MovementValidator {
        boolean canOccupy(GolemEnemy enemy, double x, double y, double width, double height);
    }

    private static final String BASE_FOLDER = "assets/Golem_01/PNG Sequences";
    private static final int ATTACK_FRAME_COUNT = 12;
    private static final int DEATH_FRAME_COUNT = 15;
    private static final int HURT_FRAME_COUNT = 12;
    private static final int IDLE_FRAME_COUNT = 12;
    private static final int WALK_FRAME_COUNT = 18;
    private static final long ATTACK_FRAME_NS = 80_000_000L;
    private static final long DEATH_FRAME_NS = 85_000_000L;
    private static final long HURT_FRAME_NS = 70_000_000L;
    private static final long IDLE_FRAME_NS = 110_000_000L;
    private static final long WALK_FRAME_NS = 70_000_000L;
    private static final long ATTACK_COOLDOWN_NS = 1_000_000_000L;
    private static final long HURT_RESTART_GUARD_NS = 180_000_000L;
    private static final double AGGRO_RANGE = 220.0;
    private static final double LEASH_EXTRA = 44.0;
    private static final double ATTACK_RANGE = 12.0;
    private static final double MOVE_SPEED = 0.42;
    private static final int MAX_HP = 80;
    private static final int DAMAGE = 5;
    private static final double RENDER_WIDTH = 112.0;
    private static final double RENDER_HEIGHT = 112.0;
    private static final double HITBOX_WIDTH = 36.0;
    private static final double HITBOX_HEIGHT = 28.0;
    private static final double HITBOX_OFFSET_X = (RENDER_WIDTH - HITBOX_WIDTH) * 0.5;
    private static final double HITBOX_OFFSET_Y = RENDER_HEIGHT - HITBOX_HEIGHT - 2.0;

    private final SpriteAnimation attackingAnimation;
    private final SpriteAnimation dyingAnimation;
    private final SpriteAnimation hurtAnimation;
    private final SpriteAnimation idleAnimation;
    private final SpriteAnimation walkingAnimation;
    private final MovementValidator movementValidator;

    private State state;
    private State resumeStateAfterHurt;
    private Entity currentTarget;
    private boolean facingRight;
    private boolean removeFromWorld;
    private long lastAttackAtNs;
    private long lastHurtAtNs;
    private boolean attackDamageAppliedThisCycle;

    public GolemEnemy(double x, double y, MovementValidator movementValidator) {
        super(
                x + HITBOX_OFFSET_X,
                y + HITBOX_OFFSET_Y,
                HITBOX_WIDTH,
                HITBOX_HEIGHT,
                MOVE_SPEED,
                MAX_HP,
                DAMAGE,
                ATTACK_COOLDOWN_NS,
                assetFrame("Walking", "Golem_01_Walking_", 0),
                1,
                1,
                WALK_FRAME_NS,
                assetFrame("Idle", "Golem_01_Idle_", 0),
                1,
                1,
                IDLE_FRAME_NS
        );
        this.attackingAnimation = new SpriteAnimation(PngSequenceLoader.loadPngSequence(BASE_FOLDER + "/Attacking", "Golem_01_Attacking_", ATTACK_FRAME_COUNT), ATTACK_FRAME_NS);
        this.dyingAnimation = new SpriteAnimation(PngSequenceLoader.loadPngSequence(BASE_FOLDER + "/Dying", "Golem_01_Dying_", DEATH_FRAME_COUNT), DEATH_FRAME_NS);
        this.hurtAnimation = new SpriteAnimation(PngSequenceLoader.loadPngSequence(BASE_FOLDER + "/Hurt", "Golem_01_Hurt_", HURT_FRAME_COUNT), HURT_FRAME_NS);
        this.idleAnimation = new SpriteAnimation(PngSequenceLoader.loadPngSequence(BASE_FOLDER + "/Idle", "Golem_01_Idle_", IDLE_FRAME_COUNT), IDLE_FRAME_NS);
        this.walkingAnimation = new SpriteAnimation(PngSequenceLoader.loadPngSequence(BASE_FOLDER + "/Walking", "Golem_01_Walking_", WALK_FRAME_COUNT), WALK_FRAME_NS);
        this.movementValidator = movementValidator;
        this.state = State.WALKING_TO_BASE;
        this.resumeStateAfterHurt = State.WALKING_TO_BASE;
        this.currentTarget = null;
        this.facingRight = true;
        this.removeFromWorld = false;
        this.lastAttackAtNs = -ATTACK_COOLDOWN_NS;
        this.lastHurtAtNs = -HURT_RESTART_GUARD_NS;
        this.attackDamageAppliedThisCycle = false;
    }

    public void updateAi(long nowNs, BaseCamp baseCamp, Player player, Iterable<FriendlyArcher> friendlies, double worldWidth, double worldHeight) {
        if (removeFromWorld) {
            return;
        }
        if (state == State.DYING) {
            if (dyingAnimation.updateOnce(nowNs)) {
                removeFromWorld = true;
            }
            return;
        }
        if (state == State.HURT) {
            if (hurtAnimation.updateOnce(nowNs)) {
                state = resumeStateAfterHurt;
            }
            return;
        }

        Entity preferredTarget = choosePreferredTarget(baseCamp, player, friendlies);
        currentTarget = preferredTarget;
        if (preferredTarget == null) {
            state = State.IDLE;
            idleAnimation.update(nowNs, true);
            return;
        }

        faceTarget(preferredTarget.getCenterX());
        if (isWithinAttackRange(preferredTarget)) {
            if (state != State.ATTACKING) {
                state = State.ATTACKING;
                attackingAnimation.reset();
                attackDamageAppliedThisCycle = false;
            }
            updateAttack(nowNs);
            return;
        }

        state = preferredTarget instanceof BaseCamp ? State.WALKING_TO_BASE : State.CHASE_TARGET;
        moveTowardTarget(preferredTarget, worldWidth, worldHeight);
        walkingAnimation.update(nowNs, true);
    }

    public boolean applyAttackIfReady(long nowNs) {
        if (state != State.ATTACKING || currentTarget == null || currentTarget.isDead()) {
            return false;
        }
        if (!isWithinAttackRange(currentTarget)) {
            return false;
        }
        int frameIndex = attackingAnimation.getCurrentFrameIndex();
        if (attackDamageAppliedThisCycle || (frameIndex != 7 && frameIndex != 8)) {
            return false;
        }
        if (nowNs - lastAttackAtNs < ATTACK_COOLDOWN_NS) {
            return false;
        }
        if (currentTarget instanceof FriendlyArcher friendlyArcher) {
            friendlyArcher.receiveDamage(damage);
        } else {
            DamageSystem.applyDamage(this, currentTarget, damage, nowNs);
        }
        lastAttackAtNs = nowNs;
        attackDamageAppliedThisCycle = true;
        return true;
    }

    @Override
    public void update(long now, Player player, double worldWidth, double worldHeight) {
        if (state == State.DYING && !removeFromWorld) {
            if (dyingAnimation.updateOnce(now)) {
                removeFromWorld = true;
            }
        }
    }

    @Override
    public void takeDamage(int amount) {
        if (amount <= 0 || removeFromWorld || state == State.DYING) {
            return;
        }
        super.takeDamage(amount);
        if (hp <= 0) {
            hp = 0;
            state = State.DYING;
            dyingAnimation.reset();
            return;
        }
        long nowNs = System.nanoTime();
        if (state != State.HURT || nowNs - lastHurtAtNs >= HURT_RESTART_GUARD_NS) {
            resumeStateAfterHurt = state == State.ATTACKING ? State.CHASE_TARGET : state;
            state = State.HURT;
            hurtAnimation.reset();
            lastHurtAtNs = nowNs;
        }
    }

    @Override
    public void draw(GraphicsContext graphicsContext, double cameraX, double cameraY, long nowNs) {
        if (removeFromWorld) {
            return;
        }
        Image frame = currentFrame();
        double screenX = Math.round(x - HITBOX_OFFSET_X - cameraX);
        double screenY = Math.round(y - HITBOX_OFFSET_Y - cameraY);
        if (frame == null || frame.isError()) {
            graphicsContext.setFill(Color.DARKSLATEGRAY);
            graphicsContext.fillRect(screenX, screenY, RENDER_WIDTH, RENDER_HEIGHT);
            return;
        }
        if (facingRight) {
            graphicsContext.drawImage(frame, screenX, screenY, RENDER_WIDTH, RENDER_HEIGHT);
            return;
        }
        graphicsContext.save();
        graphicsContext.translate(screenX + RENDER_WIDTH, screenY);
        graphicsContext.scale(-1, 1);
        graphicsContext.drawImage(frame, 0, 0, RENDER_WIDTH, RENDER_HEIGHT);
        graphicsContext.restore();
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
        return "GOLEM";
    }

    public Entity getCurrentTarget() {
        return currentTarget;
    }

    public double getAggroRange() {
        return AGGRO_RANGE;
    }

    @Override
    protected double collisionInsetLeft(double width, double height) {
        return 0.0;
    }

    @Override
    protected double collisionInsetRight(double width, double height) {
        return 0.0;
    }

    @Override
    protected double collisionInsetTop(double width, double height) {
        return 0.0;
    }

    @Override
    protected double collisionInsetBottom(double width, double height) {
        return 0.0;
    }

    private Entity choosePreferredTarget(BaseCamp baseCamp, Player player, Iterable<FriendlyArcher> friendlies) {
        Entity nearestAggroTarget = nearestAggroTarget(player, friendlies);
        if (nearestAggroTarget != null) {
            return nearestAggroTarget;
        }
        if (baseCamp != null && baseCamp.isAlive()) {
            return baseCamp;
        }
        return null;
    }

    private Entity nearestAggroTarget(Player player, Iterable<FriendlyArcher> friendlies) {
        Entity nearest = null;
        double nearestDistance = AGGRO_RANGE + LEASH_EXTRA;
        if (player != null && player.isAlive()) {
            double distance = distanceTo(player.getCenterX(), player.getCenterY());
            if (distance <= nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        if (friendlies != null) {
            for (FriendlyArcher archer : friendlies) {
                if (archer == null || !archer.isAlive()) {
                    continue;
                }
                double distance = distanceTo(archer.getCenterX(), archer.getCenterY());
                if (distance <= nearestDistance) {
                    nearest = archer;
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }

    private void moveTowardTarget(Entity target, double worldWidth, double worldHeight) {
        double dx = target.getCenterX() - getCenterX();
        double dy = target.getCenterY() - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= 0.0001) {
            return;
        }
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

    private void updateAttack(long nowNs) {
        attackingAnimation.update(nowNs, true);
        if (attackingAnimation.getCurrentFrameIndex() == 0) {
            attackDamageAppliedThisCycle = false;
        }
    }

    private boolean isWithinAttackRange(Entity target) {
        if (target == null) {
            return false;
        }
        double dx = edgeDistance(x, x + width, target.getX(), target.getX() + target.getWidth());
        double dy = edgeDistance(y, y + height, target.getY(), target.getY() + target.getHeight());
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance <= ATTACK_RANGE;
    }

    private void faceTarget(double targetCenterX) {
        facingRight = targetCenterX >= getCenterX();
    }

    private double distanceTo(double targetCenterX, double targetCenterY) {
        double dx = targetCenterX - getCenterX();
        double dy = targetCenterY - getCenterY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double edgeDistance(double minA, double maxA, double minB, double maxB) {
        if (maxA < minB) {
            return minB - maxA;
        }
        if (maxB < minA) {
            return minA - maxB;
        }
        return 0.0;
    }

    private Image currentFrame() {
        return switch (state) {
            case ATTACKING -> attackingAnimation.getCurrentFrame();
            case DYING -> dyingAnimation.getCurrentFrame();
            case HURT -> hurtAnimation.getCurrentFrame();
            case IDLE -> idleAnimation.getCurrentFrame();
            case WALKING_TO_BASE, CHASE_TARGET -> walkingAnimation.getCurrentFrame();
        };
    }

    private static String assetFrame(String folder, String prefix, int index) {
        String fileName = prefix + String.format("%03d", Math.max(0, index)) + ".png";
        Path local = Paths.get(BASE_FOLDER, folder, fileName);
        if (Files.exists(local)) {
            return local.toUri().toString();
        }
        return Paths.get(BASE_FOLDER, folder, fileName).toUri().toString();
    }
}
