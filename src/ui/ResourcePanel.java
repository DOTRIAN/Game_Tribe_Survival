package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ResourcePanel:
 * - Redesigned collected resource panel on the top-right.
 * - Displays currency, building blocks, raw resources, and food elegantly with glassmorphism styling.
 */
public class ResourcePanel extends VBox {
    private static final List<String> PINNED_ORDER = List.of("coin", "stone_wall", "wood", "stone", "carrot", "potion", "torch");

    private final Label titleLabel;
    private final VBox rowsBox;
    private final Map<String, HBox> rowCache;

    public ResourcePanel() {
        getStyleClass().add("hud-panel");
        getStyleClass().add("resource-panel");
        setSpacing(8);
        setPadding(new Insets(12, 14, 12, 14));
        setPrefWidth(220);

        this.titleLabel = new Label("COLLECTED");
        titleLabel.getStyleClass().add("hud-title");
        titleLabel.setStyle("-fx-font-size: 13px;");

        this.rowsBox = new VBox(6);
        this.rowCache = new LinkedHashMap<>();

        getChildren().addAll(titleLabel, rowsBox);
    }

    /**
     * updateResources:
     * - Refresh panel rows dynamically matching inventory snapshots.
     */
    public void updateResources(Map<String, Integer> inventorySnapshot, Map<String, ItemUiMeta> itemMetaMap) {
        rowsBox.getChildren().clear();
        rowCache.clear();

        if (inventorySnapshot == null || inventorySnapshot.isEmpty()) {
            rowsBox.getChildren().add(buildRow("coin", 0, itemMetaMap));
            rowsBox.getChildren().add(buildRow("stone_wall", 0, itemMetaMap));
            rowsBox.getChildren().add(buildRow("wood", 0, itemMetaMap));
            rowsBox.getChildren().add(buildRow("stone", 0, itemMetaMap));
            rowsBox.getChildren().add(buildRow("carrot", 0, itemMetaMap));
            return;
        }

        // Pinned order displays first
        for (String itemId : PINNED_ORDER) {
            rowsBox.getChildren().add(buildRow(itemId, inventorySnapshot.getOrDefault(itemId, 0), itemMetaMap));
        }

        // Add other collected items dynamically
        for (Map.Entry<String, Integer> entry : inventorySnapshot.entrySet()) {
            if (PINNED_ORDER.contains(entry.getKey())) {
                continue;
            }
            rowsBox.getChildren().add(buildRow(entry.getKey(), entry.getValue(), itemMetaMap));
        }
    }

    private HBox buildRow(String itemId, int amount, Map<String, ItemUiMeta> itemMetaMap) {
        ItemUiMeta meta = itemMetaMap == null ? null : itemMetaMap.get(itemId);
        HBox row = new HBox(8);
        row.getStyleClass().add("resource-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Label iconFallback = new Label(meta == null ? "•" : meta.getPlaceholderIconText());
        iconFallback.getStyleClass().add("resource-icon-text");

        if (meta != null && meta.getImageIcon() != null) {
            ImageView imageView = new ImageView(meta.getImageIcon());
            imageView.setFitWidth(16);
            imageView.setFitHeight(16);
            imageView.setPreserveRatio(true);
            row.getChildren().add(imageView);
        } else {
            row.getChildren().add(iconFallback);
        }

        Label nameLabel = new Label(meta == null ? itemId : meta.getDisplayName());
        nameLabel.getStyleClass().add("resource-name");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Label amountLabel = new Label(String.valueOf(amount));
        amountLabel.getStyleClass().add("resource-amount");

        row.getChildren().addAll(nameLabel, amountLabel);
        rowCache.put(itemId, row);
        return row;
    }
}
