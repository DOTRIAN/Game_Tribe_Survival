package entity;

import animation.SpriteAnimation;
import animation.SpriteSheetLoader;
import buildsystem.object.BuildObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import system.DamageSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class WolfEnemy extends Enemy {
    public enum State {
        PATROL,
        CHASE_PLAYER,
        RETURN_HOME,
        NIGHT_HUNT,
        ATTACKING,
        DYING
    }

    @FunctionalInterface
    public interface MovementValidator {
        boolean canOccupy(WolfEnemy enemy, double x, double y, double width, double height);
    }

    public interface WorldQuery {
        BuildObject findNearestWallToAttack(WolfEnemy enemy, double maxDistance);

        boolean damageWall(WolfEnemy enemy, BuildObject wall, long nowNs);
    }

    private static final int FRAME_COUNT = 6;
    private static final long WALK_FRAME_NS = 95_000_000L;
    private static final long ATTACK_FRAME_NS = 90_000_000L;
    private static final long DEATH_FRAME_NS = 120_000_000L;
    private static final long ATTACK_COOLDOWN_NS = 1_000_000_000L;
    private static final double MOVE_SPEED = 1.5;
    private static final double DETECTION_RANGE = 180.0;
    private static final double ATTACK_RANGE = 32.0;
    private static final double PATROL_RADIUS = 120.0;
    private static final double MAX_CHASE_DISTANCE_DAY = 350.0;
    private static final double WALL_ATTACK_SEARCH_RANGE = 86.0;
    private static final long WALL_STUCK_THRESHOLD_NS = 950_000_000L;
    private static final double SIDE_FACING_FLIP_THRESHOLD = 6.0;
    private static final int MAX_HP = 30;
    private static final int DAMAGE = 5;

    private final SpriteAnimation walkDownAnimation;
    private final SpriteAnimation walkUpAnimation;
    private final SpriteAnimation walkSideAnimation;
    private final SpriteAnimation attackDownAnimation;
    private final SpriteAnimation attackUpAnimation;
    private final SpriteAnimation attackSideAnimation;
    private final SpriteAnimation deathDownAnimation;
    private final SpriteAnimation deathUpAnimation;
    private final SpriteAnimation deathSideAnimation;
    private final MovementValidator movementValidator;
    private final WorldQuery worldQuery;
    private final Random random;

    private State state;
    private double homeX;
    private double homeY;
    private double patrolTargetX;
    private double patrolTargetY;
    private Direction facingDirection;
    private boolean removeFromWorld;
    private long stuckSinceNs;
    private long lastPatrolRetargetAtNs;
    private Entity activeEntityTarget;
    private BuildObject activeWallTarget;
    private boolean attackDamageAppliedThisCycle;

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
                MOVE_SPEED,
                MAX_HP,
                DAMAGE,
                ATTACK_COOLDOWN_NS,
                asset("D_Walk"),
                FRAME_COUNT,
                1,
                WALK_FRAME_NS,
                asset("D_Walk"),
                FRAME_COUNT,
                1,
                WALK_FRAME_NS
        );
        this.walkDownAnimation = new SpriteAnimation(SpriteSheetLoader.loadGrid(asset("D_Walk"), FRAME_COUNT, 1), WALK_FRAME_NS);
        this.walkUpAnimation = new SpriteAnimation(SpriteSheetLoader.loadGrid(asset("U_Walk"), FRAME_COUNT, 1), WALK_FRAME_NS);
        this.walkSideAnimation = new SpriteAnimation(SpriteSheetLoader.loadGrid(asset("S_Walk"), FRAME_COUNT, 1), WALK_FRAME_NS);
        this.attackDownAnimation = new SpriteAnimation(SpriteSheetLoader.loadGrid(asset("D_Attack"), FRAME_COUNT, 1), ATTACK_FRAME_NS);
        this.attackUpAnimation = new SpriteAnimation(SpriteSheetLoader.loadGrid(asset("U_Attack"), FRAME_COUNT, 1), ATTACK_FRAME_NS);
        this.attackSideAnimation = new SpriteAnimation(SpriteSheetLoader.loadGrid(asset("S_Attack"), FRAME_COUNT, 1), ATTACK_FRAME_NS);
        this.deathDownAnimation = new SpriteAnimation(SpriteSheetLoader.loadGrid(asset("D_Death"), FRAME_COUNT, 1), DEATH_FRAME_NS);
        this.deathUpAnimation = new SpriteAnimation(SpriteSheetLoader.loadGrid(asset("U_Death"), FRAME_COUNT, 1), DEATH_FRAME_NS);
        this.deathSideAnimation = new SpriteAnimation(SpriteSheetLoader.loadGrid(asset("S_Death"), FRAME_COUNT, 1), DEATH_FRAME_NS);
        this.movementValidator = movementValidator;
        this.worldQuery = worldQuery;
        this.random = random == null ? new Random() : random;
        this.state = State.PATROL;
        this.homeX = getCenterX();
        this.homeY = getCenterY();
        this.patrolTargetX = homeX;
        this.patrolTargetY = homeY;
        this.facingDirection = Direction.DOWN;
        this.removeFromWorld = false;
        this.stuckSinceNs = 0L;
        this.lastPatrolRetargetAtNs = 0L;
        this.activeEntityTarget = null;
        this.activeWallTarget = null;
        this.attackDamageAppliedThisCycle = false;
        pickNextPatrolTarget(System.nanoTime());
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
        if (state == State.DYING) {
            if (currentDeathAnimation().updateOnce(nowNs)) {
                removeFromWorld = true;
            }
            return;
        }
        if (!isAlive()) {
            state = State.DYING;
            resetDeathAnimation();
            return;
        }

        if (activeWallTarget != null && !activeWallTarget.isAlive()) {
            activeWallTarget = null;
        }

        if (activeWallTarget != null) {
            handleWallAttack(nowNs, worldWidth, worldHeight);
            return;
        }

        activeEntityTarget = resolvePrimaryTarget(isNight, player, baseCamp);
        if (activeEntityTarget != null && isWithinAttackRange(activeEntityTarget)) {
            state = State.ATTACKING;
            updateFacing(activeEntityTarget.getCenterX() - getCenterX(), activeEntityTarget.getCenterY() - getCenterY());
            updateAttackAnimation(nowNs);
            applyAttackToEntityIfReady(activeEntityTarget, nowNs);
            return;
        }

        if (state == State.ATTACKING) {
            resetAttackAnimation();
            attackDamageAppliedThisCycle = false;
        }

        if (isNight) {
            handleNightBehavior(nowNs, player, baseCamp, worldWidth, worldHeight);
            return;
        }
        handleDayBehavior(nowNs, player, worldWidth, worldHeight);
    }

    @Override
    public void update(long now, Player player, double worldWidth, double worldHeight) {
        // Wolf AI duoc dieu khien boi WolfSpawnManager.
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
            resetDeathAnimation();
            activeEntityTarget = null;
            activeWallTarget = null;
            attackDamageAppliedThisCycle = false;
        }
    }

    @Override
    public void draw(GraphicsContext graphicsContext, double cameraX, double cameraY, long nowNs) {
        if (removeFromWorld) {
            return;
        }
        Image frame = currentFrame();
        double screenX = Math.round(x - cameraX);
        double screenY = Math.round(y - cameraY);
        if (frame == null || frame.isError()) {
            graphicsContext.setFill(Color.DARKOLIVEGREEN);
            graphicsContext.fillOval(screenX, screenY, width, height);
            return;
        }
        if (!shouldFlipHorizontally()) {
            graphicsContext.drawImage(frame, screenX, screenY, width, height);
        } else {
            graphicsContext.save();
            graphicsContext.translate(screenX + width, screenY);
            graphicsContext.scale(-1, 1);
            graphicsContext.drawImage(frame, 0, 0, width, height);
            graphicsContext.restore();
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

    private void handleDayBehavior(long nowNs, Player player, double worldWidth, double worldHeight) {
        boolean playerDetected = player != null
                && player.isAlive()
                && distanceTo(player.getCenterX(), player.getCenterY()) <= DETECTION_RANGE
                && distanceToHome() <= MAX_CHASE_DISTANCE_DAY;
        if (playerDetected) {
            state = State.CHASE_PLAYER;
            moveTowardPoint(player.getCenterX(), player.getCenterY(), nowNs, worldWidth, worldHeight, false);
            return;
        }

        if (distanceToHome() > PATROL_RADIUS * 0.35) {
            state = State.RETURN_HOME;
            if (distanceTo(homeX, homeY) <= Math.max(10.0, speed * 2.0)) {
                pickNextPatrolTarget(nowNs);
                state = State.PATROL;
            } else {
                moveTowardPoint(homeX, homeY, nowNs, worldWidth, worldHeight, false);
                return;
            }
        }

        state = State.PATROL;
        if (distanceTo(patrolTargetX, patrolTargetY) <= Math.max(10.0, speed * 2.0)
                || nowNs - lastPatrolRetargetAtNs >= 2_400_000_000L) {
            pickNextPatrolTarget(nowNs);
        }
        moveTowardPoint(patrolTargetX, patrolTargetY, nowNs, worldWidth, worldHeight, false);
    }

    private void handleNightBehavior(long nowNs, Player player, BaseCamp baseCamp, double worldWidth, double worldHeight) {
        Entity preferredTarget = player != null
                && player.isAlive()
                && distanceTo(player.getCenterX(), player.getCenterY()) <= DETECTION_RANGE + 36.0
                ? player
                : (baseCamp != null && baseCamp.isAlive() ? baseCamp : null);
        if (preferredTarget == null) {
            state = State.RETURN_HOME;
            moveTowardPoint(homeX, homeY, nowNs, worldWidth, worldHeight, false);
            return;
        }
        state = preferredTarget instanceof Player ? State.CHASE_PLAYER : State.NIGHT_HUNT;
        moveTowardPoint(preferredTarget.getCenterX(), preferredTarget.getCenterY(), nowNs, worldWidth, worldHeight, true);
    }

    private void handleWallAttack(long nowNs, double worldWidth, double worldHeight) {
        if (activeWallTarget == null || !activeWallTarget.isAlive()) {
            activeWallTarget = null;
            return;
        }
        state = State.ATTACKING;
        updateFacing(activeWallTarget.getCenterX() - getCenterX(), activeWallTarget.getCenterY() - getCenterY());
        if (!isWithinAttackRange(activeWallTarget)) {
            moveTowardPoint(activeWallTarget.getCenterX(), activeWallTarget.getCenterY(), nowNs, worldWidth, worldHeight, true);
            return;
        }
        updateAttackAnimation(nowNs);
        if (attackDamageAppliedThisCycle || currentAttackAnimation().getCurrentFrameIndex() < 2 || currentAttackAnimation().getCurrentFrameIndex() > 3) {
            return;
        }
        if (!canAttackNow(nowNs)) {
            return;
        }
        if (worldQuery != null && worldQuery.damageWall(this, activeWallTarget, nowNs)) {
            markAttackNow(nowNs);
            attackDamageAppliedThisCycle = true;
            if (!activeWallTarget.isAlive()) {
                activeWallTarget = null;
            }
        }
    }

    private void moveTowardPoint(double targetX,
                                 double targetY,
                                 long nowNs,
                                 double worldWidth,
                                 double worldHeight,
                                 boolean canBreakWalls) {
        updateFacing(targetX - getCenterX(), targetY - getCenterY());
        boolean moved = attemptSmartMove(targetX, targetY, worldWidth, worldHeight);
        if (moved) {
            currentWalkAnimation().update(nowNs, true);
            stuckSinceNs = 0L;
            return;
        }
        currentWalkAnimation().update(nowNs, true);
        if (stuckSinceNs == 0L) {
            stuckSinceNs = nowNs;
            return;
        }
        if (!canBreakWalls || nowNs - stuckSinceNs < WALL_STUCK_THRESHOLD_NS || worldQuery == null) {
            return;
        }
        activeWallTarget = worldQuery.findNearestWallToAttack(this, WALL_ATTACK_SEARCH_RANGE);
        if (activeWallTarget != null) {
            resetAttackAnimation();
            attackDamageAppliedThisCycle = false;
        }
    }

    private Entity resolvePrimaryTarget(boolean isNight, Player player, BaseCamp baseCamp) {
        if (isNight) {
            if (player != null && player.isAlive() && distanceTo(player.getCenterX(), player.getCenterY()) <= DETECTION_RANGE + 36.0) {
                return player;
            }
            return baseCamp != null && baseCamp.isAlive() ? baseCamp : null;
        }
        if (player != null
                && player.isAlive()
                && distanceTo(player.getCenterX(), player.getCenterY()) <= DETECTION_RANGE
                && distanceToHome() <= MAX_CHASE_DISTANCE_DAY) {
            return player;
        }
        return null;
    }

    private boolean attemptSmartMove(double targetX, double targetY, double worldWidth, double worldHeight) {
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= 0.0001) {
            return false;
        }
        double scale = Math.min(speed, distance);
        double moveX = (dx / distance) * scale;
        double moveY = (dy / distance) * scale;

        if (tryMove(moveX, moveY, worldWidth, worldHeight)) {
            return true;
        }
        if (tryAxisSequence(moveX, moveY, worldWidth, worldHeight)) {
            return true;
        }
        if (tryAxisSequence(moveY, moveX, worldWidth, worldHeight, true)) {
            return true;
        }

        double[][] rotated = {
                rotate(moveX, moveY, 32.0),
                rotate(moveX, moveY, -32.0),
                rotate(moveX, moveY, 58.0),
                rotate(moveX, moveY, -58.0),
                {moveX * 0.75, moveY * 0.75}
        };
        for (double[] candidate : rotated) {
            if (tryMove(candidate[0], candidate[1], worldWidth, worldHeight)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryAxisSequence(double moveX, double moveY, double worldWidth, double worldHeight) {
        return tryAxisSequence(moveX, moveY, worldWidth, worldHeight, false);
    }

    private boolean tryAxisSequence(double first, double second, double worldWidth, double worldHeight, boolean swapAxes) {
        double startX = x;
        double startY = y;
        double firstMoveX = swapAxes ? 0.0 : first;
        double firstMoveY = swapAxes ? first : 0.0;
        if (!tryMove(firstMoveX, firstMoveY, worldWidth, worldHeight)) {
            return false;
        }
        double secondMoveX = swapAxes ? second : 0.0;
        double secondMoveY = swapAxes ? 0.0 : second;
        if (!tryMove(secondMoveX, secondMoveY, worldWidth, worldHeight)) {
            // Giu lai buoc dau tien neu da thoat duoc vat can.
            if (x == startX && y == startY) {
                return false;
            }
        }
        return x != startX || y != startY;
    }

    private boolean tryMove(double moveX, double moveY, double worldWidth, double worldHeight) {
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
        clampPosition(0, 0, worldWidth, worldHeight);
        return true;
    }

    private void applyAttackToEntityIfReady(Entity target, long nowNs) {
        if (target == null || target.isDead()) {
            return;
        }
        if (attackDamageAppliedThisCycle || currentAttackAnimation().getCurrentFrameIndex() < 2 || currentAttackAnimation().getCurrentFrameIndex() > 3) {
            return;
        }
        if (!canAttackNow(nowNs) || !isWithinAttackRange(target)) {
            return;
        }
        DamageSystem.applyDamage(this, target, damage, nowNs);
        markAttackNow(nowNs);
        attackDamageAppliedThisCycle = true;
    }

    private void updateAttackAnimation(long nowNs) {
        currentAttackAnimation().update(nowNs, true);
        if (currentAttackAnimation().getCurrentFrameIndex() == 0) {
            attackDamageAppliedThisCycle = false;
        }
    }

    private void pickNextPatrolTarget(long nowNs) {
        lastPatrolRetargetAtNs = nowNs;
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = 18.0 + random.nextDouble() * Math.max(24.0, PATROL_RADIUS - 18.0);
        patrolTargetX = homeX + Math.cos(angle) * radius;
        patrolTargetY = homeY + Math.sin(angle) * radius;
    }

    private boolean isWithinAttackRange(Entity target) {
        if (target == null) {
            return false;
        }
        double dx = edgeDistance(getCollisionX(), getCollisionX() + getCollisionWidth(), target.getCollisionX(), target.getCollisionX() + target.getCollisionWidth());
        double dy = edgeDistance(getCollisionY(), getCollisionY() + getCollisionHeight(), target.getCollisionY(), target.getCollisionY() + target.getCollisionHeight());
        return Math.sqrt(dx * dx + dy * dy) <= ATTACK_RANGE;
    }

    private boolean isWithinAttackRange(BuildObject object) {
        if (object == null || !object.isAlive()) {
            return false;
        }
        double dx = edgeDistance(getCollisionX(), getCollisionX() + getCollisionWidth(), object.getCollisionX(), object.getCollisionX() + object.getCollisionWidth());
        double dy = edgeDistance(getCollisionY(), getCollisionY() + getCollisionHeight(), object.getCollisionY(), object.getCollisionY() + object.getCollisionHeight());
        return Math.sqrt(dx * dx + dy * dy) <= ATTACK_RANGE;
    }

    private void updateFacing(double dx, double dy) {
        if (Math.abs(dx) > Math.abs(dy)) {
            if (Math.abs(dx) >= SIDE_FACING_FLIP_THRESHOLD
                    || (facingDirection != Direction.LEFT && facingDirection != Direction.RIGHT)) {
                facingDirection = dx >= 0 ? Direction.RIGHT : Direction.LEFT;
            }
            return;
        }
        facingDirection = dy >= 0 ? Direction.DOWN : Direction.UP;
    }

    private double distanceToHome() {
        return distanceTo(homeX, homeY);
    }

    private double distanceTo(double targetX, double targetY) {
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
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

    private double[] rotate(double x, double y, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new double[]{x * cos - y * sin, x * sin + y * cos};
    }

    private Image currentFrame() {
        return switch (state) {
            case ATTACKING -> currentAttackAnimation().getCurrentFrame();
            case DYING -> currentDeathAnimation().getCurrentFrame();
            case PATROL, CHASE_PLAYER, RETURN_HOME, NIGHT_HUNT -> currentWalkAnimation().getCurrentFrame();
        };
    }

    private SpriteAnimation currentWalkAnimation() {
        return switch (facingDirection) {
            case UP -> walkUpAnimation;
            case DOWN -> walkDownAnimation;
            case LEFT, RIGHT -> walkSideAnimation;
        };
    }

    private SpriteAnimation currentAttackAnimation() {
        return switch (facingDirection) {
            case UP -> attackUpAnimation;
            case DOWN -> attackDownAnimation;
            case LEFT, RIGHT -> attackSideAnimation;
        };
    }

    private SpriteAnimation currentDeathAnimation() {
        return switch (facingDirection) {
            case UP -> deathUpAnimation;
            case DOWN -> deathDownAnimation;
            case LEFT, RIGHT -> deathSideAnimation;
        };
    }

    private void resetAttackAnimation() {
        attackDownAnimation.reset();
        attackUpAnimation.reset();
        attackSideAnimation.reset();
    }

    private void resetDeathAnimation() {
        deathDownAnimation.reset();
        deathUpAnimation.reset();
        deathSideAnimation.reset();
    }

    private boolean shouldFlipHorizontally() {
        return facingDirection == Direction.LEFT;
    }

    private static String asset(String fileName) {
        Path local = Paths.get("assets", "wolf", fileName + ".png");
        if (Files.exists(local)) {
            return local.toUri().toString();
        }
        return local.toUri().toString();
    }
}
