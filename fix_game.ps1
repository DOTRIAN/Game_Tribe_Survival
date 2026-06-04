$path='src/core/Game.java'
$content=Get-Content $path -Raw
$start=$content.IndexOf('    private void tryUseUnlockedSkill(long now,')
$end=$content.IndexOf('    private void updateWelcomeMenuHoverByMouse() {')
if($start -lt 0 -or $end -lt 0 -or $end -le $start){ throw 'anchors not found' }
$replacement=@"
    private void tryUseUnlockedSkill(long now,
                                     Player.AttackAnimationType attackType,
                                     String skillName,
                                     int unlockLevel,
                                     String keyLabel) {
        if (player.getLevel() < unlockLevel) {
            renderer.showToast("Skill locked until level " + unlockLevel + " [" + keyLabel + "]");
            return;
        }
        if (player.consumeEnergy(SKILL_F_ENERGY_COST)) {
            performPlayerAttack(now, attackType);
        }
    }

    private String getSkillKeyLabel(Player.AttackAnimationType attackType) {
        if (attackType == null) {
            return "F";
        }
        return switch (attackType) {
            case SLICE -> "F";
            case CRUSH -> "L";
            case PIERCE -> "K";
            case HIT -> "J";
        };
    }

    // getGameOverMessage:
    // - Output: thong diep GAME OVER theo dung nguyen nhan thua.
    public String getGameOverMessage() {
        if (gameOverReason == GameOverReason.BASE_CAMP_DESTROYED) {
            return "Base camp is destroyed.";
        }
        return "Player is dead.";
    }

    private int getCurrentSurvivalDay(long now) {
        long elapsedNs = Math.max(0L, now - worldStartedAtNs);
        long dayLengthNs = (120L + 20L + 80L + 25L) * 1_000_000_000L;
        return (int) (elapsedNs / dayLengthNs) + 1;
    }

    private int findWelcomeMenuIndexAt(double mx, double my) {
        double buttonX = 350;
        double buttonY = 246;
        double buttonW = 260;
        double buttonH = 42;
        double gap = 58;

        for (int i = 0; i < MENU_COUNT; i++) {
            double top = buttonY + i * gap;
            if (mx >= buttonX && mx <= buttonX + buttonW && my >= top && my <= top + buttonH) {
                return i;
            }
        }
        return -1;
    }

"@
$new=$content.Substring(0,$start)+$replacement+$content.Substring($end)
Set-Content -Path $path -Value $new
$verify=Get-Content $path -Raw
Write-Output ('count_try=' + ([regex]::Matches($verify,'private void tryUseUnlockedSkill\(long now,')).Count)
Write-Output ('count_find=' + ([regex]::Matches($verify,'private int findWelcomeMenuIndexAt\(double mx, double my\) \{')).Count)
