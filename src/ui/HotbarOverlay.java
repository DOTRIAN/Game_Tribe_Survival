package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * HotbarOverlay:
 * - JavaFX view cho quickbar item chung.
 * - Co the hien build item, bomb va item dung nhanh tu inventory.
 */
public class HotbarOverlay extends HBox {
    public static final int SLOT_COUNT = 9;

    private final List<StackPane> slotNodes;
    private final List<StackPane> iconContainers;
    private final List<Label> amountLabels;
    private final List<Label> keyLabels;
    private final List<Label> costLabels;
    private IntConsumer selectionListener;
    private int selectedIndex;

    public HotbarOverlay() {
        this.slotNodes = new ArrayList<>();
        this.iconContainers = new ArrayList<>();
        this.amountLabels = new ArrayList<>();
        this.keyLabels = new ArrayList<>();
        this.costLabels = new ArrayList<>();
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

    public void update(List<HotbarItemStack> items) {
        for (int index = 0; index < SLOT_COUNT; index++) {
            StackPane slot = slotNodes.get(index);
            StackPane iconPane = iconContainers.get(index);
            iconPane.getChildren().clear();

            Label amountLabel = amountLabels.get(index);
            amountLabel.setText("");

            Label costLabel = costLabels.get(index);
            costLabel.setText("");

            slot.setOpacity(0.35);
            slot.setVisible(true);
            slot.setManaged(true);

            HotbarItemStack itemStack = (items == null || index >= items.size()) ? null : items.get(index);
            if (itemStack == null || itemStack.getAmount() <= 0) {
                continue;
            }

            slot.setVisible(true);
            slot.setManaged(true);
            slot.setOpacity(1.0);
            amountLabel.setText(String.valueOf(itemStack.getAmount()));
            costLabel.setText("");

            ItemUiMeta meta = itemStack.getMeta();
            Image itemImg = meta == null ? null : meta.getImageIcon();
            if (itemImg != null && !itemImg.isError()) {
                ImageView view = new ImageView(itemImg);
                view.setFitWidth(28);
                view.setFitHeight(28);
                view.setPreserveRatio(true);
                view.setSmooth(false);
                view.setMouseTransparent(true);
                iconPane.getChildren().add(view);
            } else {
                Label textIcon = new Label(abbreviationOf(itemStack));
                textIcon.getStyleClass().add("resource-icon-text");
                textIcon.setStyle("-fx-font-size: 11px;");
                iconPane.getChildren().add(textIcon);
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
        slot.setPrefSize(48, 48);
        slot.setMinSize(48, 48);
        slot.setMaxSize(48, 48);

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

        Label costLabel = new Label("");
        costLabel.getStyleClass().add("hotbar-key");
        costLabel.setStyle("-fx-font-size: 8px;");
        StackPane.setAlignment(costLabel, Pos.BOTTOM_LEFT);
        StackPane.setMargin(costLabel, new Insets(0, 0, 2, 3));
        costLabels.add(costLabel);

        slot.getChildren().addAll(iconPane, amountLabel, keyLabel, costLabel);
        slot.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (selectionListener != null) {
                selectionListener.accept(index);
            }
        });
        return slot;
    }

    private String abbreviationOf(HotbarItemStack slot) {
        if (slot == null || slot.getMeta() == null || slot.getMeta().getDisplayName() == null) {
            return "?";
        }
        String[] words = slot.getMeta().getDisplayName().trim().split("\\s+");
        if (words.length == 1) {
            String word = words[0];
            return word.length() <= 2 ? word.toUpperCase() : word.substring(0, 2).toUpperCase();
        }
        return (String.valueOf(words[0].charAt(0)) + words[1].charAt(0)).toUpperCase();
    }
}
