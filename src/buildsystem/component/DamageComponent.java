package buildsystem.component;

/**
 * DamageComponent:
 * - Dung cho trap/turret/cannon gay sat thuong.
 */
public class DamageComponent implements BuildComponent {
    private final int damage;

    public DamageComponent(int damage) {
        this.damage = Math.max(0, damage);
    }

    public int getDamage() {
        return damage;
    }
}
