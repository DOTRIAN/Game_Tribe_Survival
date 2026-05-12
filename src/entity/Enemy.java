package entity;

import animation.SpriteAnimation;
import animation.SpriteSheetLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Enemy (abstract):
 * - Lop cha cho tat ca quai trong game.
 * - Chua state + hanh vi chung:
 *   1) Vi tri/kich thuoc/toc do
 *   2) HP, sat thuong, cooldown tan cong
 *   3) AI co ban: duoi theo player
 *   4) Render animation run/idle + lat trai/phai
 *
 * Muc tieu OOP:
 * - Class con (OrcEnemy, SkeletonEnemy,...) chi can truyen asset + stat rieng.
 * - Gameplay core trong Game xu ly bang da hinh (List<Enemy>).
 */
public abstract class Enemy {
    protected double x;
    protected double y;
    protected double width;
    protected double height;
    protected double speed;

    protected int hp;
    protected int maxHp;
    protected int damage;

    private final SpriteAnimation runAnimation;
    private final SpriteAnimation idleAnimation;
    private Image currentFrame;
    private boolean facingRight;
    private long lastAttackAtNs;
    private final long attackCooldownNs;

    protected Enemy(double x,
                    double y,
                    double width,
                    double height,
                    double speed,
                    int maxHp,
                    int damage,
                    long attackCooldownNs,
                    String runSheetPath,
                    int runColumns,
                    int runRows,
                    long runFrameNs,
                    String idleSheetPath,
                    int idleColumns,
                    int idleRows,
                    long idleFrameNs) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = speed;
        this.maxHp = Math.max(1, maxHp);
        this.hp = this.maxHp;
        this.damage = Math.max(1, damage);
        this.attackCooldownNs = Math.max(1L, attackCooldownNs);

        this.runAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(runSheetPath, runColumns, runRows),
                runFrameNs
        );
        this.idleAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(idleSheetPath, idleColumns, idleRows),
                idleFrameNs
        );
        this.currentFrame = idleAnimation.getCurrentFrame();
        this.facingRight = true;
        this.lastAttackAtNs = 0L;
    }

    public void update(long now, Player player, double worldWidth, double worldHeight) {
        if (!isAlive() || player == null) {
            return;
        }

        double oldX = x;
        double oldY = y;
        moveToward(player);
        clampPosition(0, 0, worldWidth, worldHeight);

        boolean moving = oldX != x || oldY != y;
        if (moving) {
            runAnimation.update(now, true);
            currentFrame = runAnimation.getCurrentFrame();
        } else {
            idleAnimation.update(now, true);
            currentFrame = idleAnimation.getCurrentFrame();
        }
    }

    // AI co ban: duoi thang vao tam player.
    protected void moveToward(Player player) {
        double targetX = player.getX() + player.getWidth() / 2.0;
        double targetY = player.getY() + player.getHeight() / 2.0;
        double selfX = x + width / 2.0;
        double selfY = y + height / 2.0;

        double dx = targetX - selfX;
        double dy = targetY - selfY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= 0.0001) {
            return;
        }

        x += (dx / distance) * speed;
        y += (dy / distance) * speed;
        if (dx != 0) {
            facingRight = dx >= 0;
        }
    }

    // Tan cong player khi va cham va da qua cooldown.
    public void tryAttackPlayer(Player player, long now) {
        if (!isAlive() || player == null) {
            return;
        }
        if (!intersects(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
            return;
        }
        if (now - lastAttackAtNs < attackCooldownNs) {
            return;
        }
        player.takeDamage(damage);
        lastAttackAtNs = now;
    }

    public boolean intersects(double otherX, double otherY, double otherW, double otherH) {
        return x < otherX + otherW
                && x + width > otherX
                && y < otherY + otherH
                && y + height > otherY;
    }

    public void draw(GraphicsContext graphicsContext, double cameraX, double cameraY) {
        if (!isAlive()) {
            return;
        }

        double screenX = x - cameraX;
        double screenY = y - cameraY;
        if (currentFrame == null || currentFrame.isError()) {
            graphicsContext.setFill(Color.CRIMSON);
            graphicsContext.fillRect(screenX, screenY, width, height);
            return;
        }

        if (facingRight) {
            graphicsContext.drawImage(currentFrame, screenX, screenY, width, height);
        } else {
            graphicsContext.save();
            graphicsContext.translate(screenX + width, screenY);
            graphicsContext.scale(-1, 1);
            graphicsContext.drawImage(currentFrame, 0, 0, width, height);
            graphicsContext.restore();
        }
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

    public boolean isAlive() {
        return hp > 0;
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

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    // Dung cho spawn manager phan loai loai quai.
    public abstract String getEnemyType();
}

