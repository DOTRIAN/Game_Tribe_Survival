package entity;

import animation.PngSequenceLoader;
import animation.SpriteAnimation;
import buildsystem.object.BuildObject;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import system.DamageSystem;

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

    public interface WorldQuery {
        int getTileWidth();

        int getTileHeight();

        BuildObject findNearestWallToAttack(GolemEnemy enemy, double towardX, double towardY, double maxDistance);

        boolean damageWall(GolemEnemy enemy, BuildObject wall, long nowNs);
    }

    private record Node(int x, int y) {
    }

    private record PathNode(Node node, double score) {
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
    private static final long PATH_RECALC_NS = 1_500_000_000L;
    private static final long STUCK_REPATH_NS = 650_000_000L;
    private static final long STUCK_SAMPLE_NS = 700_000_000L;
    private static final double STUCK_MOVE_EPSILON = 1.8;
    private static final double AGGRO_RANGE = 260.0;
    private static final double LEASH_EXTRA = 90.0;
    private static final double WALL_SEARCH_RANGE = 190.0;
    private static final double DIRECT_TARGET_RECALC_DISTANCE = 28.0;
    private static final double PATH_POINT_REACHED = 8.0;
    private static final int PATH_MAX_EXPANSIONS = 800;
    private static final double MOVE_SPEED = 0.52;
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
    private final WorldQuery worldQuery;
    private final List<Point2D> currentPath;
    private final GolemMode mode;

    private State state;
    private State resumeStateAfterHurt;
    private Entity currentTarget;
    private BuildObject currentBuildTarget;
    private boolean facingRight;
    private AttackDirection attackDirection;
    private boolean removeFromWorld;
    private long lastAttackAtNs;
    private long lastHurtAtNs;
    private long lastPathComputeAtNs;
    private long blockedSinceNs;
    private long stuckSampleAtNs;
    private double lastPathTargetX;
    private double lastPathTargetY;
    private double stuckSampleX;
    private double stuckSampleY;
    private int currentPathIndex;
    private boolean attackDamageAppliedThisCycle;

    public GolemEnemy(double x, double y, MovementValidator movementValidator, WorldQuery worldQuery) {
        this(x, y, movementValidator, worldQuery, GolemMode.NORMAL);
    }

    public GolemEnemy(double x, double y, MovementValidator movementValidator, WorldQuery worldQuery, GolemMode mode) {
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
        this.worldQuery = worldQuery;
        this.currentPath = new ArrayList<>();
        this.mode = mode == null ? GolemMode.NORMAL : mode;
        this.state = State.WALKING_TO_BASE;
        this.resumeStateAfterHurt = State.WALKING_TO_BASE;
        this.currentTarget = null;
        this.currentBuildTarget = null;
        this.facingRight = true;
        this.attackDirection = AttackDirection.RIGHT;
        this.removeFromWorld = false;
        this.lastAttackAtNs = -ATTACK_COOLDOWN_NS;
        this.lastHurtAtNs = -HURT_RESTART_GUARD_NS;
        this.lastPathComputeAtNs = 0L;
        this.blockedSinceNs = 0L;
        this.stuckSampleAtNs = 0L;
        this.lastPathTargetX = Double.NaN;
        this.lastPathTargetY = Double.NaN;
        this.stuckSampleX = getCenterX();
        this.stuckSampleY = getCenterY();
        this.currentPathIndex = 0;
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
        if (preferredTarget == null) {
            enterIdle(nowNs);
            return;
        }

        if (currentBuildTarget != null && !currentBuildTarget.isAlive()) {
            currentBuildTarget = null;
        }

        if (currentBuildTarget != null && shouldDropBuildTargetForEntity(preferredTarget)) {
            currentBuildTarget = null;
            clearPath();
        }

        if (currentBuildTarget != null) {
            updateBuildTarget(nowNs, preferredTarget, worldWidth, worldHeight);
            return;
        }

        if (mode == GolemMode.WALL_BREAKER && worldQuery != null) {
            BuildObject wall = worldQuery.findNearestWallToAttack(this, preferredTarget.getCenterX(), preferredTarget.getCenterY(), WALL_SEARCH_RANGE);
            if (wall != null) {
                currentBuildTarget = wall;
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
            if (!isWithinAttackRange(currentBuildTarget) || worldQuery == null || !worldQuery.damageWall(this, currentBuildTarget, nowNs)) {
                return false;
            }
            if (!currentBuildTarget.isAlive()) {
                currentBuildTarget = null;
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

    public BuildObject getCurrentBuildTarget() {
        return currentBuildTarget;
    }

    public GolemMode getMode() {
        return mode;
    }

    public double getAggroRange() {
        return AGGRO_RANGE;
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
        faceTarget(target.getCenterX(), target.getCenterY());
        if (isWithinAttackRange(target)) {
            startAttack();
            updateAttack(nowNs);
            return;
        }

        State chaseState = target instanceof BaseCamp ? State.WALKING_TO_BASE : State.CHASE_TARGET;
        if (state != chaseState) {
            state = chaseState;
        }
        Point2D approach = selectEntityApproachPoint(target);
        boolean moved = followPathToPoint(approach.getX(), approach.getY(), nowNs, worldWidth, worldHeight);
        if (!moved && shouldBreakWallToward(target)) {
            currentBuildTarget = worldQuery.findNearestWallToAttack(this, target.getCenterX(), target.getCenterY(), WALL_SEARCH_RANGE);
            if (currentBuildTarget != null) {
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
        faceTarget(currentBuildTarget.getCenterX(), currentBuildTarget.getCenterY());
        if (isWithinAttackRange(currentBuildTarget)) {
            startAttack();
            updateAttack(nowNs);
            return;
        }

        state = State.CHASE_TARGET;
        Point2D approach = selectBuildApproachPoint(currentBuildTarget);
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
            attackingAnimation.reset();
            attackDamageAppliedThisCycle = false;
        }
    }

    private void enterIdle(long nowNs) {
        currentTarget = null;
        currentBuildTarget = null;
        clearPath();
        state = State.IDLE;
        idleAnimation.update(nowNs, true);
    }

    private boolean shouldDropBuildTargetForEntity(Entity target) {
        if (mode == GolemMode.WALL_BREAKER) {
            return false;
        }
        return target != null
                && target.isAlive()
                && !(target instanceof BaseCamp)
                && (isWithinAttackRange(target) || canMoveDirectlyTo(target.getCenterX(), target.getCenterY()));
    }

    private boolean shouldBreakWallToward(Entity target) {
        return worldQuery != null
                && target != null
                && target.isAlive()
                && !(target instanceof BaseCamp)
                && !canMoveDirectlyTo(target.getCenterX(), target.getCenterY());
    }

    private Entity choosePreferredTarget(BaseCamp baseCamp, Player player, Iterable<FriendlyArcher> friendlies) {
        Entity nearestAggroTarget = nearestAggroTarget(player, friendlies);
        if (nearestAggroTarget != null) {
            return nearestAggroTarget;
        }
        if (currentTarget != null && currentTarget.isAlive() && !(currentTarget instanceof BaseCamp)
                && distanceTo(currentTarget.getCenterX(), currentTarget.getCenterY()) <= AGGRO_RANGE + LEASH_EXTRA) {
            return currentTarget;
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
            nearest = currentTarget;
            nearestDistance = Math.min(AGGRO_RANGE + LEASH_EXTRA, distanceTo(currentTarget.getCenterX(), currentTarget.getCenterY()));
        }
        if (player != null && player.isAlive()) {
            double distance = distanceTo(player.getCenterX(), player.getCenterY());
            if (distance <= nearestDistance || distance <= AGGRO_RANGE) {
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
        if (canMoveDirectlyTo(targetX, targetY)) {
            clearPath();
            boolean movedDirectly = moveToward(targetX, targetY, worldWidth, worldHeight);
            if (movedDirectly) {
                blockedSinceNs = 0L;
                updateStuckSample(nowNs);
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
            return true;
        }
        if (blockedSinceNs == 0L) {
            blockedSinceNs = nowNs;
        } else if (nowNs - blockedSinceNs >= STUCK_REPATH_NS) {
            recoverFromBlockedPath(targetX, targetY);
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
        stuckSampleAtNs = nowNs;
        stuckSampleX = getCenterX();
        stuckSampleY = getCenterY();
        if (moved < STUCK_MOVE_EPSILON) {
            recoverFromBlockedPath(lastPathTargetX, lastPathTargetY);
        }
    }

    private void recoverFromBlockedPath(double targetX, double targetY) {
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
                double tentative = gScore.getOrDefault(current, Double.POSITIVE_INFINITY)
                        + distance(current.x(), current.y(), neighbor.x(), neighbor.y());
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
        return canStandCenteredAt(centerX, centerY);
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
