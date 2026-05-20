package ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * NameInputScreen:
 * - Centered character naming screen with live validation and gold accent field.
 * - Hitting ENTER in text box automatically fires confirmation.
 */
public class NameInputScreen extends StackPane {
    private final TextField nameField;
    private final Label lengthLabel;
    private final Label errorLabel;
    private final Button confirmButton;
    private final Button backButton;

    public NameInputScreen() {
        getStyleClass().add("screen-overlay");

        VBox panel = new VBox(16);
        panel.getStyleClass().add("input-panel");
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(30, 36, 30, 36));
        panel.setMaxWidth(420);

        Label title = new Label("SURVIVOR NAME");
        title.getStyleClass().add("menu-logo");
        title.setStyle("-fx-font-size: 24px; -fx-padding: 0 0 4 0;");

        this.nameField = new TextField();
        nameField.getStyleClass().add("text-input-game");
        nameField.setPromptText("Enter survivor name...");
        nameField.setMaxWidth(Double.MAX_VALUE);

        this.lengthLabel = new Label("Length: 0/14");
        lengthLabel.getStyleClass().add("resource-amount");
        lengthLabel.setStyle("-fx-text-fill: #cca35a; -fx-font-weight: bold;");

        this.errorLabel = new Label("");
        errorLabel.getStyleClass().add("error-label");

        this.confirmButton = new Button("Confirm");
        confirmButton.getStyleClass().add("menu-button");
        confirmButton.setStyle("-fx-font-size: 13px; -fx-padding: 8 16 8 16;");

        this.backButton = new Button("Back");
        backButton.getStyleClass().add("secondary-button");

        HBox actions = new HBox(12, confirmButton, backButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(8, 0, 0, 0));

        panel.getChildren().addAll(title, nameField, lengthLabel, errorLabel, actions);
        getChildren().add(panel);

        // UX Polish: Trigger confirm when pressing Enter inside the text field
        nameField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                confirmButton.fire();
            }
        });
    }

    public void setDraftName(String draftName, int maxLength) {
        String nextValue = draftName == null ? "" : draftName;
        if (!nextValue.equals(nameField.getText())) {
            nameField.setText(nextValue);
            Platform.runLater(nameField::requestFocus);
        }
        updateLength(nextValue.length(), maxLength);
        errorLabel.setText("");
    }

    public String getEnteredName() {
        return nameField.getText();
    }

    public void updateLength(int currentLength, int maxLength) {
        lengthLabel.setText("Length: " + currentLength + "/" + maxLength);
    }

    public void showError(String message) {
        errorLabel.setText(message == null ? "" : message);
    }

    public TextField getNameField() {
        return nameField;
    }

    public Button getConfirmButton() {
        return confirmButton;
    }

    public Button getBackButton() {
        return backButton;
    }
}
