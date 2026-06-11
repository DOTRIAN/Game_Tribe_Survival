package progression;

import entity.Player;

import java.util.List;

public class SurvivalProgressionService {
    public record ObjectiveStep(String title, int targetAmount, boolean foodObjective, String itemId) {}
    public record SkillUnlockInfo(int level, String name, Player.AttackAnimationType attackType) {}
    public record TutorialHintInfo(String title, String description, String keyHint, String itemId) {}
    public record FormUnlockInfo(String title,
                                 String description,
                                 String keyHint,
                                 Player.AttackAnimationType previewType,
                                 boolean epic) {}
    public record ObjectiveCompletion(String stepTitle) {}

    private static final int DAY_ONE_OBJECTIVE_WOOD = 10;
    private static final int DAY_ONE_OBJECTIVE_ROCK = 10;
    private static final int DAY_ONE_OBJECTIVE_FOOD = 5;
    private static final int DAY_ONE_OBJECTIVE_ENEMY_KILL = 1;
    private static final int DAY_ONE_OBJECTIVE_REWARD_GOLD = 10;
    private static final long SHOP_TUTORIAL_CELEBRATION_DURATION_NS = 6_500_000_000L;
    private static final long SKILL_CELEBRATION_DURATION_NS = 3_000_000_000L;
    private static final long FORM_CELEBRATION_DURATION_NS = 5_800_000_000L;
    private static final long GEM_REWARD_ANIMATION_DURATION_NS = 2_000_000_000L;
    private static final int AXE_SKILL_UNLOCK_LEVEL = 2;
    private static final int STORMBREAKER_UNLOCK_LEVEL = 3;
    private static final int DEATH_SPEAR_UNLOCK_LEVEL = 4;

    private static final List<ObjectiveStep> DAY_ONE_OBJECTIVES = List.of(
            new ObjectiveStep("Nhi\u1ec7m v\u1ee5 1", DAY_ONE_OBJECTIVE_WOOD, false, "wood"),
            new ObjectiveStep("Nhi\u1ec7m v\u1ee5 2", DAY_ONE_OBJECTIVE_ROCK, false, "rock"),
            new ObjectiveStep("Nhi\u1ec7m v\u1ee5 3", DAY_ONE_OBJECTIVE_FOOD, true, ""),
            new ObjectiveStep("Nhi\u1ec7m v\u1ee5 4", DAY_ONE_OBJECTIVE_ENEMY_KILL, false, "enemy_kill")
    );

    private static final List<SkillUnlockInfo> SKILL_UNLOCKS = List.of(
            new SkillUnlockInfo(AXE_SKILL_UNLOCK_LEVEL, "Ti\u1ec1u phu ch\u00e9m c\u1ee7i", Player.AttackAnimationType.SLICE),
            new SkillUnlockInfo(STORMBREAKER_UNLOCK_LEVEL, "StormBreaker", Player.AttackAnimationType.CRUSH),
            new SkillUnlockInfo(DEATH_SPEAR_UNLOCK_LEVEL, "Ng\u1ecdn gi\u00e1o t\u1eed th\u1ea7n", Player.AttackAnimationType.PIERCE)
    );

    private static final List<TutorialHintInfo> SHOP_TUTORIAL_HINTS = List.of(
            new TutorialHintInfo("ARCHER", "\u0110\u1ed3ng minh", "M\u1edf shop [B] v\u00e0 mua Archer.", "friendly_archer"),
            new TutorialHintInfo("FIRE BOMB", "Bomb c\u00f4ng ph\u00e1.", "Nh\u1ea5n [Q] \u0111\u1ec3 n\u00e9m.", "fire_bomb"),
            new TutorialHintInfo("ARCHER TOWER", "Th\u00e1p \u0111\u1ed3ng minh.", "\u0110\u1eb7t \u0111\u1ec3 th\u1ee7 tr\u1ea1i.", "archer_tower")
    );

    private boolean dayOneObjectiveUnlocked;
    private boolean dayOneObjectiveCompleted;
    private long dayOneObjectiveCompletedAtNs;
    private int dayOneObjectiveStep;
    private int dayOneEnemyKillCount;
    private boolean axeSkillUnlockAnnounced;
    private long axeSkillCelebrationUntilNs;
    private SkillUnlockInfo pendingSkillUnlockCelebration;
    private long formUnlockCelebrationUntilNs;
    private FormUnlockInfo pendingFormUnlockCelebration;
    private TutorialHintInfo activeShopTutorialHint;
    private int nextShopTutorialHintIndex;
    private long activeShopTutorialHintUntilNs;
    private long nextShopTutorialHintAtNs;
    private boolean gemRewardAnimationActive;
    private boolean gemRewardGranted;
    private long gemRewardAnimationStartedAtNs;
    private boolean bossNinjaAwakeningUnlocked;

    public SurvivalProgressionService() {
        reset();
    }

    public void reset() {
        dayOneObjectiveUnlocked = false;
        dayOneObjectiveCompleted = false;
        dayOneObjectiveCompletedAtNs = -1L;
        dayOneObjectiveStep = 0;
        dayOneEnemyKillCount = 0;
        axeSkillUnlockAnnounced = false;
        axeSkillCelebrationUntilNs = -1L;
        pendingSkillUnlockCelebration = null;
        formUnlockCelebrationUntilNs = -1L;
        pendingFormUnlockCelebration = null;
        activeShopTutorialHint = null;
        nextShopTutorialHintIndex = 0;
        activeShopTutorialHintUntilNs = -1L;
        nextShopTutorialHintAtNs = -1L;
        gemRewardAnimationActive = false;
        gemRewardGranted = false;
        gemRewardAnimationStartedAtNs = -1L;
        bossNinjaAwakeningUnlocked = false;
    }

    public void unlockDayOneObjectiveChain() {
        dayOneObjectiveUnlocked = true;
        dayOneObjectiveCompleted = false;
        dayOneObjectiveCompletedAtNs = -1L;
        dayOneObjectiveStep = 0;
        dayOneEnemyKillCount = 0;
        activeShopTutorialHint = null;
        nextShopTutorialHintIndex = 0;
        activeShopTutorialHintUntilNs = -1L;
        nextShopTutorialHintAtNs = -1L;
    }

    public String buildObjectiveStatus(boolean bossMode,
                                       String bossObjectiveStatus,
                                       int playerLevel,
                                       int woodAmount,
                                       int rockAmount,
                                       int foodAmount) {
        if (bossMode) {
            return bossObjectiveStatus;
        }
        if (!dayOneObjectiveUnlocked) {
            return "";
        }
        if (dayOneObjectiveCompleted) {
            return "Chu\u1ed7i nhi\u1ec7m v\u1ee5 hi\u1ec7n t\u1ea1i \u0111\u00e3 ho\u00e0n th\u00e0nh!\n"
                    + "- \u0110\u1ea1t c\u1ea5p \u0111ed\u1ed9 hi\u1ec7n t\u1ea1i: level " + playerLevel + "\n"
                    + "- S\u1eb5n s\u00e0ng s\u1ed1ng s\u00f3t qua \u0111\u00eam \u0111\u1ea7u ti\u00ean";
        }

        ObjectiveStep currentStep = getCurrentObjectiveStep();
        if (currentStep == null) {
            return "Nhi\u1ec7m v\u1ee5 5:\n- S\u1ed1ng s\u00f3t qua \u0111\u00eam \u0111\u1ea7u ti\u00ean";
        }
        return currentStep.title() + ":\n- "
                + getObjectiveProgress(currentStep, woodAmount, rockAmount, foodAmount) + "/" + currentStep.targetAmount()
                + " " + getObjectiveLabel(currentStep);
    }

    public ObjectiveCompletion advanceObjectiveIfReady(long now, int woodAmount, int rockAmount, int foodAmount) {
        if (!dayOneObjectiveUnlocked || dayOneObjectiveCompleted) {
            return null;
        }
        ObjectiveStep currentStep = getCurrentObjectiveStep();
        if (currentStep == null) {
            return null;
        }
        if (getObjectiveProgress(currentStep, woodAmount, rockAmount, foodAmount) < currentStep.targetAmount()) {
            return null;
        }
        dayOneObjectiveStep++;
        if (dayOneObjectiveStep >= DAY_ONE_OBJECTIVES.size()) {
            dayOneObjectiveCompleted = true;
            dayOneObjectiveCompletedAtNs = now;
        }
        return new ObjectiveCompletion(currentStep.title());
    }

    public String finalizeObjectiveReward(String stepTitle, long now, int leveledUpTo) {
        String rewardText = "Ho\u00e0n th\u00e0nh " + stepTitle + "! +" + DAY_ONE_OBJECTIVE_REWARD_GOLD + " v\u00e0ng, +1 level";
        SkillUnlockInfo unlockedSkill = findSkillUnlockedAtLevel(leveledUpTo);
        if (dayOneObjectiveStep >= DAY_ONE_OBJECTIVES.size()) {
            scheduleShopTutorialHints(now, unlockedSkill != null);
        }
        if (unlockedSkill == null) {
            return rewardText;
        }
        triggerSkillUnlockCelebration(unlockedSkill, now);
        return rewardText + ". M\u1edf kh\u00f3a \"" + unlockedSkill.name() + "\" [" + getSkillKeyLabel(unlockedSkill.attackType()) + "]";
    }

    public void updateShopTutorialHints(long now) {
        if (activeShopTutorialHint != null && now >= activeShopTutorialHintUntilNs) {
            activeShopTutorialHint = null;
            activeShopTutorialHintUntilNs = -1L;
            nextShopTutorialHintAtNs = now;
        }
        if (activeShopTutorialHint != null || nextShopTutorialHintAtNs < 0L) {
            return;
        }
        if (now < nextShopTutorialHintAtNs) {
            return;
        }
        if (nextShopTutorialHintIndex >= SHOP_TUTORIAL_HINTS.size()) {
            nextShopTutorialHintAtNs = -1L;
            return;
        }
        activeShopTutorialHint = SHOP_TUTORIAL_HINTS.get(nextShopTutorialHintIndex++);
        activeShopTutorialHintUntilNs = now + SHOP_TUTORIAL_CELEBRATION_DURATION_NS;
    }

    public SkillUnlockInfo getActiveSkillCelebration(long now) {
        return now < axeSkillCelebrationUntilNs ? pendingSkillUnlockCelebration : null;
    }

    public FormUnlockInfo getActiveFormCelebration(long now) {
        return now < formUnlockCelebrationUntilNs ? pendingFormUnlockCelebration : null;
    }

    public TutorialHintInfo getActiveTutorialHint(long now) {
        return activeShopTutorialHint;
    }

    public void triggerBossNinjaAwakeningCelebration(long now) {
        pendingFormUnlockCelebration = new FormUnlockInfo(
                "NINJA AWAKENING",
                "Hai vi\u00ean Ng\u1ecdc Phong \u1ea4n \u0111\u00e3 \u0111\u00e1nh th\u1ee9c chi\u1ebfn y phong \u1ea5n.",
                "B\u01b0\u1edbc qua c\u1ed5ng boss \u0111\u1ec3 h\u00f3a th\u00e2n th\u00e0nh Ninja chi\u1ebfn tr\u1eadn.",
                Player.AttackAnimationType.STRONG_ATTACK,
                true
        );
        formUnlockCelebrationUntilNs = now + FORM_CELEBRATION_DURATION_NS;
    }

    public boolean shouldStartSealGemRewardDialogue(int currentDay,
                                                    boolean isDayPhase,
                                                    int currentSealGems,
                                                    int requiredSealGems) {
        if (currentDay < 2) {
            return false;
        }
        if (dayOneObjectiveStep < DAY_ONE_OBJECTIVES.size()) {
            return false;
        }
        if (gemRewardAnimationActive) {
            return false;
        }
        if (!isDayPhase) {
            return false;
        }
        int rewardableGems = Math.min(requiredSealGems, currentDay - 1);
        if (Math.max(0, currentSealGems) >= rewardableGems) {
            return false;
        }
        dayOneObjectiveCompleted = true;
        dayOneObjectiveCompletedAtNs = System.nanoTime();
        return true;
    }

    public void activateGemRewardAnimation(long now) {
        gemRewardAnimationActive = true;
        gemRewardGranted = false;
        gemRewardAnimationStartedAtNs = now;
    }

    public boolean updateGemRewardAnimation(long now) {
        if (!gemRewardAnimationActive || gemRewardAnimationStartedAtNs < 0L) {
            return false;
        }
        if (now - gemRewardAnimationStartedAtNs < GEM_REWARD_ANIMATION_DURATION_NS) {
            return false;
        }
        gemRewardAnimationActive = false;
        gemRewardAnimationStartedAtNs = -1L;
        if (gemRewardGranted) {
            return false;
        }
        gemRewardGranted = true;
        return true;
    }

    public boolean isGemRewardAnimationActive() {
        return gemRewardAnimationActive;
    }

    public double getGemRewardAnimationProgress(long now) {
        if (!gemRewardAnimationActive || gemRewardAnimationStartedAtNs < 0L) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (double) (now - gemRewardAnimationStartedAtNs) / GEM_REWARD_ANIMATION_DURATION_NS));
    }

    public boolean shouldTriggerBossNinjaAwakening(boolean onMainMap,
                                                   int currentSealGems,
                                                   int requiredSealGems,
                                                   boolean dialogueActive) {
        if (bossNinjaAwakeningUnlocked) {
            return false;
        }
        if (!onMainMap) {
            return false;
        }
        if (currentSealGems < requiredSealGems) {
            return false;
        }
        if (gemRewardAnimationActive || dialogueActive) {
            return false;
        }
        bossNinjaAwakeningUnlocked = true;
        return true;
    }

    public void recordEnemyKill() {
        if (!dayOneObjectiveUnlocked || dayOneObjectiveCompleted) {
            return;
        }
        dayOneEnemyKillCount++;
    }

    public int getObjectiveRewardGold() {
        return DAY_ONE_OBJECTIVE_REWARD_GOLD;
    }

    public boolean isSkillUnlocked(int playerLevel, int unlockLevel) {
        return playerLevel >= unlockLevel;
    }

    public String getSkillKeyLabel(Player.AttackAnimationType attackType) {
        if (attackType == null) {
            return "F";
        }
        return switch (attackType) {
            case SLICE -> "F";
            case CRUSH -> "L";
            case PIERCE -> "K";
            case HIT -> "J";
            default -> "F";
        };
    }

    private ObjectiveStep getCurrentObjectiveStep() {
        if (dayOneObjectiveStep < 0 || dayOneObjectiveStep >= DAY_ONE_OBJECTIVES.size()) {
            return null;
        }
        return DAY_ONE_OBJECTIVES.get(dayOneObjectiveStep);
    }

    private int getObjectiveProgress(ObjectiveStep objectiveStep, int woodAmount, int rockAmount, int foodAmount) {
        if (objectiveStep == null) {
            return 0;
        }
        if (objectiveStep.foodObjective()) {
            return Math.max(0, foodAmount);
        }
        return switch (objectiveStep.itemId()) {
            case "wood" -> Math.max(0, woodAmount);
            case "rock" -> Math.max(0, rockAmount);
            case "enemy_kill" -> Math.max(0, dayOneEnemyKillCount);
            default -> 0;
        };
    }

    private String getObjectiveLabel(ObjectiveStep objectiveStep) {
        if (objectiveStep == null) {
            return "";
        }
        if (objectiveStep.foodObjective()) {
            return "th\u1ee9c \u0103n";
        }
        return switch (objectiveStep.itemId()) {
            case "wood" -> "g\u1ed7";
            case "rock" -> "\u0111\u00e1";
            case "enemy_kill" -> "qu\u00e1i";
            default -> objectiveStep.itemId();
        };
    }

    private SkillUnlockInfo findSkillUnlockedAtLevel(int level) {
        for (SkillUnlockInfo skillUnlock : SKILL_UNLOCKS) {
            if (skillUnlock.level() == level) {
                return skillUnlock;
            }
        }
        return null;
    }

    private void scheduleShopTutorialHints(long now, boolean delayedForSkillCelebration) {
        activeShopTutorialHint = null;
        nextShopTutorialHintIndex = 0;
        activeShopTutorialHintUntilNs = -1L;
        nextShopTutorialHintAtNs = now + (delayedForSkillCelebration ? SKILL_CELEBRATION_DURATION_NS : 0L);
    }

    private void triggerSkillUnlockCelebration(SkillUnlockInfo unlockedSkill, long now) {
        pendingSkillUnlockCelebration = unlockedSkill;
        axeSkillUnlockAnnounced = true;
        axeSkillCelebrationUntilNs = now + SKILL_CELEBRATION_DURATION_NS;
    }
}
