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
        IDLE,
        PATROL,
        CHASE_PLAYER,
        RETURN_HOME,
        NIGHT_ASSAULT,
        ATTACK,
        HURT,
        STUCK_RECOVER,
        DYING
    }

    private enum AnimationState {
        IDLE,
        WALK,
        RUN,
        ATTACK_1,
        ATTACK_2,
        ATTACK_3,
        RUN_ATTACK,
        JUMP,
        HURT,
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

    public interface WorldQuery {
        int getTileWidth();

        int getTileHeight();

        BuildObject findNearestWallToAttack(WolfEnemy enemy, double towardX, double towardY, double maxDistance);

        boolean damageWall(WolfEnemy enemy, BuildObject wall, long nowNs);

        boolean damageBase(WolfEnemy enemy, BaseCamp baseCamp, long nowNs);
    }

    private static final double WALK_SPEED = 1.20;
    private static final double RUN_SPEED = 2.45;
    private static final double JUMP_SPEED = 3.35;
    private static final int MAX_HP = 30;
    private static final int DAMAGE = 5;
    private static final double DETECTION_RANGE = 220.0;
    private static final double CHASE_RANGE_DAY = 340.0;
    private static final double NIGHT_PLAYER_PRIORITY_BONUS = 40.0;
    private static final double HOME_RADIUS = 92.0;
    private static final double RETURN_TOLERANCE = 10.0;
    private static final double JUMP_TRIGGER_MIN = 72.0;
    private static final double JUMP_TRIGGER_MAX = 150.0;
    private static final double RUN_ATTACK_TRIGGER = 58.0;
    private static final double BASE_APPROACH_PADDING = 14.0;
    private static final double WALL_SEARCH_RANGE = 110.0;
    private static final double DIRECT_TARGET_RECALC_DISTANCE = 28.0;
    private static final double PATH_POINT_REACHED = 9.0;
    private static final long IDLE_FRAME_NS = 120_000_000L;
    private static final long WALK_FRAME_NS = 95_000_000L;
    private static final long RUN_FRAME_NS = 82_000_000L;
    private static final long ATTACK_1_FRAME_NS = 90_000_000L;
    private static final long ATTACK_2_FRAME_NS = 90_000_000L;
    private static final long ATTACK_3_FRAME_NS = 70_000_000L;
    private static final long RUN_ATTACK_FRAME_NS = 78_000_000L;
    private static final long JUMP_FRAME_NS = 72_000_000L;
    private static final long HURT_FRAME_NS = 100_000_000L;
    private static final long DEAD_FRAME_NS = 160_000_000L;
    private static final long HURT_LOCK_NS = 220_000_000L;
    private static final long PATH_RECALC_NS = 650_000_000L;
    private static final long STUCK_REPATH_NS = 650_000_000L;
    private static final long STUCK_RECOVER_NS = 200_000_000L;
    private static final long AGGRO_DURATION_NS = 4_000_000_000L;
    private static final long JUMP_COOLDOWN_NS = 1_550_000_000L;
    private static final long LUNGE_COOLDOWN_NS = 1_250_000_000L;
    private static final int PATH_MAX_EXPANSIONS = 2200;
    private static final double STUCK_MOVE_EPSILON = 2.0;
    private static final long STUCK_SAMPLE_NS = 700_000_000L;
    private static final double FACE_MOVE_THRESHOLD = 0.10;

    private static final AttackProfile ATTACK_ONE = new AttackProfile(AnimationState.ATTACK_1, 3, 850_000_000L, ATTACK_1_FRAME_NS, 34.0, 30.0, 22.0, 6.0, DAMAGE, false);
    private static final AttackProfile ATTACK_TWO = new AttackProfile(AnimationState.ATTACK_2, 2, 1_150_000_000L, ATTACK_2_FRAME_NS, 38.0, 34.0, 24.0, 7.0, DAMAGE, false);
    private static final AttackProfile ATTACK_THREE = new AttackProfile(AnimationState.ATTACK_3, 2, 620_000_000L, ATTACK_3_FRAME_NS, 28.0, 26.0, 20.0, 5.0, DAMAGE, false);
    private static final AttackProfile RUN_ATTACK = new AttackProfile(AnimationState.RUN_ATTACK, 3, 1_300_000_000L, RUN_ATTACK_FRAME_NS, 42.0, 36.0, 24.0, 8.0, DAMAGE, true);
    private static final AttackProfile JUMP_ATTACK = new AttackProfile(AnimationState.JUMP, 6, 1_550_000_000L, JUMP_FRAME_NS, 36.0, 0.0, 0.0, 0.0, 0, true);

    private final MovementValidator movementValidator;
    private final WorldQuery worldQuery;
    private final Random random;
    private final Map<AnimationState, SpriteAnimation> animations;

    private BrainState state;
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
    private long stuckRecoverUntilNs;
    private long aggroUntilNs;
    private long hurtUntilNs;
    private long attackStartedAtNs;
    private long attackCooldownUntilNs;
    private long jumpCooldownUntilNs;
    private long lungeCooldownUntilNs;
    private boolean attackDamageAppliedThisCycle;
    private AttackProfile activeAttackProfile;
    private Entity activeEntityTarget;
    private Player aggroPlayer;
    private BuildObject activeBuildTarget;
    private final List<Point2D> currentPath;
    private int currentPathIndex;
    private double stuckSampleX;
    private double stuckSampleY;

    public WolfEnemy(double x,
                     double y,
                     double width,
                     double height,
                     MovementValidator movementValidator,
                     WorldQuery worldQuery,
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
        this.worldQuery = worldQuery;
        this.random = random == null ? new Random() : random;
        this.animations = new HashMap<>();
        animations.put(AnimationState.IDLE, new SpriteAnimation(SpriteSheetLoader.loadGrid(resolveAsset("Idle"), 8, 1), IDLE_FRAME_NS));
        animations.put(AnimationState.WALK, new SpriteAnimation(SpriteSheetLoader.loadGrid(resolveAsset("Walk"), 11, 1), WALK_FRAME_NS));
        animations.put(AnimationState.RUN, new SpriteAnimation(SpriteSheetLoader.loadGrid(resolveAsset("Run"), 9, 1), RUN_FRAME_NS));
        animations.put(AnimationState.ATTACK_1, new SpriteAnimation(SpriteSheetLoader.loadGrid(resolveAsset("Attack_1"), 6, 1), ATTACK_1_FRAME_NS));
        animations.put(AnimationState.ATTACK_2, new SpriteAnimation(SpriteSheetLoader.loadGrid(resolveAsset("Attack_2"), 5, 1), ATTACK_2_FRAME_NS));
        animations.put(AnimationState.ATTACK_3, new SpriteAnimation(SpriteSheetLoader.loadGrid(resolveAsset("Attack_3"), 5, 1), ATTACK_3_FRAME_NS));
        animations.put(AnimationState.RUN_ATTACK, new SpriteAnimation(SpriteSheetLoader.loadGrid(resolveAsset("Run+Attack"), 7, 1), RUN_ATTACK_FRAME_NS));
        animations.put(AnimationState.JUMP, new SpriteAnimation(SpriteSheetLoader.loadGrid(resolveAsset("Jump"), 11, 1), JUMP_FRAME_NS));
        animations.put(AnimationState.HURT, new SpriteAnimation(SpriteSheetLoader.loadGrid(resolveAsset("Hurt"), 2, 1), HURT_FRAME_NS));
        animations.put(AnimationState.DEAD, new SpriteAnimation(SpriteSheetLoader.loadGrid(resolveAsset("Dead"), 2, 1), DEAD_FRAME_NS));
        this.state = BrainState.IDLE;
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
        this.stuckRecoverUntilNs = 0L;
        this.aggroUntilNs = 0L;
        this.hurtUntilNs = -1L;
        this.attackStartedAtNs = -1L;
        this.attackCooldownUntilNs = 0L;
        this.jumpCooldownUntilNs = 0L;
        this.lungeCooldownUntilNs = 0L;
        this.attackDamageAppliedThisCycle = false;
        this.activeAttackProfile = null;
        this.activeEntityTarget = null;
        this.aggroPlayer = null;
        this.activeBuildTarget = null;
        this.currentPath = new ArrayList<>();
        this.currentPathIndex = 0;
        this.stuckSampleX = getCenterX();
        this.stuckSampleY = getCenterY();
        pickNextPatrolTarget();
    }

    @Override
    protected double collisionInsetLeft(double width, double height) {
        return width * 0.28;
    }

    @Override
    protected double collisionInsetRight(double width, double height) {
        return width * 0.28;
    }

    @Override
    protected double collisionInsetTop(double width, double height) {
        return height * 0.58;
    }

    @Override
    protected double collisionInsetBottom(double width, double height) {
        return height * 0.08;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = DEBUG && debugEnabled;
    }

    public void updateBehavior(long nowNs,
                               boolean isNight,
                               Player player,
                               BaseCamp baseCamp,
                               double worldWidth,
                               double worldHeight) {
        if (removeFromWorld) {
            return;
        }
        if (state == BrainState.DYING) {
            if (currentAnimation().updateOnce(nowNs)) {
                removeFromWorld = true;
            }
            return;
        }
        if (!isAlive()) {
            transitionToDeath();
            return;
        }

        if (state == BrainState.HURT) {
            animationState = AnimationState.HURT;
            if (nowNs < hurtUntilNs) {
                currentAnimation().update(nowNs, true);
                return;
            }
            state = BrainState.IDLE;
            resetAnimation(AnimationState.HURT);
        }

        if (state == BrainState.ATTACK) {
            updateAttackSequence(nowNs);
            return;
        }

        if (state == BrainState.STUCK_RECOVER) {
            animationState = AnimationState.IDLE;
            currentAnimation().update(nowNs, true);
            if (nowNs < stuckRecoverUntilNs) {
                return;
            }
            state = BrainState.IDLE;
        }

        if (activeBuildTarget != null && !activeBuildTarget.isAlive()) {
            activeBuildTarget = null;
        }
        if (activeEntityTarget != null && activeEntityTarget.isDead()) {
            activeEntityTarget = null;
        }

        Player playerTarget = resolvePlayerTarget(player, nowNs);
        if (playerTarget != null) {
            activeEntityTarget = playerTarget;
            if (tryStartAttack(playerTarget, null, nowNs, isNight)) {
                return;
            }
            state = BrainState.CHASE_PLAYER;
            runTowardEntity(playerTarget, nowNs, worldWidth, worldHeight, isNight);
            return;
        }

        Entity preferredNightTarget = pickNightPriorityTarget(player, baseCamp);
        if (isNight && preferredNightTarget != null) {
            if (tryStartAttack(preferredNightTarget, null, nowNs, isNight)) {
                return;
            }
            if (preferredNightTarget == player) {
                state = BrainState.CHASE_PLAYER;
                runTowardEntity(preferredNightTarget, nowNs, worldWidth, worldHeight, true);
                return;
            }
            state = BrainState.NIGHT_ASSAULT;
            runTowardBaseOrBarrier((BaseCamp) preferredNightTarget, player, nowNs, worldWidth, worldHeight);
            return;
        }

        activeEntityTarget = null;
        activeBuildTarget = null;
        if (distanceToHome() > HOME_RADIUS * 0.50) {
            state = BrainState.RETURN_HOME;
            animationState = AnimationState.RUN;
            currentMoveSpeed = RUN_SPEED;
            followPathToPoint(homeX, homeY, nowNs, worldWidth, worldHeight, true);
            if (distanceTo(homeX, homeY) <= RETURN_TOLERANCE) {
                clearPath();
                state = BrainState.IDLE;
            }
            return;
        }

        state = BrainState.PATROL;
        currentMoveSpeed = WALK_SPEED;
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
        if (amount <= 0 || removeFromWorld || state == BrainState.DYING) {
            return;
        }
        super.takeDamage(amount);
        if (hp <= 0) {
            hp = 0;
            transitionToDeath();
            return;
        }
        hurtUntilNs = System.nanoTime() + HURT_LOCK_NS;
        state = BrainState.HURT;
        animationState = AnimationState.HURT;
        resetAnimation(AnimationState.HURT);
        clearAttackTarget();
        clearPath();
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

    private void runTowardBaseOrBarrier(BaseCamp baseCamp,
                                        Player player,
                                        long nowNs,
                                        double worldWidth,
                                        double worldHeight) {
        if (baseCamp == null || !baseCamp.isAlive()) {
            state = BrainState.RETURN_HOME;
            followPathToPoint(homeX, homeY, nowNs, worldWidth, worldHeight, true);
            return;
        }
        if (tryStartAttack(null, baseCamp, nowNs, true)) {
            return;
        }
        state = BrainState.NIGHT_ASSAULT;
        animationState = AnimationState.RUN;
        currentMoveSpeed = RUN_SPEED;

        Point2D approach = selectApproachPointForEntity(baseCamp, ATTACK_ONE.range() + BASE_APPROACH_PADDING);
        boolean hasPath = followPathToPoint(approach.getX(), approach.getY(), nowNs, worldWidth, worldHeight, true);
        if (hasPath) {
            return;
        }
        if (worldQuery != null) {
            BuildObject barrier = worldQuery.findNearestWallToAttack(this, baseCamp.getCenterX(), baseCamp.getCenterY(), WALL_SEARCH_RANGE);
            if (barrier != null) {
                activeBuildTarget = barrier;
                if (tryStartAttack(null, null, nowNs, true)) {
                    return;
                }
                followPathToPoint(barrier.getCenterX(), barrier.getCenterY(), nowNs, worldWidth, worldHeight, true);
                return;
            }
        }
        if (player != null && player.isAlive() && distanceTo(player.getCenterX(), player.getCenterY()) < DETECTION_RANGE) {
            state = BrainState.CHASE_PLAYER;
            runTowardEntity(player, nowNs, worldWidth, worldHeight, true);
        }
    }

    private boolean canDetectPlayer(Player player) {
        return player != null
                && player.isAlive()
                && distanceTo(player.getCenterX(), player.getCenterY()) <= DETECTION_RANGE;
    }

    private Player resolvePlayerTarget(Player player, long nowNs) {
        if (player == null || !player.isAlive()) {
            aggroPlayer = null;
            return null;
        }
        if (isTargetInsideAttackReach(player, Math.max(ATTACK_ONE.range(), ATTACK_TWO.range()))) {
            aggroPlayer = player;
            return player;
        }
        if (canDetectPlayer(player)) {
            aggroPlayer = player;
            return player;
        }
        if (aggroPlayer == player && nowNs <= aggroUntilNs) {
            return player;
        }
        if (aggroPlayer == player) {
            aggroPlayer = null;
        }
        return null;
    }

    public void aggroOn(Player player, long nowNs) {
        if (player == null || !player.isAlive() || removeFromWorld || state == BrainState.DYING) {
            return;
        }
        aggroPlayer = player;
        activeEntityTarget = player;
        aggroUntilNs = nowNs + AGGRO_DURATION_NS;
        if (state != BrainState.ATTACK && state != BrainState.HURT) {
            state = BrainState.CHASE_PLAYER;
        }
    }

    private Entity pickNightPriorityTarget(Player player, BaseCamp baseCamp) {
        boolean playerAlive = player != null && player.isAlive();
        boolean baseAlive = baseCamp != null && baseCamp.isAlive();
        if (!playerAlive && !baseAlive) {
            return null;
        }
        if (!baseAlive) {
            return player;
        }
        if (!playerAlive) {
            return baseCamp;
        }
        double playerDist = distanceTo(player.getCenterX(), player.getCenterY());
        double baseDist = distanceTo(baseCamp.getCenterX(), baseCamp.getCenterY());
        if (playerDist <= baseDist - NIGHT_PLAYER_PRIORITY_BONUS) {
            return player;
        }
        if (playerDist <= DETECTION_RANGE && playerDist < baseDist) {
            return player;
        }
        return baseCamp;
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
        double dist = distanceTo(target.getCenterX(), target.getCenterY());
        if (aggressive && target instanceof Player && dist >= JUMP_TRIGGER_MIN && dist <= JUMP_TRIGGER_MAX && nowNs >= jumpCooldownUntilNs) {
            if (startAttack(JUMP_ATTACK, target, null, nowNs)) {
                jumpCooldownUntilNs = nowNs + JUMP_COOLDOWN_NS;
                return;
            }
        }
        if (dist <= RUN_ATTACK_TRIGGER && nowNs >= lungeCooldownUntilNs && aggressive) {
            if (startAttack(RUN_ATTACK, target, null, nowNs)) {
                lungeCooldownUntilNs = nowNs + LUNGE_COOLDOWN_NS;
                return;
            }
        }
        state = BrainState.CHASE_PLAYER;
        animationState = AnimationState.RUN;
        currentMoveSpeed = RUN_SPEED;
        followPathToPoint(target.getCenterX(), target.getCenterY(), nowNs, worldWidth, worldHeight, true);
    }

    private boolean tryStartAttack(Entity entityTarget, BaseCamp baseTarget, long nowNs, boolean isNight) {
        BuildObject buildTarget = activeBuildTarget;
        if (entityTarget != null) {
            activeEntityTarget = entityTarget;
        }
        if (buildTarget != null && !buildTarget.isAlive()) {
            buildTarget = null;
        }
        AttackProfile profile = chooseAttackProfile(entityTarget, baseTarget, buildTarget, isNight);
        if (profile == null || nowNs < attackCooldownUntilNs) {
            return false;
        }
        if (entityTarget != null && !isTargetInsideAttackReach(entityTarget, profile.range())) {
            return false;
        }
        if (baseTarget != null && !isTargetInsideAttackReach(baseTarget, profile.range())) {
            return false;
        }
        if (buildTarget != null && !isTargetInsideAttackReach(buildTarget, profile.range())) {
            return false;
        }
        return startAttack(profile, entityTarget != null ? entityTarget : baseTarget, buildTarget, nowNs);
    }

    private AttackProfile chooseAttackProfile(Entity entityTarget, BaseCamp baseTarget, BuildObject buildTarget, boolean isNight) {
        if (entityTarget instanceof Player player) {
            double dist = distanceTo(player.getCenterX(), player.getCenterY());
            if (dist <= 28.0) {
                return ATTACK_THREE;
            }
            if (isNight && random.nextDouble() < 0.50) {
                return ATTACK_TWO;
            }
            if (dist <= RUN_ATTACK_TRIGGER && random.nextDouble() < 0.28) {
                return RUN_ATTACK;
            }
            return ATTACK_ONE;
        }
        if (baseTarget != null) {
            return isNight && random.nextDouble() < 0.58 ? ATTACK_TWO : ATTACK_ONE;
        }
        if (buildTarget != null) {
            return isNight && random.nextDouble() < 0.48 ? ATTACK_TWO : ATTACK_ONE;
        }
        return null;
    }

    private boolean startAttack(AttackProfile profile, Entity entityTarget, BuildObject buildTarget, long nowNs) {
        if (profile == null) {
            return false;
        }
        state = BrainState.ATTACK;
        animationState = profile.animationState();
        activeAttackProfile = profile;
        activeEntityTarget = entityTarget;
        activeBuildTarget = buildTarget;
        attackStartedAtNs = nowNs;
        attackDamageAppliedThisCycle = false;
        resetAnimation(animationState);
        clearPath();
        blockedSinceNs = 0L;
        if (entityTarget != null) {
            faceTarget(entityTarget);
        } else if (buildTarget != null) {
            faceTarget(buildTarget.getCenterX(), buildTarget.getCenterY());
        }
        return true;
    }

    private void updateAttackSequence(long nowNs) {
        if (activeAttackProfile == null) {
            state = BrainState.IDLE;
            animationState = AnimationState.IDLE;
            return;
        }
        SpriteAnimation animation = currentAnimation();
        animation.update(nowNs, true);
        Entity entityTarget = activeEntityTarget;
        BuildObject buildTarget = activeBuildTarget;
        if (entityTarget != null && entityTarget.isDead()) {
            entityTarget = null;
            activeEntityTarget = null;
        }
        if (buildTarget != null && !buildTarget.isAlive()) {
            buildTarget = null;
            activeBuildTarget = null;
        }
        if (entityTarget != null) {
            faceTarget(entityTarget);
        } else if (buildTarget != null) {
            faceTarget(buildTarget.getCenterX(), buildTarget.getCenterY());
        }
        if (activeAttackProfile.lungeAttack()) {
            applyAttackLunge(entityTarget, buildTarget);
        }
        if (canApplyDamageOnCurrentFrame(animation)) {
            if (entityTarget instanceof BaseCamp baseCamp) {
                if (intersectsAttackHitbox(baseCamp, activeAttackProfile) && worldQuery != null && worldQuery.damageBase(this, baseCamp, nowNs)) {
                    attackDamageAppliedThisCycle = true;
                }
            } else if (entityTarget != null) {
                if (intersectsAttackHitbox(entityTarget, activeAttackProfile)) {
                    DamageSystem.applyDamage(this, entityTarget, activeAttackProfile.damage(), nowNs);
                    attackDamageAppliedThisCycle = true;
                }
            } else if (buildTarget != null) {
                if (intersectsAttackHitbox(buildTarget, activeAttackProfile) && worldQuery != null && worldQuery.damageWall(this, buildTarget, nowNs)) {
                    attackDamageAppliedThisCycle = true;
                }
            }
        }

        long duration = activeAttackProfile.totalDurationNs(animation.getFrameCount());
        if (nowNs - attackStartedAtNs < duration) {
            return;
        }
        attackCooldownUntilNs = nowNs + activeAttackProfile.cooldownNs();
        attackStartedAtNs = -1L;
        activeAttackProfile = null;
        clearAttackTarget();
        resetAnimation(AnimationState.ATTACK_1);
        resetAnimation(AnimationState.ATTACK_2);
        resetAnimation(AnimationState.ATTACK_3);
        resetAnimation(AnimationState.RUN_ATTACK);
        resetAnimation(AnimationState.JUMP);
        state = BrainState.IDLE;
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
        return state == AnimationState.ATTACK_1
                || state == AnimationState.ATTACK_2
                || state == AnimationState.ATTACK_3
                || state == AnimationState.RUN_ATTACK;
    }

    private void applyAttackLunge(Entity entityTarget, BuildObject buildTarget) {
        double targetX;
        double targetY;
        if (entityTarget != null) {
            targetX = entityTarget.getCenterX();
            targetY = entityTarget.getCenterY();
        } else if (buildTarget != null) {
            targetX = buildTarget.getCenterX();
            targetY = buildTarget.getCenterY();
        } else {
            return;
        }
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 1.0) {
            return;
        }
        double moveDistance = animationState == AnimationState.JUMP ? JUMP_SPEED : RUN_SPEED * 0.92;
        moveWithCollision((dx / distance) * moveDistance, (dy / distance) * moveDistance);
    }

    private boolean followPathToPoint(double targetX,
                                      double targetY,
                                      long nowNs,
                                      double worldWidth,
                                      double worldHeight,
                                      boolean allowBarrierFallback) {
        if (shouldRebuildPath(targetX, targetY, nowNs)) {
            rebuildPath(targetX, targetY);
            lastPathComputeAtNs = nowNs;
            lastPathTargetX = targetX;
            lastPathTargetY = targetY;
        }
        boolean moved = false;
        if (!currentPath.isEmpty()) {
            while (currentPathIndex < currentPath.size()) {
                Point2D waypoint = currentPath.get(currentPathIndex);
                if (!canStandCenteredAt(waypoint.getX(), waypoint.getY())) {
                    clearPath();
                    break;
                }
                if (distanceTo(waypoint.getX(), waypoint.getY()) <= PATH_POINT_REACHED) {
                    currentPathIndex++;
                    continue;
                }
                moved = moveToward(waypoint.getX(), waypoint.getY(), worldWidth, worldHeight);
                break;
            }
        }
        if (!moved && canMoveDirectlyTo(targetX, targetY)) {
            moved = moveToward(targetX, targetY, worldWidth, worldHeight);
        }
        if (moved) {
            blockedSinceNs = 0L;
            updateStuckSample(nowNs);
            currentAnimation().update(System.nanoTime(), true);
            return true;
        }
        if (blockedSinceNs == 0L) {
            blockedSinceNs = nowNs;
        } else if (nowNs - blockedSinceNs >= STUCK_REPATH_NS) {
            enterStuckRecover(nowNs, targetX, targetY, allowBarrierFallback);
            return false;
        }
        if (allowBarrierFallback && worldQuery != null) {
            BuildObject barrier = worldQuery.findNearestWallToAttack(this, targetX, targetY, WALL_SEARCH_RANGE);
            if (barrier != null) {
                activeBuildTarget = barrier;
            }
        }
        updateIdleAnimation(System.nanoTime());
        return false;
    }

    private void updateStuckSample(long nowNs) {
        if (state != BrainState.CHASE_PLAYER && state != BrainState.NIGHT_ASSAULT && state != BrainState.RETURN_HOME) {
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
        stuckSampleAtNs = nowNs;
        stuckSampleX = getCenterX();
        stuckSampleY = getCenterY();
        if (moved < STUCK_MOVE_EPSILON) {
            enterStuckRecover(nowNs, lastPathTargetX, lastPathTargetY, state != BrainState.PATROL);
        }
    }

    private void enterStuckRecover(long nowNs, double targetX, double targetY, boolean allowBarrierFallback) {
        state = BrainState.STUCK_RECOVER;
        animationState = AnimationState.IDLE;
        stuckRecoverUntilNs = nowNs + STUCK_RECOVER_NS;
        blockedSinceNs = 0L;
        clearPath();
        chooseSideStepWaypoint(targetX, targetY);
        if (allowBarrierFallback && worldQuery != null) {
            BuildObject barrier = worldQuery.findNearestWallToAttack(this, targetX, targetY, WALL_SEARCH_RANGE);
            if (barrier != null) {
                activeBuildTarget = barrier;
            }
        }
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
        double step = Math.max(18.0, Math.max(getCollisionWidth(), getCollisionHeight()) * 1.6);
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

    private void updateIdleAnimation(long nowNs) {
        animationState = AnimationState.IDLE;
        currentAnimation().update(nowNs, true);
    }

    private boolean shouldRebuildPath(double targetX, double targetY, long nowNs) {
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
        if (movementValidator == null) {
            return true;
        }
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= currentMoveSpeed + 0.001) {
            return true;
        }
        double step = Math.max(8.0, Math.min(18.0, Math.max(getCollisionWidth(), getCollisionHeight()) * 0.65));
        int samples = Math.max(1, (int) Math.ceil(distance / step));
        for (int i = 1; i <= samples; i++) {
            double t = i / (double) samples;
            double centerX = getCenterX() + dx * t;
            double centerY = getCenterY() + dy * t;
            double candidateX = centerX - width * 0.5;
            double candidateY = centerY - height * 0.5;
            if (!movementValidator.canOccupy(this, candidateX, candidateY, width, height)) {
                return false;
            }
        }
        return true;
    }

    private void rebuildPath(double targetX, double targetY) {
        clearPath();
        int tileWidth = worldQuery == null ? 32 : Math.max(1, worldQuery.getTileWidth());
        int tileHeight = worldQuery == null ? 32 : Math.max(1, worldQuery.getTileHeight());
        Node start = toNode(getCenterX(), getCenterY(), tileWidth, tileHeight);
        Set<Node> goals = buildGoalNodes(targetX, targetY, tileWidth, tileHeight);
        List<Point2D> found = findPath(start, goals, tileWidth, tileHeight);
        if (found.isEmpty()) {
            return;
        }
        currentPath.addAll(found);
        currentPathIndex = 0;
    }

    private List<Point2D> findPath(Node start, Set<Node> goals, int tileWidth, int tileHeight) {
        if (goals.isEmpty()) {
            return List.of();
        }
        PriorityQueue<PathNode> open = new PriorityQueue<>(Comparator.comparingDouble(PathNode::score));
        Map<Node, Node> cameFrom = new HashMap<>();
        Map<Node, Double> gScore = new HashMap<>();
        Set<Node> closed = new HashSet<>();
        Node bestGoal = null;
        open.add(new PathNode(start, heuristic(start, goals)));
        gScore.put(start, 0.0);
        int expansions = 0;

        while (!open.isEmpty() && expansions < PATH_MAX_EXPANSIONS) {
            PathNode currentRecord = open.poll();
            Node current = currentRecord.node();
            if (!closed.add(current)) {
                continue;
            }
            expansions++;
            if (goals.contains(current)) {
                bestGoal = current;
                break;
            }
            for (Node neighbor : neighbors(current)) {
                if (closed.contains(neighbor)) {
                    continue;
                }
                if (!isWalkableNode(neighbor, tileWidth, tileHeight)
                        || isDiagonalCornerCut(current, neighbor, tileWidth, tileHeight)) {
                    continue;
                }
                double tentative = gScore.getOrDefault(current, Double.POSITIVE_INFINITY) + distance(current.x(), current.y(), neighbor.x(), neighbor.y());
                if (tentative >= gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    continue;
                }
                cameFrom.put(neighbor, current);
                gScore.put(neighbor, tentative);
                open.add(new PathNode(neighbor, tentative + heuristic(neighbor, goals)));
            }
        }

        if (bestGoal == null) {
            return List.of();
        }
        Deque<Point2D> reversed = new ArrayDeque<>();
        Node cursor = bestGoal;
        while (cursor != null && !Objects.equals(cursor, start)) {
            reversed.addFirst(new Point2D(cursor.x() * tileWidth + tileWidth * 0.5, cursor.y() * tileHeight + tileHeight * 0.5));
            cursor = cameFrom.get(cursor);
        }
        return new ArrayList<>(reversed);
    }

    private double heuristic(Node current, Set<Node> goals) {
        double best = Double.POSITIVE_INFINITY;
        for (Node goal : goals) {
            best = Math.min(best, distance(current.x(), current.y(), goal.x(), goal.y()));
        }
        return best;
    }

    private Set<Node> buildGoalNodes(double targetX, double targetY, int tileWidth, int tileHeight) {
        Set<Node> goals = new HashSet<>();
        Node direct = toNode(targetX, targetY, tileWidth, tileHeight);
        if (isWalkableNode(direct, tileWidth, tileHeight)) {
            goals.add(direct);
        }
        int searchRadius = 4;
        for (int dy = -searchRadius; dy <= searchRadius; dy++) {
            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                if (Math.abs(dx) + Math.abs(dy) > searchRadius + 1) {
                    continue;
                }
                Node candidate = new Node(direct.x() + dx, direct.y() + dy);
                if (isWalkableNode(candidate, tileWidth, tileHeight)) {
                    goals.add(candidate);
                }
            }
        }
        return goals;
    }

    private List<Node> neighbors(Node node) {
        return List.of(
                new Node(node.x() + 1, node.y()),
                new Node(node.x() - 1, node.y()),
                new Node(node.x(), node.y() + 1),
                new Node(node.x(), node.y() - 1),
                new Node(node.x() + 1, node.y() + 1),
                new Node(node.x() - 1, node.y() + 1),
                new Node(node.x() + 1, node.y() - 1),
                new Node(node.x() - 1, node.y() - 1)
        );
    }

    private boolean isWalkableNode(Node node, int tileWidth, int tileHeight) {
        double centerX = node.x() * tileWidth + tileWidth * 0.5;
        double centerY = node.y() * tileHeight + tileHeight * 0.5;
        double candidateX = centerX - width * 0.5;
        double candidateY = centerY - height * 0.5;
        return movementValidator == null || movementValidator.canOccupy(this, candidateX, candidateY, width, height);
    }

    private boolean isDiagonalCornerCut(Node current, Node neighbor, int tileWidth, int tileHeight) {
        int dx = neighbor.x() - current.x();
        int dy = neighbor.y() - current.y();
        if (Math.abs(dx) != 1 || Math.abs(dy) != 1) {
            return false;
        }
        return !isWalkableNode(new Node(current.x() + dx, current.y()), tileWidth, tileHeight)
                || !isWalkableNode(new Node(current.x(), current.y() + dy), tileWidth, tileHeight);
    }

    private Node toNode(double worldX, double worldY, int tileWidth, int tileHeight) {
        return new Node((int) Math.floor(worldX / tileWidth), (int) Math.floor(worldY / tileHeight));
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
        if (moveWithCollision(moveX, moveY)) {
            clampPosition(0, 0, worldWidth, worldHeight);
            currentAnimation().update(System.nanoTime(), true);
            return true;
        }
        if (moveWithCollision(moveX, 0.0)) {
            clampPosition(0, 0, worldWidth, worldHeight);
            currentAnimation().update(System.nanoTime(), true);
            return true;
        }
        if (moveWithCollision(0.0, moveY)) {
            clampPosition(0, 0, worldWidth, worldHeight);
            currentAnimation().update(System.nanoTime(), true);
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

    private Point2D selectApproachPointForEntity(Entity target, double range) {
        double targetLeft = target.getCollisionX() - range;
        double targetRight = target.getCollisionX() + target.getCollisionWidth() + range;
        double targetTop = target.getCollisionY() - range;
        double targetBottom = target.getCollisionY() + target.getCollisionHeight() + range;
        List<Point2D> candidates = List.of(
                new Point2D(target.getCenterX(), targetTop),
                new Point2D(target.getCenterX(), targetBottom),
                new Point2D(targetLeft, target.getCenterY()),
                new Point2D(targetRight, target.getCenterY())
        );
        Point2D best = candidates.getFirst();
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Point2D candidate : candidates) {
            double dist = distanceTo(candidate.getX(), candidate.getY());
            if (dist < bestDistance) {
                best = candidate;
                bestDistance = dist;
            }
        }
        return best;
    }

    private boolean isTargetInsideAttackReach(Entity target, double range) {
        if (target == null || target.isDead()) {
            return false;
        }
        return rectDistance(getCollisionX(), getCollisionY(), getCollisionWidth(), getCollisionHeight(),
                target.getCollisionX(), target.getCollisionY(), target.getCollisionWidth(), target.getCollisionHeight()) <= range;
    }

    private boolean isTargetInsideAttackReach(BuildObject target, double range) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        return rectDistance(getCollisionX(), getCollisionY(), getCollisionWidth(), getCollisionHeight(),
                target.getCollisionX(), target.getCollisionY(), target.getCollisionWidth(), target.getCollisionHeight()) <= range;
    }

    private boolean intersectsAttackHitbox(Entity target, AttackProfile profile) {
        Rectangle2D hitbox = buildAttackHitbox(profile);
        return hitbox.getWidth() > 0.0
                && isTargetInFront(target)
                && hitbox.intersects(
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
    }

    private boolean intersectsAttackHitbox(BuildObject target, AttackProfile profile) {
        Rectangle2D hitbox = buildAttackHitbox(profile);
        return hitbox.getWidth() > 0.0
                && isTargetInFront(target.getCenterX(), target.getCenterY())
                && hitbox.intersects(
                target.getCollisionX(),
                target.getCollisionY(),
                target.getCollisionWidth(),
                target.getCollisionHeight()
        );
    }

    private boolean isTargetInFront(Entity target) {
        return target != null && isTargetInFront(target.getCenterX(), target.getCenterY());
    }

    private boolean isTargetInFront(double targetX, double targetY) {
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        return switch (facingDirection) {
            case LEFT -> dx <= 10.0 && Math.abs(dy) <= 58.0;
            case RIGHT -> dx >= -10.0 && Math.abs(dy) <= 58.0;
            case UP -> dy <= 10.0 && Math.abs(dx) <= 58.0;
            case DOWN -> dy >= -10.0 && Math.abs(dx) <= 58.0;
        };
    }

    private Rectangle2D buildAttackHitbox(AttackProfile profile) {
        if (profile == null || profile.hitboxWidth() <= 0.0 || profile.hitboxHeight() <= 0.0) {
            return new Rectangle2D(0.0, 0.0, 0.0, 0.0);
        }
        double hitboxWidth = profile.hitboxWidth();
        double hitboxHeight = profile.hitboxHeight();
        double centerX = getCenterX();
        double centerY = getCollisionY() + getCollisionHeight() * 0.45;
        double offset = profile.forwardOffset();
        return switch (facingDirection) {
            case LEFT -> new Rectangle2D(getCollisionX() - offset - hitboxWidth, centerY - hitboxHeight * 0.5, hitboxWidth, hitboxHeight);
            case RIGHT -> new Rectangle2D(getCollisionX() + getCollisionWidth() + offset, centerY - hitboxHeight * 0.5, hitboxWidth, hitboxHeight);
            case UP -> new Rectangle2D(centerX - hitboxHeight * 0.5, getCollisionY() - offset - hitboxWidth, hitboxHeight, hitboxWidth);
            case DOWN -> new Rectangle2D(centerX - hitboxHeight * 0.5, getCollisionY() + getCollisionHeight() + offset, hitboxHeight, hitboxWidth);
        };
    }

    private void drawDebug(GraphicsContext graphicsContext, double cameraX, double cameraY) {
        graphicsContext.save();
        graphicsContext.setLineWidth(1.2);
        graphicsContext.setStroke(Color.color(1.0, 1.0, 1.0, 0.95));
        graphicsContext.strokeRect(getCollisionX() - cameraX, getCollisionY() - cameraY, getCollisionWidth(), getCollisionHeight());

        graphicsContext.setStroke(Color.color(1.0, 0.8, 0.15, 0.80));
        graphicsContext.strokeOval(getCenterX() - DETECTION_RANGE - cameraX, getCenterY() - DETECTION_RANGE - cameraY, DETECTION_RANGE * 2.0, DETECTION_RANGE * 2.0);

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

        if (state == BrainState.ATTACK && activeAttackProfile != null && isDamageAnimation(activeAttackProfile.animationState())) {
            Rectangle2D attackHitbox = buildAttackHitbox(activeAttackProfile);
            graphicsContext.setStroke(Color.color(1.0, 0.05, 0.05, 0.98));
            graphicsContext.strokeRect(attackHitbox.getMinX() - cameraX, attackHitbox.getMinY() - cameraY, attackHitbox.getWidth(), attackHitbox.getHeight());
        }
        graphicsContext.setFill(Color.color(1.0, 1.0, 1.0, 0.98));
        graphicsContext.fillText(debugStateName(), x - cameraX, y - cameraY - 5.0);
        graphicsContext.restore();
    }

    private String debugStateName() {
        if (state == BrainState.CHASE_PLAYER) {
            return "CHASE";
        }
        if (state == BrainState.DYING) {
            return "DEAD";
        }
        if (state == BrainState.NIGHT_ASSAULT) {
            return "CHASE";
        }
        if (state == BrainState.STUCK_RECOVER) {
            return "STUCK";
        }
        return state.name();
    }

    private void transitionToDeath() {
        state = BrainState.DYING;
        animationState = AnimationState.DEAD;
        clearAttackTarget();
        clearPath();
        resetAnimation(AnimationState.DEAD);
    }

    private void clearAttackTarget() {
        activeEntityTarget = null;
        activeBuildTarget = null;
    }

    private void clearPath() {
        currentPath.clear();
        currentPathIndex = 0;
        lastPathTargetX = Double.NaN;
        lastPathTargetY = Double.NaN;
        blockedSinceNs = 0L;
    }

    private void pickNextPatrolTarget() {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 20.0 + random.nextDouble() * (HOME_RADIUS - 20.0);
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
        updateFacing(targetX - getCenterX(), targetY - getCenterY());
    }

    private void updateFacing(double dx, double dy) {
        if (Math.abs(dx) >= Math.abs(dy)) {
            facingDirection = dx >= 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            facingDirection = dy >= 0 ? Direction.DOWN : Direction.UP;
        }
    }

    private double distanceToHome() {
        return distanceTo(homeX, homeY);
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

    private record Node(int x, int y) {
    }

    private record PathNode(Node node, double score) {
    }
}
