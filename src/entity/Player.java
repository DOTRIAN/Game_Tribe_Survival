package entity;

import animation.SpriteAnimation;
import animation.SpriteSheetLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class Player extends Entity {
    private enum FacingDirection {
        DOWN,
        SIDE,
        UP
    }
    public enum AttackAnimationType {
        HIT,
        SLICE
    }

    // Nang luong dung cho movement/skill.
    private double energy;
    private double maxEnergy;
    // Tien trinh phat trien nhan vat.
    private int level;
    private int experience;
    private int experienceToNextLevel;
    // Hieu ung level-up (popup text + mui ten) de tao feedback manh cho nguoi choi.
    private long levelUpEffectStartedAtNs;
    private long levelUpEffectDurationNs;
    private int lastLeveledUpTo;

    private final SpriteAnimation idleDownAnimation;
    private final SpriteAnimation idleSideAnimation;
    private final SpriteAnimation idleUpAnimation;
    private final SpriteAnimation walkDownAnimation;
    private final SpriteAnimation walkSideAnimation;
    private final SpriteAnimation walkUpAnimation;
    private final SpriteAnimation sliceDownAnimation;
    private final SpriteAnimation sliceSideAnimation;
    private final SpriteAnimation sliceUpAnimation;
    private final SpriteAnimation hitDownAnimation;
    private final SpriteAnimation hitSideAnimation;
    private final SpriteAnimation hitUpAnimation;

    private FacingDirection facingDirection;
    private boolean facingRight;
    private Image currentFrame;
    // Ten hien thi tren dau nhan vat khi render trong world.
    private String playerName;
    // Trang thai tan cong (slice) de hien animation chem.
    private boolean attacking;
    private long attackStartedAtNs;
    private long attackDurationNs;
    private AttackAnimationType currentAttackType;

    public Player(double x, double y, double width, double height, double speed, int maxHp) {
        super(x, y, width, height, speed, maxHp);
        this.maxEnergy = 100;
        this.energy = maxEnergy;
        this.level = 1;
        this.experience = 0;
        this.experienceToNextLevel = 10;
        this.levelUpEffectStartedAtNs = -1L;
        this.levelUpEffectDurationNs = 2_200_000_000L;
        this.lastLeveledUpTo = 1;

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
                        8,
                        1
                ),
                80_000_000L
        );
        this.sliceSideAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(
                        "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Slice_Base/Slice_Side-Sheet.png",
                        8,
                        1
                ),
                80_000_000L
        );
        this.sliceUpAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(
                        "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Slice_Base/Slice_Up-Sheet.png",
                        8,
                        1
                ),
                80_000_000L
        );
        this.hitDownAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(
                        "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Hit_Base/Hit_Down-Sheet.png",
                        4,
                        1
                ),
                90_000_000L
        );
        this.hitSideAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(
                        "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Hit_Base/Hit_Side-Sheet.png",
                        4,
                        1
                ),
                90_000_000L
        );
        this.hitUpAnimation = new SpriteAnimation(
                SpriteSheetLoader.loadGrid(
                        "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Hit_Base/Hit_Up-Sheet.png",
                        4,
                        1
                ),
                90_000_000L
        );

        this.facingDirection = FacingDirection.DOWN;
        this.facingRight = true;
        this.currentFrame = idleDownAnimation.getCurrentFrame();
        this.playerName = "Player";
        this.attacking = false;
        this.attackStartedAtNs = 0L;
        this.attackDurationNs = 360_000_000L;
        this.currentAttackType = AttackAnimationType.HIT;
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

    public double getEnergy() {
        return energy;
    }

    public double getMaxEnergy() {
        return maxEnergy;
    }

    public int getLevel() {
        return level;
    }

    public int getExperience() {
        return experience;
    }

    public int getExperienceToNextLevel() {
        return experienceToNextLevel;
    }

    public boolean isLevelUpEffectActive(long nowNs) {
        return levelUpEffectStartedAtNs >= 0
                && nowNs - levelUpEffectStartedAtNs <= levelUpEffectDurationNs;
    }

    public double getLevelUpEffectProgress(long nowNs) {
        if (!isLevelUpEffectActive(nowNs)) {
            return 1.0;
        }
        double elapsed = (double) (nowNs - levelUpEffectStartedAtNs);
        return Math.max(0, Math.min(1, elapsed / levelUpEffectDurationNs));
    }

    public int getLastLeveledUpTo() {
        return lastLeveledUpTo;
    }

    public void reset(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.hp = maxHp;
        this.energy = maxEnergy;
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
        // Neu dang tan cong, khoa animation movement va uu tien animation tan cong.
        if (attacking) {
            SpriteAnimation attackAnimation;
            if (currentAttackType == AttackAnimationType.SLICE) {
                if (facingDirection == FacingDirection.UP) {
                    attackAnimation = sliceUpAnimation;
                } else if (facingDirection == FacingDirection.SIDE) {
                    attackAnimation = sliceSideAnimation;
                } else {
                    attackAnimation = sliceDownAnimation;
                }
            } else {
                if (facingDirection == FacingDirection.UP) {
                    attackAnimation = hitUpAnimation;
                } else if (facingDirection == FacingDirection.SIDE) {
                    attackAnimation = hitSideAnimation;
                } else {
                    attackAnimation = hitDownAnimation;
                }
            }

            attackAnimation.update(now, true);
            currentFrame = attackAnimation.getCurrentFrame();

            if (now - attackStartedAtNs >= attackDurationNs) {
                attacking = false;
                sliceDownAnimation.reset();
                sliceSideAnimation.reset();
                sliceUpAnimation.reset();
                hitDownAnimation.reset();
                hitSideAnimation.reset();
                hitUpAnimation.reset();
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

    // Bat dau 1 lan tan cong neu player dang ranh.
    // HIT: danh thuong, SLICE: de danh cho vu khi sau nay.
    public boolean startAttack(long now, AttackAnimationType attackType) {
        if (attacking) {
            return false;
        }
        attacking = true;
        attackStartedAtNs = now;
        currentAttackType = attackType == null ? AttackAnimationType.HIT : attackType;

        // Reset tat ca animation tan cong de lan moi bat dau tu frame 0.
        sliceDownAnimation.reset();
        sliceSideAnimation.reset();
        sliceUpAnimation.reset();
        hitDownAnimation.reset();
        hitSideAnimation.reset();
        hitUpAnimation.reset();

        if (currentAttackType == AttackAnimationType.SLICE) {
            attackDurationNs = sliceDownAnimation.getFrameCount() * 80_000_000L;
        } else {
            attackDurationNs = hitDownAnimation.getFrameCount() * 90_000_000L;
        }
        return true;
    }

    public boolean startAttack(long now) {
        return startAttack(now, AttackAnimationType.HIT);
    }

    public boolean isAttacking() {
        return attacking;
    }

    // Tru nang luong, tra ve false neu khong du de thuc hien hanh dong.
    public boolean consumeEnergy(double amount) {
        if (amount <= 0) {
            return true;
        }
        if (energy < amount) {
            return false;
        }
        energy -= amount;
        if (energy < 0) {
            energy = 0;
        }
        return true;
    }

    // Hoi nang luong co clamp [0..maxEnergy].
    public void recoverEnergy(double amount) {
        if (amount <= 0) {
            return;
        }
        energy += amount;
        if (energy > maxEnergy) {
            energy = maxEnergy;
        }
    }

    // Cong kinh nghiem va tu dong level-up khi dat nguong.
    // Level-up don gian: tang size player de tao feedback ro rang trong MVP.
    public void addExperience(int amount) {
        if (amount <= 0) {
            return;
        }
        experience += amount;
        while (experience >= experienceToNextLevel) {
            experience -= experienceToNextLevel;
            level++;
            growOnLevelUp();
        }
    }

    private void growOnLevelUp() {
        // Tang nhe kich thuoc moi level de tranh pha collision qua manh.
        // Dat 10% de nhin ro hon trong gameplay.
        double scale = 1.10;
        double oldCenterX = x + width / 2.0;
        double oldCenterY = y + height / 2.0;

        width *= scale;
        height *= scale;

        // Giu tam nhan vat de khong giat manh vi tri khi level-up.
        x = oldCenterX - width / 2.0;
        y = oldCenterY - height / 2.0;

        // Tang toi da nang luong nhe theo level de tao phan thuong lau dai.
        maxEnergy += 4;
        energy = maxEnergy;
        // Kick hieu ung popup level-up.
        levelUpEffectStartedAtNs = System.nanoTime();
        lastLeveledUpTo = level;
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
