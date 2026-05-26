package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ChestOverlay extends StackPane {
    private final Button closeButton;

    public ChestOverlay() {
        getStyleClass().add("screen-overlay");

        VBox panel = new VBox(14);
        panel.getStyleClass().add("inventory-panel");
        panel.getStyleClass().add("chest-panel");
        panel.setPadding(new Insets(22, 24, 22, 24));
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setFillWidth(false);
        panel.setMaxWidth(Region.USE_PREF_SIZE);
        panel.setMaxHeight(Region.USE_PREF_SIZE);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("RUONG DO");
        title.getStyleClass().add("menu-logo");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        this.closeButton = new Button("Close");
        closeButton.getStyleClass().add("secondary-button");
        header.getChildren().addAll(title, spacer, closeButton);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                StackPane slot = new StackPane();
                slot.getStyleClass().add("inventory-slot");
                slot.setMinSize(76, 76);
                slot.setPrefSize(76, 76);
                slot.setMaxSize(76, 76);
                grid.add(slot, column, row);
            }
        }

        panel.getChildren().addAll(header, grid);
        getChildren().add(panel);
    }

    public Button getCloseButton() {
        return closeButton;
    }
}
