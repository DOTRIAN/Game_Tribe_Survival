package entity;

import animation.SpriteAnimation;
import animation.SpriteSheetLoader;
import buildsystem.object.BuildObject;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import system.DamageSystem;
import system.MovementSlideSystem;
import system.resource.ResourceNode;

import java.io.IOException;
import java.nio.file.DirectoryStream;
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
import java.util.Random;
import java.util.Set;

public class WolfEnemy extends Enemy {
    private static final boolean DEBUG = true;

    private enum BrainState {
        PATROL,
        CHASE,
        ATTACK,
        DEATH
    }

    private enum AnimationState {
        IDLE,
        WALK,
        RUN,
        ATTACK_2,
        DEAD
    }

    private record AttackProfile(AnimationState animationState,
                                 int impactFrame,
                                 long cooldownNs,
                                 long frameDurationNs,
                                 double range,
                                 double hitboxWidth,
                                 double hitboxHeight,
                                 double forwardOffset,
                                 int damage,
                                 boolean lungeAttack) {
        long totalDurationNs(int frameCount) {
            return Math.max(frameDurationNs, frameDurationNs * Math.max(1, frameCount));
        }
    }

    @FunctionalInterface
    public interface MovementValidator {
        boolean canOccupy(WolfEnemy enemy, double x, double y, double width, double height);
    }

    private static final double WALK_SPEED = 1.38;
    private static final double RUN_SPEED = 2.70;
    private static final double JUMP_SPEED = 3.35;
    private static final int MAX_HP = 30;
    private static final int DAMAGE = 5;
    private static final double DETECTION_RANGE = 220.0;
    private static final double DISENGAGE_RANGE = 260.0;
    private static final double HOME_RADIUS = 150.0;
    private static final double CHASE_HOME_RADIUS = 240.0;
    private static final double DAY_DETECTION_RANGE = DETECTION_RANGE * 0.5;
    private static final double DAY_DISENGAGE_RANGE = DISENGAGE_RANGE * 0.5;
    private static final double DAY_HOME_RADIUS = HOME_RADIUS * 0.5;
    private static final double DAY_CHASE_HOME_RADIUS = CHASE_HOME_RADIUS * 0.5;
    private static final double NIGHT_PLAYER_DEFEND_RADIUS = 220.0;
    private static final double NIGHT_WALL_BREAK_DISTANCE = 92.0;
    private static final double RETURN_TOLERANCE = 10.0;
    private static final double DIRECT_TARGET_RECALC_DISTANCE = 28.0;
    private static final double PATH_POINT_REACHED = 9.0;
    private static final long IDLE_FRAME_NS = 120_000_000L;
    private static final long WALK_FRAME_NS = 95_000_000L;
    private static final long RUN_FRAME_NS = 82_000_000L;
    private static final long ATTACK_2_FRAME_NS = 90_000_000L;
    private static final long DEAD_FRAME_NS = 160_000_000L;
    private static final long PATH_RECALC_NS = 500_000_000L;
    private static final long PATH_FAIL_RETRY_NS = 1_000_000_000L;
    private static final long STUCK_REPATH_NS = 650_000_000L;
    private static final long STUCK_WAIT_NS = 300_000_000L;
    private static final long STUCK_TRIGGER_NS = 700_000_000L;
    private static final long OBSTACLE_SEARCH_RETRY_NS = 500_000_000L;
    private static final long ESCAPE_OBSTACLE_SEARCH_RETRY_NS = 420_000_000L;
    private static final long DIRECT_PATH_SUCCESS_CACHE_NS = 220_000_000L;
    private static final long DIRECT_PATH_FAIL_CACHE_NS = 650_000_000L;
    private static final int STUCK_REPATH_AFTER_BLOCKS = 2;
    private static final int STUCK_BREAK_AFTER_BLOCKS = 4;
    private static final int STUCK_BREAK_BLOCKED_DIRECTIONS = 3;
    private static final double STUCK_MOVE_EPSILON = 2.0;
    private static final long STUCK_SAMPLE_NS = 700_000_000L;
    private static final double RETURN_HOME_DISTANCE = 34.0;
    private static final double PLAYER_ATTACK_APPROACH_DISTANCE = 72.0;
    private static final double PLAYER_CHASE_APPROACH_DISTANCE = 38.0;
    private static final double LOCAL_ESCAPE_DISTANCE = 30.0;
    private static final double ESCAPE_SEARCH_STEP = 18.0;
    private static final int ESCAPE_SEARCH_RINGS = 6;
    private static final int ESCAPE_SEARCH_SAMPLES = 16;
    private static final double FACE_MOVE_THRESHOLD = 0.10;
    private static final int OBSTACLE_RAY_TILES = 30;
    private static final int OBSTACLE_NEARBY_RADIUS_TILES = 5;
    private static final int MAX_DIRECT_PATH_SAMPLES = 6;

    private static final AttackProfile ATTACK_TWO = new AttackProfile(AnimationState.ATTACK_2, 2, 0L, ATTACK_2_FRAME_NS, 0.0, 24.0, 26.0, 0.0, DAMAGE, false);

    private final MovementValidator movementValidator;
    private final EnemyNavigationContext navigationContext;
    private final Random random;
    private final Map<AnimationState, SpriteAnimation> animations;

    private BrainState state;
    private EnemyAiState aiState;
    private AnimationState animationState;
    private Direction facingDirection;
    private double homeX;
    private double homeY;
    private double patrolTargetX;
    private double patrolTargetY;
    private double lastPathTargetX;
    private double lastPathTargetY;
    private double currentMoveSpeed;
    private boolean removeFromWorld;
    private boolean debugEnabled;
    private long lastPathComputeAtNs;
    private long blockedSinceNs;
    private long stuckSampleAtNs;
    private long nextPathAllowedAtNs;
    private long waitUntilNs;
    private long nextObstacleSearchAtNs;
    private long attackStartedAtNs;
    private long attackCooldownUntilNs;
    private boolean attackDamageAppliedThisCycle;
    private AttackProfile activeAttackProfile;
    private Entity activeEntityTarget;
    private Player aggroPlayer;
    private BuildObject activeBuildTarget;
    private ResourceNode activeResourceTarget;
    private EnemyObstacleTarget escapeObstacleTarget;
    private final List<Point2D> currentPath;
    private int currentPathIndex;
    private double stuckSampleX;
    private double stuckSampleY;
    private double desiredMoveDirX;
    private double desiredMoveDirY;
    private long stuckTimerNs;
    private int blockedCount;
    private int blockedDirections;
    private int pathFailCount;
    private boolean pendingPathRequest;
    private boolean lastPathFailed;
    private long currentUpdateNowNs;
    private boolean expensiveNavigationAllowed;
    private long lastDirectPathCheckAtNs;
    private double lastDirectPathOriginX;
    private double lastDirectPathOriginY;
    private double lastDirectPathProbeTargetX;
    private double lastDirectPathProbeTargetY;
    private boolean lastDirectPathResult;
    private long nextEscapeObstacleSearchAtNs;
    private BuildObject cachedBlockingObstacle;
    private EnemyObstacleTarget cachedEscapeObstacle;
    private boolean siegeMode;

    public WolfEnemy(double x,
                     double y,
                     double width,
                     double height,
                     MovementValidator movementValidator,
                     EnemyNavigationContext navigationContext,
                     Random random) {
        super(
                x,
                y,
                width,
                height,
                RUN_SPEED,
                MAX_HP,
                DAMAGE,
                1L,
                resolveAsset("Run"),
                9,
                1,
                RUN_FRAME_NS,
                resolveAsset("Idle"),
                8,
                1,
                IDLE_FRAME_NS
        );
        this.movementValidator = movementValidator;
        this.navigationContext = navigationContext;
        this.random = random == null ? new Random() : random;
        this.animations = new HashMap<>();
        animations.put(AnimationState.IDLE, new SpriteAnimation(SpriteSheetLoader.loadGrid(resolveAsset("Idle"), 8, 1), IDLE_FRAME_NS));
        animations.put(AnimationState.WALK, new SpriteAnimation(SpriteSheetLoader.loadGrid(resolveAsset("Walk"), 11, 1), WALK_FRAME_NS));
        animations.put(AnimationState.RUN, new SpriteAnimation(SpriteSheetLoader.loadGrid(resolveAsset("Run"), 9, 1), RUN_FRAME_NS));
        animations.put(AnimationState.ATTACK_2, new SpriteAnimation(SpriteSheetLoader.loadHorizontalStrip(resolveAsset("Attack_2"), 4), ATTACK_2_FRAME_NS));
        animations.put(AnimationState.DEAD, new SpriteAnimation(SpriteSheetLoader.loadGrid(resolveAsset("Dead"), 2, 1), DEAD_FRAME_NS));
        this.state = BrainState.PATROL;
        this.aiState = EnemyAiState.PATROL;
        this.animationState = AnimationState.IDLE;
        this.facingDirection = Direction.RIGHT;
        this.homeX = getCenterX();
        this.homeY = getCenterY();
        this.patrolTargetX = homeX;
        this.patrolTargetY = homeY;
        this.lastPathTargetX = Double.NaN;
        this.lastPathTargetY = Double.NaN;
        this.currentMoveSpeed = WALK_SPEED;
        this.removeFromWorld = false;
        this.debugEnabled = DEBUG;
        this.lastPathComputeAtNs = 0L;
        this.blockedSinceNs = 0L;
        this.stuckSampleAtNs = 0L;
        this.nextPathAllowedAtNs = 0L;
        this.waitUntilNs = 0L;
        this.nextObstacleSearchAtNs = 0L;
        this.attackStartedAtNs = -1L;
        this.attackCooldownUntilNs = 0L;
        this.attackDamageAppliedThisCycle = false;
        this.activeAttackProfile = null;
        this.activeEntityTarget = null;
        this.aggroPlayer = null;
        this.activeBuildTarget = null;
        this.activeResourceTarget = null;
        this.escapeObstacleTarget = null;
        this.currentPath = new ArrayList<>();
        this.currentPathIndex = 0;
        this.stuckSampleX = getCenterX();
        this.stuckSampleY = getCenterY();
        this.desiredMoveDirX = 0.0;
        this.desiredMoveDirY = 0.0;
        this.stuckTimerNs = 0L;
        this.blockedCount = 0;
        this.blockedDirections = 0;
        this.pathFailCount = 0;
        this.pendingPathRequest = false;
        this.lastPathFailed = false;
        this.currentUpdateNowNs = 0L;
        this.expensiveNavigationAllowed = true;
        this.lastDirectPathCheckAtNs = Long.MIN_VALUE;
        this.lastDirectPathOriginX = Double.NaN;
        this.lastDirectPathOriginY = Double.NaN;
        this.lastDirectPathProbeTargetX = Double.NaN;
        this.lastDirectPathProbeTargetY = Double.NaN;
        this.lastDirectPathResult = false;
        this.nextEscapeObstacleSearchAtNs = 0L;
        this.cachedBlockingObstacle = null;
        this.cachedEscapeObstacle = null;
        this.siegeMode = false;
        pickNextPatrolTarget();
    }

    @Override
    protected double collisionInsetLeft(double width, double height) {
        return width * 0.25;
    }

    @Override
    protected double collisionInsetRight(double width, double height) {
        return width * 0.25;
    }

    @Override
    protected double collisionInsetTop(double width, double height) {
        return height * 0.50;
    }

    @Override
    protected double collisionInsetBottom(double width, double height) {
        return 0.0;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = DEBUG && debugEnabled;
    }

    public static void preloadAssets() {
        SpriteSheetLoader.loadGrid(resolveAsset("Idle"), 8, 1);
        SpriteSheetLoader.loadGrid(resolveAsset("Walk"), 11, 1);
        SpriteSheetLoader.loadGrid(resolveAsset("Run"), 9, 1);
        SpriteSheetLoader.loadHorizontalStrip(resolveAsset("Attack_2"), 4);
        SpriteSheetLoader.loadGrid(resolveAsset("Dead"), 2, 1);
    }

    public void updateBehavior(long nowNs,
                               boolean isNight,
                               Player player,
                               BaseCamp baseCamp,
                               double worldWidth,
                               double worldHeight,
                               boolean allowExpensiveNavigation) {
        currentUpdateNowNs = nowNs;
        expensiveNavigationAllowed = allowExpensiveNavigation;
        if (removeFromWorld) {
            return;
        }
        if (state == BrainState.DEATH) {
            aiState = EnemyAiState.DEATH;
            if (currentAnimation().updateOnce(nowNs)) {
                removeFromWorld = true;
            }
            return;
        }
        if (!isAlive()) {
            transitionToDeath();
            return;
        }

        if (state == BrainState.ATTACK) {
            updateAttackSequence(nowNs);
            return;
        }

        if (activeBuildTarget != null && !activeBuildTarget.isAlive()) {
            activeBuildTarget = null;
        }
        if (activeResourceTarget != null && !activeResourceTarget.isAlive()) {
            activeResourceTarget = null;
        }
        if (activeEntityTarget != null && activeEntityTarget.isDead()) {
            activeEntityTarget = null;
        }
        if (escapeObstacleTarget != null && !escapeObstacleTarget.isAlive()) {
            escapeObstacleTarget = null;
            activeResourceTarget = null;
        }

        Player playerTarget = isNight
                ? resolveNightVillagePlayerTarget(player, baseCamp)
                : resolvePlayerTarget(player, nowNs);
        if (playerTarget != null) {
            if (escapeObstacleTarget != null) {
                clearObstacleFocus();
            }
            activeEntityTarget = playerTarget;
            activeBuildTarget = null;
            activeResourceTarget = null;
            if (tryStartAttack(playerTarget, null, nowNs, isNight)) {
                return;
            }
            state = BrainState.CHASE;
            aiState = EnemyAiState.CHASE_PLAYER;
            runTowardEntity(playerTarget, nowNs, worldWidth, worldHeight, isNight);
            return;
        }

        if (!isNight && escapeObstacleTarget != null) {
            clearObstacleFocus();
        }
        if (escapeObstacleTarget != null) {
            updateEscapeObstacleTarget(nowNs, worldWidth, worldHeight);
            return;
        }

        if (isNight) {
            siegeMode = false;
        }

        if (state == BrainState.CHASE) {
            clearPath();
        }
        activeEntityTarget = null;
        activeBuildTarget = null;
        activeResourceTarget = null;
        currentMoveSpeed = WALK_SPEED;
        double distanceFromHome = distanceTo(homeX, homeY);
        if (distanceFromHome > RETURN_HOME_DISTANCE) {
            state = BrainState.PATROL;
            aiState = EnemyAiState.RETURN_HOME;
            animationState = AnimationState.WALK;
            patrolTargetX = homeX;
            patrolTargetY = homeY;
            followPathToPoint(homeX, homeY, nowNs, worldWidth, worldHeight, false);
            return;
        }
        state = BrainState.PATROL;
        aiState = EnemyAiState.PATROL;
        animationState = distanceTo(patrolTargetX, patrolTargetY) <= RETURN_TOLERANCE ? AnimationState.IDLE : AnimationState.WALK;
        if (distanceTo(patrolTargetX, patrolTargetY) <= RETURN_TOLERANCE) {
            if (random.nextDouble() < 0.08) {
                pickNextPatrolTarget();
            } else {
                updateIdleAnimation(nowNs);
                return;
            }
        }
        followPathToPoint(patrolTargetX, patrolTargetY, nowNs, worldWidth, worldHeight, false);
    }

    @Override
    public void update(long now, Player player, double worldWidth, double worldHeight) {
        // Wolf AI duoc dieu khien boi WolfSpawnManager.
    }

    @Override
    public void takeDamage(int amount) {
        if (amount <= 0 || removeFromWorld || state == BrainState.DEATH) {
            return;
        }
        super.takeDamage(amount);
        if (hp <= 0) {
            hp = 0;
            transitionToDeath();
        }
    }

    @Override
    public void draw(GraphicsContext graphicsContext, double cameraX, double cameraY, long nowNs) {
        if (removeFromWorld) {
            return;
        }
        Image frame = currentAnimation().getCurrentFrame();
        double screenX = Math.round(x - cameraX);
        double screenY = Math.round(y - cameraY);
        if (frame == null || frame.isError()) {
            graphicsContext.setFill(Color.DARKOLIVEGREEN);
            graphicsContext.fillOval(screenX, screenY, width, height);
        } else if (facingDirection == Direction.LEFT) {
            graphicsContext.save();
            graphicsContext.translate(screenX + width, screenY);
            graphicsContext.scale(-1, 1);
            graphicsContext.drawImage(frame, 0, 0, width, height);
            graphicsContext.restore();
        } else {
            graphicsContext.drawImage(frame, screenX, screenY, width, height);
        }
        if (isHitFlashActive(nowNs)) {
            graphicsContext.save();
            graphicsContext.setGlobalAlpha(0.25);
            graphicsContext.setFill(getHitFlashColor());
            graphicsContext.fillRect(screenX, screenY, width, height);
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
        return "WOLF";
    }

    public void setHome(double homeX, double homeY) {
        this.homeX = homeX;
        this.homeY = homeY;
        this.patrolTargetX = homeX;
        this.patrolTargetY = homeY;
    }

    public void setInitialPathDelayNs(long delayNs) {
        long allowedAtNs = System.nanoTime() + Math.max(0L, delayNs);
        nextPathAllowedAtNs = allowedAtNs;
        nextObstacleSearchAtNs = allowedAtNs;
    }

    private boolean canDetectPlayer(Player player) {
        return intersectsDetectionRange(player);
    }

    private Player resolvePlayerTarget(Player player, long nowNs) {
        if (player == null || !player.isAlive()) {
            aggroPlayer = null;
            return null;
        }
        if (isPlayerInsideAggroZone(player)) {
            aggroPlayer = player;
            return player;
        }
        if (aggroPlayer == player && isPlayerStillInsideChaseZone(player)) {
            return player;
        }
        if (aggroPlayer == player) {
            aggroPlayer = null;
        }
        return null;
    }

    public void aggroOn(Player player, long nowNs) {
        if (player == null || !player.isAlive() || removeFromWorld || state == BrainState.DEATH) {
            return;
        }
        if (!isPlayerInsideAggroZone(player)) {
            return;
        }
        aggroPlayer = player;
        activeEntityTarget = player;
        if (state != BrainState.ATTACK && intersectsDetectionRange(player)) {
            state = BrainState.CHASE;
            aiState = EnemyAiState.CHASE_PLAYER;
        }
    }

    private void runTowardEntity(Entity target,
                                 long nowNs,
                                 double worldWidth,
                                 double worldHeight,
                                 boolean aggressive) {
        if (target == null) {
            return;
        }
        activeEntityTarget = target;
        activeBuildTarget = null;
        activeResourceTarget = null;
        faceTarget(target);
        state = BrainState.CHASE;
        aiState = target instanceof Player ? EnemyAiState.CHASE_PLAYER : EnemyAiState.MOVE_TO_BASE;
        animationState = AnimationState.RUN;
        currentMoveSpeed = RUN_SPEED;
        Point2D chasePoint = aggressive && target instanceof Player
                ? new Point2D(target.getCenterX(), target.getCenterY())
                : selectEntityChasePoint(target, ATTACK_TWO);
        boolean moved;
        if (aggressive && target instanceof Player) {
            siegeMode = true;
            moved = followDirectSiegeToPoint(chasePoint.getX(), chasePoint.getY(), nowNs, worldWidth, worldHeight);
        } else {
            moved = followPathToPoint(chasePoint.getX(), chasePoint.getY(), nowNs, worldWidth, worldHeight, aggressive);
        }
        faceTarget(target);
    }

    private void runTowardBuildTarget(BuildObject target,
                                      long nowNs,
                                      double worldWidth,
                                      double worldHeight) {
        if (target == null || !target.isAlive()) {
            activeBuildTarget = null;
            return;
        }
        siegeMode = true;
        activeBuildTarget = target;
        activeResourceTarget = null;
        faceTarget(target.getCenterX(), target.getCenterY());
        state = BrainState.CHASE;
        aiState = EnemyAiState.BREAK_FENCE;
        animationState = AnimationState.RUN;
        currentMoveSpeed = RUN_SPEED;
        if (tryStartAttack(null, null, nowNs, true)) {
            return;
        }
        Point2D approachPoint = selectAttackApproachPoint(target, ATTACK_TWO);
        if (siegeMode) {
            setDesiredMove(approachPoint.getX() - getCenterX(), approachPoint.getY() - getCenterY());
            if (expensiveNavigationAllowed) {
                attemptDirectSiegeMovement(approachPoint.getX(), approachPoint.getY(), worldWidth, worldHeight);
            } else if (canUseCachedDirectStep(nowNs, approachPoint.getX(), approachPoint.getY())) {
                moveToward(approachPoint.getX(), approachPoint.getY(), worldWidth, worldHeight);
            }
        } else {
            followPathToPoint(approachPoint.getX(), approachPoint.getY(), nowNs, worldWidth, worldHeight, false);
        }
        faceTarget(target.getCenterX(), target.getCenterY());
    }

    private void runTowardResourceTarget(ResourceNode target,
                                         long nowNs,
                                         double worldWidth,
                                         double worldHeight) {
        if (target == null || !target.isAlive()) {
            activeResourceTarget = null;
            escapeObstacleTarget = null;
            return;
        }
        activeBuildTarget = null;
        activeResourceTarget = target;
        faceTarget(target.getCenterX(), target.getCenterY());
        state = BrainState.CHASE;
        aiState = siegeMode ? EnemyAiState.BREAK_FENCE : EnemyAiState.MOVE_TO_OBSTACLE;
        animationState = AnimationState.RUN;
        currentMoveSpeed = RUN_SPEED;
        if (tryStartAttack(null, null, nowNs, true)) {
            return;
        }
        Point2D approachPoint = selectAttackApproachPoint(target, ATTACK_TWO);
        followPathToPoint(approachPoint.getX(), approachPoint.getY(), nowNs, worldWidth, worldHeight, false);
        faceTarget(target.getCenterX(), target.getCenterY());
    }

    private Player resolveNightVillagePlayerTarget(Player player, BaseCamp baseCamp) {
        if (player == null || !player.isAlive()) {
            return null;
        }
        if (canDetectPlayer(player)) {
            aggroPlayer = player;
            return player;
        }
        if (baseCamp == null || !baseCamp.isAlive()) {
            return null;
        }
        double playerToCamp = rectDistance(
                player.getCollisionX(),
                player.getCollisionY(),
                player.getCollisionWidth(),
                player.getCollisionHeight(),
                baseCamp.getCollisionX(),
                baseCamp.getCollisionY(),
                baseCamp.getCollisionWidth(),
                baseCamp.getCollisionHeight()
        );
        if (playerToCamp <= NIGHT_PLAYER_DEFEND_RADIUS) {
            aggroPlayer = player;
            return player;
        }
        return null;
    }

    private boolean runTowardNightEntityTarget(Entity target,
                                               long nowNs,
                                               double worldWidth,
                                               double worldHeight) {
        if (target == null) {
            return false;
        }
        activeEntityTarget = target;
        activeBuildTarget = null;
        activeResourceTarget = null;
        faceTarget(target);
        state = BrainState.CHASE;
        aiState = EnemyAiState.CHASE_PLAYER;
        animationState = AnimationState.RUN;
        currentMoveSpeed = RUN_SPEED;
        Point2D chasePoint = target instanceof Player
                ? new Point2D(target.getCenterX(), target.getCenterY())
                : selectEntityChasePoint(target, ATTACK_TWO);
        boolean moved;
        if (siegeMode || target instanceof Player) {
            siegeMode = true;
            moved = followDirectSiegeToPoint(chasePoint.getX(), chasePoint.getY(), nowNs, worldWidth, worldHeight);
        } else {
            moved = followPathToPoint(chasePoint.getX(), chasePoint.getY(), nowNs, worldWidth, worldHeight, false);
            if (!moved && lastPathFailed) {
                siegeMode = true;
                moved = followDirectSiegeToPoint(chasePoint.getX(), chasePoint.getY(), nowNs, worldWidth, worldHeight);
            }
        }
        faceTarget(target);
        return moved;
    }

    private boolean tryStartAttack(Entity entityTarget, BaseCamp baseTarget, long nowNs, boolean isNight) {
        BuildObject buildTarget = activeBuildTarget;
        ResourceNode resourceTarget = activeResourceTarget;
        if (entityTarget != null) {
            activeEntityTarget = entityTarget;
        }
        if (buildTarget != null && !buildTarget.isAlive()) {
            buildTarget = null;
        }
        if (resourceTarget != null && !resourceTarget.isAlive()) {
            resourceTarget = null;
        }
        AttackProfile profile = chooseAttackProfile(entityTarget, baseTarget, buildTarget, resourceTarget, isNight);
        if (profile == null || nowNs < attackCooldownUntilNs) {
            return false;
        }
        if (entityTarget != null) {
            faceTarget(entityTarget);
        } else if (baseTarget != null) {
            faceTarget(baseTarget.getCenterX(), baseTarget.getCenterY());
        } else if (buildTarget != null) {
            faceTarget(buildTarget.getCenterX(), buildTarget.getCenterY());
        } else if (resourceTarget != null) {
            faceTarget(resourceTarget.getCenterX(), resourceTarget.getCenterY());
        }
        if (entityTarget != null && !intersectsAttackHitbox(entityTarget, profile)) {
            return false;
        }
        if (baseTarget != null && !intersectsAttackHitbox(baseTarget, profile)) {
            return false;
        }
        if (buildTarget != null && !intersectsBuildAttackRange(buildTarget, profile)) {
            return false;
        }
        if (resourceTarget != null && !intersectsBuildAttackRange(resourceTarget, profile)) {
            return false;
        }
        return startAttack(profile, entityTarget != null ? entityTarget : baseTarget, buildTarget, resourceTarget, nowNs);
    }

    private AttackProfile chooseAttackProfile(Entity entityTarget, BaseCamp baseTarget, BuildObject buildTarget, ResourceNode resourceTarget, boolean isNight) {
        if (entityTarget instanceof Player) {
            return ATTACK_TWO;
        }
        if (buildTarget != null || resourceTarget != null) {
            return ATTACK_TWO;
        }
        return null;
    }

    private boolean startAttack(AttackProfile profile, Entity entityTarget, BuildObject buildTarget, ResourceNode resourceTarget, long nowNs) {
        if (profile == null) {
            return false;
        }
        state = BrainState.ATTACK;
        if (buildTarget != null) {
            aiState = EnemyAiState.ATTACK_OBSTACLE;
        } else if (entityTarget instanceof BaseCamp) {
            aiState = EnemyAiState.ATTACK_BASE;
        } else {
            aiState = EnemyAiState.ATTACK_PLAYER;
        }
        animationState = profile.animationState();
        activeAttackProfile = profile;
        activeEntityTarget = entityTarget;
        activeBuildTarget = buildTarget;
        activeResourceTarget = resourceTarget;
        attackStartedAtNs = nowNs;
        attackDamageAppliedThisCycle = false;
        resetAnimation(animationState);
        clearPath();
        blockedSinceNs = 0L;
        if (entityTarget != null) {
            faceTarget(entityTarget);
        } else if (buildTarget != null) {
            faceTarget(buildTarget.getCenterX(), buildTarget.getCenterY());
        } else if (resourceTarget != null) {
            faceTarget(resourceTarget.getCenterX(), resourceTarget.getCenterY());
        }
        return true;
    }

    private void updateAttackSequence(long nowNs) {
        if (activeAttackProfile == null) {
            state = BrainState.PATROL;
            aiState = EnemyAiState.PATROL;
            animationState = AnimationState.IDLE;
            return;
        }
        SpriteAnimation animation = currentAnimation();
        boolean attackFinished = animation.updateOnce(nowNs);
        Entity entityTarget = activeEntityTarget;
        BuildObject buildTarget = activeBuildTarget;
        ResourceNode resourceTarget = activeResourceTarget;
        if (entityTarget != null && entityTarget.isDead()) {
            entityTarget = null;
            activeEntityTarget = null;
        }
        if (buildTarget != null && !buildTarget.isAlive()) {
            buildTarget = null;
            activeBuildTarget = null;
        }
        if (resourceTarget != null && !resourceTarget.isAlive()) {
            resourceTarget = null;
            activeResourceTarget = null;
        }
        if (entityTarget != null) {
            faceTarget(entityTarget);
        } else if (buildTarget != null) {
            faceTarget(buildTarget.getCenterX(), buildTarget.getCenterY());
        } else if (resourceTarget != null) {
            faceTarget(resourceTarget.getCenterX(), resourceTarget.getCenterY());
        }
        if (canApplyDamageOnCurrentFrame(animation)) {
            if (entityTarget != null) {
                if (intersectsAttackHitbox(entityTarget, activeAttackProfile)) {
                    DamageSystem.applyDamage(this, entityTarget, activeAttackProfile.damage(), nowNs);
                    attackDamageAppliedThisCycle = true;
                }
            } else if (buildTarget != null) {
                if (intersectsBuildAttackRange(buildTarget, activeAttackProfile) && navigationContext != null && navigationContext.damageWall(this, buildTarget, nowNs)) {
                    if (navigationContext != null) {
                        navigationContext.recordFenceAttack(getEnemyType());
                    }
                    attackDamageAppliedThisCycle = true;
                }
            } else if (resourceTarget != null) {
                if (intersectsBuildAttackRange(resourceTarget, activeAttackProfile)
                        && navigationContext != null
                        && navigationContext.damageObstacle(this, EnemyObstacleTarget.forResource(resourceTarget), nowNs)) {
                    attackDamageAppliedThisCycle = true;
                }
            }
        }
        if (!attackFinished) {
            return;
        }
        attackCooldownUntilNs = nowNs + activeAttackProfile.cooldownNs();
        attackStartedAtNs = -1L;
        activeAttackProfile = null;
        attackDamageAppliedThisCycle = false;
        resetAnimation(AnimationState.ATTACK_2);
        if (buildTarget != null && buildTarget.isAlive()) {
            activeBuildTarget = buildTarget;
            activeResourceTarget = null;
            state = BrainState.CHASE;
            aiState = siegeMode ? EnemyAiState.BREAK_FENCE : EnemyAiState.MOVE_TO_OBSTACLE;
            animationState = AnimationState.RUN;
            clearPath();
            return;
        }
        if (resourceTarget != null && resourceTarget.isAlive()) {
            activeBuildTarget = null;
            activeResourceTarget = resourceTarget;
            state = BrainState.CHASE;
            aiState = siegeMode ? EnemyAiState.BREAK_FENCE : EnemyAiState.MOVE_TO_OBSTACLE;
            animationState = AnimationState.RUN;
            clearPath();
            return;
        }
        Player chaseTarget = null;
        if (entityTarget instanceof Player player && player.isAlive()) {
            chaseTarget = player;
        } else if (aggroPlayer != null && aggroPlayer.isAlive()) {
            chaseTarget = aggroPlayer;
        }
        if (chaseTarget != null) {
            faceTarget(chaseTarget);
        }
        if (chaseTarget != null && intersectsAttackHitbox(chaseTarget, ATTACK_TWO)) {
            activeEntityTarget = chaseTarget;
            startAttack(ATTACK_TWO, chaseTarget, null, null, nowNs);
            return;
        }
        if (chaseTarget != null && isInsideDisengageRange(chaseTarget)) {
            activeEntityTarget = chaseTarget;
            aggroPlayer = chaseTarget;
            activeBuildTarget = null;
            state = BrainState.CHASE;
            aiState = EnemyAiState.CHASE_PLAYER;
            animationState = AnimationState.RUN;
            clearPath();
            return;
        }
        clearAttackTarget();
        state = BrainState.PATROL;
        aiState = EnemyAiState.PATROL;
        animationState = AnimationState.IDLE;
    }

    private boolean canApplyDamageOnCurrentFrame(SpriteAnimation animation) {
        return activeAttackProfile != null
                && activeAttackProfile.damage() > 0
                && !attackDamageAppliedThisCycle
                && isDamageAnimation(activeAttackProfile.animationState())
                && animation.getCurrentFrameIndex() == activeAttackProfile.impactFrame();
    }

    private boolean isDamageAnimation(AnimationState state) {
        return state == AnimationState.ATTACK_2;
    }

    private boolean followPathToPoint(double targetX,
                                      double targetY,
                                      long nowNs,
                                      double worldWidth,
                                      double worldHeight,
                                      boolean allowBarrierFallback) {
        if (waitUntilNs > nowNs) {
            updateIdleAnimation(nowNs);
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
                currentAnimation().update(System.nanoTime(), true);
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
                if (!canStandCenteredAt(waypoint.getX(), waypoint.getY()) || !canMoveDirectlyTo(waypoint.getX(), waypoint.getY())) {
                    clearPath();
                    requestPath(targetX, targetY, nowNs);
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
            currentAnimation().update(System.nanoTime(), true);
            return true;
        }
        if (blockedSinceNs == 0L) {
            blockedSinceNs = nowNs;
        } else if (nowNs - blockedSinceNs >= STUCK_REPATH_NS) {
            recoverFromBlockedPath(targetX, targetY, nowNs, allowBarrierFallback);
            return false;
        }
        if (state == BrainState.CHASE) {
            animationState = AnimationState.RUN;
            currentAnimation().update(System.nanoTime(), true);
        } else {
            updateIdleAnimation(System.nanoTime());
        }
        return false;
    }

    private void updateStuckSample(long nowNs) {
        if (state != BrainState.CHASE) {
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
                recoverFromBlockedPath(lastPathTargetX, lastPathTargetY, nowNs, shouldAllowObstacleFallback());
            }
            return;
        }
        stuckTimerNs = 0L;
    }

    private void recoverFromBlockedPath(double targetX, double targetY, long nowNs, boolean allowBarrierFallback) {
        animationState = state == BrainState.CHASE ? AnimationState.RUN : AnimationState.IDLE;
        blockedSinceNs = 0L;
        blockedCount++;
        blockedDirections = countBlockedDirections();
        stuckTimerNs = 0L;
        clearPath();
        if (navigationContext != null) {
            navigationContext.recordStuck(getEnemyType());
        }
        aiState = EnemyAiState.STUCK_RECOVERY;

        if (isLightlyBlocked() && chooseSideStepWaypoint(targetX, targetY)) {
            waitUntilNs = 0L;
            return;
        }

        if (activeEntityTarget instanceof Player) {
            siegeMode = true;
        }

        if (shouldRepathBeforeBreaking(targetX, targetY)) {
            requestPath(targetX, targetY, nowNs);
            waitUntilNs = nowNs + STUCK_WAIT_NS;
            return;
        }

        if (chooseOrderedEscapeWaypoint(targetX, targetY)) {
            waitUntilNs = 0L;
            return;
        }

        boolean canBreakObstacle = shouldBreakObstacle(allowBarrierFallback || shouldAllowObstacleFallback());
        if (canBreakObstacle) {
            BuildObject obstacle = findBlockingObstacleThrottled(targetX, targetY, nowNs);
            if (obstacle != null) {
                siegeMode = true;
                escapeObstacleTarget = EnemyObstacleTarget.forBuild(obstacle);
                activeBuildTarget = obstacle;
                activeEntityTarget = null;
                activeResourceTarget = null;
                waitUntilNs = 0L;
                return;
            }
        }
        waitUntilNs = nowNs + STUCK_WAIT_NS;
    }

    private boolean isLightlyBlocked() {
        return blockedCount <= 1 && blockedDirections <= 1;
    }

    private boolean shouldRepathBeforeBreaking(double targetX, double targetY) {
        return navigationContext != null
                && !pendingPathRequest
                && !Double.isNaN(targetX)
                && !Double.isNaN(targetY)
                && blockedCount <= STUCK_REPATH_AFTER_BLOCKS
                && !shouldBreakObstacle(shouldAllowObstacleFallback());
    }

    private boolean shouldBreakObstacle(boolean allowBarrierFallback) {
        return allowBarrierFallback
                && (lastPathFailed
                || blockedCount >= STUCK_BREAK_AFTER_BLOCKS
                || blockedDirections >= STUCK_BREAK_BLOCKED_DIRECTIONS);
    }

    private boolean shouldAllowObstacleFallback() {
        return activeBuildTarget != null
                || activeResourceTarget != null
                || lastPathFailed
                || blockedCount >= STUCK_BREAK_AFTER_BLOCKS
                || blockedDirections >= STUCK_BREAK_BLOCKED_DIRECTIONS;
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
            desiredX = facingDirection == Direction.LEFT ? -1.0 : 1.0;
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

    private boolean chooseSideStepWaypoint(double targetX, double targetY) {
        if (Double.isNaN(targetX) || Double.isNaN(targetY)) {
            return false;
        }
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.001) {
            return false;
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
        double step = Math.max(18.0, Math.max(getCollisionWidth(), getCollisionHeight()) * 1.6);
        for (double[] candidate : candidates) {
            double centerX = getCenterX() + candidate[0] * step;
            double centerY = getCenterY() + candidate[1] * step;
            if (!canStandCenteredAt(centerX, centerY) || !canMoveDirectlyTo(centerX, centerY)) {
                continue;
            }
            currentPath.add(new Point2D(centerX, centerY));
            currentPathIndex = 0;
            return true;
        }
        return false;
    }

    private void updateIdleAnimation(long nowNs) {
        animationState = AnimationState.IDLE;
        currentAnimation().update(nowNs, true);
    }

    private boolean shouldRequestPath(double targetX, double targetY, long nowNs) {
        if (!expensiveNavigationAllowed) {
            return false;
        }
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

    private boolean canMoveDirectlyTo(double targetX, double targetY) {
        if (movementValidator == null && navigationContext == null) {
            return true;
        }
        long nowNs = currentUpdateNowNs > 0L ? currentUpdateNowNs : System.nanoTime();
        double originX = getCenterX();
        double originY = getCenterY();
        boolean sameProbe = !Double.isNaN(lastDirectPathProbeTargetX)
                && distance(lastDirectPathProbeTargetX, lastDirectPathProbeTargetY, targetX, targetY) <= DIRECT_TARGET_RECALC_DISTANCE * 0.5
                && distance(lastDirectPathOriginX, lastDirectPathOriginY, originX, originY) <= PATH_POINT_REACHED;
        long cacheWindowNs = lastDirectPathResult ? DIRECT_PATH_SUCCESS_CACHE_NS : DIRECT_PATH_FAIL_CACHE_NS;
        if (sameProbe && nowNs - lastDirectPathCheckAtNs <= cacheWindowNs) {
            return lastDirectPathResult;
        }
        if (!expensiveNavigationAllowed && lastDirectPathCheckAtNs != Long.MIN_VALUE) {
            return sameProbe && lastDirectPathResult;
        }
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= currentMoveSpeed + 0.001) {
            cacheDirectPathProbe(nowNs, originX, originY, targetX, targetY, true);
            return true;
        }
        double step = Math.max(8.0, Math.min(18.0, Math.max(getCollisionWidth(), getCollisionHeight()) * 0.65));
        int samples = Math.min(MAX_DIRECT_PATH_SAMPLES, Math.max(1, (int) Math.ceil(distance / step)));
        for (int i = 1; i <= samples; i++) {
            double t = i / (double) samples;
            double centerX = getCenterX() + dx * t;
            double centerY = getCenterY() + dy * t;
            double candidateX = centerX - width * 0.5;
            double candidateY = centerY - height * 0.5;
            boolean canOccupy = navigationContext != null
                    ? navigationContext.canPathOccupy(this, candidateX, candidateY, width, height)
                    : movementValidator.canOccupy(this, candidateX, candidateY, width, height);
            if (!canOccupy) {
                cacheDirectPathProbe(nowNs, originX, originY, targetX, targetY, false);
                return false;
            }
        }
        cacheDirectPathProbe(nowNs, originX, originY, targetX, targetY, true);
        return true;
    }

    private boolean followDirectSiegeToPoint(double targetX,
                                             double targetY,
                                             long nowNs,
                                             double worldWidth,
                                             double worldHeight) {
        activeBuildTarget = null;
        activeResourceTarget = null;
        state = BrainState.CHASE;
        aiState = EnemyAiState.DIRECT_SIEGE_TO_PLAYER;
        animationState = AnimationState.RUN;
        currentMoveSpeed = RUN_SPEED;
        clearPath();
        setDesiredMove(targetX - getCenterX(), targetY - getCenterY());
        if (!expensiveNavigationAllowed) {
            if (canUseCachedDirectStep(nowNs, targetX, targetY)) {
                return moveToward(targetX, targetY, worldWidth, worldHeight);
            }
            currentAnimation().update(nowNs, true);
            return false;
        }
        if (attemptDirectSiegeMovement(targetX, targetY, worldWidth, worldHeight)) {
            blockedSinceNs = 0L;
            blockedCount = 0;
            blockedDirections = 0;
            stuckTimerNs = 0L;
            waitUntilNs = 0L;
            updateStuckSample(nowNs);
            return true;
        }
        if (blockedSinceNs == 0L) {
            blockedSinceNs = nowNs;
        }
        BuildObject obstacle = findBlockingObstacleThrottled(targetX, targetY, nowNs);
        if (obstacle != null) {
            siegeMode = true;
            escapeObstacleTarget = EnemyObstacleTarget.forBuild(obstacle);
            activeBuildTarget = obstacle;
            activeEntityTarget = null;
            waitUntilNs = 0L;
            return false;
        }
        if (chooseOrderedEscapeWaypoint(targetX, targetY)) {
            waitUntilNs = 0L;
            return false;
        }
        if (moveBackwardStep(worldWidth, worldHeight)) {
            waitUntilNs = nowNs + 80_000_000L;
            return true;
        }
        if (nowNs - blockedSinceNs >= STUCK_TRIGGER_NS) {
            BuildObject forcedObstacle = findBlockingObstacleThrottled(targetX, targetY, nowNs);
            if (forcedObstacle != null) {
                siegeMode = true;
                escapeObstacleTarget = EnemyObstacleTarget.forBuild(forcedObstacle);
                activeBuildTarget = forcedObstacle;
                activeEntityTarget = null;
                waitUntilNs = 0L;
            }
        }
        waitUntilNs = nowNs + STUCK_WAIT_NS;
        return false;
    }

    private boolean canUseCachedDirectStep(long nowNs, double targetX, double targetY) {
        return lastDirectPathResult
                && !Double.isNaN(lastDirectPathProbeTargetX)
                && nowNs - lastDirectPathCheckAtNs <= DIRECT_PATH_SUCCESS_CACHE_NS
                && distance(lastDirectPathProbeTargetX, lastDirectPathProbeTargetY, targetX, targetY) <= DIRECT_TARGET_RECALC_DISTANCE;
    }

    private boolean attemptDirectSiegeMovement(double targetX, double targetY, double worldWidth, double worldHeight) {
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.001) {
            return false;
        }
        double dirX = dx / length;
        double dirY = dy / length;
        double step = Math.min(currentMoveSpeed, length);
        if (tryStepInDirection(dirX, dirY, step, worldWidth, worldHeight)) {
            return true;
        }
        double[] angles = {
                Math.toRadians(30.0),
                Math.toRadians(-30.0),
                Math.toRadians(50.0),
                Math.toRadians(-50.0),
                Math.toRadians(75.0),
                Math.toRadians(-75.0)
        };
        for (double angle : angles) {
            double rotatedX = dirX * Math.cos(angle) - dirY * Math.sin(angle);
            double rotatedY = dirX * Math.sin(angle) + dirY * Math.cos(angle);
            if (tryStepInDirection(rotatedX, rotatedY, step * 0.92, worldWidth, worldHeight)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryStepInDirection(double dirX,
                                       double dirY,
                                       double distance,
                                       double worldWidth,
                                       double worldHeight) {
        double length = Math.sqrt(dirX * dirX + dirY * dirY);
        if (length <= 0.001 || distance <= 0.001) {
            return false;
        }
        double targetX = getCenterX() + (dirX / length) * distance;
        double targetY = getCenterY() + (dirY / length) * distance;
        double candidateX = targetX - width * 0.5;
        double candidateY = targetY - height * 0.5;
        if (navigationContext != null && !navigationContext.canPathOccupy(this, candidateX, candidateY, width, height)) {
            return false;
        }
        return moveToward(targetX, targetY, worldWidth, worldHeight);
    }

    private boolean moveBackwardStep(double worldWidth, double worldHeight) {
        double backX = -desiredMoveDirX;
        double backY = -desiredMoveDirY;
        double length = Math.sqrt(backX * backX + backY * backY);
        if (length <= 0.001) {
            return false;
        }
        return tryStepInDirection(backX / length, backY / length, Math.max(10.0, currentMoveSpeed * 0.85), worldWidth, worldHeight);
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
            pathFailCount++;
            nextPathAllowedAtNs = nowNs + PATH_FAIL_RETRY_NS;
            return;
        }
        currentPath.addAll(result.waypoints());
        currentPathIndex = 0;
        lastPathTargetX = result.targetX();
        lastPathTargetY = result.targetY();
        pathFailCount = 0;
        lastPathFailed = false;
    }

    private long randomizedPathCooldownNs(boolean failed) {
        if (failed) {
            return PATH_FAIL_RETRY_NS;
        }
        return 500_000_000L + (long) (random.nextDouble() * 700_000_000L);
    }

    private BuildObject findBlockingObstacleThrottled(double targetX, double targetY, long nowNs) {
        if (cachedBlockingObstacle != null && (!cachedBlockingObstacle.isAlive() || !isAttackableObstacle(cachedBlockingObstacle))) {
            cachedBlockingObstacle = null;
        }
        if (cachedBlockingObstacle != null
                && distance(cachedBlockingObstacle.getCenterX(), cachedBlockingObstacle.getCenterY(), getCenterX(), getCenterY())
                <= Math.max(1, OBSTACLE_NEARBY_RADIUS_TILES) * navigationContext.getTileWidth() * 2.5) {
            return cachedBlockingObstacle;
        }
        if (navigationContext == null || nowNs < nextObstacleSearchAtNs || !expensiveNavigationAllowed) {
            return null;
        }
        nextObstacleSearchAtNs = nowNs + OBSTACLE_SEARCH_RETRY_NS + (long) (random.nextDouble() * 300_000_000L);
        cachedBlockingObstacle = navigationContext.findBlockingObstacle(this, targetX, targetY, OBSTACLE_RAY_TILES, OBSTACLE_NEARBY_RADIUS_TILES);
        return cachedBlockingObstacle;
    }

    private boolean moveToward(double targetX, double targetY, double worldWidth, double worldHeight) {
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 0.001) {
            return false;
        }
        double speedForStep = Math.min(currentMoveSpeed, distance);
        double moveX = dx / distance * speedForStep;
        double moveY = dy / distance * speedForStep;
        if (movementValidator == null) {
            if (!moveWithCollision(moveX, moveY)) {
                return false;
            }
            clampPosition(0, 0, worldWidth, worldHeight);
            currentAnimation().update(System.nanoTime(), true);
            return true;
        }

        MovementSlideSystem.MoveResult result = MovementSlideSystem.steerToward(
                x,
                y,
                width,
                height,
                moveX,
                moveY,
                (nextX, nextY, nextWidth, nextHeight) -> movementValidator.canOccupy(this, nextX, nextY, nextWidth, nextHeight)
        );
        double movedX = result.x() - x;
        double movedY = result.y() - y;
        if (Math.abs(movedX) < 0.0001 && Math.abs(movedY) < 0.0001) {
            return false;
        }
        x = result.x();
        y = result.y();
        updateFacingFromMovement(movedX, movedY);
        clampPosition(0, 0, worldWidth, worldHeight);
        currentAnimation().update(System.nanoTime(), true);
        return true;
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

    private boolean intersectsDetectionRange(Entity target) {
        if (target == null || target.isDead()) {
            return false;
        }
        return intersectsCircleAndRect(
                getCollisionX() + getCollisionWidth() * 0.5,
                getCollisionY() + getCollisionHeight() * 0.5,
                DETECTION_RANGE,
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
    }

    private boolean isInsideDisengageRange(Entity target) {
        if (target == null || target.isDead()) {
            return false;
        }
        return intersectsCircleAndRect(
                getCollisionX() + getCollisionWidth() * 0.5,
                getCollisionY() + getCollisionHeight() * 0.5,
                DISENGAGE_RANGE,
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
    }

    private boolean isPlayerInsideAggroZone(Player player) {
        return player != null && intersectsRange(player, DAY_DETECTION_RANGE);
    }

    private boolean isPlayerStillInsideChaseZone(Player player) {
        return player != null
                && intersectsRange(player, DAY_DISENGAGE_RANGE)
                && distance(homeX, homeY, player.getCenterX(), player.getCenterY()) <= DAY_CHASE_HOME_RADIUS + 20.0;
    }

    private boolean intersectsAttackHitbox(Entity target, AttackProfile profile) {
        AttackArea attackArea = buildAttackArea(profile);
        return attackArea.radiusX() > 0.0
                && intersectsEllipseAndRect(
                attackArea.centerX(),
                attackArea.centerY(),
                attackArea.radiusX(),
                attackArea.radiusY(),
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
    }

    private boolean intersectsAttackHitbox(BuildObject target, AttackProfile profile) {
        AttackArea attackArea = buildAttackArea(profile);
        return attackArea.radiusX() > 0.0
                && intersectsEllipseAndRect(
                attackArea.centerX(),
                attackArea.centerY(),
                attackArea.radiusX(),
                attackArea.radiusY(),
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
    }

    private boolean intersectsBuildAttackRange(BuildObject target, AttackProfile profile) {
        if (target == null || profile == null) {
            return false;
        }
        double paddingX = Math.max(10.0, profile.hitboxWidth() * 0.65);
        double paddingY = Math.max(10.0, profile.hitboxHeight() * 0.65);
        return intersectsRect(
                getCollisionX() - paddingX,
                getCollisionY() - paddingY,
                getCollisionWidth() + paddingX * 2.0,
                getCollisionHeight() + paddingY * 2.0,
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
    }

    private boolean intersectsBuildAttackRange(ResourceNode target, AttackProfile profile) {
        if (target == null || profile == null) {
            return false;
        }
        double paddingX = Math.max(42.0, profile.hitboxWidth() * 1.4);
        double paddingY = Math.max(42.0, profile.hitboxHeight() * 1.4);
        return intersectsRect(
                getCollisionX() - paddingX,
                getCollisionY() - paddingY,
                getCollisionWidth() + paddingX * 2.0,
                getCollisionHeight() + paddingY * 2.0,
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
    }

    private Point2D selectAttackApproachPoint(Entity target, AttackProfile profile) {
        if (target == null || profile == null) {
            return new Point2D(getCenterX(), getCenterY());
        }
        Point2D best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        Direction preferredDirection = horizontalDirectionToward(target.getCenterX());
        Direction[] directions = preferredDirection == Direction.RIGHT
                ? new Direction[]{Direction.RIGHT, Direction.LEFT}
                : new Direction[]{Direction.LEFT, Direction.RIGHT};
        double[] yOffsets = {0.0, -6.0, 6.0, -12.0, 12.0, -18.0, 18.0};
        for (Direction direction : directions) {
            for (double yOffset : yOffsets) {
                Point2D candidate = attackApproachPointForEntity(target, profile, direction, yOffset);
                if (!canStandCenteredAt(candidate.getX(), candidate.getY())) {
                    continue;
                }
                double sidePenalty = direction == preferredDirection ? 0.0 : 18.0;
                double offsetPenalty = Math.abs(yOffset) * 0.75;
                double score = distanceTo(candidate.getX(), candidate.getY()) + sidePenalty + offsetPenalty;
                if (score >= bestScore) {
                    continue;
                }
                best = candidate;
                bestScore = score;
            }
        }
        if (best != null) {
            return best;
        }
        Point2D fallback = findWalkablePointNearTarget(target);
        if (fallback != null) {
            return fallback;
        }
        return attackApproachPointForEntity(target, profile, preferredDirection, 0.0);
    }

    private Point2D selectEntityChasePoint(Entity target, AttackProfile profile) {
        if (!(target instanceof Player) || profile == null) {
            return selectAttackApproachPoint(target, profile);
        }
        double targetDistance = rectDistance(
                getCollisionX(),
                getCollisionY(),
                getCollisionWidth(),
                getCollisionHeight(),
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
        if (targetDistance > PLAYER_ATTACK_APPROACH_DISTANCE) {
            Point2D chasePoint = findWalkablePointNearTarget(target, PLAYER_CHASE_APPROACH_DISTANCE, true);
            if (chasePoint != null) {
                return chasePoint;
            }
            Point2D fallback = findWalkablePointNearTarget(target);
            if (fallback != null) {
                return fallback;
            }
        }
        return selectAttackApproachPoint(target, profile);
    }

    private Point2D attackApproachPointForEntity(Entity target, AttackProfile profile, Direction approachDirection, double yOffset) {
        double bodyWidth = getCollisionWidth();
        double bodyHeight = getCollisionHeight();
        double hitboxWidth = profile.hitboxWidth();
        double overlap = 2.0;
        double targetLeft = target.getCollisionX();
        double targetRight = target.getCollisionX() + target.getCollisionWidth();
        double targetCenterY = target.getCollisionY() + target.getCollisionHeight() * 0.5 + yOffset;

        double bodyX;
        double bodyY;
        switch (approachDirection) {
            case RIGHT -> {
                bodyX = targetLeft - hitboxWidth + overlap - bodyWidth;
                bodyY = targetCenterY - bodyHeight * 0.5;
            }
            case LEFT -> {
                bodyX = targetRight + hitboxWidth - overlap;
                bodyY = targetCenterY - bodyHeight * 0.5;
            }
            default -> {
                bodyX = targetLeft - hitboxWidth + overlap - bodyWidth;
                bodyY = targetCenterY - bodyHeight * 0.5;
            }
        }
        return new Point2D(
                bodyX - collisionInsetLeft(width, height) + width * 0.5,
                bodyY - collisionInsetTop(width, height) + height * 0.5
        );
    }

    private Point2D findWalkablePointNearTarget(Entity target) {
        return findWalkablePointNearTarget(target, Math.max(18.0, Math.max(getCollisionWidth(), getCollisionHeight()) * 1.15), false);
    }

    private Point2D findWalkablePointNearTarget(Entity target, double startRadius, boolean preferDirectPath) {
        if (target == null) {
            return null;
        }
        double centerX = target.getCollisionX() + target.getCollisionWidth() * 0.5;
        double centerY = target.getCollisionY() + target.getCollisionHeight() * 0.5;
        double baseRadius = Math.max(18.0, startRadius);
        double bestScore = Double.POSITIVE_INFINITY;
        Point2D best = null;
        for (int ring = 0; ring < 4; ring++) {
            double radius = baseRadius + ring * 10.0;
            int samples = 16 + ring * 8;
            for (int i = 0; i < samples; i++) {
                double angle = (Math.PI * 2.0 * i) / samples;
                double candidateX = centerX + Math.cos(angle) * radius;
                double candidateY = centerY + Math.sin(angle) * radius;
                if (!canStandCenteredAt(candidateX, candidateY)) {
                    continue;
                }
                double directPenalty = preferDirectPath && !canMoveDirectlyTo(candidateX, candidateY) ? 140.0 : 0.0;
                double targetSideX = candidateX - centerX;
                double targetSideY = candidateY - centerY;
                double wolfSideX = getCenterX() - centerX;
                double wolfSideY = getCenterY() - centerY;
                double sideDot = targetSideX * wolfSideX + targetSideY * wolfSideY;
                double sidePenalty = sideDot < 0.0 ? 28.0 : 0.0;
                double score = distanceTo(candidateX, candidateY) + ring * 8.0 + directPenalty + sidePenalty;
                if (score >= bestScore) {
                    continue;
                }
                best = new Point2D(candidateX, candidateY);
                bestScore = score;
            }
            if (best != null && !preferDirectPath) {
                return best;
            }
        }
        return null;
    }

    private Point2D selectAttackApproachPoint(BuildObject target, AttackProfile profile) {
        if (target == null || profile == null) {
            return new Point2D(getCenterX(), getCenterY());
        }
        return selectAttackApproachPoint(
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight(),
                profile
        );
    }

    private Point2D selectAttackApproachPoint(ResourceNode target, AttackProfile profile) {
        if (target == null || profile == null) {
            return new Point2D(getCenterX(), getCenterY());
        }
        return selectAttackApproachPoint(
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight(),
                profile
        );
    }

    private Point2D selectAttackApproachPoint(double collisionX, double collisionY, double collisionWidth, double collisionHeight, AttackProfile profile) {
        double bodyWidth = getCollisionWidth();
        double bodyHeight = getCollisionHeight();
        double hitboxWidth = profile.hitboxWidth();
        double hitboxHeight = profile.hitboxHeight();
        double overlap = 2.0;
        double targetLeft = collisionX;
        double targetRight = collisionX + collisionWidth;
        double targetTop = collisionY;
        double targetBottom = collisionY + collisionHeight;
        double targetCenterX = collisionX + collisionWidth * 0.5;
        double targetCenterY = collisionY + collisionHeight * 0.5;
        double dx = targetCenterX - getCollisionX() - bodyWidth * 0.5;
        double dy = targetCenterY - getCollisionY() - bodyHeight * 0.5;

        double bodyX;
        double bodyY;
        if (Math.abs(dy) > Math.abs(dx)) {
            bodyX = targetCenterX - bodyWidth * 0.5;
            bodyY = dy >= 0.0
                    ? targetTop - hitboxHeight + overlap - bodyHeight
                    : targetBottom + hitboxHeight - overlap;
        } else if (dx >= 0.0) {
            bodyX = targetLeft - hitboxWidth + overlap - bodyWidth;
            bodyY = targetCenterY - bodyHeight * 0.5;
        } else {
            bodyX = targetRight + hitboxWidth - overlap;
            bodyY = targetCenterY - bodyHeight * 0.5;
        }
        return new Point2D(
                bodyX - collisionInsetLeft(width, height) + width * 0.5,
                bodyY - collisionInsetTop(width, height) + height * 0.5
        );
    }

    private AttackArea buildAttackArea(AttackProfile profile) {
        if (profile == null || profile.hitboxWidth() <= 0.0 || profile.hitboxHeight() <= 0.0) {
            return new AttackArea(0.0, 0.0, 0.0, 0.0);
        }
        double radiusX = profile.hitboxWidth() * 0.5;
        double radiusY = profile.hitboxHeight() * 0.5;
        double bodyX = getCollisionX();
        double bodyY = getCollisionY();
        double bodyWidth = getCollisionWidth();
        double bodyHeight = getCollisionHeight();
        double centerX = bodyX + bodyWidth * 0.5;
        double centerY = bodyY + bodyHeight * 0.5;
        return switch (facingDirection) {
            case LEFT -> new AttackArea(bodyX - radiusX, centerY, radiusX, radiusY);
            case RIGHT -> new AttackArea(bodyX + bodyWidth + radiusX, centerY, radiusX, radiusY);
            default -> new AttackArea(bodyX + bodyWidth + radiusX, centerY, radiusX, radiusY);
        };
    }

    private void drawDebug(GraphicsContext graphicsContext, double cameraX, double cameraY) {
        graphicsContext.save();
        graphicsContext.setLineWidth(1.2);
        graphicsContext.setStroke(Color.color(0.15, 0.95, 0.25, 0.95));
        graphicsContext.strokeRect(getCollisionX() - cameraX, getCollisionY() - cameraY, getCollisionWidth(), getCollisionHeight());

        graphicsContext.setStroke(Color.color(1.0, 0.8, 0.15, 0.80));
        graphicsContext.strokeOval(getCenterX() - DETECTION_RANGE - cameraX, getCenterY() - DETECTION_RANGE - cameraY, DETECTION_RANGE * 2.0, DETECTION_RANGE * 2.0);

        graphicsContext.setStroke(Color.color(1.0, 0.15, 0.15, 0.70));
        double chaseLeash = DAY_CHASE_HOME_RADIUS + 20.0;
        graphicsContext.strokeOval(homeX - chaseLeash - cameraX, homeY - chaseLeash - cameraY, chaseLeash * 2.0, chaseLeash * 2.0);

        if (!currentPath.isEmpty()) {
            graphicsContext.setStroke(Color.color(0.15, 0.95, 1.0, 0.90));
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

        AttackProfile debugAttackProfile = activeAttackProfile != null ? activeAttackProfile : ATTACK_TWO;
        AttackArea attackHitbox = buildAttackArea(debugAttackProfile);
        graphicsContext.setStroke(Color.color(1.0, 0.05, 0.05, 0.98));
        graphicsContext.strokeOval(
                attackHitbox.centerX() - attackHitbox.radiusX() - cameraX,
                attackHitbox.centerY() - attackHitbox.radiusY() - cameraY,
                attackHitbox.radiusX() * 2.0,
                attackHitbox.radiusY() * 2.0
        );
        Entity debugTarget = activeEntityTarget != null ? activeEntityTarget : aggroPlayer;
        if (debugTarget != null && !debugTarget.isDead()) {
            graphicsContext.setStroke(Color.color(1.0, 1.0, 1.0, 0.98));
            graphicsContext.strokeRect(
                    debugTarget.getCollisionX() - cameraX,
                    debugTarget.getCollisionY() - cameraY,
                    debugTarget.getCollisionWidth(),
                    debugTarget.getCollisionHeight()
            );
        }
        graphicsContext.setFill(Color.color(1.0, 1.0, 1.0, 0.98));
        String obstacleLabel = escapeObstacleTarget == null ? "-" : escapeObstacleTarget.getDebugLabel();
        graphicsContext.fillText(debugStateName() + " s=" + (stuckTimerNs / 1_000_000L) + "ms b=" + blockedDirections + " o=" + obstacleLabel,
                x - cameraX, y - cameraY - 5.0);
        graphicsContext.restore();
    }

    private String debugStateName() {
        return aiState.name();
    }

    private void transitionToDeath() {
        state = BrainState.DEATH;
        aiState = EnemyAiState.DEATH;
        animationState = AnimationState.DEAD;
        clearAttackTarget();
        clearPath();
        resetAnimation(AnimationState.DEAD);
    }

    public EnemyAiState getAiState() {
        return aiState;
    }

    private void clearAttackTarget() {
        activeEntityTarget = null;
        activeBuildTarget = null;
        activeResourceTarget = null;
        escapeObstacleTarget = null;
        cachedEscapeObstacle = null;
        cachedBlockingObstacle = null;
        siegeMode = false;
    }

    private void clearObstacleFocus() {
        activeBuildTarget = null;
        activeResourceTarget = null;
        escapeObstacleTarget = null;
        cachedEscapeObstacle = null;
        clearPath();
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
            activeBuildTarget = null;
            activeResourceTarget = null;
            cachedEscapeObstacle = null;
            return;
        }
        if (escapeObstacleTarget.isBuildObject()) {
            runTowardBuildTarget(escapeObstacleTarget.buildObject(), nowNs, worldWidth, worldHeight);
            return;
        }
        runTowardResourceTarget(escapeObstacleTarget.resourceNode(), nowNs, worldWidth, worldHeight);
    }

    private void cacheDirectPathProbe(long nowNs,
                                      double originX,
                                      double originY,
                                      double targetX,
                                      double targetY,
                                      boolean result) {
        lastDirectPathCheckAtNs = nowNs;
        lastDirectPathOriginX = originX;
        lastDirectPathOriginY = originY;
        lastDirectPathProbeTargetX = targetX;
        lastDirectPathProbeTargetY = targetY;
        lastDirectPathResult = result;
    }

    private boolean isAttackableObstacle(BuildObject object) {
        return object != null && object.isAlive();
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

    private void pickNextPatrolTarget() {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 8.0 + random.nextDouble() * Math.max(8.0, DAY_HOME_RADIUS - 8.0);
            double candidateX = homeX + Math.cos(angle) * radius;
            double candidateY = homeY + Math.sin(angle) * radius;
            if (canStandCenteredAt(candidateX, candidateY)) {
                patrolTargetX = candidateX;
                patrolTargetY = candidateY;
                clearPath();
                return;
            }
        }
        patrolTargetX = homeX;
        patrolTargetY = homeY;
        clearPath();
    }

    private boolean canStandCenteredAt(double centerX, double centerY) {
        return movementValidator == null || movementValidator.canOccupy(this, centerX - width * 0.5, centerY - height * 0.5, width, height);
    }

    private boolean intersectsRange(Entity target, double radius) {
        if (target == null || target.isDead() || radius <= 0.0) {
            return false;
        }
        return intersectsCircleAndRect(
                getCollisionX() + getCollisionWidth() * 0.5,
                getCollisionY() + getCollisionHeight() * 0.5,
                radius,
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
    }

    private void updateFacingFromMovement(double dx, double dy) {
        if (Math.abs(dx) < FACE_MOVE_THRESHOLD && Math.abs(dy) < FACE_MOVE_THRESHOLD) {
            return;
        }
        updateFacing(dx, dy);
    }

    private void faceTarget(Entity target) {
        if (target != null) {
            faceTarget(target.getCenterX(), target.getCenterY());
        }
    }

    private void faceTarget(double targetX, double targetY) {
        updateFacing(targetX - getCenterX(), 0.0);
    }

    private Direction horizontalDirectionToward(double targetX) {
        return targetX >= getCenterX() ? Direction.RIGHT : Direction.LEFT;
    }

    private void updateFacing(double dx, double dy) {
        if (Math.abs(dx) >= FACE_MOVE_THRESHOLD) {
            facingDirection = dx >= 0 ? Direction.RIGHT : Direction.LEFT;
        }
    }

    private double distanceTo(double targetX, double targetY) {
        return distance(getCenterX(), getCenterY(), targetX, targetY);
    }

    private double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double rectDistance(double ax, double ay, double aw, double ah,
                                double bx, double by, double bw, double bh) {
        double dx = edgeDistance(ax, ax + aw, bx, bx + bw);
        double dy = edgeDistance(ay, ay + ah, by, by + bh);
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

    private boolean intersectsRect(double ax,
                                   double ay,
                                   double aw,
                                   double ah,
                                   double bx,
                                   double by,
                                   double bw,
                                   double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private boolean intersectsCircleAndRect(double circleX,
                                            double circleY,
                                            double radius,
                                            double rectX,
                                            double rectY,
                                            double rectWidth,
                                            double rectHeight) {
        double nearestX = Math.max(rectX, Math.min(circleX, rectX + rectWidth));
        double nearestY = Math.max(rectY, Math.min(circleY, rectY + rectHeight));
        double dx = circleX - nearestX;
        double dy = circleY - nearestY;
        return dx * dx + dy * dy <= radius * radius;
    }

    private boolean intersectsEllipseAndRect(double ellipseX,
                                             double ellipseY,
                                             double radiusX,
                                             double radiusY,
                                             double rectX,
                                             double rectY,
                                             double rectWidth,
                                             double rectHeight) {
        if (radiusX <= 0.0 || radiusY <= 0.0) {
            return false;
        }
        double nearestX = Math.max(rectX, Math.min(ellipseX, rectX + rectWidth));
        double nearestY = Math.max(rectY, Math.min(ellipseY, rectY + rectHeight));
        double normalizedX = (nearestX - ellipseX) / radiusX;
        double normalizedY = (nearestY - ellipseY) / radiusY;
        return normalizedX * normalizedX + normalizedY * normalizedY <= 1.0;
    }

    private SpriteAnimation currentAnimation() {
        return animations.getOrDefault(animationState, animations.get(AnimationState.IDLE));
    }

    private void resetAnimation(AnimationState state) {
        SpriteAnimation animation = animations.get(state);
        if (animation != null) {
            animation.reset();
        }
    }

    private static String resolveAsset(String baseName) {
        Path folder = Paths.get("assets", "wolf");
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

    private record AttackArea(double centerX, double centerY, double radiusX, double radiusY) {
    }
}
