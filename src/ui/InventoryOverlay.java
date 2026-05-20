package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * InventoryOverlay:
 * - Grid tui do co tooltip item.
 * - Input: inventory snapshot va metadata item.
 * - Output: grid slot hien item + so luong.
 */
public class InventoryOverlay extends StackPane {
    private final GridPane gridPane;
    private final Button closeButton;

    public InventoryOverlay() {
        getStyleClass().add("screen-overlay");

        VBox panel = new VBox(14);
        panel.getStyleClass().add("inventory-panel");
        panel.setPadding(new Insets(22, 24, 22, 24));
        panel.setMaxWidth(540);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Inventory");
        title.getStyleClass().add("menu-logo");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        this.closeButton = new Button("Close");
        closeButton.getStyleClass().add("secondary-button");
        header.getChildren().addAll(title, spacer, closeButton);

        this.gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        panel.getChildren().addAll(header, gridPane);
        getChildren().add(panel);
    }

    public void updateInventory(Map<String, Integer> inventorySnapshot, Map<String, ItemUiMeta> itemMetaMap) {
        gridPane.getChildren().clear();
        int column = 0;
        int row = 0;

        if (inventorySnapshot == null || inventorySnapshot.isEmpty()) {
            gridPane.add(new Label("Bag is empty"), 0, 0);
            return;
        }

        for (Map.Entry<String, Integer> entry : inventorySnapshot.entrySet()) {
            StackPane slot = buildSlot(entry.getKey(), entry.getValue(), itemMetaMap == null ? null : itemMetaMap.get(entry.getKey()));
            gridPane.add(slot, column, row);
            column++;
            if (column >= 5) {
                column = 0;
                row++;
            }
        }
    }

    public Button getCloseButton() {
        return closeButton;
    }

    private StackPane buildSlot(String itemId, int amount, ItemUiMeta meta) {
        StackPane slot = new StackPane();
        slot.getStyleClass().add("inventory-slot");
        slot.setPrefSize(84, 84);

        if (meta != null && meta.getImageIcon() != null) {
            ImageView imageView = new ImageView(meta.getImageIcon());
            imageView.setFitWidth(40);
            imageView.setFitHeight(40);
            imageView.setPreserveRatio(true);
            slot.getChildren().add(imageView);
        } else {
            Label iconLabel = new Label(meta == null ? "•" : meta.getPlaceholderIconText());
            iconLabel.getStyleClass().add("inventory-icon");
            slot.getChildren().add(iconLabel);
        }

        Label amountLabel = new Label("x" + amount);
        amountLabel.getStyleClass().add("hotbar-amount");
        StackPane.setAlignment(amountLabel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(amountLabel, new Insets(0, 4, 4, 0));
        slot.getChildren().add(amountLabel);

        String tooltipText = (meta == null ? itemId : meta.getDisplayName())
                + "\n"
                + (meta == null ? "No description" : meta.getDescription());
        Tooltip.install(slot, new Tooltip(tooltipText));
        return slot;
    }
}
