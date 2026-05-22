package buildsystem.object;

import buildsystem.component.CollisionComponent;
import buildsystem.component.HealthComponent;
import buildsystem.component.RotationComponent;
import buildsystem.core.BuildDefinition;
import buildsystem.core.BuildObjectSeed;
import core.GameBalance;

/** Archer tower that auto targets enemies and fires arrows. */
public class ArcherTower extends BuildObject {
    private final double range;
    private final int damage;
    private final long attackCooldownNs;
    private boolean attacking;
    private long lastAttackAtNs;

    public ArcherTower(BuildDefinition definition, BuildObjectSeed seed) {
        super(seed.getObjectId(),
                definition.getType(),
                seed.getTileX(),
                seed.getTileY(),
                seed.getTileWidth(),
                seed.getTileHeight(),
                definition.getAutoTileGroup(),
                GameBalance.ARCHER_TOWER_WORLD_WIDTH,
                GameBalance.ARCHER_TOWER_WORLD_HEIGHT,
                0,
                (seed.getTileHeight() - GameBalance.ARCHER_TOWER_WORLD_HEIGHT) / 2.0,
                seed.getSpriteKey(),
                seed.getRotationDegrees(),
                seed.getHealth());
        addComponent(new CollisionComponent(GameBalance.ARCHER_TOWER_WORLD_WIDTH, GameBalance.ARCHER_TOWER_WORLD_HEIGHT));
        addComponent(new HealthComponent(seed.getHealth(), definition.getHealth()));
        addComponent(new RotationComponent(seed.getRotationDegrees()));
        this.range = GameBalance.ARCHER_TOWER_RANGE;
        this.damage = GameBalance.ARCHER_TOWER_DAMAGE;
        this.attackCooldownNs = GameBalance.ARCHER_TOWER_ATTACK_COOLDOWN_NS;
        this.attacking = false;
        this.lastAttackAtNs = -this.attackCooldownNs;
    }

    public double getRange() {
        return range;
    }

    public int getDamage() {
        return damage;
    }

    public long getAttackCooldownNs() {
        return attackCooldownNs;
    }

    public void setAttacking(boolean attacking) {
        this.attacking = attacking;
    }

    public boolean isAttacking() {
        return attacking;
    }

    public boolean canAttack(long nowNs) {
        return nowNs - lastAttackAtNs >= attackCooldownNs;
    }

    public long getLastAttackAtNs() {
        return lastAttackAtNs;
    }

    public void markAttack(long lastAttackAtNs) {
        this.lastAttackAtNs = lastAttackAtNs;
    }
}
