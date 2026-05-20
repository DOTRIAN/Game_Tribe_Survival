package wallbuilder.item;

import javafx.scene.image.Image;

/**
 * Item:
 * - Contract chung cho item trong hotbar.
 * - Muc tieu la giu API on dinh de hotbar co the hien thi bat ky item nao.
 */
public interface Item {
    // Output: ma item duy nhat cho save/load va so sanh logic.
    String getId();

    // Output: ten hien thi cua item tren UI.
    String getDisplayName();

    // Output: icon anh hien thi trong hotbar.
    Image getIcon();
}
