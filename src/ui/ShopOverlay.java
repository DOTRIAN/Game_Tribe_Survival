package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * ShopOverlay:
 * - Panel shop giua man hinh.
 * - Mua item bang coin va phat callback de Game xu ly inventory that.
 */
public class ShopOverlay extends StackPane {
    private final Label coinLabel;
    private final Button closeButton;
    private final VBox listBox;
    private Consumer<String> buyListener;
    private String lastShopFingerprint;

    public ShopOverlay() {
        getStyleClass().add("screen-overlay");

        VBox panel = new VBox(14);
        panel.getStyleClass().add("shop-panel");
        panel.setPadding(new Insets(22, 24, 22, 24));
        panel.setMaxWidth(560);
        panel.setMaxHeight(480);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("SHOP");
        title.getStyleClass().add("menu-logo");
        this.coinLabel = new Label("Coins: 0");
        coinLabel.getStyleClass().add("hud-value");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        this.closeButton = new Button("Close");
        closeButton.getStyleClass().add("secondary-button");
        header.getChildren().addAll(title, spacer, coinLabel, closeButton);

        this.listBox = new VBox(10);
        this.lastShopFingerprint = "";
        ScrollPane scrollPane = new ScrollPane(listBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(370);
        scrollPane.getStyleClass().add("overlay-scroll");

        panel.getChildren().addAll(header, scrollPane);
        getChildren().add(panel);
    }

    public void setBuyListener(Consumer<String> buyListener) {
        this.buyListener = buyListener;
    }

    public Button getCloseButton() {
        return closeButton;
    }

    public void updateShop(Map<String, Integer> inventorySnapshot, List<ItemUiMeta> shopItems) {
        int coins = inventorySnapshot == null ? 0 : inventorySnapshot.getOrDefault("coin", 0);
        coinLabel.setText("Coins: " + coins);

        if (shopItems == null) {
            listBox.getChildren().clear();
            lastShopFingerprint = "";
            return;
        }

        StringBuilder fingerprintBuilder = new StringBuilder();
        fingerprintBuilder.append(coins).append('|');
        for (ItemUiMeta item : shopItems) {
            if (item == null) {
                continue;
            }
            fingerprintBuilder.append(item.getItemId()).append(':').append(item.getPrice()).append(';');
        }
        String nextFingerprint = fingerprintBuilder.toString();
        if (nextFingerprint.equals(lastShopFingerprint) && !listBox.getChildren().isEmpty()) {
            return;
        }

        listBox.getChildren().clear();
        lastShopFingerprint = nextFingerprint;
        for (ItemUiMeta item : new ArrayList<>(shopItems)) {
            if (item == null) {
                continue;
            }
            listBox.getChildren().add(buildCard(item));
        }
    }

    private HBox buildCard(ItemUiMeta item) {
        HBox card = new HBox(14);
        card.getStyleClass().add("shop-item-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 12, 12, 12));

        StackPane iconPane = new StackPane();
        iconPane.getStyleClass().add("shop-item-icon");
        iconPane.setPrefSize(52, 52);
        if (item.getImageIcon() != null) {
            ImageView imageView = new ImageView(item.getImageIcon());
            imageView.setFitWidth(44);
            imageView.setFitHeight(44);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(false);
            iconPane.getChildren().add(imageView);
        } else {
            Label iconLabel = new Label(item.getPlaceholderIconText());
            iconLabel.getStyleClass().add("resource-icon-text");
            iconPane.getChildren().add(iconLabel);
        }

        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        Label nameLabel = new Label(item.getDisplayName());
        nameLabel.getStyleClass().add("hud-title");
        Label descriptionLabel = new Label(item.getDescription());
        descriptionLabel.getStyleClass().add("shop-description");
        Label priceLabel = new Label(item.getPrice() + " coin");
        priceLabel.getStyleClass().add("resource-amount");
        infoBox.getChildren().addAll(nameLabel, descriptionLabel, priceLabel);

        Button buyButton = new Button("Buy");
        buyButton.getStyleClass().add("shop-buy-button");
        buyButton.setWrapText(false);
        buyButton.setMinWidth(76);
        buyButton.setPrefWidth(76);
        buyButton.setMaxWidth(Region.USE_PREF_SIZE);
        buyButton.setOnAction(event -> {
            if (buyListener != null) {
                buyListener.accept(item.getItemId());
            }
        });

        card.getChildren().addAll(iconPane, infoBox, buyButton);
        return card;
    }
}
