package entity;

import core.GameBalance;

public class FriendlyArcher extends AllyUnit {
    private static final boolean DEBUG_ARCHER = false;
    private static final double PLAYER_RENDER_HEIGHT = 58.0;
    private static final double ARCHER_RENDER_SCALE = 0.82;
    private static final double FRAME_BASE_WIDTH = 128.0;
    private static final double FRAME_BASE_HEIGHT = 128.0;

    private final double renderWidth;
    private final double renderHeight;
    private double animationTimer;
    private int frameIndex;
    private long lastUpdateAtNs;
    private long lastShotAtNs;
    private long lastMeleeAtNs;
    private boolean shotReleasedThisCycle;
    private boolean meleeAppliedThisCycle;
    private boolean removeRequested;

    public FriendlyArcher(double x, double y, double width, double height, double moveSpeed, int wanderRadiusTiles) {
        super(x, y, width, height, moveSpeed, GameBalance.FRIENDLY_ARCHER_MAX_HP, Math.max(3, Math.min(5, wanderRadiusTiles)));
        this.renderHeight = PLAYER_RENDER_HEIGHT * ARCHER_RENDER_SCALE;
        this.renderWidth = FRAME_BASE_WIDTH * this.renderHeight / FRAME_BASE_HEIGHT;
        this.idleDuration = 1.25;
        this.animationTimer = 0.0;
        this.frameIndex = 0;
        this.lastUpdateAtNs = -1L;
        this.lastShotAtNs = -GameBalance.FRIENDLY_ARCHER_SHOT_COOLDOWN_NS;
        this.lastMeleeAtNs = -GameBalance.FRIENDLY_ARCHER_MELEE_COOLDOWN_NS;
        this.shotReleasedThisCycle = false;
        this.meleeAppliedThisCycle = false;
        this.removeRequested = false;
        if (DEBUG_ARCHER) {
            System.out.println("[Archer] spawn renderWidth=" + renderWidth + " renderHeight=" + renderHeight);
        }
    }

    @Override
    public void setState(AllyState state) {
        if (state == null || this.state == state) {
            return;
        }
        AllyState previousState = this.state;
        this.state = state;
        restartAnimationCycle();
        if (DEBUG_ARCHER) {
            System.out.println("[Archer] state " + previousState + " -> " + state
                    + " renderWidth=" + renderWidth
                    + " renderHeight=" + renderHeight);
        }
    }

    public void restartAnimationCycle() {
        this.animationTimer = 0.0;
        this.frameIndex = 0;
        this.shotReleasedThisCycle = false;
        this.meleeAppliedThisCycle = false;
    }

    public double beginUpdate(long nowNs) {
        if (lastUpdateAtNs < 0L) {
            lastUpdateAtNs = nowNs;
            return 0.0;
        }
        double deltaSeconds = (nowNs - lastUpdateAtNs) / 1_000_000_000.0;
        lastUpdateAtNs = nowNs;
        if (deltaSeconds < 0.0) {
            return 0.0;
        }
        return Math.min(0.10, deltaSeconds);
    }

    public void updateAnimation(double deltaSeconds) {
        int frameCount = frameCountForState();
        if (frameCount <= 1) {
            frameIndex = 0;
            animationTimer = 0.0;
            return;
        }

        animationTimer += Math.max(0.0, deltaSeconds);
        double frameDuration = frameDurationSeconds();
        while (animationTimer >= frameDuration) {
            animationTimer -= frameDuration;
            frameIndex++;
            if (frameIndex >= frameCount) {
                if (isLoopingAnimation()) {
                    frameIndex = 0;
                } else {
                    frameIndex = frameCount - 1;
                    animationTimer = 0.0;
                    break;
                }
            }
        }
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public int frameCountForState() {
        return switch (state) {
            case SHOT -> 14;
            case ATTACK -> 5;
            case HURT -> 3;
            case DEAD -> 5;
            case WALK, RETURN_HOME -> 8;
            case IDLE -> 9;
        };
    }

    public boolean isLoopingAnimation() {
        return state == AllyState.IDLE
                || state == AllyState.WALK
                || state == AllyState.RETURN_HOME;
    }

    public double frameDurationSeconds() {
        return switch (state) {
            case WALK, RETURN_HOME -> 0.10;
            case SHOT -> 0.065;
            case ATTACK -> 0.085;
            case HURT -> 0.11;
            case DEAD -> 0.10;
            case IDLE -> 0.12;
        };
    }

    public String getAnimationKey() {
        return switch (state) {
            case SHOT -> "friendly_archer_shot";
            case ATTACK -> "friendly_archer_attack";
            case HURT -> "friendly_archer_hurt";
            case DEAD -> "friendly_archer_dead";
            case WALK, RETURN_HOME -> "friendly_archer_walk";
            case IDLE -> "friendly_archer_idle";
        };
    }

    public boolean canShoot(long nowNs) {
        return nowNs - lastShotAtNs >= GameBalance.FRIENDLY_ARCHER_SHOT_COOLDOWN_NS;
    }

    public void markShot(long nowNs) {
        this.lastShotAtNs = nowNs;
    }

    public boolean canMelee(long nowNs) {
        return nowNs - lastMeleeAtNs >= GameBalance.FRIENDLY_ARCHER_MELEE_COOLDOWN_NS;
    }

    public void markMelee(long nowNs) {
        this.lastMeleeAtNs = nowNs;
    }

    public boolean isShotReleasedThisCycle() {
        return shotReleasedThisCycle;
    }

    public void setShotReleasedThisCycle(boolean shotReleasedThisCycle) {
        this.shotReleasedThisCycle = shotReleasedThisCycle;
    }

    public boolean isMeleeAppliedThisCycle() {
        return meleeAppliedThisCycle;
    }

    public void setMeleeAppliedThisCycle(boolean meleeAppliedThisCycle) {
        this.meleeAppliedThisCycle = meleeAppliedThisCycle;
    }

    public double getVisionRange() {
        return GameBalance.FRIENDLY_ARCHER_VISION_RANGE;
    }

    public double getShootRange() {
        return GameBalance.FRIENDLY_ARCHER_SHOOT_RANGE;
    }

    public double getMeleeRange() {
        return GameBalance.FRIENDLY_ARCHER_MELEE_RANGE;
    }

    public int getRangedDamage() {
        return GameBalance.FRIENDLY_ARCHER_RANGED_DAMAGE;
    }

    public int getMeleeDamage() {
        return GameBalance.FRIENDLY_ARCHER_MELEE_DAMAGE;
    }

    public double getArrowSpeed() {
        return GameBalance.FRIENDLY_ARCHER_ARROW_SPEED;
    }

    public double getRenderWidth() {
        return renderWidth;
    }

    public double getRenderHeight() {
        return renderHeight;
    }

    public double getRenderX() {
        return x + width * 0.5 - renderWidth * 0.5;
    }

    public double getRenderY() {
        return y + height - renderHeight;
    }

    @Override
    protected double collisionInsetLeft(double width, double height) {
        return width * 0.18;
    }

    @Override
    protected double collisionInsetRight(double width, double height) {
        return width * 0.18;
    }

    @Override
    protected double collisionInsetTop(double width, double height) {
        return height * 0.24;
    }

    @Override
    protected double collisionInsetBottom(double width, double height) {
        return height * 0.10;
    }

    public boolean hasFinishedNonLoopingAnimation() {
        return !isLoopingAnimation() && frameIndex >= frameCountForState() - 1;
    }

    public void receiveDamage(int damage) {
        if (damage <= 0 || state == AllyState.DEAD) {
            return;
        }
        takeDamage(damage);
        clearWanderTarget();
        resetIdleTimer();
        if (!isAlive()) {
            setState(AllyState.DEAD);
            return;
        }
        setState(AllyState.HURT);
    }

    public void requestRemoval() {
        this.removeRequested = true;
    }

    public boolean isRemovalRequested() {
        return removeRequested;
    }
}
