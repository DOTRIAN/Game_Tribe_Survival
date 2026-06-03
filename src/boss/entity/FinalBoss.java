package boss.entity;

import animation.SpriteAnimation;
import boss.BossSpriteLoader;
import boss.BossState;
import entity.Enemy;
import entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import system.DamageSystem;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class FinalBoss extends Enemy {
    @FunctionalInterface
    public interface BarrageEmitter {
        void emit(double originX, double originY, double dirX, double dirY, int orbCount, long nowNs);
    }

    private enum AttackDirection {
        LEFT,
        RIGHT,
        UP,
        DOWN
    }

    private static final String STATE_ROOT = "assets/boss_assets/fire_boss/boss_demon_slime_FREE_v1.0/state_boss";
    private static final String PLACEHOLDER_FRAME = STATE_ROOT + "/01_demon_idle/demon_idle_1.png";
    private static final long IDLE_FRAME_NS = 170_000_000L;
    private static final long WALK_FRAME_NS = 90_000_000L;
    private static final long CLEAVE_FRAME_NS = 75_000_000L;
    private static final long BARRAGE_CHARGE_NS = 800_000_000L;
    private static final long BARRAGE_INTERVAL_NS = 10_000_000_000L;
    private static final long TAKE_HIT_FRAME_NS = 85_000_000L;
    private static final long DEATH_FRAME_NS = 95_000_000L;
    private static final long ATTACK_COOLDOWN_NS = 1_600_000_000L;
    private static final double FACE_FLIP_THRESHOLD_X = 18.0;
    private static final double HP_BAR_WIDTH = 180.0;
    private static final double HP_BAR_HEIGHT = 12.0;
    private static final double FOOT_CENTER_Y_RATIO = 0.88;
    private static final double ATTACK_OFFSET = 14.0;
    private static final double ATTACK_RADIUS_X = 14.0;
    private static final double ATTACK_RADIUS_Y = 8.0;
    private static final double APPROACH_SNAP_DISTANCE = 3.0;
    private static final int MAX_HP = 140;
    private static final int CLEAVE_DAMAGE = 14;
    private static final long BARRAGE_FLASH_NS = 220_000_000L;
    private static final Map<BossState, Image[]> RAW_FRAMES = loadRawFrames();

    private final Map<BossState, SpriteAnimation> animations;
    private BossState state;
    private Image currentFrame;
    private boolean facingRight;
    private AttackDirection attackDirection;
    private long lastAttackAtNs;
    private long nextBarrageAtNs;
    private long barrageStartedAtNs;
    private long encounterStartedAtNs;
    private long barrageFlashUntilNs;
    private boolean cleaveDamageApplied;
    private boolean deathAnimationFinished;
    private boolean debugEnabled;
    private double lastAttackDirX;
    private double lastAttackDirY;
    private BarrageEmitter barrageEmitter;

    public FinalBoss(double x, double y) {
        super(
                x,
                y,
                190,
                190,
                1.45,
                MAX_HP,
                CLEAVE_DAMAGE,
                ATTACK_COOLDOWN_NS,
                PLACEHOLDER_FRAME,
                1,
                1,
                IDLE_FRAME_NS,
                PLACEHOLDER_FRAME,
                1,
                1,
                IDLE_FRAME_NS
        );
        this.animations = buildAnimations();
        this.state = BossState.IDLE;
        this.currentFrame = resolveFrame(BossState.IDLE);
        this.facingRight = true;
        this.attackDirection = AttackDirection.LEFT;
        this.lastAttackAtNs = -ATTACK_COOLDOWN_NS;
        this.nextBarrageAtNs = -1L;
        this.barrageStartedAtNs = -1L;
        this.encounterStartedAtNs = -1L;
        this.barrageFlashUntilNs = -1L;
        this.cleaveDamageApplied = false;
        this.deathAnimationFinished = false;
        this.debugEnabled = false;
        this.lastAttackDirX = 0.0;
        this.lastAttackDirY = 1.0;
        this.barrageEmitter = null;
    }

    @Override
    protected double collisionInsetLeft(double width, double height) {
        return width * 0.24;
    }

    @Override
    protected double collisionInsetRight(double width, double height) {
        return width * 0.24;
    }

    @Override
    protected double collisionInsetTop(double width, double height) {
        return height * 0.34;
    }

    @Override
    protected double collisionInsetBottom(double width, double height) {
        return height * 0.08;
    }

    @Override
    public void update(long now, Player player, double worldWidth, double worldHeight) {
        if (player == null) {
            return;
        }
        if (encounterStartedAtNs < 0L) {
            encounterStartedAtNs = now;
            nextBarrageAtNs = now + BARRAGE_INTERVAL_NS;
        }

        if (hp <= 0 && state != BossState.DEATH) {
            transitionTo(BossState.DEATH);
        }

        switch (state) {
            case DEATH -> updateDeath(now);
            case TAKE_HIT -> updateTakeHit(now);
            case CLEAVE -> updateCleave(now, player);
            case BARRAGE -> updateBarrage(now, player);
            case IDLE, WALK -> updateMovementState(now, player, worldWidth, worldHeight);
        }
    }

    @Override
    public void takeDamage(int amount) {
        boolean wasAlive = isAlive();
        super.takeDamage(amount);
        if (!wasAlive) {
            return;
        }
        if (hp <= 0) {
            transitionTo(BossState.DEATH);
            return;
        }
    }

    @Override
    public void draw(GraphicsContext graphicsContext, double cameraX, double cameraY, long nowNs) {
        Image frame = currentFrame;
        double screenX = Math.round(x - cameraX);
        double screenY = Math.round(y - cameraY);
        if (frame == null || frame.isError()) {
            graphicsContext.setFill(Color.DARKRED);
            graphicsContext.fillRect(screenX, screenY, width, height);
        } else if (facingRight) {
            graphicsContext.drawImage(frame, screenX, screenY, width, height);
        } else {
            graphicsContext.save();
            graphicsContext.translate(screenX + width, screenY);
            graphicsContext.scale(-1, 1);
            graphicsContext.drawImage(frame, 0, 0, width, height);
            graphicsContext.restore();
        }

        if (isHitFlashActive(nowNs) && frame != null && !frame.isError()) {
            graphicsContext.save();
            graphicsContext.setGlobalBlendMode(javafx.scene.effect.BlendMode.SCREEN);
            graphicsContext.setGlobalAlpha(0.34);
            graphicsContext.setFill(Color.rgb(255, 96, 96));
            graphicsContext.fillOval(screenX + 26, screenY + 18, width - 52, height - 24);
            graphicsContext.restore();
        }

        if (nowNs <= barrageFlashUntilNs) {
            double progress = 1.0 - Math.max(0.0, (double) (barrageFlashUntilNs - nowNs) / BARRAGE_FLASH_NS);
            double flashRadius = 28.0 + progress * 38.0;
            graphicsContext.save();
            graphicsContext.setGlobalBlendMode(javafx.scene.effect.BlendMode.SCREEN);
            graphicsContext.setFill(Color.color(1.0, 0.52, 0.12, 0.34));
            graphicsContext.fillOval(
                    getCenterX() - cameraX - flashRadius,
                    getCenterY() - cameraY - flashRadius,
                    flashRadius * 2.0,
                    flashRadius * 2.0
            );
            graphicsContext.restore();
        }

        drawBossHealthBar(graphicsContext, screenX, screenY);
        if (debugEnabled) {
            drawDebug(graphicsContext, cameraX, cameraY);
        }
    }

    @Override
    public boolean shouldRender(long nowNs) {
        return currentFrame != null;
    }

    @Override
    public boolean shouldRemoveFromWorld() {
        return false;
    }

    @Override
    public String getEnemyType() {
        return "final_boss";
    }

    @Override
    public void tryAttackPlayer(Player player, long now) {
        // Boss dung he thong cleave rieng, khong dung logic tan cong mac dinh theo va cham than.
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public boolean isEncounterFinished() {
        return deathAnimationFinished;
    }

    public BossState getState() {
        return state;
    }

    public void setBarrageEmitter(BarrageEmitter barrageEmitter) {
        this.barrageEmitter = barrageEmitter;
    }

    public double getDebugFootCenterX() {
        return footCenterX();
    }

    public double getDebugFootCenterY() {
        return footCenterY();
    }

    public double getDebugAttackHitboxCenterX() {
        return attackHitboxCenterX();
    }

    public double getDebugAttackHitboxCenterY() {
        return attackHitboxCenterY();
    }

    public double getDebugAttackRadiusX() {
        return ATTACK_RADIUS_X;
    }

    public double getDebugAttackRadiusY() {
        return ATTACK_RADIUS_Y;
    }

    private void updateMovementState(long now, Player player, double worldWidth, double worldHeight) {
        double dx = player.getCenterX() - getCenterX();
        double dy = player.getCenterY() - getCenterY();
        updateAttackDirection(dx, dy);
        updateFacingDirection(dx);

        updateAttackVector(player);
        if (shouldStartBarrage(now)) {
            transitionTo(BossState.BARRAGE);
            barrageStartedAtNs = now;
            return;
        }
        Point2D desiredPosition = desiredBossPositionForCleave(player);
        boolean playerInsidePrimaryHitbox = intersectsCleaveHitbox(player);
        if (playerInsidePrimaryHitbox && now - lastAttackAtNs >= ATTACK_COOLDOWN_NS) {
            transitionTo(BossState.CLEAVE);
            return;
        }

        double moveDx = desiredPosition.getX() - x;
        double moveDy = desiredPosition.getY() - y;
        double moveDistance = Math.hypot(moveDx, moveDy);
        boolean moving = !playerInsidePrimaryHitbox && moveDistance > APPROACH_SNAP_DISTANCE;
        if (moving && moveDistance > 0.001) {
            double step = Math.min(speed, moveDistance);
            x += (moveDx / moveDistance) * step;
            y += (moveDy / moveDistance) * step;
            clampPosition(0, 0, worldWidth, worldHeight);
        } else if (!playerInsidePrimaryHitbox) {
            x = desiredPosition.getX();
            y = desiredPosition.getY();
            clampPosition(0, 0, worldWidth, worldHeight);
        }

        transitionTo(moving ? BossState.WALK : BossState.IDLE);
        SpriteAnimation animation = animationFor(state);
        animation.update(now, moving);
        currentFrame = animation.getCurrentFrame();
    }

    private void updateTakeHit(long now) {
        SpriteAnimation animation = animationFor(BossState.TAKE_HIT);
        boolean finished = animation.updateOnce(now);
        currentFrame = animation.getCurrentFrame();
        if (finished) {
            transitionTo(BossState.IDLE);
        }
    }

    private void updateCleave(long now, Player player) {
        SpriteAnimation animation = animationFor(BossState.CLEAVE);
        boolean finished = animation.updateOnce(now);
        currentFrame = animation.getCurrentFrame();

        updateAttackVector(player);
        int impactFrameStart = Math.max(0, animation.getFrameCount() - 2);
        int impactFrameEnd = Math.max(impactFrameStart, animation.getFrameCount() - 1);
        int currentFrameIndex = animation.getCurrentFrameIndex();
        if (!cleaveDamageApplied && currentFrameIndex >= impactFrameStart && currentFrameIndex <= impactFrameEnd) {
            cleaveDamageApplied = true;
            if (intersectsCleaveHitbox(player)) {
                DamageSystem.applyDamage(this, player, damage, now);
            }
        }

        if (!finished) {
            return;
        }
        lastAttackAtNs = now;
        transitionTo(BossState.IDLE);
    }

    private void updateBarrage(long now, Player player) {
        double dx = player == null ? 0.0 : player.getCenterX() - getCenterX();
        double dy = player == null ? 0.0 : player.getCenterY() - getCenterY();
        updateAttackDirection(dx, dy);
        updateFacingDirection(dx);
        updateAttackVector(player);

        SpriteAnimation animation = animationFor(BossState.BARRAGE);
        animation.update(now, true);
        currentFrame = animation.getCurrentFrame();

        if (barrageStartedAtNs < 0L) {
            barrageStartedAtNs = now;
        }
        if (now - barrageStartedAtNs < BARRAGE_CHARGE_NS) {
            return;
        }
        fireOrbBarrage(now);
        nextBarrageAtNs = now + BARRAGE_INTERVAL_NS;
        barrageStartedAtNs = -1L;
        transitionTo(BossState.IDLE);
    }

    private void updateDeath(long now) {
        SpriteAnimation animation = animationFor(BossState.DEATH);
        deathAnimationFinished = animation.updateOnce(now);
        currentFrame = animation.getCurrentFrame();
    }

    private void transitionTo(BossState nextState) {
        if (nextState == null) {
            return;
        }
        if (state != nextState) {
            state = nextState;
            animationFor(nextState).reset();
            if (nextState == BossState.CLEAVE) {
                cleaveDamageApplied = false;
            }
            if (nextState != BossState.BARRAGE) {
                barrageStartedAtNs = -1L;
            }
        }
        currentFrame = resolveFrame(nextState);
    }

    private SpriteAnimation animationFor(BossState targetState) {
        SpriteAnimation animation = animations.get(targetState);
        if (animation != null) {
            return animation;
        }
        return animations.get(BossState.IDLE);
    }

    private Image resolveFrame(BossState targetState) {
        SpriteAnimation animation = animationFor(targetState);
        return animation == null ? null : animation.getCurrentFrame();
    }

    private void updateAttackDirection(double dx, double dy) {
        if (Math.abs(dx) >= Math.abs(dy)) {
            attackDirection = dx < 0 ? AttackDirection.LEFT : AttackDirection.RIGHT;
            return;
        }
        attackDirection = dy < 0 ? AttackDirection.UP : AttackDirection.DOWN;
    }

    private void updateFacingDirection(double dx) {
        if (dx <= -FACE_FLIP_THRESHOLD_X) {
            facingRight = true;
            return;
        }
        if (dx >= FACE_FLIP_THRESHOLD_X) {
            facingRight = false;
        }
    }

    private void updateAttackVector(Player player) {
        double footX = footCenterX();
        double dirX = player == null ? 0.0 : player.getCenterX() - footX;
        if (Math.abs(dirX) < 0.0001) {
            return;
        }
        lastAttackDirX = dirX < 0 ? -1.0 : 1.0;
        lastAttackDirY = 0.0;
    }

    private double footCenterX() {
        return x + width * 0.5;
    }

    private double footCenterY() {
        return y + height * FOOT_CENTER_Y_RATIO;
    }

    private double attackHitboxCenterX() {
        return footCenterX() + lastAttackDirX * ATTACK_OFFSET;
    }

    private double attackHitboxCenterY() {
        return footCenterY();
    }

    private Point2D desiredBossPositionForCleave(Player player) {
        if (player == null) {
            return new Point2D(x, y);
        }
        double desiredX = player.getCenterX() - width * 0.5 - lastAttackDirX * ATTACK_OFFSET;
        double desiredY = player.getCenterY() - height * FOOT_CENTER_Y_RATIO - lastAttackDirY * ATTACK_OFFSET;
        return new Point2D(desiredX, desiredY);
    }

    private boolean intersectsCleaveHitbox(Player player) {
        if (player == null) {
            return false;
        }
        return intersectsEllipseAndRect(
                attackHitboxCenterX(),
                attackHitboxCenterY(),
                ATTACK_RADIUS_X,
                ATTACK_RADIUS_Y,
                player.getCollisionX(),
                player.getCollisionY(),
                player.getCollisionWidth(),
                player.getCollisionHeight()
        );
    }

    private boolean intersectsEllipseAndRect(double ellipseCenterX,
                                             double ellipseCenterY,
                                             double radiusX,
                                             double radiusY,
                                             double rectX,
                                             double rectY,
                                             double rectWidth,
                                             double rectHeight) {
        double nearestX = clamp(ellipseCenterX, rectX, rectX + rectWidth);
        double nearestY = clamp(ellipseCenterY, rectY, rectY + rectHeight);
        double dx = (nearestX - ellipseCenterX) / Math.max(0.0001, radiusX);
        double dy = (nearestY - ellipseCenterY) / Math.max(0.0001, radiusY);
        return dx * dx + dy * dy <= 1.0;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void drawDebug(GraphicsContext graphicsContext, double cameraX, double cameraY) {
        graphicsContext.save();
        graphicsContext.setLineWidth(1.2);
        graphicsContext.setStroke(Color.color(0.3, 0.8, 1.0, 0.95));
        graphicsContext.strokeRect(
                getCollisionX() - cameraX,
                getCollisionY() - cameraY,
                getCollisionWidth(),
                getCollisionHeight()
        );
        graphicsContext.setFill(Color.color(1.0, 0.15, 0.15, 0.16));
        graphicsContext.fillOval(
                attackHitboxCenterX() - ATTACK_RADIUS_X - cameraX,
                attackHitboxCenterY() - ATTACK_RADIUS_Y - cameraY,
                ATTACK_RADIUS_X * 2.0,
                ATTACK_RADIUS_Y * 2.0
        );
        graphicsContext.setStroke(Color.color(1.0, 0.1, 0.1, 0.98));
        graphicsContext.strokeOval(
                attackHitboxCenterX() - ATTACK_RADIUS_X - cameraX,
                attackHitboxCenterY() - ATTACK_RADIUS_Y - cameraY,
                ATTACK_RADIUS_X * 2.0,
                ATTACK_RADIUS_Y * 2.0
        );
        graphicsContext.setFill(Color.color(1.0, 0.95, 0.2, 0.95));
        graphicsContext.fillOval(
                footCenterX() - 3.0 - cameraX,
                footCenterY() - 3.0 - cameraY,
                6.0,
                6.0
        );
        graphicsContext.setFill(Color.color(1.0, 0.98, 0.78, 0.95));
        graphicsContext.fillText(
                state.name() + " dir=" + attackDirection + " face=" + (facingRight ? "R" : "L"),
                x - cameraX,
                y - cameraY - 6.0
        );
        graphicsContext.restore();
    }

    private Map<BossState, SpriteAnimation> buildAnimations() {
        Map<BossState, SpriteAnimation> map = new EnumMap<>(BossState.class);
        List<Image[]> sequences = RAW_FRAMES.values().stream().toList();
        int maxWidth = Math.max(1, BossSpriteLoader.findMaxWidth(sequences));
        int maxHeight = Math.max(1, BossSpriteLoader.findMaxHeight(sequences));
        for (BossState bossState : BossState.values()) {
            Image[] raw = RAW_FRAMES.getOrDefault(bossState, new Image[0]);
            Image[] normalized = BossSpriteLoader.normalizeFrames(raw, maxWidth, maxHeight);
            long frameDurationNs = frameDurationFor(bossState);
            map.put(bossState, new SpriteAnimation(normalized, frameDurationNs));
        }
        return map;
    }

    private long frameDurationFor(BossState bossState) {
        return switch (bossState) {
            case IDLE -> IDLE_FRAME_NS;
            case WALK -> WALK_FRAME_NS;
            case CLEAVE -> CLEAVE_FRAME_NS;
            case BARRAGE -> IDLE_FRAME_NS;
            case TAKE_HIT -> TAKE_HIT_FRAME_NS;
            case DEATH -> DEATH_FRAME_NS;
        };
    }

    private void drawBossHealthBar(GraphicsContext graphicsContext, double screenX, double screenY) {
        double hpRatio = Math.max(0.0, Math.min(1.0, hp / (double) maxHp));
        double barX = screenX + (width - HP_BAR_WIDTH) * 0.5;
        double barY = screenY - 22.0;
        graphicsContext.setFill(Color.color(0.08, 0.08, 0.1, 0.82));
        graphicsContext.fillRoundRect(barX, barY, HP_BAR_WIDTH, HP_BAR_HEIGHT, 8, 8);
        graphicsContext.setFill(Color.color(0.86, 0.16, 0.14, 0.96));
        graphicsContext.fillRoundRect(barX + 1.5, barY + 1.5, Math.max(0.0, (HP_BAR_WIDTH - 3.0) * hpRatio), HP_BAR_HEIGHT - 3.0, 6, 6);
        graphicsContext.setStroke(Color.color(1.0, 0.92, 0.72, 0.85));
        graphicsContext.strokeRoundRect(barX, barY, HP_BAR_WIDTH, HP_BAR_HEIGHT, 8, 8);
    }

    private static Map<BossState, Image[]> loadRawFrames() {
        Map<BossState, Image[]> frames = new EnumMap<>(BossState.class);
        frames.put(BossState.IDLE, BossSpriteLoader.loadSequence(STATE_ROOT + "/01_demon_idle"));
        frames.put(BossState.WALK, BossSpriteLoader.loadSequence(STATE_ROOT + "/02_demon_walk"));
        frames.put(BossState.CLEAVE, BossSpriteLoader.loadSequence(STATE_ROOT + "/03_demon_cleave"));
        frames.put(BossState.BARRAGE, BossSpriteLoader.loadSequence(STATE_ROOT + "/01_demon_idle"));
        frames.put(BossState.TAKE_HIT, BossSpriteLoader.loadSequence(STATE_ROOT + "/04_demon_take_hit"));
        frames.put(BossState.DEATH, BossSpriteLoader.loadSequence(STATE_ROOT + "/05_demon_death"));
        return frames;
    }

    private boolean shouldStartBarrage(long nowNs) {
        return state != BossState.BARRAGE
                && state != BossState.CLEAVE
                && state != BossState.TAKE_HIT
                && nextBarrageAtNs > 0L
                && nowNs >= nextBarrageAtNs;
    }

    private void fireOrbBarrage(long nowNs) {
        if (barrageEmitter == null) {
            return;
        }
        int orbCount = resolveBarrageOrbCount(nowNs);
        double[] forward = resolveBarrageForwardVector();
        barrageEmitter.emit(getCenterX(), getCenterY(), forward[0], forward[1], orbCount, nowNs);
        barrageFlashUntilNs = nowNs + BARRAGE_FLASH_NS;
        triggerHitFlash(nowNs, BARRAGE_FLASH_NS, Color.rgb(255, 170, 70));
    }

    private int resolveBarrageOrbCount(long nowNs) {
        long encounterAgeNs = encounterStartedAtNs < 0L ? 0L : Math.max(0L, nowNs - encounterStartedAtNs);
        long encounterAgeSec = encounterAgeNs / 1_000_000_000L;
        int min;
        int max;
        if (encounterAgeSec < 30L) {
            min = 5;
            max = 6;
        } else if (encounterAgeSec < 60L) {
            min = 6;
            max = 7;
        } else if (encounterAgeSec < 90L) {
            min = 7;
            max = 8;
        } else if (encounterAgeSec < 120L) {
            min = 8;
            max = 9;
        } else {
            min = 9;
            max = 10;
        }
        return min + ThreadLocalRandom.current().nextInt(max - min + 1);
    }

    private double[] resolveBarrageForwardVector() {
        return switch (attackDirection) {
            case LEFT -> new double[]{-1.0, 0.0};
            case RIGHT -> new double[]{1.0, 0.0};
            case UP -> new double[]{0.0, -1.0};
            case DOWN -> new double[]{0.0, 1.0};
        };
    }
}
