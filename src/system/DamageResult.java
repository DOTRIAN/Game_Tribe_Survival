package system;

/**
 * DamageResult:
 * - Goi gon ket qua 1 lan gay sat thuong.
 * - Dung de render floating text, crit text, va trigger hieu ung hit.
 */
public class DamageResult {
    private final int finalDamage;
    private final boolean critical;

    public DamageResult(int finalDamage, boolean critical) {
        this.finalDamage = Math.max(0, finalDamage);
        this.critical = critical;
    }

    public int getFinalDamage() {
        return finalDamage;
    }

    public boolean isCritical() {
        return critical;
    }

    public boolean hasDamage() {
        return finalDamage > 0;
    }
}
