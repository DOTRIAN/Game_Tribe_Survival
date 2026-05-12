package system.resource;

/**
 * ResourceNode:
 * - Runtime instance cua 1 tai nguyen dat tren map.
 * - Moi object trong layer "Resources" (hoac object co schema resource) se map thanh 1 node.
 * - Noi day quan ly trang thai thay doi theo gameplay:
 *   + currentHp
 *   + alive/depleted
 *   + thoi diem bi pha (de tinh respawn neu can)
 */
public class ResourceNode {
    private final int objectId;
    private final String kind;
    private final ResourceType resourceType;

    // Hitbox gameplay trong world space (dung cho collision + hit detection)
    private final double x;
    private final double y;
    private final double width;
    private final double height;

    // Cac gia tri gameplay da "resolve" xong tu map property + default config.
    private final String dropItem;
    private final int dropMin;
    private final int dropMax;
    private final int respawnSeconds;
    private final int maxHp;

    // Runtime mutable state.
    private int currentHp;
    private boolean alive;
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
                        int maxHp) {
        this.objectId = objectId;
        this.kind = kind == null ? "unknown" : kind;
        this.resourceType = resourceType == null ? ResourceType.UNKNOWN : resourceType;
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.dropItem = dropItem == null ? "" : dropItem;
        this.dropMin = Math.max(0, dropMin);
        this.dropMax = Math.max(this.dropMin, dropMax);
        this.respawnSeconds = respawnSeconds;
        this.maxHp = Math.max(1, maxHp);
        this.currentHp = this.maxHp;
        this.alive = true;
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

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
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

    public int getMaxHp() {
        return maxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public boolean isAlive() {
        return alive;
    }

    public long getDestroyedAtNs() {
        return destroyedAtNs;
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
        if (!alive) {
            return false;
        }

        int realDamage = Math.max(0, damage);
        currentHp -= realDamage;
        if (currentHp <= 0) {
            currentHp = 0;
            alive = false;
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
        if (alive) {
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
        alive = true;
        currentHp = maxHp;
        destroyedAtNs = -1L;
    }
}

