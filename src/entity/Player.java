package entity;

import animation.SpriteAnimation;
import animation.SpriteSheetLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class Player {
    private enum FacingDirection {
        DOWN,
        SIDE,
        UP
    }

    private double x;
    private double y;
    private double width;
    private double height;
    private double speed;
    private int hp;
    private int maxHp;

    private final SpriteAnimation idleDownAnimation;
    private final SpriteAnimation idleSideAnimation;
    private final SpriteAnimation idleUpAnimation;
    private final SpriteAnimation walkDownAnimation;
    private final SpriteAnimation walkSideAnimation;
    private final SpriteAnimation walkUpAnimation;
    private final SpriteAnimation sliceDownAnimation;
    private final SpriteAnimation sliceSideAnimation;
    private final SpriteAnimation sliceUpAnimation;

    private FacingDirection facingDirection;
    private boolean facingRight;
    private Image currentFrame;
    // Ten hien thi tren dau nhan vat khi render trong world.
    private String playerName;
    // Trang thai tan cong (slice) de hien animation chem.
    private boolean attacking;
    private long attackStartedAtNs;
    private long attackDurationNs;

    public Player(double x, double y, double width, double height, double speed, int maxHp) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = speed;
        this.maxHp = maxHp;
        this.hp = maxHp;

        this.idleDownAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(
                        "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Idle_Base/Idle_Down-Sheet.png",
                        4,
                        1
                ),
                180_000_000L
        );
        this.idleSideAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(
                        "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Idle_Base/Idle_Side-Sheet.png",
                        4,
                        1
                ),
                180_000_000L
        );
        this.idleUpAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(
                        "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Idle_Base/Idle_Up-Sheet.png",
                        4,
                        1
                ),
                180_000_000L
        );
        this.walkDownAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(
                        "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Walk_Base/Walk_Down-Sheet.png",
                        6,
                        1
                ),
                90_000_000L
        );
        this.walkSideAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(
                        "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Walk_Base/Walk_Side-Sheet.png",
                        6,
                        1
                ),
                90_000_000L
        );
        this.walkUpAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(
                        "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Walk_Base/Walk_Up-Sheet.png",
                        6,
                        1
                ),
                90_000_000L
        );
        this.sliceDownAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(
                        "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Slice_Base/Slice_Down-Sheet.png",
                        6,
                        1
                ),
                80_000_000L
        );
        this.sliceSideAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(
                        "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Slice_Base/Slice_Side-Sheet.png",
                        6,
                        1
                ),
                80_000_000L
        );
        this.sliceUpAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(
                        "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Slice_Base/Slice_Up-Sheet.png",
                        6,
                        1
                ),
                80_000_000L
        );

        this.facingDirection = FacingDirection.DOWN;
        this.facingRight = true;
        this.currentFrame = idleDownAnimation.getCurrentFrame();
        this.playerName = "Player";
        this.attacking = false;
        this.attackStartedAtNs = 0L;
        // Slice co 6 frame * 80ms = 480ms de chay tron ven 1 cycle chem.
        this.attackDurationNs = 480_000_000L;
    }

    public void moveLeft() {
        x -= speed;
    }

    public void moveRight() {
        x += speed;
    }

    public void moveUp() {
        y -= speed;
    }

    public void moveDown() {
        y += speed;
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

    public void takeDamage(int amount) {
        hp -= amount;
        if (hp < 0) {
            hp = 0;
        }
    }

    public void heal(int amount) {
        hp += amount;
        if (hp > maxHp) {
            hp = maxHp;
        }
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public void reset(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.hp = maxHp;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // Getter/Setter ten nhan vat:
    // - Ten duoc validate ben Game (do ngu canh nhap ten o menu nam ben Game).
    // - Player chi dong vai tro luu du lieu + expose cho render.
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void updateAnimation(long now, boolean moving, boolean moveUp, boolean moveDown, boolean moveLeft, boolean moveRight) {
        // Neu dang tan cong, khoa animation movement va uu tien slice.
        if (attacking) {
            SpriteAnimation slashAnimation;
            if (facingDirection == FacingDirection.UP) {
                slashAnimation = sliceUpAnimation;
            } else if (facingDirection == FacingDirection.SIDE) {
                slashAnimation = sliceSideAnimation;
            } else {
                slashAnimation = sliceDownAnimation;
            }

            slashAnimation.update(now, true);
            currentFrame = slashAnimation.getCurrentFrame();

            if (now - attackStartedAtNs >= attackDurationNs) {
                attacking = false;
                sliceDownAnimation.reset();
                sliceSideAnimation.reset();
                sliceUpAnimation.reset();
            }
            return;
        }

        if (moveLeft || moveRight) {
            facingDirection = FacingDirection.SIDE;
            if (moveLeft) {
                facingRight = false;
            } else if (moveRight) {
                facingRight = true;
            }
        } else if (moveUp) {
            facingDirection = FacingDirection.UP;
        } else if (moveDown) {
            facingDirection = FacingDirection.DOWN;
        }

        SpriteAnimation activeAnimation;
        if (moving) {
            if (facingDirection == FacingDirection.UP) {
                activeAnimation = walkUpAnimation;
            } else if (facingDirection == FacingDirection.SIDE) {
                activeAnimation = walkSideAnimation;
            } else {
                activeAnimation = walkDownAnimation;
            }
        } else {
            if (facingDirection == FacingDirection.UP) {
                activeAnimation = idleUpAnimation;
            } else if (facingDirection == FacingDirection.SIDE) {
                activeAnimation = idleSideAnimation;
            } else {
                activeAnimation = idleDownAnimation;
            }
        }

        activeAnimation.update(now, true);
        currentFrame = activeAnimation.getCurrentFrame();
    }

    // Bat dau 1 lan slash neu player dang ranh.
    public boolean startAttack(long now) {
        if (attacking) {
            return false;
        }
        attacking = true;
        attackStartedAtNs = now;
        // Reset ca 3 animation slash de lan attack moi luon bat dau o frame 0.
        sliceDownAnimation.reset();
        sliceSideAnimation.reset();
        sliceUpAnimation.reset();
        return true;
    }

    public boolean isAttacking() {
        return attacking;
    }

    // Tra ve hitbox tan cong don gian theo huong dang quay mat.
    // [0]=x, [1]=y, [2]=w, [3]=h
    public double[] buildAttackHitbox() {
        double hitboxWidth = width * 0.90;
        double hitboxHeight = height * 0.90;
        double range = 26; // tam danh them ve phia truoc.

        double attackX = x + (width - hitboxWidth) / 2;
        double attackY = y + (height - hitboxHeight) / 2;

        if (facingDirection == FacingDirection.UP) {
            attackY -= range;
        } else if (facingDirection == FacingDirection.DOWN) {
            attackY += range;
        } else if (facingRight) {
            attackX += range;
        } else {
            attackX -= range;
        }

        return new double[]{attackX, attackY, hitboxWidth, hitboxHeight};
    }

    public void draw(GraphicsContext graphicsContext, double cameraX, double cameraY) {
        double screenX = x - cameraX;
        double screenY = y - cameraY;

        if (currentFrame == null || currentFrame.isError()) {
            graphicsContext.setFill(Color.DODGERBLUE);
            graphicsContext.fillRect(screenX, screenY, width, height);
            return;
        }

        if (facingDirection == FacingDirection.SIDE && !facingRight) {
            graphicsContext.save();
            graphicsContext.translate(screenX + width, screenY);
            graphicsContext.scale(-1, 1);
            graphicsContext.drawImage(currentFrame, 0, 0, width, height);
            graphicsContext.restore();
        } else {
            graphicsContext.drawImage(currentFrame, screenX, screenY, width, height);
        }

        // Ve ten nhan vat o phia tren dau sprite.
        // Dat sau khi ve player de chac chan ten nam tren layer entity.
        drawPlayerName(graphicsContext, screenX, screenY);
    }

    private void drawPlayerName(GraphicsContext graphicsContext, double screenX, double screenY) {
        // Neu ten null/blank thi khong ve gi de tranh tao rac UI.
        if (playerName == null || playerName.trim().isEmpty()) {
            return;
        }

        String visibleName = playerName.trim();

        // Luu state hien tai de khong lam anh huong font/mau cua cac phan render khac.
        graphicsContext.save();
        // Giam nhe size ten theo yeu cau de khong che gameplay.
        graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 8));

        // Tinh canh giua ten theo be ngang sprite.
        double nameWidth = measureTextWidth(graphicsContext, visibleName);
        double textX = screenX + (width - nameWidth) / 2;
        // Day ten len cao hon mot chut de tranh sat dau sprite.
        double textY = screenY - 0.1;

        // Ve stroke den truoc de ten doc ro tren nen sang/toi bat ky.
        graphicsContext.setStroke(Color.color(0, 0, 0, 0.8));
        graphicsContext.strokeText(visibleName, textX, textY);

        // Ve fill sang ben trong.
        graphicsContext.setFill(Color.color(1, 1, 1, 0.95));
        graphicsContext.fillText(visibleName, textX, textY);

        graphicsContext.restore();
    }

    // Ham helper do rong text theo font hien tai cua graphics context.
    private double measureTextWidth(GraphicsContext graphicsContext, String text) {
        Text helper = new Text(text);
        helper.setFont(graphicsContext.getFont());
        return helper.getLayoutBounds().getWidth();
    }
}
