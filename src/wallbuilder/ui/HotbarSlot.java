package wallbuilder.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import wallbuilder.item.Item;

/**
 * HotbarSlot:
 * - 1 o trong hotbar.
 * - Slot hien icon item va highlight khi duoc chon.
 */
public class HotbarSlot extends StackPane {
    private final int slotIndex;
    private final ImageView iconView;
    private final Label keyLabel;
    private Item item;

    public HotbarSlot(int slotIndex, Item item) {
        this.slotIndex = slotIndex;
        this.item = item;
        this.iconView = new ImageView();
        this.keyLabel = new Label(String.valueOf(slotIndex + 1));

        setPrefSize(68, 68);
        setMinSize(68, 68);
        setMaxSize(68, 68);
        setAlignment(Pos.CENTER);

        iconView.setFitWidth(36);
        iconView.setFitHeight(36);
        iconView.setPreserveRatio(true);
        keyLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
        StackPane.setAlignment(keyLabel, Pos.BOTTOM_CENTER);

        getChildren().addAll(iconView, keyLabel);
        refreshItem();
        setSelected(false);
    }

    public void setSelected(boolean selected) {
        String borderColor = selected ? "#ffffff" : "#404040";
        String borderWidth = selected ? "4" : "2";
        setStyle(
                "-fx-background-color: linear-gradient(to bottom, #8d8d8d, #6b6b6b);"
                        + "-fx-border-color: " + borderColor + ";"
                        + "-fx-border-width: " + borderWidth + ";"
                        + "-fx-background-radius: 6;"
                        + "-fx-border-radius: 6;"
        );
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
        refreshItem();
    }

    private void refreshItem() {
        iconView.setImage(item == null ? null : item.getIcon());
    }
}
