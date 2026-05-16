package ui;

/**
 * FloatingDamageText:
 * - Chu so sat thuong bay len + mo dan.
 * - Dung cho enemy va resource khi nhan damage.
 */
public class FloatingDamageText {
    private final String text;
    private final double worldX;
    private final double worldY;
    private final long createdAtNs;
    private final long lifetimeNs;
    private final boolean critical;

    public FloatingDamageText(String text, double worldX, double worldY, long createdAtNs, long lifetimeNs, boolean critical) {
        this.text = text == null ? "" : text;
        this.worldX = worldX;
        this.worldY = worldY;
        this.createdAtNs = createdAtNs;
        this.lifetimeNs = Math.max(1L, lifetimeNs);
        this.critical = critical;
    }

    public String getText() {
        return text;
    }

    public double getWorldX() {
        return worldX;
    }

    public double getWorldY() {
        return worldY;
    }

    public boolean isCritical() {
        return critical;
    }

    public double getProgress(long nowNs) {
        double elapsed = (double) (nowNs - createdAtNs);
        return Math.max(0.0, Math.min(1.0, elapsed / lifetimeNs));
    }

    public boolean isExpired(long nowNs) {
        return nowNs - createdAtNs >= lifetimeNs;
    }
}
