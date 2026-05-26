package system;

import entity.Entity;
import entity.Player;
import entity.WolfEnemy;
import javafx.scene.paint.Color;

import java.util.concurrent.ThreadLocalRandom;

/**
 * DamageSystem:
 * - Diem duy nhat xu ly viec 1 entity gay sat thuong len entity khac.
 * - Hien tai calculateDamage dang don gian, nhung da co hook de sau nay them:
 *   + giap
 *   + crit
 *   + ne don
 *   + buff/debuff
 */
public final class DamageSystem {
    private DamageSystem() {
    }

    public static void applyDamage(Entity attacker, Entity target, int damage) {
        applyDamage(attacker, target, damage, System.nanoTime());
    }

    public static DamageResult applyDamage(Entity attacker, Entity target, int damage, long nowNs) {
        if (target == null || target.isDead()) {
            return new DamageResult(0, false);
        }

        boolean critical = shouldCritical(attacker);
        int finalDamage = calculateDamage(attacker, target, damage, critical);
        target.takeDamage(finalDamage);
        if (finalDamage > 0) {
            // Quái/nhân vật bị đánh sẽ nháy đỏ ~120ms.
            target.triggerHitFlash(nowNs, 130_000_000L, Color.rgb(255, 24, 24));
            if (attacker instanceof Player player && target instanceof WolfEnemy wolfEnemy) {
                wolfEnemy.aggroOn(player, nowNs);
            }
        }
        return new DamageResult(finalDamage, critical);
    }

    private static int calculateDamage(Entity attacker, Entity target, int damage, boolean critical) {
        // attacker/target duoc truyen vao de sau nay co the doc stat tu chinh entity.
        int base = Math.max(0, damage);
        if (!critical) {
            return base;
        }
        return Math.max(1, (int) Math.round(base * 1.8));
    }

    private static boolean shouldCritical(Entity attacker) {
        // Tạm thời cho player có tỉ lệ crit cơ bản 20%.
        if (!(attacker instanceof Player)) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() < 0.20;
    }
}
