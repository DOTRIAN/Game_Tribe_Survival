package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * HotbarOverlay:
 * - Redesigned sleek hotbar aligned at bottom-center.
 * - Dynamically updates item icons and counts for the 7 primary survival tools based on inventory snapshot.
 * - Supports Minecraft-style 1-9 shortcuts and selected slot gold border.
 */
public class HotbarOverlay extends HBox {
    public static final int SLOT_COUNT = 9;

    private static final List<String> HOTBAR_ITEMS = List.of(
            "stone_wall",
            "wood_wall",
            "potion",
            "torch",
            "basic_sword",
            "pickaxe",
            "carrot",
            "",
            ""
    );

    private final List<StackPane> slotNodes;
    private final List<StackPane> iconContainers;
    private final List<Label> amountLabels;
    private final List<Label> keyLabels;
    private final Image fallbackWallIcon;
    private IntConsumer selectionListener;
    private int selectedIndex;

    public HotbarOverlay(Image fallbackWallIcon) {
        this.fallbackWallIcon = fallbackWallIcon;
        this.slotNodes = new ArrayList<>();
        this.iconContainers = new ArrayList<>();
        this.amountLabels = new ArrayList<>();
        this.keyLabels = new ArrayList<>();
        this.selectedIndex = 0;

        getStyleClass().add("hotbar");
        setSpacing(6);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(8, 10, 8, 10));
        setPickOnBounds(false);

        for (int index = 0; index < SLOT_COUNT; index++) {
            StackPane slot = buildSlot(index);
            slotNodes.add(slot);
            getChildren().add(slot);
        }
        setSelectedIndex(0);
    }

    /**
     * update:
     * - Dynamically populates hotbar items using the player's active inventory.
     */
    public void update(int selectedIndex, Map<String, Integer> inventorySnapshot, Map<String, ItemUiMeta> itemMetaMap) {
        setSelectedIndex(selectedIndex);

        for (int index = 0; index < SLOT_COUNT; index++) {
            StackPane iconPane = iconContainers.get(index);
            iconPane.getChildren().clear();

            Label amountLabel = amountLabels.get(index);
            amountLabel.setText("");

            StackPane slot = slotNodes.get(index);

            String itemId = HOTBAR_ITEMS.get(index);
            if (itemId.isEmpty()) {
                slot.setOpacity(1.0);
                continue;
            }

            int count = inventorySnapshot == null ? 0 : inventorySnapshot.getOrDefault(itemId, 0);
            ItemUiMeta meta = itemMetaMap == null ? null : itemMetaMap.get(itemId);

            if (meta != null) {
                // If we own the item, show it active. Otherwise, translucent placeholder.
                if (count > 0) {
                    slot.setOpacity(1.0);
                    amountLabel.setText(String.valueOf(count));
                } else {
                    slot.setOpacity(0.35);
                }

                // Render dynamic image or abbreviation text
                Image itemImg = meta.getImageIcon();
                if (itemId.equals("stone_wall") && itemImg == null) {
                    itemImg = fallbackWallIcon;
                }

                if (itemImg != null && !itemImg.isError()) {
                    ImageView view = new ImageView(itemImg);
                    view.setFitWidth(24);
                    view.setFitHeight(24);
                    view.setPreserveRatio(true);
                    view.setMouseTransparent(true);
                    iconPane.getChildren().add(view);
                } else {
                    Label textIcon = new Label(meta.getPlaceholderIconText());
                    textIcon.getStyleClass().add("resource-icon-text");
                    textIcon.setStyle("-fx-font-size: 11px;");
                    iconPane.getChildren().add(textIcon);
                }
            } else {
                slot.setOpacity(0.35);
            }
        }
    }

    public void setSelectionListener(IntConsumer selectionListener) {
        this.selectionListener = selectionListener;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
        for (int index = 0; index < slotNodes.size(); index++) {
            StackPane slot = slotNodes.get(index);
            slot.getStyleClass().remove("hotbar-slot-selected");
            if (index == selectedIndex) {
                slot.getStyleClass().add("hotbar-slot-selected");
            }
        }
    }

    public int findSlotIndexAt(double sceneX, double sceneY) {
        for (int index = 0; index < slotNodes.size(); index++) {
            if (slotNodes.get(index).localToScene(slotNodes.get(index).getBoundsInLocal()).contains(sceneX, sceneY)) {
                return index;
            }
        }
        return -1;
    }

    public boolean containsScenePoint(double sceneX, double sceneY) {
        return localToScene(getBoundsInLocal()).contains(sceneX, sceneY);
    }

    private StackPane buildSlot(int index) {
        StackPane slot = new StackPane();
        slot.getStyleClass().add("hotbar-slot");
        slot.setPrefSize(40, 40);
        slot.setMinSize(40, 40);
        slot.setMaxSize(40, 40);

        StackPane iconPane = new StackPane();
        iconPane.setAlignment(Pos.CENTER);
        iconContainers.add(iconPane);

        Label amountLabel = new Label("");
        amountLabel.getStyleClass().add("hotbar-amount");
        amountLabel.setStyle("-fx-font-size: 10px;");
        StackPane.setAlignment(amountLabel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(amountLabel, new Insets(0, 3, 2, 0));
        amountLabels.add(amountLabel);

        Label keyLabel = new Label(String.valueOf(index + 1));
        keyLabel.getStyleClass().add("hotbar-key");
        StackPane.setAlignment(keyLabel, Pos.TOP_LEFT);
        StackPane.setMargin(keyLabel, new Insets(2, 0, 0, 3));
        keyLabels.add(keyLabel);

        slot.getChildren().addAll(iconPane, amountLabel, keyLabel);
        slot.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (selectionListener != null) {
                selectionListener.accept(index);
            }
        });
        return slot;
    }
}
