package entity;

import animation.SpriteAnimation;
import animation.SpriteSheetLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class Player extends Entity {
    private static final String PLAYER_ASSET_ROOT = "file:assets/player/";
    private static final long IDLE_FRAME_NS = 140_000_000L;
    private static final long WALK_FRAME_NS = 100_000_000L;
    private static final long RUN_FRAME_NS = 80_000_000L;
    private static final long HIT_FRAME_NS = 85_000_000L;
    private static final long SLICE_FRAME_NS = 80_000_000L;
    private static final long CRUSH_FRAME_NS = 80_000_000L;
    private static final long PIERCE_FRAME_NS = 80_000_000L;
    private static final long DEATH_FRAME_NS = 110_000_000L;

    private enum FacingDirection {
        DOWN,
        SIDE,
        UP
    }

    public enum AttackAnimationType {
        HIT,
        SLICE,
        CRUSH,
        PIERCE
    }

    public enum EquipmentMode {
        HAND_MODE,
        AXE_MODE
    }

    private double energy;
    private double maxEnergy;
    private int level;
    private int experience;
    private int experienceToNextLevel;
    private long levelUpEffectStartedAtNs;
    private long levelUpEffectDurationNs;
    private int lastLeveledUpTo;

    private final SpriteAnimation idleDownAnimation;
    private final SpriteAnimation idleSideAnimation;
    private final SpriteAnimation idleUpAnimation;
    private final SpriteAnimation walkDownAnimation;
    private final SpriteAnimation walkSideAnimation;
    private final SpriteAnimation walkUpAnimation;
    private final SpriteAnimation runDownAnimation;
    private final SpriteAnimation runSideAnimation;
    private final SpriteAnimation runUpAnimation;
    private final SpriteAnimation sliceDownAnimation;
    private final SpriteAnimation sliceSideAnimation;
    private final SpriteAnimation sliceUpAnimation;
    private final SpriteAnimation crushDownAnimation;
    private final SpriteAnimation crushSideAnimation;
    private final SpriteAnimation crushUpAnimation;
    private final SpriteAnimation pierceDownAnimation;
    private final SpriteAnimation pierceSideAnimation;
    private final SpriteAnimation pierceUpAnimation;
    private final SpriteAnimation hitDownAnimation;
    private final SpriteAnimation hitSideAnimation;
    private final SpriteAnimation hitUpAnimation;
    private final SpriteAnimation deathDownAnimation;
    private final SpriteAnimation deathSideAnimation;
    private final SpriteAnimation deathUpAnimation;

    private FacingDirection facingDirection;
    private boolean facingRight;
    private Image currentFrame;
    private String playerName;
    private boolean attacking;
    private long attackStartedAtNs;
    private long attackDurationNs;
    private AttackAnimationType currentAttackType;
    private boolean sprinting;
    private EquipmentMode equipmentMode;
    private static final double SPRINT_SPEED_MULTIPLIER = 1.8;

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

        Image[] idleDownFrames = loadStrip("Idle_Base/Idle_Down-Sheet.png", 4);
        Image[] idleSideFrames = loadStrip("Idle_Base/Idle_Side-Sheet.png", 4);
        Image[] idleUpFrames = loadStrip("Idle_Base/Idle_Up-Sheet.png", 4);
        Image[] walkDownFrames = loadStrip("Walk_Base/Walk_Down-Sheet.png", 6);
        Image[] walkSideFrames = loadStrip("Walk_Base/Walk_Side-Sheet.png", 6);
        Image[] walkUpFrames = loadStrip("Walk_Base/Walk_Up-Sheet.png", 6);
        Image[] runDownFrames = loadStrip("Run_Base/Run_Down-Sheet.png", 6);
        Image[] runSideFrames = loadStrip("Run_Base/Run_Side-Sheet.png", 6);
        Image[] runUpFrames = loadStrip("Run_Base/Run_Up-Sheet.png", 6);
        Image[] sliceDownFrames = loadStrip("Slice_Base/Slice_Down-Sheet.png", 8);
        Image[] sliceSideFrames = loadStrip("Slice_Base/Slice_Side-Sheet.png", 8);
        Image[] sliceUpFrames = loadStrip("Slice_Base/Slice_Up-Sheet.png", 8);
        Image[] crushDownFrames = loadStrip("../tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Crush_Base/Crush_Down-Sheet.png", 8);
        Image[] crushSideFrames = loadStrip("../tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Crush_Base/Crush_Side-Sheet.png", 8);
        Image[] crushUpFrames = loadStrip("../tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Crush_Base/Crush_Up-Sheet.png", 8);
        Image[] pierceDownFrames = loadStrip("../tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Pierce_Base/Pierce_Down-Sheet.png", 8);
        Image[] pierceSideFrames = loadStrip("../tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Pierce_Base/Pierce_Side-Sheet.png", 8);
        Image[] pierceUpFrames = loadStrip("../tilesets/Pixel Crawler - Free Pack/Entities/Characters/Body_A/Animations/Pierce_Base/Pierce_Top-Sheet.png", 8);
        Image[] hitDownFrames = loadStrip("Hit_Base/Hit_Down-Sheet.png", 4);
        Image[] hitSideFrames = loadStrip("Hit_Base/Hit_Side-Sheet.png", 4);
        Image[] hitUpFrames = loadStrip("Hit_Base/Hit_Up-Sheet.png", 4);
        Image[] deathDownFrames = loadStrip("Death_Base/Death_Down-Sheet.png", 8);
        Image[] deathSideFrames = loadStrip("Death_Base/Death_Side-Sheet.png", 8);
        Image[] deathUpFrames = loadStrip("Death_Base/Death_Up-Sheet.png", 8);

        int canvasWidth = findMaxFrameWidth(
                idleDownFrames, idleSideFrames, idleUpFrames,
                walkDownFrames, walkSideFrames, walkUpFrames,
                runDownFrames, runSideFrames, runUpFrames,
                sliceDownFrames, sliceSideFrames, sliceUpFrames,
                crushDownFrames, crushSideFrames, crushUpFrames,
                pierceDownFrames, pierceSideFrames, pierceUpFrames,
                hitDownFrames, hitSideFrames, hitUpFrames,
                deathDownFrames, deathSideFrames, deathUpFrames
        );
        int canvasHeight = findMaxFrameHeight(
                idleDownFrames, idleSideFrames, idleUpFrames,
                walkDownFrames, walkSideFrames, walkUpFrames,
                runDownFrames, runSideFrames, runUpFrames,
                sliceDownFrames, sliceSideFrames, sliceUpFrames,
                crushDownFrames, crushSideFrames, crushUpFrames,
                pierceDownFrames, pierceSideFrames, pierceUpFrames,
                hitDownFrames, hitSideFrames, hitUpFrames,
                deathDownFrames, deathSideFrames, deathUpFrames
        );

        this.idleDownAnimation = new SpriteAnimation(normalizeFrames(idleDownFrames, canvasWidth, canvasHeight), IDLE_FRAME_NS);
        this.idleSideAnimation = new SpriteAnimation(normalizeFrames(idleSideFrames, canvasWidth, canvasHeight), IDLE_FRAME_NS);
        this.idleUpAnimation = new SpriteAnimation(normalizeFrames(idleUpFrames, canvasWidth, canvasHeight), IDLE_FRAME_NS);
        this.walkDownAnimation = new SpriteAnimation(normalizeFrames(walkDownFrames, canvasWidth, canvasHeight), WALK_FRAME_NS);
        this.walkSideAnimation = new SpriteAnimation(normalizeFrames(walkSideFrames, canvasWidth, canvasHeight), WALK_FRAME_NS);
        this.walkUpAnimation = new SpriteAnimation(normalizeFrames(walkUpFrames, canvasWidth, canvasHeight), WALK_FRAME_NS);
        this.runDownAnimation = new SpriteAnimation(normalizeFrames(runDownFrames, canvasWidth, canvasHeight), RUN_FRAME_NS);
        this.runSideAnimation = new SpriteAnimation(normalizeFrames(runSideFrames, canvasWidth, canvasHeight), RUN_FRAME_NS);
        this.runUpAnimation = new SpriteAnimation(normalizeFrames(runUpFrames, canvasWidth, canvasHeight), RUN_FRAME_NS);
        this.sliceDownAnimation = new SpriteAnimation(normalizeFrames(sliceDownFrames, canvasWidth, canvasHeight), SLICE_FRAME_NS);
        this.sliceSideAnimation = new SpriteAnimation(normalizeFrames(sliceSideFrames, canvasWidth, canvasHeight), SLICE_FRAME_NS);
        this.sliceUpAnimation = new SpriteAnimation(normalizeFrames(sliceUpFrames, canvasWidth, canvasHeight), SLICE_FRAME_NS);
        this.crushDownAnimation = new SpriteAnimation(normalizeFrames(crushDownFrames, canvasWidth, canvasHeight), CRUSH_FRAME_NS);
        this.crushSideAnimation = new SpriteAnimation(normalizeFrames(crushSideFrames, canvasWidth, canvasHeight), CRUSH_FRAME_NS);
        this.crushUpAnimation = new SpriteAnimation(normalizeFrames(crushUpFrames, canvasWidth, canvasHeight), CRUSH_FRAME_NS);
        this.pierceDownAnimation = new SpriteAnimation(normalizeFrames(pierceDownFrames, canvasWidth, canvasHeight), PIERCE_FRAME_NS);
        this.pierceSideAnimation = new SpriteAnimation(normalizeFrames(pierceSideFrames, canvasWidth, canvasHeight), PIERCE_FRAME_NS);
        this.pierceUpAnimation = new SpriteAnimation(normalizeFrames(pierceUpFrames, canvasWidth, canvasHeight), PIERCE_FRAME_NS);
        this.hitDownAnimation = new SpriteAnimation(normalizeFrames(hitDownFrames, canvasWidth, canvasHeight), HIT_FRAME_NS);
        this.hitSideAnimation = new SpriteAnimation(normalizeFrames(hitSideFrames, canvasWidth, canvasHeight), HIT_FRAME_NS);
        this.hitUpAnimation = new SpriteAnimation(normalizeFrames(hitUpFrames, canvasWidth, canvasHeight), HIT_FRAME_NS);
        this.deathDownAnimation = new SpriteAnimation(normalizeFrames(deathDownFrames, canvasWidth, canvasHeight), DEATH_FRAME_NS);
        this.deathSideAnimation = new SpriteAnimation(normalizeFrames(deathSideFrames, canvasWidth, canvasHeight), DEATH_FRAME_NS);
        this.deathUpAnimation = new SpriteAnimation(normalizeFrames(deathUpFrames, canvasWidth, canvasHeight), DEATH_FRAME_NS);

        this.facingDirection = FacingDirection.DOWN;
        this.facingRight = true;
        this.currentFrame = idleDownAnimation.getCurrentFrame();
        this.playerName = "Player";
        this.attacking = false;
        this.attackStartedAtNs = 0L;
        this.attackDurationNs = 360_000_000L;
        this.currentAttackType = AttackAnimationType.HIT;
        this.sprinting = false;
        this.equipmentMode = EquipmentMode.HAND_MODE;
    }

    public void moveLeft() {
        x -= getCurrentMoveSpeed();
    }

    public void moveRight() {
        x += getCurrentMoveSpeed();
    }

    public void moveUp() {
        y -= getCurrentMoveSpeed();
    }

    public void moveDown() {
        y += getCurrentMoveSpeed();
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

    public Image getSkillUnlockPreviewFrame(long nowNs, AttackAnimationType attackType) {
        AttackAnimationType resolvedType = attackType == null ? AttackAnimationType.SLICE : attackType;
        SpriteAnimation previewAnimation = switch (resolvedType) {
            case CRUSH -> crushDownAnimation;
            case PIERCE -> pierceDownAnimation;
            case HIT -> hitDownAnimation;
            case SLICE -> sliceDownAnimation;
        };
        long frameDurationNs = switch (resolvedType) {
            case CRUSH -> CRUSH_FRAME_NS;
            case PIERCE -> PIERCE_FRAME_NS;
            case HIT -> HIT_FRAME_NS;
            case SLICE -> SLICE_FRAME_NS;
        };
        int frameCount = Math.max(1, previewAnimation.getFrameCount());
        int frameIndex = (int) ((nowNs / frameDurationNs) % frameCount);
        return previewAnimation.getFrameAtIndex(frameIndex);
    }

    public int getLastLeveledUpTo() {
        return lastLeveledUpTo;
    }

    public void reset(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.hp = maxHp;
        this.energy = maxEnergy;
        this.attacking = false;
        resetAllAttackAnimations();
        resetDeathAnimations();
        this.currentFrame = idleDownAnimation.getCurrentFrame();
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setEnergyForLoad(double restoredEnergy) {
        this.energy = Math.max(0, Math.min(maxEnergy, restoredEnergy));
    }

    public void updateAnimation(long now, boolean moving, boolean moveUp, boolean moveDown, boolean moveLeft, boolean moveRight) {
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

        if (!isAlive()) {
            SpriteAnimation deathAnimation = currentDeathAnimation();
            deathAnimation.updateOnce(now);
            currentFrame = deathAnimation.getCurrentFrame();
            return;
        }

        if (attacking) {
            SpriteAnimation attackAnimation = currentAttackAnimation();
            attackAnimation.update(now, true);
            currentFrame = attackAnimation.getCurrentFrame();

            if (now - attackStartedAtNs >= attackDurationNs) {
                attacking = false;
                resetAllAttackAnimations();
            }
            return;
        }

        SpriteAnimation activeAnimation;
        if (moving) {
            if (sprinting) {
                activeAnimation = switch (facingDirection) {
                    case UP -> runUpAnimation;
                    case SIDE -> runSideAnimation;
                    case DOWN -> runDownAnimation;
                };
            } else {
                activeAnimation = switch (facingDirection) {
                    case UP -> walkUpAnimation;
                    case SIDE -> walkSideAnimation;
                    case DOWN -> walkDownAnimation;
                };
            }
        } else {
            activeAnimation = switch (facingDirection) {
                case UP -> idleUpAnimation;
                case SIDE -> idleSideAnimation;
                case DOWN -> idleDownAnimation;
            };
        }

        activeAnimation.update(now, true);
        currentFrame = activeAnimation.getCurrentFrame();
    }

    public boolean startAttack(long now, AttackAnimationType attackType) {
        if (!isAlive() || attacking) {
            return false;
        }
        attacking = true;
        attackStartedAtNs = now;
        currentAttackType = attackType == null ? getAttackAnimationTypeForCurrentMode() : attackType;
        resetAllAttackAnimations();
        attackDurationNs = currentAttackAnimation().getFrameCount()
                * switch (currentAttackType) {
                    case SLICE -> SLICE_FRAME_NS;
                    case CRUSH -> CRUSH_FRAME_NS;
                    case PIERCE -> PIERCE_FRAME_NS;
                    case HIT -> HIT_FRAME_NS;
                };
        return true;
    }

    public boolean startAttack(long now) {
        return startAttack(now, getAttackAnimationTypeForCurrentMode());
    }

    public boolean isAttacking() {
        return attacking;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public double getCurrentMoveSpeed() {
        return sprinting ? speed * SPRINT_SPEED_MULTIPLIER : speed;
    }

    public EquipmentMode getEquipmentMode() {
        return equipmentMode;
    }

    public void setEquipmentMode(EquipmentMode equipmentMode) {
        this.equipmentMode = equipmentMode == null ? EquipmentMode.HAND_MODE : equipmentMode;
    }

    public boolean isAxeEquipped() {
        return equipmentMode == EquipmentMode.AXE_MODE;
    }

    public AttackAnimationType getAttackAnimationTypeForCurrentMode() {
        return isAxeEquipped() ? AttackAnimationType.SLICE : AttackAnimationType.HIT;
    }

    @Override
    protected double collisionInsetLeft(double width, double height) {
        return width * 0.22;
    }

    @Override
    protected double collisionInsetRight(double width, double height) {
        return width * 0.22;
    }

    @Override
    protected double collisionInsetTop(double width, double height) {
        return height * 0.30;
    }

    @Override
    protected double collisionInsetBottom(double width, double height) {
        return height * 0.08;
    }

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

    public void recoverEnergy(double amount) {
        if (amount <= 0) {
            return;
        }
        energy += amount;
        if (energy > maxEnergy) {
            energy = maxEnergy;
        }
    }

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
        double scale = 1.01;
        double oldCenterX = x + width / 2.0;
        double oldCenterY = y + height / 2.0;

        width *= scale;
        height *= scale;

        x = oldCenterX - width / 2.0;
        y = oldCenterY - height / 2.0;

        maxEnergy += 4;
        energy = maxEnergy;
        levelUpEffectStartedAtNs = System.nanoTime();
        lastLeveledUpTo = level;
    }

    public double[] buildAttackHitbox() {
        double hitboxWidth = width * 0.90;
        double hitboxHeight = height * 0.90;
        double range = 26;

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

        drawPlayerName(graphicsContext, screenX, screenY);
    }

    private void drawPlayerName(GraphicsContext graphicsContext, double screenX, double screenY) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return;
        }

        String visibleName = playerName.trim();
        graphicsContext.save();
        graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 8));

        double nameWidth = measureTextWidth(graphicsContext, visibleName);
        double textX = screenX + (width - nameWidth) / 2;
        double textY = screenY - 0.1;

        graphicsContext.setStroke(Color.color(0, 0, 0, 0.8));
        graphicsContext.strokeText(visibleName, textX, textY);

        graphicsContext.setFill(Color.color(1, 1, 1, 0.95));
        graphicsContext.fillText(visibleName, textX, textY);

        graphicsContext.restore();
    }

    private double measureTextWidth(GraphicsContext graphicsContext, String text) {
        Text helper = new Text(text);
        helper.setFont(graphicsContext.getFont());
        return helper.getLayoutBounds().getWidth();
    }

    private Image[] loadStrip(String relativePath, int columns) {
        return SpriteSheetLoader.loadGrid(PLAYER_ASSET_ROOT + relativePath, columns, 1);
    }

    private int findMaxFrameWidth(Image[]... frameGroups) {
        int max = 1;
        for (Image[] frames : frameGroups) {
            if (frames == null) {
                continue;
            }
            for (Image frame : frames) {
                if (frame != null) {
                    max = Math.max(max, (int) Math.ceil(frame.getWidth()));
                }
            }
        }
        return max;
    }

    private int findMaxFrameHeight(Image[]... frameGroups) {
        int max = 1;
        for (Image[] frames : frameGroups) {
            if (frames == null) {
                continue;
            }
            for (Image frame : frames) {
                if (frame != null) {
                    max = Math.max(max, (int) Math.ceil(frame.getHeight()));
                }
            }
        }
        return max;
    }

    private Image[] normalizeFrames(Image[] frames, int canvasWidth, int canvasHeight) {
        List<Image> normalized = new ArrayList<>();
        if (frames == null) {
            return new Image[0];
        }
        for (Image frame : frames) {
            normalized.add(centerFrame(frame, canvasWidth, canvasHeight));
        }
        return normalized.toArray(new Image[0]);
    }

    private Image centerFrame(Image frame, int canvasWidth, int canvasHeight) {
        if (frame == null || frame.getPixelReader() == null) {
            return frame;
        }
        int width = Math.max(1, canvasWidth);
        int height = Math.max(1, canvasHeight);
        WritableImage canvas = new WritableImage(width, height);
        PixelWriter writer = canvas.getPixelWriter();
        PixelReader reader = frame.getPixelReader();
        int frameWidth = (int) Math.ceil(frame.getWidth());
        int frameHeight = (int) Math.ceil(frame.getHeight());
        int drawX = Math.max(0, (width - frameWidth) / 2);
        int drawY = Math.max(0, height - frameHeight);
        for (int y = 0; y < frameHeight; y++) {
            for (int x = 0; x < frameWidth; x++) {
                writer.setArgb(drawX + x, drawY + y, reader.getArgb(x, y));
            }
        }
        return canvas;
    }

    private SpriteAnimation currentAttackAnimation() {
        return switch (currentAttackType) {
            case SLICE -> switch (facingDirection) {
                case UP -> sliceUpAnimation;
                case SIDE -> sliceSideAnimation;
                case DOWN -> sliceDownAnimation;
            };
            case CRUSH -> switch (facingDirection) {
                case UP -> crushUpAnimation;
                case SIDE -> crushSideAnimation;
                case DOWN -> crushDownAnimation;
            };
            case PIERCE -> switch (facingDirection) {
                case UP -> pierceUpAnimation;
                case SIDE -> pierceSideAnimation;
                case DOWN -> pierceDownAnimation;
            };
            case HIT -> switch (facingDirection) {
                case UP -> hitUpAnimation;
                case SIDE -> hitSideAnimation;
                case DOWN -> hitDownAnimation;
            };
        };
    }

    private SpriteAnimation currentDeathAnimation() {
        return switch (facingDirection) {
            case UP -> deathUpAnimation;
            case SIDE -> deathSideAnimation;
            case DOWN -> deathDownAnimation;
        };
    }

    private void resetAllAttackAnimations() {
        sliceDownAnimation.reset();
        sliceSideAnimation.reset();
        sliceUpAnimation.reset();
        crushDownAnimation.reset();
        crushSideAnimation.reset();
        crushUpAnimation.reset();
        pierceDownAnimation.reset();
        pierceSideAnimation.reset();
        pierceUpAnimation.reset();
        hitDownAnimation.reset();
        hitSideAnimation.reset();
        hitUpAnimation.reset();
    }

    private void resetDeathAnimations() {
        deathDownAnimation.reset();
        deathSideAnimation.reset();
        deathUpAnimation.reset();
    }
}
