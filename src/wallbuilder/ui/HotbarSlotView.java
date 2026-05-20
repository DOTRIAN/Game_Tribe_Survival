package wallbuilder.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import wallbuilder.model.HotbarItem;

/**
 * HotbarSlotView:
 * - 1 o vuong trong hotbar.
 * - Chiu trach nhiem hien icon item, so thu tu phim va trang thai dang chon.
 */
public class HotbarSlotView extends StackPane {
    private final Label iconLabel;
    private final Label indexLabel;
    private HotbarItem item;

    public HotbarSlotView(int slotIndex, HotbarItem item) {
        this.item = item;
        this.iconLabel = new Label(item.getIconText());
        this.indexLabel = new Label(String.valueOf(slotIndex + 1));

        setPrefSize(64, 64);
        setMinSize(64, 64);
        setMaxSize(64, 64);
        setAlignment(Pos.CENTER);

        iconLabel.setStyle("-fx-font-size: 22px;");
        indexLabel.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #d9d9d9;"
                        + "-fx-padding: 0 0 4 0;"
        );
        StackPane.setAlignment(indexLabel, Pos.BOTTOM_CENTER);

        getChildren().addAll(iconLabel, indexLabel);
        refresh();
        setSelected(false);
    }

    public void setItem(HotbarItem item) {
        this.item = item == null ? HotbarItem.EMPTY : item;
        refresh();
    }

    public HotbarItem getItem() {
        return item;
    }

    public void setSelected(boolean selected) {
        String borderColor = selected ? "#ffffff" : "#3b3b3b";
        String borderWidth = selected ? "4" : "2";
        setStyle(
                "-fx-background-color: linear-gradient(to bottom, #8f8f8f, #6d6d6d);"
                        + "-fx-border-color: " + borderColor + ";"
                        + "-fx-border-width: " + borderWidth + ";"
                        + "-fx-background-insets: 0;"
                        + "-fx-border-insets: 0;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 8, 0.25, 0, 2);"
        );
    }

    private void refresh() {
        iconLabel.setText(item.getIconText());
    }
}
