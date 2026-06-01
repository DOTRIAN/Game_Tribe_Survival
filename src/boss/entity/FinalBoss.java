package boss.entity;

import animation.SpriteAnimation;
import boss.BossSpriteLoader;
import boss.BossState;
import entity.Enemy;
import entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import system.CollisionSystem;
import system.DamageSystem;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class FinalBoss extends Enemy {
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
    private static final long TAKE_HIT_FRAME_NS = 85_000_000L;
    private static final long DEATH_FRAME_NS = 95_000_000L;
    private static final long ATTACK_COOLDOWN_NS = 1_600_000_000L;
    private static final double ATTACK_RANGE = 132.0;
    private static final double APPROACH_STOP_DISTANCE = 104.0;
    private static final double FACE_FLIP_THRESHOLD_X = 4.0;
    private static final double HP_BAR_WIDTH = 180.0;
    private static final double HP_BAR_HEIGHT = 12.0;
    private static final int MAX_HP = 140;
    private static final int CLEAVE_DAMAGE = 14;
    private static final Map<BossState, Image[]> RAW_FRAMES = loadRawFrames();

    private final Map<BossState, SpriteAnimation> animations;
    private BossState state;
    private Image currentFrame;
    private boolean facingRight;
    private AttackDirection attackDirection;
    private long lastAttackAtNs;
    private boolean cleaveDamageApplied;
    private boolean deathAnimationFinished;

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
        this.cleaveDamageApplied = false;
        this.deathAnimationFinished = false;
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

        if (hp <= 0 && state != BossState.DEATH) {
            transitionTo(BossState.DEATH);
        }

        switch (state) {
            case DEATH -> updateDeath(now);
            case TAKE_HIT -> updateTakeHit(now);
            case CLEAVE -> updateCleave(now, player);
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
        if (state != BossState.CLEAVE && state != BossState.DEATH) {
            transitionTo(BossState.TAKE_HIT);
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

        drawBossHealthBar(graphicsContext, screenX, screenY);
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

    public boolean isEncounterFinished() {
        return deathAnimationFinished;
    }

    public BossState getState() {
        return state;
    }

    private void updateMovementState(long now, Player player, double worldWidth, double worldHeight) {
        double dx = player.getCenterX() - getCenterX();
        double dy = player.getCenterY() - getCenterY();
        double distance = Math.hypot(dx, dy);
        updateAttackDirection(dx, dy);
        if (attackDirection == AttackDirection.LEFT) {
            facingRight = true;
        } else if (attackDirection == AttackDirection.RIGHT) {
            facingRight = false;
        } else if (Math.abs(dx) >= FACE_FLIP_THRESHOLD_X) {
            // Asset hien chi co 2 huong ngang, nen khi player dung tren/duoi
            // ta giu huong ngang gan nhat theo truc X.
            facingRight = dx < 0;
        }

        if (distance <= ATTACK_RANGE && now - lastAttackAtNs >= ATTACK_COOLDOWN_NS) {
            transitionTo(BossState.CLEAVE);
            return;
        }

        boolean moving = distance > APPROACH_STOP_DISTANCE;
        if (moving && distance > 0.001) {
            x += (dx / distance) * speed;
            y += (dy / distance) * speed;
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

        if (!cleaveDamageApplied && animation.getCurrentFrameIndex() >= Math.max(1, animation.getFrameCount() / 2)) {
            cleaveDamageApplied = true;
            double attackWidth;
            double attackHeight;
            double attackX;
            double attackY;
            if (attackDirection == AttackDirection.UP || attackDirection == AttackDirection.DOWN) {
                attackWidth = getCollisionWidth() + 32.0;
                attackHeight = getCollisionHeight() + 68.0;
                attackX = getCenterX() - attackWidth * 0.5;
                attackY = attackDirection == AttackDirection.UP
                        ? getCenterY() - attackHeight + 12.0
                        : getCenterY() - 12.0;
            } else {
                attackWidth = getCollisionWidth() + 64.0;
                attackHeight = getCollisionHeight() + 32.0;
                attackX = attackDirection == AttackDirection.LEFT
                        ? getCenterX() - attackWidth + 12.0
                        : getCenterX() - 12.0;
                attackY = getCollisionY() + 4.0;
            }
            if (CollisionSystem.intersects(
                    player.getCollisionX(),
                    player.getCollisionY(),
                    player.getCollisionWidth(),
                    player.getCollisionHeight(),
                    attackX,
                    attackY,
                    attackWidth,
                    attackHeight)) {
                DamageSystem.applyDamage(this, player, damage, now);
            }
        }

        if (!finished) {
            return;
        }
        lastAttackAtNs = now;
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
        frames.put(BossState.TAKE_HIT, BossSpriteLoader.loadSequence(STATE_ROOT + "/04_demon_take_hit"));
        frames.put(BossState.DEATH, BossSpriteLoader.loadSequence(STATE_ROOT + "/05_demon_death"));
        return frames;
    }
}
