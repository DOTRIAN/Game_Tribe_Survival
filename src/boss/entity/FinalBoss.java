package boss.entity;

import animation.SpriteAnimation;
import animation.SpriteSheetLoader;
import boss.BossSpriteLoader;
import boss.BossState;
import entity.Enemy;
import entity.Player;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import system.DamageSystem;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
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

    private enum LaoPhase {
        DASH,
        SLAM,
        EXPLOSION
    }

    private static final String STATE_ROOT = "assets/boss_assets/fire_boss/boss_demon_slime_FREE_v1.0/state_boss";
    private static final String LAO_SHEET = STATE_ROOT + "/lao.png";
    private static final String CHEM_SHEET = STATE_ROOT + "/chem.png";
    private static final String VU_NO_SHEET = STATE_ROOT + "/vu_no.png";
    private static final String PLACEHOLDER_FRAME = STATE_ROOT + "/01_demon_idle/demon_idle_1.png";

    private static final long IDLE_FRAME_NS = 170_000_000L;
    private static final long WALK_FRAME_NS = 90_000_000L;
    private static final long LAO_FRAME_NS = 120_000_000L;
    private static final long LAO_DASH_FRAME_NS = 120_000_000L;
    private static final long LAO_SLAM_FRAME_NS = 150_000_000L;
    private static final long LAO_EXPLOSION_FRAME_NS = 180_000_000L;
    private static final long CLEAVE_FRAME_NS = 75_000_000L;
    private static final long BARRAGE_CHARGE_NS = 800_000_000L;
    private static final long BARRAGE_INTERVAL_NS = 10_000_000_000L;
    private static final long LAO_INTERVAL_NS = 15_000_000_000L;
    private static final long TAKE_HIT_FRAME_NS = 85_000_000L;
    private static final long DEATH_FRAME_NS = 95_000_000L;
    private static final long ATTACK_COOLDOWN_NS = 1_600_000_000L;
    private static final long BARRAGE_FLASH_NS = 220_000_000L;

    private static final double FACE_FLIP_THRESHOLD_X = 18.0;
    private static final double HP_BAR_WIDTH = 180.0;
    private static final double HP_BAR_HEIGHT = 12.0;
    private static final double FOOT_CENTER_Y_RATIO = 0.88;
    private static final double ATTACK_OFFSET = 14.0;
    private static final double ATTACK_RADIUS_X = 14.0;
    private static final double ATTACK_RADIUS_Y = 8.0;
    private static final double APPROACH_SNAP_DISTANCE = 3.0;
    private static final double LAO_PLAYER_TARGET_OFFSET_X = 34.0;
    private static final double LAO_PLAYER_TARGET_OFFSET_Y = 18.0;
    private static final double LAO_DASH_SPEED_MULTIPLIER = 3.6;
    private static final double LAO_DASH_RENDER_SCALE = 1.08;
    private static final double LAO_SLAM_RENDER_SCALE = 0.62;
    private static final double LAO_EXPLOSION_BOSS_RENDER_SCALE = 0.62;
    private static final double LAO_EXPLOSION_EFFECT_RENDER_SCALE = 0.76;
    private static final double EXPLOSION_RADIUS = 100.0;
    private static final double MATTE_BG_TOLERANCE = 0.18;

    private static final int MAX_HP = 140;
    private static final int CLEAVE_DAMAGE = 14;
    private static final int DASH_DAMAGE = 12;
    private static final int EXPLOSION_DAMAGE = 30;
    private static final int LAO_COLUMNS = 6;
    private static final int LAO_ROWS = 3;
    private static final int LAO_DASH_START = 0;
    private static final int LAO_DASH_END = 5;
    private static final int LAO_SLAM_START = 6;
    private static final int LAO_SLAM_END = 10;
    private static final int LAO_EXPLOSION_START = 11;
    private static final int LAO_EXPLOSION_PEAK_END = 13;
    private static final int LAO_EXPLOSION_END = 15;
    private static final double LAO_EXPLOSION_KNOCKBACK = 26.0;

    private static final Image[] LAO_BOSS_FRAMES = loadLaoBossFrames();
    private static final Image[] LAO_EXPLOSION_FRAMES = loadLaoExplosionFrames();
    private static final Map<BossState, Image[]> RAW_FRAMES = loadRawFrames();

    private final Map<BossState, SpriteAnimation> animations;
    private BossState state;
    private Image currentFrame;
    private boolean facingRight;
    private AttackDirection attackDirection;
    private long lastAttackAtNs;
    private long nextBarrageAtNs;
    private long nextLaoAtNs;
    private long barrageStartedAtNs;
    private long encounterStartedAtNs;
    private long barrageFlashUntilNs;
    private boolean cleaveDamageApplied;
    private boolean hasDealtLaoDamage;
    private boolean hasDealtDashDamage;
    private boolean hasDealtExplosionDamage;
    private boolean slamImpactResolved;
    private boolean deathAnimationFinished;
    private boolean debugEnabled;
    private double lastAttackDirX;
    private double lastAttackDirY;
    private LaoPhase laoPhase;
    private Point2D laoStartPosition;
    private Point2D targetDashPosition;
    private Point2D explosionCenter;
    private double laoDashDirX;
    private double laoDashDirY;
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
        this.nextLaoAtNs = -1L;
        this.barrageStartedAtNs = -1L;
        this.encounterStartedAtNs = -1L;
        this.barrageFlashUntilNs = -1L;
        this.cleaveDamageApplied = false;
        this.hasDealtLaoDamage = false;
        this.hasDealtDashDamage = false;
        this.hasDealtExplosionDamage = false;
        this.slamImpactResolved = false;
        this.deathAnimationFinished = false;
        this.debugEnabled = false;
        this.lastAttackDirX = 0.0;
        this.lastAttackDirY = 1.0;
        this.laoPhase = LaoPhase.DASH;
        this.laoStartPosition = new Point2D(x, y);
        this.targetDashPosition = new Point2D(x, y);
        this.explosionCenter = new Point2D(x + width * 0.5, y + height * 0.85);
        this.laoDashDirX = 1.0;
        this.laoDashDirY = 0.0;
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
            nextLaoAtNs = now + LAO_INTERVAL_NS;
        }

        if (hp <= 0 && state != BossState.DEATH) {
            transitionTo(BossState.DEATH);
        }

        switch (state) {
            case DEATH -> updateDeath(now);
            case TAKE_HIT -> updateTakeHit(now);
            case LAO -> updateLao(now, player, worldWidth, worldHeight);
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
        }
    }

    @Override
    public void draw(GraphicsContext graphicsContext, double cameraX, double cameraY, long nowNs) {
        Image frame = currentFrame;
        double screenX = Math.round(x - cameraX);
        double screenY = Math.round(y - cameraY);
        double renderWidth = renderWidthForCurrentState();
        double renderHeight = renderHeightForCurrentState();
        double drawX = Math.round(screenX + (width - renderWidth) * 0.5);
        double drawY = Math.round(screenY + (height - renderHeight));
        if (frame == null || frame.isError()) {
            graphicsContext.setFill(Color.DARKRED);
            graphicsContext.fillRect(drawX, drawY, renderWidth, renderHeight);
        } else if (facingRight) {
            graphicsContext.drawImage(frame, drawX, drawY, renderWidth, renderHeight);
        } else {
            graphicsContext.save();
            graphicsContext.translate(drawX + renderWidth, drawY);
            graphicsContext.scale(-1, 1);
            graphicsContext.drawImage(frame, 0, 0, renderWidth, renderHeight);
            graphicsContext.restore();
        }

        if (state == BossState.LAO && laoPhase == LaoPhase.EXPLOSION && slamImpactResolved) {
            drawLaoExplosionEffect(graphicsContext, cameraX);
        }

        if (isHitFlashActive(nowNs) && frame != null && !frame.isError()) {
            graphicsContext.save();
            graphicsContext.setGlobalBlendMode(javafx.scene.effect.BlendMode.SCREEN);
            graphicsContext.setGlobalAlpha(0.34);
            graphicsContext.setFill(Color.rgb(255, 96, 96));
            graphicsContext.fillOval(drawX + 26, drawY + 18, Math.max(12.0, renderWidth - 52), Math.max(12.0, renderHeight - 24));
            graphicsContext.restore();
        }

        if (state == BossState.LAO && laoPhase == LaoPhase.DASH) {
            drawDashTrail(graphicsContext, cameraX, cameraY);
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
        // Boss dung state-machine rieng cho CLEAVE/LAO, khong danh theo body hitbox mac dinh.
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

        if (shouldStartLao(now)) {
            beginLao(now, player, worldWidth, worldHeight);
            return;
        }
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

    private void updateLao(long now, Player player, double worldWidth, double worldHeight) {
        SpriteAnimation animation = animationFor(BossState.LAO);
        int previousFrameIndex = animation.getCurrentFrameIndex();
        boolean finished = animation.updateOnce(now, laoFrameDurationFor(previousFrameIndex));
        int frameIndex = animation.getCurrentFrameIndex();
        currentFrame = resolveLaoBossFrame(frameIndex);
        updateLaoPhase(frameIndex);
        updateFacingDirection(laoDashDirX);
        updateAttackDirection(laoDashDirX, laoDashDirY);
        updateDashMovement(frameIndex, worldWidth, worldHeight);
        explosionCenter = new Point2D(x + width * 0.5, y + height * 0.85);

        if (!slamImpactResolved && previousFrameIndex == LAO_SLAM_END && frameIndex > LAO_SLAM_END) {
            slamImpactResolved = true;
        }

        if (frameIndex >= 2 && frameIndex <= LAO_DASH_END
                && laoPhase == LaoPhase.DASH
                && !hasDealtDashDamage
                && intersectsDashHitbox(player)) {
            DamageSystem.applyDamage(this, player, DASH_DAMAGE, now);
            hasDealtDashDamage = true;
        }

        if (slamImpactResolved
                && laoPhase == LaoPhase.EXPLOSION
                && frameIndex >= LAO_EXPLOSION_START
                && frameIndex <= LAO_EXPLOSION_PEAK_END
                && !hasDealtExplosionDamage) {
            hasDealtExplosionDamage = true;
            hasDealtLaoDamage = true;
            if (intersectsExplosionHitbox(player)) {
                DamageSystem.applyDamage(this, player, EXPLOSION_DAMAGE, now);
                applyExplosionKnockback(player, worldWidth, worldHeight);
            }
        }

        if (!finished) {
            return;
        }
        x = targetDashPosition.getX();
        y = targetDashPosition.getY();
        clampPosition(0, 0, worldWidth, worldHeight);
        transitionTo(BossState.IDLE);
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
            if (nextState == BossState.LAO) {
                hasDealtLaoDamage = false;
                hasDealtDashDamage = false;
                hasDealtExplosionDamage = false;
                slamImpactResolved = false;
                laoPhase = LaoPhase.DASH;
            }
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

    private boolean intersectsDashHitbox(Player player) {
        if (player == null) {
            return false;
        }
        double hitboxWidth = width * 1.2;
        double hitboxHeight = height * 0.6;
        boolean movingRight = laoDashDirX >= 0.0;
        double hitboxX = movingRight ? x + width * 0.25 : x - width * 0.45;
        double hitboxY = y + height * 0.25;
        return intersectsEllipseAndRect(
                hitboxX + hitboxWidth * 0.5,
                hitboxY + hitboxHeight * 0.5,
                hitboxWidth * 0.5,
                hitboxHeight * 0.5,
                player.getCollisionX(),
                player.getCollisionY(),
                player.getCollisionWidth(),
                player.getCollisionHeight()
        );
    }

    private boolean intersectsExplosionHitbox(Player player) {
        if (player == null) {
            return false;
        }
        double centerX = x + width * 0.5;
        double centerY = y + height * 0.85;
        double playerCenterX = player.getCollisionX() + player.getCollisionWidth() * 0.5;
        double playerCenterY = player.getCollisionY() + player.getCollisionHeight() * 0.5;
        return Math.hypot(playerCenterX - centerX, playerCenterY - centerY) <= EXPLOSION_RADIUS;
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

    private void updateLaoPhase(int frameIndex) {
        if (frameIndex <= LAO_DASH_END) {
            laoPhase = LaoPhase.DASH;
            return;
        }
        if (frameIndex <= LAO_SLAM_END) {
            laoPhase = LaoPhase.SLAM;
            return;
        }
        laoPhase = LaoPhase.EXPLOSION;
    }

    private void beginLao(long now, Player player, double worldWidth, double worldHeight) {
        laoStartPosition = new Point2D(x, y);
        targetDashPosition = resolveDashTargetPosition(player, worldWidth, worldHeight);
        explosionCenter = new Point2D(targetDashPosition.getX() + width * 0.5, targetDashPosition.getY() + height * 0.85);
        double dx = targetDashPosition.getX() - laoStartPosition.getX();
        double dy = targetDashPosition.getY() - laoStartPosition.getY();
        double distance = Math.hypot(dx, dy);
        if (distance <= 0.001) {
            laoDashDirX = facingRight ? 1.0 : -1.0;
            laoDashDirY = 0.0;
        } else {
            laoDashDirX = dx / distance;
            laoDashDirY = dy / distance;
        }
        nextLaoAtNs = now + LAO_INTERVAL_NS;
        transitionTo(BossState.LAO);
    }

    private Point2D resolveDashTargetPosition(Player player, double worldWidth, double worldHeight) {
        if (player == null) {
            return new Point2D(x, y);
        }
        double offsetX = ThreadLocalRandom.current().nextDouble(-LAO_PLAYER_TARGET_OFFSET_X, LAO_PLAYER_TARGET_OFFSET_X);
        double offsetY = ThreadLocalRandom.current().nextDouble(-LAO_PLAYER_TARGET_OFFSET_Y, LAO_PLAYER_TARGET_OFFSET_Y);
        double targetX = player.getCenterX() - width * 0.5 + offsetX;
        double targetY = player.getCenterY() - height * FOOT_CENTER_Y_RATIO + offsetY;
        targetX = clamp(targetX, 0.0, Math.max(0.0, worldWidth - width));
        targetY = clamp(targetY, 0.0, Math.max(0.0, worldHeight - height));
        return new Point2D(targetX, targetY);
    }

    private void updateDashMovement(int frameIndex, double worldWidth, double worldHeight) {
        if (frameIndex < LAO_DASH_START || frameIndex > LAO_DASH_END) {
            if (frameIndex >= LAO_SLAM_START) {
                x = targetDashPosition.getX();
                y = targetDashPosition.getY();
                clampPosition(0, 0, worldWidth, worldHeight);
            }
            return;
        }

        double frameProgress = (frameIndex - LAO_DASH_START + 1.0) / (LAO_DASH_END - LAO_DASH_START + 1.0);
        double dashDistance = laoStartPosition.distance(targetDashPosition);
        double speedReach = Math.min(
                1.0,
                (speed * LAO_DASH_SPEED_MULTIPLIER * (frameIndex + 1)) / Math.max(1.0, dashDistance)
        );
        double progress = Math.max(frameProgress, speedReach);
        progress = clamp(progress, 0.0, 1.0);
        x = laoStartPosition.getX() + (targetDashPosition.getX() - laoStartPosition.getX()) * progress;
        y = laoStartPosition.getY() + (targetDashPosition.getY() - laoStartPosition.getY()) * progress;
        clampPosition(0, 0, worldWidth, worldHeight);
    }

    private void drawDashTrail(GraphicsContext graphicsContext, double cameraX, double cameraY) {
        double trailLength = Math.min(90.0, Math.max(42.0, laoStartPosition.distance(x, y) * 0.35));
        double anchorX = x + width * 0.5 - laoDashDirX * trailLength;
        double anchorY = y + height * 0.68 - laoDashDirY * 14.0;
        graphicsContext.save();
        graphicsContext.setFill(Color.color(1.0, 0.45, 0.08, 0.22));
        graphicsContext.fillOval(
                anchorX - cameraX - trailLength * 0.5,
                anchorY - cameraY - 18.0,
                trailLength,
                36.0
        );
        graphicsContext.setFill(Color.color(1.0, 0.78, 0.18, 0.14));
        graphicsContext.fillOval(
                anchorX - cameraX - trailLength * 0.35,
                anchorY - cameraY - 12.0,
                trailLength * 0.7,
                24.0
        );
        graphicsContext.restore();
    }

    private void drawLaoExplosionEffect(GraphicsContext graphicsContext, double cameraX) {
        int frameIndex = animationFor(BossState.LAO).getCurrentFrameIndex();
        int explosionIndex = frameIndex - LAO_EXPLOSION_START;
        if (explosionIndex < 0 || explosionIndex >= LAO_EXPLOSION_FRAMES.length) {
            return;
        }
        Image effectFrame = LAO_EXPLOSION_FRAMES[explosionIndex];
        if (effectFrame == null || effectFrame.isError()) {
            return;
        }
        double drawWidth = effectFrame.getWidth() * LAO_EXPLOSION_EFFECT_RENDER_SCALE;
        double drawHeight = effectFrame.getHeight() * LAO_EXPLOSION_EFFECT_RENDER_SCALE;
        double drawX = explosionCenter.getX() - drawWidth * 0.5 - cameraX;
        double drawY = explosionCenter.getY() - drawHeight * 0.82;
        graphicsContext.drawImage(effectFrame, Math.round(drawX), Math.round(drawY), drawWidth, drawHeight);
    }

    private double renderWidthForCurrentState() {
        if (state == BossState.LAO) {
            return width * laoBossRenderScaleForCurrentPhase();
        }
        return width;
    }

    private double renderHeightForCurrentState() {
        if (state == BossState.LAO) {
            return height * laoBossRenderScaleForCurrentPhase();
        }
        return height;
    }

    private double laoBossRenderScaleForCurrentPhase() {
        return switch (laoPhase) {
            case DASH -> LAO_DASH_RENDER_SCALE;
            case SLAM -> LAO_SLAM_RENDER_SCALE;
            case EXPLOSION -> LAO_EXPLOSION_BOSS_RENDER_SCALE;
        };
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
        if (state == BossState.LAO) {
            double dashHitboxWidth = width * 1.2;
            double dashHitboxHeight = height * 0.6;
            boolean movingRight = laoDashDirX >= 0.0;
            double dashHitboxX = movingRight ? x + width * 0.25 : x - width * 0.45;
            double dashHitboxY = y + height * 0.25;
            graphicsContext.setStroke(Color.color(1.0, 0.5, 0.08, 0.95));
            graphicsContext.strokeOval(
                    dashHitboxX - cameraX,
                    dashHitboxY - cameraY,
                    dashHitboxWidth,
                    dashHitboxHeight
            );
            graphicsContext.setStroke(Color.color(1.0, 0.8, 0.1, 0.95));
            graphicsContext.strokeOval(
                    x + width * 0.5 - EXPLOSION_RADIUS - cameraX,
                    y + height * 0.85 - EXPLOSION_RADIUS - cameraY,
                    EXPLOSION_RADIUS * 2.0,
                    EXPLOSION_RADIUS * 2.0
            );
        }
        graphicsContext.setFill(Color.color(1.0, 0.98, 0.78, 0.95));
        graphicsContext.fillText(
                state.name() + " phase=" + laoPhase + " face=" + (facingRight ? "R" : "L"),
                x - cameraX,
                y - cameraY - 6.0
        );
        graphicsContext.restore();
    }

    private Map<BossState, SpriteAnimation> buildAnimations() {
        Map<BossState, SpriteAnimation> map = new EnumMap<>(BossState.class);
        for (BossState bossState : BossState.values()) {
            Image[] raw = RAW_FRAMES.getOrDefault(bossState, new Image[0]);
            List<Image[]> singleSequence = Collections.singletonList(raw);
            int maxWidth = Math.max(1, BossSpriteLoader.findMaxWidth(singleSequence));
            int maxHeight = Math.max(1, BossSpriteLoader.findMaxHeight(singleSequence));
            Image[] normalized = BossSpriteLoader.normalizeFrames(raw, maxWidth, maxHeight);
            map.put(bossState, new SpriteAnimation(normalized, frameDurationFor(bossState)));
        }
        return map;
    }

    private long frameDurationFor(BossState bossState) {
        return switch (bossState) {
            case IDLE -> IDLE_FRAME_NS;
            case WALK -> WALK_FRAME_NS;
            case LAO -> LAO_FRAME_NS;
            case CLEAVE -> CLEAVE_FRAME_NS;
            case BARRAGE -> IDLE_FRAME_NS;
            case TAKE_HIT -> TAKE_HIT_FRAME_NS;
            case DEATH -> DEATH_FRAME_NS;
        };
    }

    private long laoFrameDurationFor(int frameIndex) {
        if (frameIndex <= LAO_DASH_END) {
            return LAO_DASH_FRAME_NS;
        }
        if (frameIndex <= LAO_SLAM_END) {
            return LAO_SLAM_FRAME_NS;
        }
        return LAO_EXPLOSION_FRAME_NS;
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
        frames.put(BossState.LAO, LAO_BOSS_FRAMES);
        frames.put(BossState.CLEAVE, BossSpriteLoader.loadSequence(STATE_ROOT + "/03_demon_cleave"));
        frames.put(BossState.BARRAGE, BossSpriteLoader.loadSequence(STATE_ROOT + "/01_demon_idle"));
        frames.put(BossState.TAKE_HIT, BossSpriteLoader.loadSequence(STATE_ROOT + "/04_demon_take_hit"));
        frames.put(BossState.DEATH, BossSpriteLoader.loadSequence(STATE_ROOT + "/05_demon_death"));
        return frames;
    }

    private static Image[] loadLaoBossFrames() {
        Image[] dashFrames = SpriteSheetLoader.loadRowCells(LAO_SHEET, 0, LAO_ROWS, LAO_COLUMNS, 0, 6);
        Image[] slamFrames = SpriteSheetLoader.loadHorizontalStrip(CHEM_SHEET, 5);
        slamFrames = BossSpriteLoader.removeEdgeBackground(slamFrames, MATTE_BG_TOLERANCE);
        slamFrames = BossSpriteLoader.trimTransparentVerticalKeepWidth(slamFrames);
        Image[] merged = new Image[dashFrames.length + slamFrames.length + 5];
        System.arraycopy(dashFrames, 0, merged, 0, dashFrames.length);
        System.arraycopy(slamFrames, 0, merged, dashFrames.length, slamFrames.length);
        Image[] processed = BossSpriteLoader.trimNearBlackFrames(merged);
        Image holdFrame = processed[Math.min(processed.length - 1, LAO_SLAM_END)];
        for (int index = LAO_EXPLOSION_START; index <= LAO_EXPLOSION_END; index++) {
            processed[index] = holdFrame;
        }
        return processed;
    }

    private static Image[] loadLaoExplosionFrames() {
        Image[] effectFrames = SpriteSheetLoader.loadHorizontalStrip(VU_NO_SHEET, 5);
        effectFrames = BossSpriteLoader.removeEdgeBackground(effectFrames, MATTE_BG_TOLERANCE);
        effectFrames = BossSpriteLoader.trimTransparentVerticalKeepWidth(effectFrames);
        return BossSpriteLoader.trimNearBlackFrames(effectFrames);
    }

    private Image resolveLaoBossFrame(int frameIndex) {
        if (LAO_BOSS_FRAMES.length == 0) {
            return resolveFrame(BossState.IDLE);
        }
        int safeIndex = Math.max(0, Math.min(LAO_BOSS_FRAMES.length - 1, frameIndex));
        return LAO_BOSS_FRAMES[safeIndex];
    }

    private boolean shouldStartLao(long nowNs) {
        return state != BossState.LAO
                && state != BossState.BARRAGE
                && state != BossState.CLEAVE
                && state != BossState.TAKE_HIT
                && state != BossState.DEATH
                && nextLaoAtNs > 0L
                && nowNs >= nextLaoAtNs;
    }

    private boolean shouldStartBarrage(long nowNs) {
        return state != BossState.BARRAGE
                && state != BossState.LAO
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

    private void applyExplosionKnockback(Player player, double worldWidth, double worldHeight) {
        if (player == null) {
            return;
        }
        double centerX = x + width * 0.5;
        double centerY = y + height * 0.85;
        double playerCenterX = player.getCollisionX() + player.getCollisionWidth() * 0.5;
        double playerCenterY = player.getCollisionY() + player.getCollisionHeight() * 0.5;
        double dx = playerCenterX - centerX;
        double dy = playerCenterY - centerY;
        double distance = Math.hypot(dx, dy);
        double pushX;
        double pushY;
        if (distance <= 0.001) {
            pushX = laoDashDirX * LAO_EXPLOSION_KNOCKBACK;
            pushY = 0.0;
        } else {
            pushX = (dx / distance) * LAO_EXPLOSION_KNOCKBACK;
            pushY = (dy / distance) * LAO_EXPLOSION_KNOCKBACK;
        }
        player.setPosition(player.getX() + pushX, player.getY() + pushY);
        player.clampPosition(0, 0, worldWidth, worldHeight);
    }
}
