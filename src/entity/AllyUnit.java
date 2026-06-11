package entity;

public abstract class AllyUnit extends Entity {
    public enum AllyState {
        IDLE,
        WALK,
        SHOT,
        ATTACK,
        HURT,
        DEAD,
        RETURN_HOME
    }

    protected AllyState state;
    protected boolean facingRight;
    protected final double homeX;
    protected final double homeY;
    protected double wanderTargetX;
    protected double wanderTargetY;
    protected double idleTimer;
    protected double idleDuration;
    protected int wanderRadiusTiles;
    protected boolean hasWanderTarget;
    protected Enemy currentTargetEnemy;

    protected AllyUnit(double x, double y, double width, double height, double speed, int maxHp, int wanderRadiusTiles) {
        super(x, y, width, height, speed, maxHp);
        this.state = AllyState.IDLE;
        this.facingRight = true;
        this.homeX = x;
        this.homeY = y;
        this.wanderTargetX = x;
        this.wanderTargetY = y;
        this.idleTimer = 0.0;
        this.idleDuration = 1.0;
        this.wanderRadiusTiles = Math.max(1, wanderRadiusTiles);
        this.hasWanderTarget = false;
        this.currentTargetEnemy = null;
    }

    public AllyState getState() {
        return state;
    }

    public void setState(AllyState state) {
        if (state != null) {
            this.state = state;
        }
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    public void setFacingRight(boolean facingRight) {
        this.facingRight = facingRight;
    }

    public double getHomeX() {
        return homeX;
    }

    public double getHomeY() {
        return homeY;
    }

    public double getWanderTargetX() {
        return wanderTargetX;
    }

    public double getWanderTargetY() {
        return wanderTargetY;
    }

    public void setWanderTarget(double wanderTargetX, double wanderTargetY) {
        this.wanderTargetX = wanderTargetX;
        this.wanderTargetY = wanderTargetY;
        this.hasWanderTarget = true;
    }

    public boolean hasWanderTarget() {
        return hasWanderTarget;
    }

    public void clearWanderTarget() {
        this.hasWanderTarget = false;
        this.wanderTargetX = x;
        this.wanderTargetY = y;
    }

    public double getIdleTimer() {
        return idleTimer;
    }

    public void resetIdleTimer() {
        this.idleTimer = 0.0;
    }

    public void addIdleTime(double deltaSeconds) {
        this.idleTimer += Math.max(0.0, deltaSeconds);
    }

    public double getIdleDuration() {
        return idleDuration;
    }

    public void setIdleDuration(double idleDuration) {
        this.idleDuration = Math.max(0.1, idleDuration);
    }

    public int getWanderRadiusTiles() {
        return wanderRadiusTiles;
    }

    public Enemy getCurrentTargetEnemy() {
        return currentTargetEnemy;
    }

    public void setCurrentTargetEnemy(Enemy currentTargetEnemy) {
        this.currentTargetEnemy = currentTargetEnemy;
    }

    public void faceTargetX(double targetCenterX) {
        this.facingRight = targetCenterX >= getCenterX();
    }

    public double distanceTo(double targetX, double targetY) {
        double dx = targetX - x;
        double dy = targetY - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double distanceToHome() {
        return distanceTo(homeX, homeY);
    }

    public boolean isWithinHomeRadius(double tileWidth, double tileHeight) {
        double maxDistance = (wanderRadiusTiles + 2.0) * Math.max(tileWidth, tileHeight);
        return distanceToHome() <= maxDistance;
    }

    public double getFootY() {
        return y + height;
    }
}
