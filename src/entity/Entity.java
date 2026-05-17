package entity;

import javafx.scene.paint.Color;

/**
 * Entity:
 * - Lop nen chung cho tat ca doi tuong gameplay co the:
 *   1) ton tai trong world space
 *   2) co hitbox AABB
 *   3) co HP / bi damage / chet
 *
 * Muc tieu cua lop nay:
 * - Gom chung du lieu co ban cho Player, quai, cay, vat the pha duoc...
 * - Cho phep system ben ngoai (DamageSystem, CollisionSystem) chi phu thuoc vao 1 contract duy nhat.
 * - Giam viec viet lai getX/getY/getWidth/getHeight/isAlive/takeDamage o moi class con.
 */
public abstract class Entity {
    protected double x;
    protected double y;
    protected double width;
    protected double height;

    // speed khong bat buoc moi entity phai dung, nhung de san trong base class
    // vi Player va Enemy deu can.
    protected double speed;

    // HP chung:
    // - Entity "song" thi maxHp > 0
    // - Entity tinh/static van co the co hp de pha duoc (vd: cay, da)
    protected int hp;
    protected int maxHp;
    // Hieu ung "bi danh" cho moi entity:
    // - trigger khi nhan damage
    // - renderer doc de ve overlay nhay mau trong mot khoang thoi gian ngan
    protected long hitFlashUntilNs;
    protected Color hitFlashColor;

    protected Entity(double x, double y, double width, double height, double speed, int maxHp) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = speed;
        this.maxHp = Math.max(1, maxHp);
        this.hp = this.maxHp;
        this.hitFlashUntilNs = -1L;
        this.hitFlashColor = Color.TRANSPARENT;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getSpeed() {
        return speed;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public double getCenterX() {
        return x + width / 2.0;
    }

    public double getCenterY() {
        return y + height / 2.0;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void clampPosition(double minX, double minY, double maxWidth, double maxHeight) {
        if (x < minX) {
            x = minX;
        }
        if (y < minY) {
            y = minY;
        }
        if (x + width > maxWidth) {
            x = maxWidth - width;
        }
        if (y + height > maxHeight) {
            y = maxHeight - height;
        }
    }

    public void takeDamage(int amount) {
        hp -= Math.max(0, amount);
        if (hp < 0) {
            hp = 0;
        }
    }

    public void heal(int amount) {
        hp += Math.max(0, amount);
        if (hp > maxHp) {
            hp = maxHp;
        }
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public boolean isDead() {
        return !isAlive();
    }

    // Danh dau entity dang o trang thai bi danh de renderer ve flash mau.
    public void triggerHitFlash(long nowNs, long durationNs, Color color) {
        this.hitFlashUntilNs = nowNs + Math.max(1L, durationNs);
        this.hitFlashColor = color == null ? Color.TRANSPARENT : color;
    }

    public boolean isHitFlashActive(long nowNs) {
        return nowNs <= hitFlashUntilNs;
    }

    public Color getHitFlashColor() {
        return hitFlashColor;
    }
}
