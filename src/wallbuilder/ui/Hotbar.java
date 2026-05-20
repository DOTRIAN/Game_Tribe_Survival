package wallbuilder.ui;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import wallbuilder.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Hotbar:
 * - Thanh chon do kieu Minecraft.
 * - Dung HBox va highlight slot dang duoc chon.
 */
public class Hotbar extends HBox {
    private final List<HotbarSlot> slots;
    private int selectedIndex;

    public Hotbar(List<Item> items) {
        this.slots = new ArrayList<>();
        this.selectedIndex = 0;

        setSpacing(8);
        setAlignment(Pos.CENTER);
        setStyle(
                "-fx-padding: 12;"
                        + "-fx-background-color: rgba(50,50,50,0.75);"
                        + "-fx-background-radius: 10;"
        );

        for (int index = 0; index < items.size(); index++) {
            HotbarSlot slot = new HotbarSlot(index, items.get(index));
            slots.add(slot);
            getChildren().add(slot);
        }
        updateSelection(0);
    }

    public void updateSelection(int newIndex) {
        if (newIndex < 0 || newIndex >= slots.size()) {
            return;
        }
        selectedIndex = newIndex;
        for (int index = 0; index < slots.size(); index++) {
            slots.get(index).setSelected(index == selectedIndex);
        }
    }

    public Item getSelectedItem() {
        return slots.get(selectedIndex).getItem();
    }

    public List<HotbarSlot> getSlots() {
        return slots;
    }
}
