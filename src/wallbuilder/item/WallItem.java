package wallbuilder.item;

import javafx.scene.image.Image;

/**
 * WallItem:
 * - Item xay tuong da.
 * - Hien tai la item build duy nhat, nhung duoc tach rieng de sau nay them trap/tower.
 */
public class WallItem extends BuildItem {
    public WallItem(String id, String displayName, Image icon, int tileWidth, int tileHeight) {
        super(id, displayName, icon, tileWidth, tileHeight);
    }
}
