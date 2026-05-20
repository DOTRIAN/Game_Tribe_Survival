package build;

import javafx.scene.image.Image;

/**
 * WallPreview:
 * - Runtime state cua preview dat wall.
 * - Tach rieng class nay de preview co the dung CHINH sprite wall that, khong can Rectangle xanh nua.
 */
public class WallPreview {
    private boolean visible;
    private boolean valid;
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
    private Image image;
    private double opacity;

    public WallPreview() {
        this.visible = false;
        this.valid = false;
        this.tileX = -1;
        this.tileY = -1;
        this.renderX = 0;
        this.renderY = 0;
        this.width = Wall.WALL_RENDER_WIDTH;
        this.height = Wall.WALL_RENDER_HEIGHT;
        this.spriteKey = "wall_single";
        this.rotationLabel = "N";
        this.rotationDegrees = 0.0;
        this.neighborMask = 0;
        this.image = null;
        this.opacity = 0.5;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public int getTileX() {
        return tileX;
    }

    public void setTileX(int tileX) {
        this.tileX = tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public void setTileY(int tileY) {
        this.tileY = tileY;
    }

    public double getRenderX() {
        return renderX;
    }

    public void setRenderX(double renderX) {
        this.renderX = renderX;
    }

    public double getRenderY() {
        return renderY;
    }

    public void setRenderY(double renderY) {
        this.renderY = renderY;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public String getSpriteKey() {
        return spriteKey;
    }

    public void setSpriteKey(String spriteKey) {
        this.spriteKey = spriteKey;
    }

    public String getRotationLabel() {
        return rotationLabel;
    }

    public void setRotationLabel(String rotationLabel) {
        this.rotationLabel = rotationLabel;
    }

    public double getRotationDegrees() {
        return rotationDegrees;
    }

    public void setRotationDegrees(double rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
    }

    public int getNeighborMask() {
        return neighborMask;
    }

    public void setNeighborMask(int neighborMask) {
        this.neighborMask = neighborMask;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public double getOpacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        this.opacity = opacity;
    }
}
