package buildsystem.component;

/**
 * HealthComponent:
 * - Chuan bi san cho object co the bi pha huy (wall, turret, door...).
 */
public class HealthComponent implements BuildComponent {
    private int hp;
    private final int maxHp;

    public HealthComponent(int hp, int maxHp) {
        this.hp = hp;
        this.maxHp = Math.max(1, maxHp);
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setHp(int hp) {
        this.hp = Math.max(0, Math.min(maxHp, hp));
    }
}
