package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * MainMenuScreen:
 * - Redesigned survival menu screen with modern layout and glassmorphism panel.
 * - CSS manages active glow-scale hover animations on survival buttons.
 */
public class MainMenuScreen extends StackPane {
    private final Button playButton;
    private final Button continueButton;
    private final Button guideButton;
    private final Button settingsButton;
    private final Button exitButton;

    public MainMenuScreen() {
        getStyleClass().add("screen-overlay");

        VBox panel = new VBox(16);
        panel.getStyleClass().add("menu-panel");
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(32, 44, 32, 44));
        panel.setPrefWidth(460);
        panel.setMaxWidth(460);

        Label logo = new Label("TRIBE SURVIVAL");
        logo.getStyleClass().add("menu-logo");
        logo.setWrapText(true);
        logo.setMaxWidth(Double.MAX_VALUE);
        logo.setTextAlignment(TextAlignment.CENTER);
        logo.setAlignment(Pos.CENTER);
        logo.setStyle("-fx-font-size: 30px;");

        Label subtitle = new Label("2D Top-Down Survival Game");
        subtitle.getStyleClass().add("resource-name");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(Double.MAX_VALUE);
        subtitle.setTextAlignment(TextAlignment.CENTER);
        subtitle.setAlignment(Pos.CENTER);
        subtitle.setStyle("-fx-font-family: 'Georgia'; -fx-font-style: italic; -fx-text-fill: #cca35a; -fx-padding: 0 0 12 0;");

        this.playButton = createButton("PLAY");
        this.continueButton = createButton("CONTINUE");
        this.guideButton = createButton("GUIDE");
        this.settingsButton = createButton("SETTINGS");
        this.exitButton = createButton("EXIT");

        panel.getChildren().addAll(logo, subtitle, playButton, continueButton, guideButton, settingsButton, exitButton);
        getChildren().add(panel);
    }

    public void setContinueEnabled(boolean enabled) {
        continueButton.setDisable(!enabled);
        continueButton.setOpacity(enabled ? 1.0 : 0.35);
    }

    public Button getPlayButton() {
        return playButton;
    }

    public Button getContinueButton() {
        return continueButton;
    }

    public Button getGuideButton() {
        return guideButton;
    }

    public Button getSettingsButton() {
        return settingsButton;
    }

    public Button getExitButton() {
        return exitButton;
    }

    private Button createButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("menu-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }
}
