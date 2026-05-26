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
    private static final double DISENGAGE_RANGE = 520.0;
    private static final double HOME_RADIUS = 92.0;
    private static final double RETURN_TOLERANCE = 10.0;
    private static final double DIRECT_TARGET_RECALC_DISTANCE = 28.0;
    private static final double PATH_POINT_REACHED = 9.0;
    private static final long IDLE_FRAME_NS = 120_000_000L;
    private static final long WALK_FRAME_NS = 95_000_000L;
    private static final long RUN_FRAME_NS = 82_000_000L;
    private static final long ATTACK_2_FRAME_NS = 90_000_000L;
    private static final long DEAD_FRAME_NS = 160_000_000L;
    private static final long PATH_RECALC_NS = 1_400_000_000L;
    private static final long STUCK_REPATH_NS = 650_000_000L;
    private static final double STUCK_MOVE_EPSILON = 2.0;
    private static final long STUCK_SAMPLE_NS = 700_000_000L;
    private static final int PATH_MAX_EXPANSIONS = 700;
    private static final double FACE_MOVE_THRESHOLD = 0.10;

    private static final AttackProfile ATTACK_TWO = new AttackProfile(AnimationState.ATTACK_2, 2, 0L, ATTACK_2_FRAME_NS, 0.0, 24.0, 26.0, 0.0, DAMAGE, false);

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
    private long attackStartedAtNs;
    private long attackCooldownUntilNs;
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
        animations.put(AnimationState.ATTACK_2, new SpriteAnimation(SpriteSheetLoader.loadHorizontalStrip(resolveAsset("Attack_2"), 4), ATTACK_2_FRAME_NS));
        animations.put(AnimationState.DEAD, new SpriteAnimation(SpriteSheetLoader.loadGrid(resolveAsset("Dead"), 2, 1), DEAD_FRAME_NS));
        this.state = BrainState.PATROL;
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
        this.attackStartedAtNs = -1L;
        this.attackCooldownUntilNs = 0L;
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
        if (state == BrainState.DEATH) {
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
        if (activeEntityTarget != null && activeEntityTarget.isDead()) {
            activeEntityTarget = null;
        }

        Player playerTarget = resolvePlayerTarget(player, nowNs);
        if (playerTarget != null) {
            activeEntityTarget = playerTarget;
            if (activeBuildTarget != null && activeBuildTarget.isAlive() && !canMoveDirectlyTo(playerTarget.getCenterX(), playerTarget.getCenterY())) {
                runTowardBuildTarget(activeBuildTarget, nowNs, worldWidth, worldHeight);
                return;
            }
            activeBuildTarget = null;
            if (tryStartAttack(playerTarget, null, nowNs, isNight)) {
                return;
            }
            state = BrainState.CHASE;
            runTowardEntity(playerTarget, nowNs, worldWidth, worldHeight, isNight);
            return;
        }

        activeEntityTarget = null;
        activeBuildTarget = null;
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

    private boolean canDetectPlayer(Player player) {
        return intersectsDetectionRange(player);
    }

    private Player resolvePlayerTarget(Player player, long nowNs) {
        if (player == null || !player.isAlive()) {
            aggroPlayer = null;
            return null;
        }
        if (canDetectPlayer(player)) {
            aggroPlayer = player;
            return player;
        }
        if (aggroPlayer == player && isInsideDisengageRange(player)) {
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
        aggroPlayer = player;
        activeEntityTarget = player;
        if (state != BrainState.ATTACK && intersectsDetectionRange(player)) {
            state = BrainState.CHASE;
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
        faceTarget(target);
        state = BrainState.CHASE;
        animationState = AnimationState.RUN;
        currentMoveSpeed = RUN_SPEED;
        Point2D approachPoint = selectAttackApproachPoint(target, ATTACK_TWO);
        boolean moved = followPathToPoint(approachPoint.getX(), approachPoint.getY(), nowNs, worldWidth, worldHeight, true);
        if (!moved && aggressive && worldQuery != null) {
            BuildObject wallTarget = worldQuery.findNearestWallToAttack(this, target.getCenterX(), target.getCenterY(), DETECTION_RANGE);
            if (wallTarget != null && wallTarget.isAlive()) {
                activeBuildTarget = wallTarget;
                runTowardBuildTarget(wallTarget, nowNs, worldWidth, worldHeight);
                return;
            }
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
        activeBuildTarget = target;
        faceTarget(target.getCenterX(), target.getCenterY());
        state = BrainState.CHASE;
        animationState = AnimationState.RUN;
        currentMoveSpeed = RUN_SPEED;
        if (tryStartAttack(null, null, nowNs, true)) {
            return;
        }
        Point2D approachPoint = selectAttackApproachPoint(target, ATTACK_TWO);
        followPathToPoint(approachPoint.getX(), approachPoint.getY(), nowNs, worldWidth, worldHeight, false);
        faceTarget(target.getCenterX(), target.getCenterY());
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
        if (entityTarget != null) {
            faceTarget(entityTarget);
        } else if (baseTarget != null) {
            faceTarget(baseTarget.getCenterX(), baseTarget.getCenterY());
        } else if (buildTarget != null) {
            faceTarget(buildTarget.getCenterX(), buildTarget.getCenterY());
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
        return startAttack(profile, entityTarget != null ? entityTarget : baseTarget, buildTarget, nowNs);
    }

    private AttackProfile chooseAttackProfile(Entity entityTarget, BaseCamp baseTarget, BuildObject buildTarget, boolean isNight) {
        if (entityTarget instanceof Player) {
            return ATTACK_TWO;
        }
        if (buildTarget != null) {
            return ATTACK_TWO;
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
            state = BrainState.PATROL;
            animationState = AnimationState.IDLE;
            return;
        }
        SpriteAnimation animation = currentAnimation();
        boolean attackFinished = animation.updateOnce(nowNs);
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
                if (intersectsBuildAttackRange(buildTarget, activeAttackProfile) && worldQuery != null && worldQuery.damageWall(this, buildTarget, nowNs)) {
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
            state = BrainState.CHASE;
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
            startAttack(ATTACK_TWO, chaseTarget, null, nowNs);
            return;
        }
        if (chaseTarget != null && isInsideDisengageRange(chaseTarget)) {
            activeEntityTarget = chaseTarget;
            aggroPlayer = chaseTarget;
            activeBuildTarget = null;
            state = BrainState.CHASE;
            animationState = AnimationState.RUN;
            clearPath();
            return;
        }
        clearAttackTarget();
        state = BrainState.PATROL;
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
        if (canMoveDirectlyTo(targetX, targetY)) {
            clearPath();
            boolean movedDirectly = moveToward(targetX, targetY, worldWidth, worldHeight);
            if (movedDirectly) {
                blockedSinceNs = 0L;
                updateStuckSample(nowNs);
                currentAnimation().update(System.nanoTime(), true);
            }
            return movedDirectly;
        }
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
            recoverFromBlockedPath(targetX, targetY);
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
        stuckSampleAtNs = nowNs;
        stuckSampleX = getCenterX();
        stuckSampleY = getCenterY();
        if (moved < STUCK_MOVE_EPSILON) {
            recoverFromBlockedPath(lastPathTargetX, lastPathTargetY);
        }
    }

    private void recoverFromBlockedPath(double targetX, double targetY) {
        animationState = state == BrainState.CHASE ? AnimationState.RUN : AnimationState.IDLE;
        blockedSinceNs = 0L;
        clearPath();
        chooseSideStepWaypoint(targetX, targetY);
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
        if (target == null) {
            return null;
        }
        double centerX = target.getCollisionX() + target.getCollisionWidth() * 0.5;
        double centerY = target.getCollisionY() + target.getCollisionHeight() * 0.5;
        double startRadius = Math.max(18.0, Math.max(getCollisionWidth(), getCollisionHeight()) * 1.15);
        double bestScore = Double.POSITIVE_INFINITY;
        Point2D best = null;
        for (int ring = 0; ring < 4; ring++) {
            double radius = startRadius + ring * 10.0;
            int samples = 16 + ring * 8;
            for (int i = 0; i < samples; i++) {
                double angle = (Math.PI * 2.0 * i) / samples;
                double candidateX = centerX + Math.cos(angle) * radius;
                double candidateY = centerY + Math.sin(angle) * radius;
                if (!canStandCenteredAt(candidateX, candidateY)) {
                    continue;
                }
                double score = distanceTo(candidateX, candidateY) + ring * 8.0;
                if (score >= bestScore) {
                    continue;
                }
                best = new Point2D(candidateX, candidateY);
                bestScore = score;
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private Point2D selectAttackApproachPoint(BuildObject target, AttackProfile profile) {
        if (target == null || profile == null) {
            return new Point2D(getCenterX(), getCenterY());
        }
        double bodyWidth = getCollisionWidth();
        double bodyHeight = getCollisionHeight();
        double hitboxWidth = profile.hitboxWidth();
        double hitboxHeight = profile.hitboxHeight();
        double overlap = 2.0;
        double targetLeft = target.getCollisionX();
        double targetRight = target.getCollisionX() + target.getCollisionWidth();
        double targetTop = target.getCollisionY();
        double targetBottom = target.getCollisionY() + target.getCollisionHeight();
        double targetCenterX = target.getCollisionX() + target.getCollisionWidth() * 0.5;
        double targetCenterY = target.getCollisionY() + target.getCollisionHeight() * 0.5;
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
        graphicsContext.fillText(debugStateName(), x - cameraX, y - cameraY - 5.0);
        graphicsContext.restore();
    }

    private String debugStateName() {
        return state.name();
    }

    private void transitionToDeath() {
        state = BrainState.DEATH;
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

    private record Node(int x, int y) {
    }

    private record PathNode(Node node, double score) {
    }

    private record AttackArea(double centerX, double centerY, double radiusX, double radiusY) {
    }
}
