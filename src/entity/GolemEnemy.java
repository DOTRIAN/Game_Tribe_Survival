package entity;

import animation.PngSequenceLoader;
import animation.SpriteAnimation;
import animation.SpriteSheetLoader;
import buildsystem.object.BuildObject;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import system.DamageSystem;
import system.resource.ResourceNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

public class GolemEnemy extends Enemy {
    public enum State {
        IDLE,
        WALKING_TO_BASE,
        CHASE_TARGET,
        ATTACKING,
        HURT,
        DYING
    }

    public enum GolemMode {
        NORMAL,
        WALL_BREAKER
    }

    private enum AttackDirection {
        LEFT,
        RIGHT,
        UP,
        DOWN
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
    private static final long PATH_RECALC_NS = 500_000_000L;
    private static final long PATH_FAIL_RETRY_NS = 1_000_000_000L;
    private static final long STUCK_REPATH_NS = 650_000_000L;
    private static final long STUCK_WAIT_NS = 300_000_000L;
    private static final long STUCK_TRIGGER_NS = 700_000_000L;
    private static final long OBSTACLE_SEARCH_RETRY_NS = 500_000_000L;
    private static final long STUCK_SAMPLE_NS = 700_000_000L;
    private static final double STUCK_MOVE_EPSILON = 1.8;
    private static final double LOCAL_ESCAPE_DISTANCE = 30.0;
    private static final double ESCAPE_SEARCH_STEP = 18.0;
    private static final int ESCAPE_SEARCH_RINGS = 6;
    private static final int ESCAPE_SEARCH_SAMPLES = 16;
    private static final double AGGRO_RANGE = 260.0;
    private static final double LEASH_EXTRA = 90.0;
    private static final double PLAYER_PRIORITY_RANGE = 120.0;
    private static final double PLAYER_LEASH_RANGE = 155.0;
    private static final double DIRECT_TARGET_RECALC_DISTANCE = 28.0;
    private static final double PATH_POINT_REACHED = 8.0;
    private static final int OBSTACLE_RAY_TILES = 30;
    private static final int OBSTACLE_NEARBY_RADIUS_TILES = 5;
    private static final double MOVE_SPEED = 1.50;
    private static final int MAX_HP = 20;
    private static final int DAMAGE = 5;
    public static final double RENDER_WIDTH = 56.0;
    public static final double RENDER_HEIGHT = 56.0;
    private static final double HITBOX_WIDTH = 18.0;
    private static final double HITBOX_HEIGHT = 14.0;
    private static final double HITBOX_OFFSET_X = (RENDER_WIDTH - HITBOX_WIDTH) * 0.5;
    private static final double HITBOX_OFFSET_Y = RENDER_HEIGHT - HITBOX_HEIGHT - 1.0;
    private static final double ATTACK_HITBOX_WIDTH = 24.0;
    private static final double ATTACK_HITBOX_HEIGHT = 22.0;
    private static final double ATTACK_FORWARD_GAP = 1.0;
    private static final double ATTACK_TARGET_OVERLAP = 4.0;

    private final SpriteAnimation attackingAnimation;
    private final SpriteAnimation dyingAnimation;
    private final SpriteAnimation hurtAnimation;
    private final SpriteAnimation idleAnimation;
    private final SpriteAnimation walkingAnimation;
    private final MovementValidator movementValidator;
    private final EnemyNavigationContext navigationContext;
    private final List<Point2D> currentPath;
    private final GolemMode mode;

    private State state;
    private EnemyAiState aiState;
    private State resumeStateAfterHurt;
    private Entity currentTarget;
    private BuildObject currentBuildTarget;
    private ResourceNode currentResourceTarget;
    private EnemyObstacleTarget escapeObstacleTarget;
    private boolean facingRight;
    private AttackDirection attackDirection;
    private boolean removeFromWorld;
    private boolean debugEnabled;
    private long lastAttackAtNs;
    private long lastHurtAtNs;
    private long lastPathComputeAtNs;
    private long blockedSinceNs;
    private long stuckSampleAtNs;
    private long nextPathAllowedAtNs;
    private long waitUntilNs;
    private long nextObstacleSearchAtNs;
    private double lastPathTargetX;
    private double lastPathTargetY;
    private double stuckSampleX;
    private double stuckSampleY;
    private double desiredMoveDirX;
    private double desiredMoveDirY;
    private long stuckTimerNs;
    private int blockedCount;
    private int blockedDirections;
    private int currentPathIndex;
    private boolean attackDamageAppliedThisCycle;
    private boolean pendingPathRequest;
    private boolean lastPathFailed;

    public GolemEnemy(double x, double y, MovementValidator movementValidator, EnemyNavigationContext navigationContext) {
        this(x, y, movementValidator, navigationContext, GolemMode.NORMAL);
    }

    public GolemEnemy(double x, double y, MovementValidator movementValidator, EnemyNavigationContext navigationContext, GolemMode mode) {
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
        this.navigationContext = navigationContext;
        this.currentPath = new ArrayList<>();
        this.mode = mode == null ? GolemMode.NORMAL : mode;
        this.state = State.WALKING_TO_BASE;
        this.aiState = EnemyAiState.MOVE_TO_BASE;
        this.resumeStateAfterHurt = State.WALKING_TO_BASE;
        this.currentTarget = null;
        this.currentBuildTarget = null;
        this.currentResourceTarget = null;
        this.escapeObstacleTarget = null;
        this.facingRight = true;
        this.attackDirection = AttackDirection.RIGHT;
        this.removeFromWorld = false;
        this.debugEnabled = false;
        this.lastAttackAtNs = -ATTACK_COOLDOWN_NS;
        this.lastHurtAtNs = -HURT_RESTART_GUARD_NS;
        this.lastPathComputeAtNs = 0L;
        this.blockedSinceNs = 0L;
        this.stuckSampleAtNs = 0L;
        this.nextPathAllowedAtNs = 0L;
        this.waitUntilNs = 0L;
        this.nextObstacleSearchAtNs = 0L;
        this.lastPathTargetX = Double.NaN;
        this.lastPathTargetY = Double.NaN;
        this.stuckSampleX = getCenterX();
        this.stuckSampleY = getCenterY();
        this.desiredMoveDirX = 0.0;
        this.desiredMoveDirY = 0.0;
        this.stuckTimerNs = 0L;
        this.blockedCount = 0;
        this.blockedDirections = 0;
        this.currentPathIndex = 0;
        this.attackDamageAppliedThisCycle = false;
        this.pendingPathRequest = false;
        this.lastPathFailed = false;
    }

    public static void preloadAssets() {
        PngSequenceLoader.loadPngSequence(BASE_FOLDER + "/Attacking", "Golem_01_Attacking_", ATTACK_FRAME_COUNT);
        PngSequenceLoader.loadPngSequence(BASE_FOLDER + "/Dying", "Golem_01_Dying_", DEATH_FRAME_COUNT);
        PngSequenceLoader.loadPngSequence(BASE_FOLDER + "/Hurt", "Golem_01_Hurt_", HURT_FRAME_COUNT);
        PngSequenceLoader.loadPngSequence(BASE_FOLDER + "/Idle", "Golem_01_Idle_", IDLE_FRAME_COUNT);
        PngSequenceLoader.loadPngSequence(BASE_FOLDER + "/Walking", "Golem_01_Walking_", WALK_FRAME_COUNT);
        SpriteSheetLoader.loadGrid(assetFrame("Walking", "Golem_01_Walking_", 0), 1, 1);
        SpriteSheetLoader.loadGrid(assetFrame("Idle", "Golem_01_Idle_", 0), 1, 1);
    }

    public void updateAi(long nowNs, BaseCamp baseCamp, Player player, Iterable<FriendlyArcher> friendlies, double worldWidth, double worldHeight) {
        if (removeFromWorld) {
            return;
        }
        if (state == State.DYING) {
            aiState = EnemyAiState.DEATH;
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
        if (currentBuildTarget != null && !currentBuildTarget.isAlive()) {
            currentBuildTarget = null;
        }
        if (currentResourceTarget != null && !currentResourceTarget.isAlive()) {
            currentResourceTarget = null;
        }
        if (escapeObstacleTarget != null && !escapeObstacleTarget.isAlive()) {
            escapeObstacleTarget = null;
            currentResourceTarget = null;
        }
        if (escapeObstacleTarget != null) {
            updateEscapeObstacleTarget(nowNs, worldWidth, worldHeight);
            return;
        }

        Entity preferredTarget = choosePreferredTarget(baseCamp, player, friendlies);
        if (preferredTarget == null) {
            enterIdle(nowNs);
            return;
        }

        if (preferredTarget instanceof Player && isWithinAttackRange(preferredTarget)) {
            updateEntityTarget(nowNs, preferredTarget, worldWidth, worldHeight);
            return;
        }

        if (currentBuildTarget != null && shouldDropBuildTargetForEntity(preferredTarget)) {
            currentBuildTarget = null;
            clearPath();
        }

        if (currentBuildTarget != null) {
            updateBuildTarget(nowNs, preferredTarget, worldWidth, worldHeight);
            return;
        }
        if (currentResourceTarget != null) {
            updateResourceTarget(nowNs, preferredTarget, worldWidth, worldHeight);
            return;
        }

        if (baseCamp != null && baseCamp.isAlive()) {
            BuildObject wall = findBlockingObstacleThrottled(baseCamp.getCenterX(), baseCamp.getCenterY(), nowNs);
            if (wall != null) {
                currentBuildTarget = wall;
                aiState = EnemyAiState.MOVE_TO_OBSTACLE;
                clearPath();
                updateBuildTarget(nowNs, preferredTarget, worldWidth, worldHeight);
                return;
            }
        }

        updateEntityTarget(nowNs, preferredTarget, worldWidth, worldHeight);
    }

    public boolean applyAttackIfReady(long nowNs) {
        if (state != State.ATTACKING) {
            return false;
        }
        int frameIndex = attackingAnimation.getCurrentFrameIndex();
        if (attackDamageAppliedThisCycle || (frameIndex != 7 && frameIndex != 8)) {
            return false;
        }
        if (nowNs - lastAttackAtNs < ATTACK_COOLDOWN_NS) {
            return false;
        }

        if (currentBuildTarget != null) {
            if (!isWithinAttackRange(currentBuildTarget) || navigationContext == null || !navigationContext.damageWall(this, currentBuildTarget, nowNs)) {
                return false;
            }
            navigationContext.recordFenceAttack(getEnemyType());
            if (!currentBuildTarget.isAlive()) {
                if (navigationContext != null) {
                    navigationContext.onObstacleDestroyed(currentBuildTarget, nowNs);
                }
                currentBuildTarget = null;
                clearPath();
            }
            lastAttackAtNs = nowNs;
            attackDamageAppliedThisCycle = true;
            return true;
        }
        if (currentResourceTarget != null) {
            if (!isWithinAttackRange(currentResourceTarget)
                    || navigationContext == null
                    || !navigationContext.damageObstacle(this, EnemyObstacleTarget.forResource(currentResourceTarget), nowNs)) {
                return false;
            }
            if (!currentResourceTarget.isAlive()) {
                currentResourceTarget = null;
                escapeObstacleTarget = null;
                clearPath();
            }
            lastAttackAtNs = nowNs;
            attackDamageAppliedThisCycle = true;
            return true;
        }

        if (currentTarget == null || currentTarget.isDead() || !isWithinAttackRange(currentTarget)) {
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
            aiState = EnemyAiState.DEATH;
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
        } else {
            graphicsContext.save();
            graphicsContext.translate(screenX + RENDER_WIDTH, screenY);
            graphicsContext.scale(-1, 1);
            graphicsContext.drawImage(frame, 0, 0, RENDER_WIDTH, RENDER_HEIGHT);
            graphicsContext.restore();
        }
        if (debugEnabled) {
            drawDebug(graphicsContext, cameraX, cameraY);
        }
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

    public BuildObject getCurrentBuildTarget() {
        return currentBuildTarget;
    }

    public GolemMode getMode() {
        return mode;
    }

    public void setInitialPathDelayNs(long delayNs) {
        long allowedAtNs = System.nanoTime() + Math.max(0L, delayNs);
        nextPathAllowedAtNs = allowedAtNs;
        nextObstacleSearchAtNs = allowedAtNs;
    }

    public double getAggroRange() {
        return AGGRO_RANGE;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public EnemyAiState getAiState() {
        return aiState;
    }

    public double getAttackHitboxX() {
        return switch (attackDirection) {
            case RIGHT -> getCollisionX() + getCollisionWidth() + ATTACK_FORWARD_GAP;
            case LEFT -> getCollisionX() - ATTACK_FORWARD_GAP - ATTACK_HITBOX_WIDTH;
            case UP, DOWN -> getCollisionX() + getCollisionWidth() * 0.5 - ATTACK_HITBOX_WIDTH * 0.5;
        };
    }

    public double getAttackHitboxY() {
        return switch (attackDirection) {
            case DOWN -> getCollisionY() + getCollisionHeight() + ATTACK_FORWARD_GAP;
            case UP -> getCollisionY() - ATTACK_FORWARD_GAP - ATTACK_HITBOX_HEIGHT;
            case LEFT, RIGHT -> getCollisionY() + getCollisionHeight() * 0.5 - ATTACK_HITBOX_HEIGHT * 0.5;
        };
    }

    public double getAttackHitboxWidth() {
        return attackDirection == AttackDirection.UP || attackDirection == AttackDirection.DOWN
                ? ATTACK_HITBOX_HEIGHT
                : ATTACK_HITBOX_WIDTH;
    }

    public double getAttackHitboxHeight() {
        return attackDirection == AttackDirection.UP || attackDirection == AttackDirection.DOWN
                ? ATTACK_HITBOX_WIDTH
                : ATTACK_HITBOX_HEIGHT;
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

    private void updateEntityTarget(long nowNs, Entity target, double worldWidth, double worldHeight) {
        currentTarget = target;
        currentResourceTarget = null;
        faceTarget(target.getCenterX(), target.getCenterY());
        if (isWithinAttackRange(target)) {
            startAttack();
            updateAttack(nowNs);
            return;
        }

        State chaseState = target instanceof BaseCamp ? State.WALKING_TO_BASE : State.CHASE_TARGET;
        aiState = target instanceof BaseCamp ? EnemyAiState.MOVE_TO_BASE : EnemyAiState.CHASE_PLAYER;
        if (state != chaseState) {
            state = chaseState;
        }
        boolean moved;
        if (target instanceof BaseCamp) {
            moved = followFlowFieldToBase((BaseCamp) target, nowNs, worldWidth, worldHeight);
        } else {
            Point2D approach = selectEntityApproachPoint(target);
            moved = followPathToPoint(approach.getX(), approach.getY(), nowNs, worldWidth, worldHeight);
        }
        if (!moved && target instanceof BaseCamp && navigationContext != null) {
            currentBuildTarget = findBlockingObstacleThrottled(target.getCenterX(), target.getCenterY(), nowNs);
            if (currentBuildTarget != null) {
                aiState = EnemyAiState.MOVE_TO_OBSTACLE;
                clearPath();
                updateBuildTarget(nowNs, target, worldWidth, worldHeight);
                return;
            }
        }
        walkingAnimation.update(nowNs, true);
    }

    private void updateBuildTarget(long nowNs, Entity preferredEntity, double worldWidth, double worldHeight) {
        if (currentBuildTarget == null || !currentBuildTarget.isAlive()) {
            currentBuildTarget = null;
            updateEntityTarget(nowNs, preferredEntity, worldWidth, worldHeight);
            return;
        }
        currentTarget = null;
        currentResourceTarget = null;
        faceTarget(currentBuildTarget.getCenterX(), currentBuildTarget.getCenterY());
        if (isWithinAttackRange(currentBuildTarget)) {
            startAttack();
            updateAttack(nowNs);
            return;
        }

        state = State.CHASE_TARGET;
        aiState = EnemyAiState.MOVE_TO_OBSTACLE;
        Point2D approach = selectBuildApproachPoint(currentBuildTarget);
        boolean moved = followPathToPoint(approach.getX(), approach.getY(), nowNs, worldWidth, worldHeight);
        if (!moved && canMoveDirectlyTo(approach.getX(), approach.getY())) {
            moveToward(approach.getX(), approach.getY(), worldWidth, worldHeight);
        }
        walkingAnimation.update(nowNs, true);
    }

    private void updateResourceTarget(long nowNs, Entity preferredEntity, double worldWidth, double worldHeight) {
        if (currentResourceTarget == null || !currentResourceTarget.isAlive()) {
            currentResourceTarget = null;
            updateEntityTarget(nowNs, preferredEntity, worldWidth, worldHeight);
            return;
        }
        currentTarget = null;
        faceTarget(currentResourceTarget.getCenterX(), currentResourceTarget.getCenterY());
        if (isWithinAttackRange(currentResourceTarget)) {
            startAttack();
            updateAttack(nowNs);
            return;
        }

        state = State.CHASE_TARGET;
        aiState = EnemyAiState.MOVE_TO_OBSTACLE;
        Point2D approach = selectResourceApproachPoint(currentResourceTarget);
        boolean moved = followPathToPoint(approach.getX(), approach.getY(), nowNs, worldWidth, worldHeight);
        if (!moved && canMoveDirectlyTo(approach.getX(), approach.getY())) {
            moveToward(approach.getX(), approach.getY(), worldWidth, worldHeight);
        }
        walkingAnimation.update(nowNs, true);
    }

    private void startAttack() {
        clearPath();
        if (state != State.ATTACKING) {
            state = State.ATTACKING;
            if (currentBuildTarget != null || currentResourceTarget != null) {
                aiState = EnemyAiState.ATTACK_OBSTACLE;
            } else if (currentTarget instanceof BaseCamp) {
                aiState = EnemyAiState.ATTACK_BASE;
            } else {
                aiState = EnemyAiState.ATTACK_PLAYER;
            }
            attackingAnimation.reset();
            attackDamageAppliedThisCycle = false;
        }
    }

    private void enterIdle(long nowNs) {
        currentTarget = null;
        currentBuildTarget = null;
        currentResourceTarget = null;
        escapeObstacleTarget = null;
        clearPath();
        state = State.IDLE;
        aiState = EnemyAiState.IDLE;
        idleAnimation.update(nowNs, true);
    }

    private boolean shouldDropBuildTargetForEntity(Entity target) {
        return target != null
                && target.isAlive()
                && !(target instanceof BaseCamp)
                && (isWithinAttackRange(target) || canMoveDirectlyTo(target.getCenterX(), target.getCenterY()));
    }

    private Entity choosePreferredTarget(BaseCamp baseCamp, Player player, Iterable<FriendlyArcher> friendlies) {
        Entity nearestAggroTarget = nearestAggroTarget(player, friendlies);
        if (nearestAggroTarget != null) {
            return nearestAggroTarget;
        }
        if (currentTarget != null && currentTarget.isAlive() && !(currentTarget instanceof BaseCamp)) {
            double currentDistance = distanceTo(currentTarget.getCenterX(), currentTarget.getCenterY());
            if (!(currentTarget instanceof Player) && currentDistance <= AGGRO_RANGE + LEASH_EXTRA) {
                return currentTarget;
            }
            if (currentTarget instanceof Player && currentDistance <= PLAYER_LEASH_RANGE) {
                return currentTarget;
            }
        }
        if (baseCamp != null && baseCamp.isAlive()) {
            return baseCamp;
        }
        return null;
    }

    private Entity nearestAggroTarget(Player player, Iterable<FriendlyArcher> friendlies) {
        Entity nearest = null;
        double nearestDistance = AGGRO_RANGE;
        if (currentTarget != null && currentTarget.isAlive() && !(currentTarget instanceof BaseCamp)) {
            double currentDistance = distanceTo(currentTarget.getCenterX(), currentTarget.getCenterY());
            if (!(currentTarget instanceof Player) || currentDistance <= PLAYER_LEASH_RANGE) {
                nearest = currentTarget;
                nearestDistance = Math.min(AGGRO_RANGE + LEASH_EXTRA, currentDistance);
            }
        }
        if (player != null && player.isAlive()) {
            double distance = distanceTo(player.getCenterX(), player.getCenterY());
            if (distance <= PLAYER_PRIORITY_RANGE || isWithinAttackRange(player)) {
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

    private Point2D selectEntityApproachPoint(Entity target) {
        if (target == null) {
            return new Point2D(getCenterX(), getCenterY());
        }
        return selectApproachPoint(
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
    }

    private Point2D selectBuildApproachPoint(BuildObject target) {
        if (target == null) {
            return new Point2D(getCenterX(), getCenterY());
        }
        return selectApproachPoint(
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
    }

    private Point2D selectResourceApproachPoint(ResourceNode target) {
        if (target == null) {
            return new Point2D(getCenterX(), getCenterY());
        }
        return selectApproachPoint(
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
    }

    private Point2D selectApproachPoint(double targetX, double targetY, double targetWidth, double targetHeight) {
        double targetCenterX = targetX + targetWidth * 0.5;
        double targetCenterY = targetY + targetHeight * 0.5;
        faceTarget(targetCenterX, targetCenterY);
        return switch (attackDirection) {
            case RIGHT -> new Point2D(targetX - ATTACK_FORWARD_GAP - ATTACK_HITBOX_WIDTH + ATTACK_TARGET_OVERLAP - getCollisionWidth() * 0.5, targetCenterY);
            case LEFT -> new Point2D(targetX + targetWidth + ATTACK_FORWARD_GAP + ATTACK_HITBOX_WIDTH - ATTACK_TARGET_OVERLAP + getCollisionWidth() * 0.5, targetCenterY);
            case DOWN -> new Point2D(targetCenterX, targetY - ATTACK_FORWARD_GAP - ATTACK_HITBOX_HEIGHT + ATTACK_TARGET_OVERLAP - getCollisionHeight() * 0.5);
            case UP -> new Point2D(targetCenterX, targetY + targetHeight + ATTACK_FORWARD_GAP + ATTACK_HITBOX_HEIGHT - ATTACK_TARGET_OVERLAP + getCollisionHeight() * 0.5);
        };
    }

    private boolean followPathToPoint(double targetX, double targetY, long nowNs, double worldWidth, double worldHeight) {
        if (waitUntilNs > nowNs) {
            return false;
        }
        applyPendingPathResult(nowNs);
        setDesiredMove(targetX - getCenterX(), targetY - getCenterY());
        if (canMoveDirectlyTo(targetX, targetY)) {
            clearPath();
            boolean movedDirectly = moveToward(targetX, targetY, worldWidth, worldHeight);
            if (movedDirectly) {
                blockedSinceNs = 0L;
                blockedCount = 0;
                blockedDirections = 0;
                stuckTimerNs = 0L;
                updateStuckSample(nowNs);
            }
            return movedDirectly;
        }
        if (shouldRequestPath(targetX, targetY, nowNs)) {
            requestPath(targetX, targetY, nowNs);
        }
        boolean moved = false;
        if (!currentPath.isEmpty()) {
            if (currentPathIndex < currentPath.size()) {
                Point2D waypoint = currentPath.get(currentPathIndex);
                if (!canStandCenteredAt(waypoint.getX(), waypoint.getY())) {
                    clearPath();
                } else if (distanceTo(waypoint.getX(), waypoint.getY()) <= PATH_POINT_REACHED) {
                    currentPathIndex++;
                } else {
                    moved = moveToward(waypoint.getX(), waypoint.getY(), worldWidth, worldHeight);
                }
            }
        }
        if (!moved && canMoveDirectlyTo(targetX, targetY)) {
            moved = moveToward(targetX, targetY, worldWidth, worldHeight);
        }
        if (moved) {
            blockedSinceNs = 0L;
            blockedCount = 0;
            blockedDirections = 0;
            stuckTimerNs = 0L;
            updateStuckSample(nowNs);
            return true;
        }
        if (blockedSinceNs == 0L) {
            blockedSinceNs = nowNs;
        } else if (nowNs - blockedSinceNs >= STUCK_REPATH_NS) {
            recoverFromBlockedPath(targetX, targetY, nowNs);
        }
        return false;
    }

    private void updateStuckSample(long nowNs) {
        if (state != State.CHASE_TARGET && state != State.WALKING_TO_BASE) {
            stuckSampleAtNs = nowNs;
            stuckSampleX = getCenterX();
            stuckSampleY = getCenterY();
            return;
        }
        if (stuckSampleAtNs == 0L) {
            stuckSampleAtNs = nowNs;
            stuckSampleX = getCenterX();
            stuckSampleY = getCenterY();
            return;
        }
        if (nowNs - stuckSampleAtNs < STUCK_SAMPLE_NS) {
            return;
        }
        double moved = distance(stuckSampleX, stuckSampleY, getCenterX(), getCenterY());
        long elapsedNs = nowNs - stuckSampleAtNs;
        stuckSampleAtNs = nowNs;
        stuckSampleX = getCenterX();
        stuckSampleY = getCenterY();
        if (moved < STUCK_MOVE_EPSILON) {
            stuckTimerNs += Math.max(0L, elapsedNs);
            if (stuckTimerNs >= STUCK_TRIGGER_NS) {
                recoverFromBlockedPath(lastPathTargetX, lastPathTargetY, nowNs);
            }
            return;
        }
        stuckTimerNs = 0L;
    }

    private void recoverFromBlockedPath(double targetX, double targetY, long nowNs) {
        blockedSinceNs = 0L;
        blockedCount++;
        blockedDirections = countBlockedDirections();
        stuckTimerNs = 0L;
        clearPath();
        if (navigationContext != null) {
            navigationContext.recordStuck(getEnemyType());
        }
        aiState = EnemyAiState.STUCK_RECOVERY;
        if (navigationContext != null && (blockedDirections >= 2 || blockedCount >= 2)) {
            EnemyObstacleTarget escapeTarget = navigationContext.findEscapeObstacle(this, desiredMoveDirX, desiredMoveDirY, 3);
            if (escapeTarget != null) {
                escapeObstacleTarget = escapeTarget;
                currentTarget = null;
                currentBuildTarget = escapeTarget.buildObject();
                currentResourceTarget = escapeTarget.resourceNode();
                aiState = EnemyAiState.MOVE_TO_OBSTACLE;
                waitUntilNs = 0L;
                return;
            }
        }
        if (chooseOrderedEscapeWaypoint(targetX, targetY)) {
            waitUntilNs = 0L;
            return;
        }
        if (navigationContext != null) {
            EnemyObstacleTarget escapeTarget = navigationContext.findEscapeObstacle(this, desiredMoveDirX, desiredMoveDirY, 3);
            if (escapeTarget != null) {
                escapeObstacleTarget = escapeTarget;
                currentTarget = null;
                currentBuildTarget = escapeTarget.buildObject();
                currentResourceTarget = escapeTarget.resourceNode();
                aiState = EnemyAiState.MOVE_TO_OBSTACLE;
                waitUntilNs = 0L;
                return;
            }
        }
        waitUntilNs = nowNs + STUCK_WAIT_NS;
    }

    private boolean chooseOrderedEscapeWaypoint(double targetX, double targetY) {
        double desiredX = desiredMoveDirX;
        double desiredY = desiredMoveDirY;
        if ((Math.abs(desiredX) < 0.001 && Math.abs(desiredY) < 0.001) && !Double.isNaN(targetX) && !Double.isNaN(targetY)) {
            desiredX = targetX - getCenterX();
            desiredY = targetY - getCenterY();
        }
        double length = Math.sqrt(desiredX * desiredX + desiredY * desiredY);
        if (length < 0.001) {
            desiredX = facingRight ? 1.0 : -1.0;
            desiredY = 0.0;
            length = 1.0;
        }
        double forwardX = desiredX / length;
        double forwardY = desiredY / length;
        double[][] candidates = {
                {-forwardX, -forwardY},
                {-forwardY, forwardX},
                {forwardY, -forwardX},
                {(-forwardX - forwardY) * 0.70710678118, (-forwardY + forwardX) * 0.70710678118},
                {(-forwardX + forwardY) * 0.70710678118, (-forwardY - forwardX) * 0.70710678118}
        };
        for (double[] candidate : candidates) {
            double centerX = getCenterX() + candidate[0] * LOCAL_ESCAPE_DISTANCE;
            double centerY = getCenterY() + candidate[1] * LOCAL_ESCAPE_DISTANCE;
            if (!canStandCenteredAt(centerX, centerY) || !canMoveDirectlyTo(centerX, centerY)) {
                continue;
            }
            currentPath.add(new Point2D(centerX, centerY));
            currentPathIndex = 0;
            return true;
        }
        return false;
    }

    private int countBlockedDirections() {
        int blocked = 0;
        double step = Math.max(10.0, Math.min(getCollisionWidth(), getCollisionHeight()));
        if (!canStandCenteredAt(getCenterX() + step, getCenterY())) {
            blocked++;
        }
        if (!canStandCenteredAt(getCenterX() - step, getCenterY())) {
            blocked++;
        }
        if (!canStandCenteredAt(getCenterX(), getCenterY() + step)) {
            blocked++;
        }
        if (!canStandCenteredAt(getCenterX(), getCenterY() - step)) {
            blocked++;
        }
        return blocked;
    }

    private boolean chooseEscapeWaypoint(double targetX, double targetY) {
        double targetBiasX = Double.isNaN(targetX) ? 0.0 : targetX - getCenterX();
        double targetBiasY = Double.isNaN(targetY) ? 0.0 : targetY - getCenterY();
        double targetBiasLength = Math.sqrt(targetBiasX * targetBiasX + targetBiasY * targetBiasY);
        if (targetBiasLength > 0.001) {
            targetBiasX /= targetBiasLength;
            targetBiasY /= targetBiasLength;
        }

        Point2D best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (int ring = 1; ring <= ESCAPE_SEARCH_RINGS; ring++) {
            double radius = ESCAPE_SEARCH_STEP * ring;
            int samples = ESCAPE_SEARCH_SAMPLES + ring * 4;
            for (int i = 0; i < samples; i++) {
                double angle = (Math.PI * 2.0 * i) / samples;
                double dx = Math.cos(angle);
                double dy = Math.sin(angle);
                double centerX = getCenterX() + dx * radius;
                double centerY = getCenterY() + dy * radius;
                if (!canStandCenteredAt(centerX, centerY)) {
                    continue;
                }
                double targetPenalty = targetBiasLength <= 0.001 ? 0.0 : Math.max(0.0, -(dx * targetBiasX + dy * targetBiasY)) * 22.0;
                double score = radius + targetPenalty;
                if (score < bestScore) {
                    bestScore = score;
                    best = new Point2D(centerX, centerY);
                }
            }
            if (best != null) {
                currentPath.add(best);
                currentPathIndex = 0;
                return true;
            }
        }
        return false;
    }

    private void chooseSideStepWaypoint(double targetX, double targetY) {
        if (Double.isNaN(targetX) || Double.isNaN(targetY)) {
            return;
        }
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.001) {
            return;
        }
        double nx = dx / dist;
        double ny = dy / dist;
        double[][] candidates = {
                {-ny, nx},
                {ny, -nx},
                {-nx, -ny},
                {nx * 0.35 - ny, ny * 0.35 + nx},
                {nx * 0.35 + ny, ny * 0.35 - nx}
        };
        double step = Math.max(16.0, Math.max(getCollisionWidth(), getCollisionHeight()) * 1.8);
        for (double[] candidate : candidates) {
            double centerX = getCenterX() + candidate[0] * step;
            double centerY = getCenterY() + candidate[1] * step;
            if (!canStandCenteredAt(centerX, centerY)) {
                continue;
            }
            currentPath.add(new Point2D(centerX, centerY));
            currentPathIndex = 0;
            return;
        }
    }

    private boolean shouldRequestPath(double targetX, double targetY, long nowNs) {
        if (pendingPathRequest || navigationContext == null || nowNs < nextPathAllowedAtNs) {
            return false;
        }
        if (currentPath.isEmpty() || currentPathIndex >= currentPath.size()) {
            return true;
        }
        if (nowNs - lastPathComputeAtNs >= PATH_RECALC_NS) {
            return true;
        }
        if (Double.isNaN(lastPathTargetX) || Double.isNaN(lastPathTargetY)) {
            return true;
        }
        return distance(lastPathTargetX, lastPathTargetY, targetX, targetY) >= DIRECT_TARGET_RECALC_DISTANCE;
    }

    private void requestPath(double targetX, double targetY, long nowNs) {
        if (navigationContext == null) {
            return;
        }
        navigationContext.requestPath(
                this,
                getCenterX(),
                getCenterY(),
                targetX,
                targetY,
                (candidateX, candidateY, candidateWidth, candidateHeight) ->
                        navigationContext.canPathOccupy(this, candidateX, candidateY, candidateWidth, candidateHeight),
                nowNs
        );
        pendingPathRequest = true;
        nextPathAllowedAtNs = nowNs + randomizedPathCooldownNs(false);
        lastPathTargetX = targetX;
        lastPathTargetY = targetY;
    }

    private void applyPendingPathResult(long nowNs) {
        if (navigationContext == null) {
            return;
        }
        PathfindingManager.PathResult result = navigationContext.consumePathResult(this);
        if (result == null) {
            return;
        }
        pendingPathRequest = false;
        lastPathComputeAtNs = nowNs;
        clearPath();
        if (!result.success()) {
            lastPathFailed = true;
            nextPathAllowedAtNs = nowNs + PATH_FAIL_RETRY_NS;
            return;
        }
        currentPath.addAll(result.waypoints());
        currentPathIndex = 0;
        lastPathTargetX = result.targetX();
        lastPathTargetY = result.targetY();
        lastPathFailed = false;
    }

    private boolean followFlowFieldToBase(BaseCamp baseCamp, long nowNs, double worldWidth, double worldHeight) {
        Point2D waypoint = navigationContext == null ? null : navigationContext.getFlowFieldWaypoint(getCenterX(), getCenterY());
        if (waypoint != null) {
            return followPathToPoint(waypoint.getX(), waypoint.getY(), nowNs, worldWidth, worldHeight);
        }
        return followPathToPoint(baseCamp.getCenterX(), baseCamp.getCenterY(), nowNs, worldWidth, worldHeight);
    }

    private long randomizedPathCooldownNs(boolean failed) {
        if (failed) {
            return PATH_FAIL_RETRY_NS;
        }
        return 500_000_000L + (long) (Math.random() * 700_000_000L);
    }

    private BuildObject findBlockingObstacleThrottled(double targetX, double targetY, long nowNs) {
        if (navigationContext == null || nowNs < nextObstacleSearchAtNs) {
            return null;
        }
        nextObstacleSearchAtNs = nowNs + OBSTACLE_SEARCH_RETRY_NS + (long) (Math.random() * 300_000_000L);
        return navigationContext.findBlockingObstacle(this, targetX, targetY, OBSTACLE_RAY_TILES, OBSTACLE_NEARBY_RADIUS_TILES);
    }

    private boolean canMoveDirectlyTo(double targetX, double targetY) {
        if (movementValidator == null) {
            return true;
        }
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= speed + 0.001) {
            return true;
        }
        double step = Math.max(8.0, Math.min(18.0, Math.max(getCollisionWidth(), getCollisionHeight()) * 0.8));
        int samples = Math.max(1, (int) Math.ceil(distance / step));
        for (int i = 1; i <= samples; i++) {
            double t = i / (double) samples;
            if (!canStandCenteredAt(getCenterX() + dx * t, getCenterY() + dy * t)) {
                return false;
            }
        }
        return true;
    }

    private boolean canStandCenteredAt(double centerX, double centerY) {
        double candidateX = centerX - width * 0.5;
        double candidateY = centerY - height * 0.5;
        return movementValidator == null || movementValidator.canOccupy(this, candidateX, candidateY, width, height);
    }

    private boolean moveToward(double targetX, double targetY, double worldWidth, double worldHeight) {
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 0.001) {
            return false;
        }
        double step = Math.min(speed, distance);
        double moveX = dx / distance * step;
        double moveY = dy / distance * step;
        if (moveWithCollision(moveX, moveY)) {
            clampPosition(0, 0, worldWidth, worldHeight);
            return true;
        }
        if (moveWithCollision(moveX, 0.0)) {
            clampPosition(0, 0, worldWidth, worldHeight);
            return true;
        }
        if (moveWithCollision(0.0, moveY)) {
            clampPosition(0, 0, worldWidth, worldHeight);
            return true;
        }
        return false;
    }

    private boolean moveWithCollision(double moveX, double moveY) {
        if (Math.abs(moveX) < 0.0001 && Math.abs(moveY) < 0.0001) {
            return false;
        }
        double nextX = x + moveX;
        double nextY = y + moveY;
        if (movementValidator != null && !movementValidator.canOccupy(this, nextX, nextY, width, height)) {
            return false;
        }
        x = nextX;
        y = nextY;
        updateFacingFromMovement(moveX, moveY);
        return true;
    }

    private void drawDebug(GraphicsContext graphicsContext, double cameraX, double cameraY) {
        graphicsContext.save();
        graphicsContext.setLineWidth(1.2);
        graphicsContext.setStroke(Color.color(0.3, 0.8, 1.0, 0.95));
        graphicsContext.strokeRect(getCollisionX() - cameraX, getCollisionY() - cameraY, getCollisionWidth(), getCollisionHeight());
        graphicsContext.setStroke(Color.color(1.0, 0.25, 0.1, 0.95));
        graphicsContext.strokeRect(
                getAttackHitboxX() - cameraX,
                getAttackHitboxY() - cameraY,
                getAttackHitboxWidth(),
                getAttackHitboxHeight()
        );
        if (!currentPath.isEmpty()) {
            graphicsContext.setStroke(Color.color(0.2, 1.0, 0.4, 0.85));
            double fromX = getCenterX() - cameraX;
            double fromY = getCenterY() - cameraY;
            for (int i = Math.max(0, currentPathIndex); i < currentPath.size(); i++) {
                Point2D point = currentPath.get(i);
                double toX = point.getX() - cameraX;
                double toY = point.getY() - cameraY;
                graphicsContext.strokeLine(fromX, fromY, toX, toY);
                graphicsContext.strokeOval(toX - 2.5, toY - 2.5, 5.0, 5.0);
                fromX = toX;
                fromY = toY;
            }
        }
        if (currentBuildTarget != null && currentBuildTarget.isAlive()) {
            graphicsContext.setStroke(Color.color(1.0, 0.85, 0.1, 0.95));
            graphicsContext.strokeRect(
                    currentBuildTarget.getCollisionX() - cameraX,
                    currentBuildTarget.getCollisionY() - cameraY,
                    currentBuildTarget.getCollisionWidth(),
                    currentBuildTarget.getCollisionHeight()
            );
        }
        if (currentResourceTarget != null && currentResourceTarget.isAlive()) {
            graphicsContext.setStroke(Color.color(0.95, 0.65, 0.15, 0.95));
            graphicsContext.strokeRect(
                    currentResourceTarget.getCollisionX() - cameraX,
                    currentResourceTarget.getCollisionY() - cameraY,
                    currentResourceTarget.getCollisionWidth(),
                    currentResourceTarget.getCollisionHeight()
            );
        }
        graphicsContext.setFill(Color.color(1.0, 1.0, 1.0, 0.98));
        String obstacleLabel = escapeObstacleTarget == null ? "-" : escapeObstacleTarget.getDebugLabel();
        graphicsContext.fillText(aiState.name() + " s=" + (stuckTimerNs / 1_000_000L) + "ms b=" + blockedDirections + " o=" + obstacleLabel,
                x - cameraX, y - HITBOX_OFFSET_Y - cameraY - 5.0);
        graphicsContext.restore();
    }

    private void updateFacingFromMovement(double moveX, double moveY) {
        if (Math.abs(moveX) > Math.abs(moveY)) {
            attackDirection = moveX >= 0.0 ? AttackDirection.RIGHT : AttackDirection.LEFT;
            facingRight = attackDirection == AttackDirection.RIGHT;
            return;
        }
        if (Math.abs(moveY) > 0.08) {
            attackDirection = moveY >= 0.0 ? AttackDirection.DOWN : AttackDirection.UP;
        }
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
        return intersectsAttackHitbox(
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
    }

    private boolean isWithinAttackRange(BuildObject target) {
        if (target == null) {
            return false;
        }
        return intersectsAttackHitbox(
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
    }

    private boolean isWithinAttackRange(ResourceNode target) {
        if (target == null) {
            return false;
        }
        return intersectsAttackHitbox(
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        ) || intersectsRect(
                getCollisionX() - 40.0,
                getCollisionY() - 40.0,
                getCollisionWidth() + 80.0,
                getCollisionHeight() + 80.0,
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
    }

    private boolean intersectsAttackHitbox(double targetX, double targetY, double targetWidth, double targetHeight) {
        return intersectsRect(
                getAttackHitboxX(),
                getAttackHitboxY(),
                getAttackHitboxWidth(),
                getAttackHitboxHeight(),
                targetX,
                targetY,
                targetWidth,
                targetHeight
        );
    }

    private void faceTarget(double targetCenterX, double targetCenterY) {
        double dx = targetCenterX - getCenterX();
        double dy = targetCenterY - getCenterY();
        if (Math.abs(dx) >= Math.abs(dy)) {
            attackDirection = dx >= 0.0 ? AttackDirection.RIGHT : AttackDirection.LEFT;
            facingRight = attackDirection == AttackDirection.RIGHT;
        } else {
            attackDirection = dy >= 0.0 ? AttackDirection.DOWN : AttackDirection.UP;
        }
    }

    private double distanceTo(double targetCenterX, double targetCenterY) {
        double dx = targetCenterX - getCenterX();
        double dy = targetCenterY - getCenterY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double distance(double ax, double ay, double bx, double by) {
        double dx = ax - bx;
        double dy = ay - by;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private boolean intersectsRect(double ax, double ay, double aw, double ah, double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private void clearPath() {
        currentPath.clear();
        currentPathIndex = 0;
        lastPathTargetX = Double.NaN;
        lastPathTargetY = Double.NaN;
        blockedSinceNs = 0L;
    }

    private void updateEscapeObstacleTarget(long nowNs, double worldWidth, double worldHeight) {
        if (escapeObstacleTarget == null || !escapeObstacleTarget.isAlive()) {
            escapeObstacleTarget = null;
            currentBuildTarget = null;
            currentResourceTarget = null;
            return;
        }
        if (escapeObstacleTarget.isBuildObject()) {
            currentBuildTarget = escapeObstacleTarget.buildObject();
            updateBuildTarget(nowNs, null, worldWidth, worldHeight);
            return;
        }
        currentResourceTarget = escapeObstacleTarget.resourceNode();
        updateResourceTarget(nowNs, null, worldWidth, worldHeight);
    }

    private void setDesiredMove(double dx, double dy) {
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.001) {
            desiredMoveDirX = 0.0;
            desiredMoveDirY = 0.0;
            return;
        }
        desiredMoveDirX = dx / length;
        desiredMoveDirY = dy / length;
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
