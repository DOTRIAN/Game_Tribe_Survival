package buildsystem.core;

import javafx.scene.image.Image;

/**
 * BuildPreview:
 * - Runtime du lieu ghost preview dung chung cho moi BuildObject.
 * - Preview flow:
 *   1) BuildManager snap world cursor -> tile/grid.
 *   2) PlacementValidator tra ket qua valid/invalid.
 *   3) Sprite resolver chon sprite/rotation/auto-tile.
 *   4) Renderer doc BuildPreview de ve ghost co opacity + invalid tint.
 */
public class BuildPreview {
    private boolean visible;
    private boolean valid;
    private BuildType type;
    private int tileX;
    private int tileY;
    private double renderX;
    private double renderY;
    private double width;
    private double height;
    private String spriteKey;
    private String rotationLabel;
    private double rotationDegrees;
    private int neighborMask;
    private String validationMessage;
    private Image image;
    private double opacity;

    public BuildPreview() {
        this.visible = false;
        this.valid = false;
        this.type = null;
        this.tileX = -1;
        this.tileY = -1;
        this.opacity = 0.5;
        this.rotationLabel = "N";
        this.validationMessage = "";
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public BuildType getType() { return type; }
    public void setType(BuildType type) { this.type = type; }
    public int getTileX() { return tileX; }
    public void setTileX(int tileX) { this.tileX = tileX; }
    public int getTileY() { return tileY; }
    public void setTileY(int tileY) { this.tileY = tileY; }
    public double getRenderX() { return renderX; }
    public void setRenderX(double renderX) { this.renderX = renderX; }
    public double getRenderY() { return renderY; }
    public void setRenderY(double renderY) { this.renderY = renderY; }
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    public String getSpriteKey() { return spriteKey; }
    public void setSpriteKey(String spriteKey) { this.spriteKey = spriteKey; }
    public String getRotationLabel() { return rotationLabel; }
    public void setRotationLabel(String rotationLabel) { this.rotationLabel = rotationLabel; }
    public double getRotationDegrees() { return rotationDegrees; }
    public void setRotationDegrees(double rotationDegrees) { this.rotationDegrees = rotationDegrees; }
    public int getNeighborMask() { return neighborMask; }
    public void setNeighborMask(int neighborMask) { this.neighborMask = neighborMask; }
    public String getValidationMessage() { return validationMessage; }
    public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage == null ? "" : validationMessage; }
    public Image getImage() { return image; }
    public void setImage(Image image) { this.image = image; }
    public double getOpacity() { return opacity; }
    public void setOpacity(double opacity) { this.opacity = opacity; }
}
