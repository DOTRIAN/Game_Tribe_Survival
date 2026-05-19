package system.resource;

import entity.Entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * ResourceNode:
 * - Runtime instance cua 1 tai nguyen dat tren map.
 * - Moi object trong layer "Resources" (hoac object co schema resource) se map thanh 1 node.
 * - Node nay da ke thua Entity de:
 *   1) dung chung contract HP / damage / hitbox
 *   2) co the noi vao DamageSystem / CollisionSystem khi can
 * - Noi day quan ly trang thai thay doi theo gameplay:
 *   + hp hien tai (thong qua Entity.hp)
 *   + alive/depleted (thong qua Entity.isAlive())
 *   + thoi diem bi pha (de tinh respawn neu can)
 */
public class ResourceNode extends Entity {
    private final int objectId;
    private final String kind;
    private final ResourceType resourceType;

    // Cac gia tri gameplay da "resolve" xong tu map property + default config.
    private final String dropItem;
    private final int dropMin;
    private final int dropMax;
    private final int respawnSeconds;
    // Danh sach tile visual dung de hide/flash chinh xac (khong quet theo bbox rong).
    private final Set<Long> visualTileKeys;
    private long destroyedAtNs;

    public ResourceNode(int objectId,
                        String kind,
                        ResourceType resourceType,
                        double x,
                        double y,
                        double width,
                        double height,
                        String dropItem,
                        int dropMin,
                        int dropMax,
                        int respawnSeconds,
                        int maxHp,
                        Set<Long> visualTileKeys) {
        super(x, y, width, height, 0, maxHp);
        this.objectId = objectId;
        this.kind = kind == null ? "unknown" : kind;
        this.resourceType = resourceType == null ? ResourceType.UNKNOWN : resourceType;
        this.dropItem = dropItem == null ? "" : dropItem;
        this.dropMin = Math.max(0, dropMin);
        this.dropMax = Math.max(this.dropMin, dropMax);
        this.respawnSeconds = respawnSeconds;
        this.visualTileKeys = visualTileKeys == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(visualTileKeys));
        this.destroyedAtNs = -1L;
    }

    public int getObjectId() {
        return objectId;
    }

    public String getKind() {
        return kind;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getDropItem() {
        return dropItem;
    }

    public int getDropMin() {
        return dropMin;
    }

    public int getDropMax() {
        return dropMax;
    }

    public int getRespawnSeconds() {
        return respawnSeconds;
    }

    public int getCurrentHp() {
        return hp;
    }

    public long getDestroyedAtNs() {
        return destroyedAtNs;
    }

    public boolean hasVisualTiles() {
        return !visualTileKeys.isEmpty();
    }

    public boolean containsVisualTile(int tileX, int tileY) {
        if (visualTileKeys.isEmpty()) {
            return false;
        }
        return visualTileKeys.contains(packTileKey(tileX, tileY));
    }

    /**
     * applyDamage:
     * - Tru hp cho resource node.
     * - Neu hp ve 0 thi danh dau da bi pha.
     *
     * @param damage sat thuong dau vao
     * @param nowNs  timestamp hien tai (nano) de luu moc pha huy
     * @return true neu node vua chuyen tu song -> bi pha
     */
    public boolean applyDamage(int damage, long nowNs) {
        if (isDead()) {
            return false;
        }

        int realDamage = Math.max(0, damage);
        takeDamage(realDamage);
        if (isDead()) {
            destroyedAtNs = nowNs;
            return true;
        }
        return false;
    }

    /**
     * intersects:
     * - Utility check AABB collision de dung cho movement/collision system.
     */
    public boolean intersects(double otherX, double otherY, double otherW, double otherH) {
        return x < otherX + otherW
                && x + width > otherX
                && y < otherY + otherH
                && y + height > otherY;
    }

    /**
     * shouldRespawn:
     * - Node chi hoi lai neu respawnSeconds > 0.
     * - Dung moc nowNs - destroyedAtNs de tinh.
     */
    public boolean shouldRespawn(long nowNs) {
        if (isAlive()) {
            return false;
        }
        if (respawnSeconds <= 0 || destroyedAtNs < 0) {
            return false;
        }
        long needNs = respawnSeconds * 1_000_000_000L;
        return nowNs - destroyedAtNs >= needNs;
    }

    /**
     * respawn:
     * - Dua node ve trang thai moi.
     */
    public void respawn() {
        hp = maxHp;
        destroyedAtNs = -1L;
    }

    public static long packTileKey(int tileX, int tileY) {
        return (((long) tileX) << 32) ^ (tileY & 0xffffffffL);
    }
}

