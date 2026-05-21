package buildsystem.placement;

import entity.Player;

/**
 * PlacementContext:
 * - Dong goi du lieu runtime can cho validator va preview.
 * - Bao gom ca world-space goc chuot va tile-space sau khi snap de strategy co the mo rong tu grid sang free placement.
 */
public class PlacementContext {
    private final double worldX;
    private final double worldY;
    private final int tileX;
    private final int tileY;
    private final double rotationDegrees;
    private final Player player;
    private final boolean mouseOverUi;

    public PlacementContext(double worldX,
                            double worldY,
                            int tileX,
                            int tileY,
                            double rotationDegrees,
                            Player player,
                            boolean mouseOverUi) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.tileX = tileX;
        this.tileY = tileY;
        this.rotationDegrees = rotationDegrees;
        this.player = player;
        this.mouseOverUi = mouseOverUi;
    }

    public double getWorldX() { return worldX; }
    public double getWorldY() { return worldY; }
    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public double getRotationDegrees() { return rotationDegrees; }
    public Player getPlayer() { return player; }
    public boolean isMouseOverUi() { return mouseOverUi; }
}
