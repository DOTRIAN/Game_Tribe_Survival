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

import java.nio.file.Path;
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
    private static final String BOSS_PLAYER_ASSET_ROOT = Path.of(
            "assets",
            "Samurai #3 2D Pixel Art v1.2",
            "Samurai #3 2D Pixel Art v1.2",
            "Samurai #3 2D Pixel Art v1.2",
            "samurai"
    ).toString();
    private static final long BOSS_IDLE_FRAME_NS = 110_000_000L;
    private static final long BOSS_RUN_FRAME_NS = 85_000_000L;
    private static final long BOSS_ATTACK_FRAME_NS = 75_000_000L;
    private static final long BOSS_DASH_ATTACK_FRAME_NS = 65_000_000L;
    private static final long BOSS_DEFEND_FRAME_NS = 85_000_000L;
    private static final long BOSS_THROW_FRAME_NS = 80_000_000L;
    private static final long BOSS_JUMP_FRAME_NS = 95_000_000L;
    private static final long BOSS_HURT_FRAME_NS = 95_000_000L;
    private static final long BOSS_DEATH_FRAME_NS = 100_000_000L;
    private static final double BOSS_MODE_WIDTH = 112.0;
    private static final double BOSS_MODE_HEIGHT = 112.0;

    private enum FacingDirection {
        DOWN,
        SIDE,
        UP
    }

    public enum AttackAnimationType {
        HIT,
        SLICE,
        CRUSH,
        PIERCE,
        ATTACK_TWO,
        DASH_ATTACK,
        DEFEND,
        THROW,
        STRONG_ATTACK,
        JUMP
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
    private final SpriteAnimation bossIdleAnimation;
    private final SpriteAnimation bossRunAnimation;
    private final SpriteAnimation bossAttackTwoAnimation;
    private final SpriteAnimation bossDashAttackAnimation;
    private final SpriteAnimation bossDefendAnimation;
    private final SpriteAnimation bossThrowAnimation;
    private final SpriteAnimation bossStrongAttackAnimation;
    private final SpriteAnimation bossJumpAnimation;
    private final SpriteAnimation bossHurtAnimation;
    private final SpriteAnimation bossDeathAnimation;

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
    private boolean bossCombatMode;
    private boolean takingHit;
    private long invulnerableUntilNs;
    private final double defaultWidth;
    private final double defaultHeight;
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
        this.bossIdleAnimation = new SpriteAnimation(loadBossStrip("IDLE.png", 14), BOSS_IDLE_FRAME_NS);
        this.bossRunAnimation = new SpriteAnimation(loadBossStrip("RUN.png", 8), BOSS_RUN_FRAME_NS);
        this.bossAttackTwoAnimation = new SpriteAnimation(loadBossStrip("ATTACK 2.png", 5), BOSS_ATTACK_FRAME_NS);
        this.bossDashAttackAnimation = new SpriteAnimation(loadBossStrip("DASH ATTACK.png", 9), BOSS_DASH_ATTACK_FRAME_NS);
        this.bossDefendAnimation = new SpriteAnimation(loadBossStrip("DEFEND.png", 6), BOSS_DEFEND_FRAME_NS);
        this.bossThrowAnimation = new SpriteAnimation(loadBossStrip("THROW.png", 7), BOSS_THROW_FRAME_NS);
        this.bossStrongAttackAnimation = new SpriteAnimation(loadBossStrip("STRONG ATTACK.png", 11), BOSS_ATTACK_FRAME_NS);
        this.bossJumpAnimation = new SpriteAnimation(loadBossStrip("JUMP.png", 3), BOSS_JUMP_FRAME_NS);
        this.bossHurtAnimation = new SpriteAnimation(loadBossStrip("HURT.png", 4), BOSS_HURT_FRAME_NS);
        this.bossDeathAnimation = new SpriteAnimation(loadBossStrip("DEATH.png", 10), BOSS_DEATH_FRAME_NS);

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
        this.bossCombatMode = false;
        this.takingHit = false;
        this.invulnerableUntilNs = -1L;
        this.defaultWidth = width;
        this.defaultHeight = height;
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
            case ATTACK_TWO -> bossAttackTwoAnimation;
            case DASH_ATTACK -> bossDashAttackAnimation;
            case DEFEND -> bossDefendAnimation;
            case THROW -> bossThrowAnimation;
            case STRONG_ATTACK -> bossStrongAttackAnimation;
            case JUMP -> bossJumpAnimation;
            case CRUSH -> crushDownAnimation;
            case PIERCE -> pierceDownAnimation;
            case HIT -> hitDownAnimation;
            case SLICE -> sliceDownAnimation;
            default -> sliceDownAnimation;
        };
        long frameDurationNs = switch (resolvedType) {
            case ATTACK_TWO -> BOSS_ATTACK_FRAME_NS;
            case DASH_ATTACK -> BOSS_DASH_ATTACK_FRAME_NS;
            case DEFEND -> BOSS_DEFEND_FRAME_NS;
            case THROW -> BOSS_THROW_FRAME_NS;
            case STRONG_ATTACK -> BOSS_ATTACK_FRAME_NS;
            case JUMP -> BOSS_JUMP_FRAME_NS;
            case CRUSH -> CRUSH_FRAME_NS;
            case PIERCE -> PIERCE_FRAME_NS;
            case HIT -> HIT_FRAME_NS;
            case SLICE -> SLICE_FRAME_NS;
            default -> SLICE_FRAME_NS;
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
        this.takingHit = false;
        this.invulnerableUntilNs = -1L;
        resetAllAttackAnimations();
        resetHitAnimations();
        resetDeathAnimations();
        this.currentFrame = bossCombatMode ? bossIdleAnimation.getCurrentFrame() : idleDownAnimation.getCurrentFrame();
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
        if (bossCombatMode) {
            updateBossAnimation(now, moving, moveLeft, moveRight);
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

        if (!isAlive()) {
            SpriteAnimation deathAnimation = currentDeathAnimation();
            deathAnimation.updateOnce(now);
            currentFrame = deathAnimation.getCurrentFrame();
            return;
        }

        if (takingHit) {
            SpriteAnimation hitAnimation = currentHitAnimation();
            boolean finished = hitAnimation.updateOnce(now);
            currentFrame = hitAnimation.getCurrentFrame();
            if (finished) {
                takingHit = false;
                resetHitAnimations();
            }
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
                    case ATTACK_TWO -> BOSS_ATTACK_FRAME_NS;
                    case DASH_ATTACK -> BOSS_DASH_ATTACK_FRAME_NS;
                    case DEFEND -> BOSS_DEFEND_FRAME_NS;
                    case THROW -> BOSS_THROW_FRAME_NS;
                    case STRONG_ATTACK -> BOSS_ATTACK_FRAME_NS;
                    case JUMP -> BOSS_JUMP_FRAME_NS;
                };
        return true;
    }

    public boolean startAttack(long now) {
        return startAttack(now, getAttackAnimationTypeForCurrentMode());
    }

    public boolean isAttacking() {
        return attacking;
    }

    public boolean isBossCombatMode() {
        return bossCombatMode;
    }

    public void setBossCombatMode(boolean bossCombatMode) {
        if (this.bossCombatMode == bossCombatMode) {
            return;
        }
        double centerX = getCenterX();
        double feetY = y + height;
        this.bossCombatMode = bossCombatMode;
        this.width = bossCombatMode ? BOSS_MODE_WIDTH : defaultWidth;
        this.height = bossCombatMode ? BOSS_MODE_HEIGHT : defaultHeight;
        this.x = centerX - width / 2.0;
        this.y = feetY - height;
        this.attacking = false;
        this.takingHit = false;
        this.sprinting = false;
        this.invulnerableUntilNs = -1L;
        resetAllAttackAnimations();
        resetHitAnimations();
        resetDeathAnimations();
        currentFrame = bossCombatMode ? bossIdleAnimation.getCurrentFrame() : idleDownAnimation.getCurrentFrame();
    }

    public boolean isDefending() {
        return bossCombatMode
                && attacking
                && currentAttackType == AttackAnimationType.DEFEND
                && isAlive();
    }

    public void grantInvulnerability(long untilNs) {
        invulnerableUntilNs = Math.max(invulnerableUntilNs, untilNs);
    }

    public boolean isInvulnerable(long nowNs) {
        return nowNs <= invulnerableUntilNs;
    }

    public boolean isFacingRight() {
        return facingRight;
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
        if (bossCombatMode) {
            return AttackAnimationType.ATTACK_TWO;
        }
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
            energy = 0;
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
        if (bossCombatMode) {
            return buildBossAttackHitbox();
        }
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

    @Override
    public void takeDamage(int amount) {
        boolean aliveBefore = isAlive();
        super.takeDamage(amount);
        if (!aliveBefore || hp <= 0) {
            takingHit = false;
            return;
        }
        takingHit = true;
        attacking = false;
        resetAllAttackAnimations();
        resetHitAnimations();
        currentFrame = currentHitAnimation().getCurrentFrame();
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

    private Image[] loadBossStrip(String fileName, int frameCount) {
        return SpriteSheetLoader.loadHorizontalStrip(Path.of(BOSS_PLAYER_ASSET_ROOT, fileName).toString(), frameCount);
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
        if (bossCombatMode) {
            return switch (currentAttackType) {
                case DASH_ATTACK -> bossDashAttackAnimation;
                case DEFEND -> bossDefendAnimation;
                case THROW -> bossThrowAnimation;
                case STRONG_ATTACK -> bossStrongAttackAnimation;
                case JUMP -> bossJumpAnimation;
                case ATTACK_TWO, HIT, SLICE, CRUSH, PIERCE -> bossAttackTwoAnimation;
            };
        }
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
            case ATTACK_TWO, DASH_ATTACK, DEFEND, THROW, STRONG_ATTACK, JUMP -> hitSideAnimation;
        };
    }

    private SpriteAnimation currentDeathAnimation() {
        if (bossCombatMode) {
            return bossDeathAnimation;
        }
        return switch (facingDirection) {
            case UP -> deathUpAnimation;
            case SIDE -> deathSideAnimation;
            case DOWN -> deathDownAnimation;
        };
    }

    private SpriteAnimation currentHitAnimation() {
        if (bossCombatMode) {
            return bossHurtAnimation;
        }
        return switch (facingDirection) {
            case UP -> hitUpAnimation;
            case SIDE -> hitSideAnimation;
            case DOWN -> hitDownAnimation;
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
        bossAttackTwoAnimation.reset();
        bossDashAttackAnimation.reset();
        bossDefendAnimation.reset();
        bossThrowAnimation.reset();
        bossStrongAttackAnimation.reset();
        bossJumpAnimation.reset();
    }

    private void resetDeathAnimations() {
        deathDownAnimation.reset();
        deathSideAnimation.reset();
        deathUpAnimation.reset();
        bossDeathAnimation.reset();
    }

    private void resetHitAnimations() {
        hitDownAnimation.reset();
        hitSideAnimation.reset();
        hitUpAnimation.reset();
        bossHurtAnimation.reset();
    }

    private void updateBossAnimation(long now, boolean moving, boolean moveLeft, boolean moveRight) {
        facingDirection = FacingDirection.SIDE;
        if (moveLeft) {
            facingRight = false;
        } else if (moveRight) {
            facingRight = true;
        }

        if (!isAlive()) {
            boolean finished = bossDeathAnimation.updateOnce(now);
            currentFrame = bossDeathAnimation.getCurrentFrame();
            if (finished) {
                bossDeathAnimation.updateOnce(now);
            }
            return;
        }

        if (takingHit) {
            boolean finished = bossHurtAnimation.updateOnce(now);
            currentFrame = bossHurtAnimation.getCurrentFrame();
            if (finished) {
                takingHit = false;
                bossHurtAnimation.reset();
            }
            return;
        }

        if (attacking) {
            SpriteAnimation attackAnimation = currentAttackAnimation();
            boolean finished = attackAnimation.updateOnce(now);
            currentFrame = attackAnimation.getCurrentFrame();
            if (finished || now - attackStartedAtNs >= attackDurationNs) {
                attacking = false;
                resetAllAttackAnimations();
            }
            return;
        }

        SpriteAnimation activeAnimation = moving ? bossRunAnimation : bossIdleAnimation;
        activeAnimation.update(now, true);
        currentFrame = activeAnimation.getCurrentFrame();
    }

    private double[] buildBossAttackHitbox() {
        double hitboxWidth;
        double hitboxHeight;
        double range;
        switch (currentAttackType) {
            case DASH_ATTACK -> {
                hitboxWidth = width * 1.15;
                hitboxHeight = height * 0.88;
                range = 72.0;
            }
            case THROW -> {
                hitboxWidth = width * 0.95;
                hitboxHeight = height * 0.70;
                range = 140.0;
            }
            case STRONG_ATTACK -> {
                hitboxWidth = width * 1.35;
                hitboxHeight = height * 0.96;
                range = 92.0;
            }
            case JUMP -> {
                hitboxWidth = width * 0.80;
                hitboxHeight = height * 0.70;
                range = 18.0;
            }
            case DEFEND -> {
                hitboxWidth = width * 0.72;
                hitboxHeight = height * 0.76;
                range = 0.0;
            }
            default -> {
                hitboxWidth = width * 1.02;
                hitboxHeight = height * 0.86;
                range = 40.0;
            }
        }

        double attackX = x + (width - hitboxWidth) / 2.0 + (facingRight ? range : -range);
        double attackY = y + height * 0.18;
        return new double[]{attackX, attackY, hitboxWidth, hitboxHeight};
    }
}
