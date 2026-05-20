package wallbuilder.ui;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import wallbuilder.model.HotbarItem;

/**
 * HotbarView:
 * - Thanh chon do gom 9 o, can giua sat canh duoi.
 * - Dung HBox de dung theo yeu cau de bai.
 */
public class HotbarView extends HBox {
    private final HotbarSlotView[] slots;
    private int selectedIndex;

    public HotbarView() {
        this.slots = new HotbarSlotView[9];
        this.selectedIndex = 0;

        setSpacing(8);
        setAlignment(Pos.CENTER);
        setStyle(
                "-fx-padding: 12 14 12 14;"
                        + "-fx-background-color: rgba(50, 50, 50, 0.68);"
                        + "-fx-background-radius: 10;"
        );

        for (int index = 0; index < slots.length; index++) {
            HotbarItem defaultItem = HotbarItem.EMPTY;
            if (index == 0) {
                defaultItem = HotbarItem.SMALL_STONE;
            } else if (index == 1) {
                defaultItem = HotbarItem.STONE_WALL;
            }

            slots[index] = new HotbarSlotView(index, defaultItem);
            getChildren().add(slots[index]);
        }

        updateSelection(0);
    }

    public void updateSelection(int selectedIndex) {
        if (selectedIndex < 0 || selectedIndex >= slots.length) {
            return;
        }
        this.selectedIndex = selectedIndex;
        for (int index = 0; index < slots.length; index++) {
            slots[index].setSelected(index == selectedIndex);
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public HotbarItem getSelectedItem() {
        return slots[selectedIndex].getItem();
    }
}
