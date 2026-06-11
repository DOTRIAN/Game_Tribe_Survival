package entity;

/**
 * BaseCamp:
 * - Nha chinh/trai trung tam cua world sinh ton.
 * - Dieu kien thua: BaseCamp bi pha huy.
 */
public class BaseCamp extends Entity {
    private double collisionInsetLeft;
    private double collisionInsetRight;
    private double collisionInsetTop;
    private double collisionInsetBottom;
    private double hpBarOffsetY;

    // Constructor:
    // - Input: toa do va kich thuoc nha chinh.
    // - Output: entity camp co hp lon de phong thu nhieu dem.
    public BaseCamp(double x, double y, double width, double height, int maxHp) {
        super(x, y, width, height, 0, maxHp);
        applyTentLayout();
    }

    public void configure(double x, double y, double width, double height, int maxHp) {
        this.x = x;
        this.y = y;
        this.width = Math.max(1.0, width);
        this.height = Math.max(1.0, height);
        this.maxHp = Math.max(1, maxHp);
        this.hp = Math.min(this.hp, this.maxHp);
        if (this.hp <= 0) {
            this.hp = this.maxHp;
        }
        applyTentLayout();
    }

    private void applyTentLayout() {
        collisionInsetLeft = width * 0.22;
        collisionInsetRight = width * 0.22;
        collisionInsetTop = height * 0.62;
        collisionInsetBottom = Math.max(6.0, height * 0.05);
        hpBarOffsetY = Math.max(18.0, height * 0.16);
    }

    public double getCollisionX() {
        return x + collisionInsetLeft;
    }

    public double getCollisionY() {
        return y + collisionInsetTop;
    }

    public double getCollisionWidth() {
        return Math.max(12.0, width - collisionInsetLeft - collisionInsetRight);
    }

    public double getCollisionHeight() {
        return Math.max(10.0, height - collisionInsetTop - collisionInsetBottom);
    }

    public double getHpBarScreenY(double cameraY) {
        return Math.round(y - cameraY - hpBarOffsetY);
    }
}
