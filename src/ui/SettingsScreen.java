package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * SettingsScreen:
 * - Redesigned settings overlay using glassmorphism styling and custom CSS sliders/checkboxes.
 */
public class SettingsScreen extends StackPane {
    private final Slider musicSlider;
    private final Slider sfxSlider;
    private final CheckBox fullscreenToggle;
    private final CheckBox showFpsToggle;
    private final CheckBox debugGridToggle;
    private final Button backButton;

    public SettingsScreen() {
        getStyleClass().add("screen-overlay");

        VBox panel = new VBox(16);
        panel.getStyleClass().add("settings-panel");
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(30, 36, 30, 36));
        panel.setMaxWidth(440);

        Label title = new Label("SETTINGS");
        title.getStyleClass().add("menu-logo");
        title.setStyle("-fx-font-size: 24px; -fx-padding: 0 0 4 0;");

        this.musicSlider = buildSlider();
        this.sfxSlider = buildSlider();
        this.fullscreenToggle = buildToggle("Fullscreen Mode");
        this.showFpsToggle = buildToggle("Show FPS Counter");
        this.debugGridToggle = buildToggle("Show Debug Grid");

        this.backButton = new Button("Back");
        backButton.getStyleClass().add("secondary-button");
        backButton.setMaxWidth(Double.MAX_VALUE);

        VBox contentBox = new VBox(14);
        contentBox.getChildren().addAll(
                buildSliderRow("Music Volume", musicSlider),
                buildSliderRow("SFX Volume", sfxSlider),
                fullscreenToggle,
                showFpsToggle,
                debugGridToggle
        );
        contentBox.setPadding(new Insets(4, 0, 12, 0));

        panel.getChildren().addAll(title, contentBox, backButton);

        VBox wrapper = new VBox(panel);
        wrapper.setAlignment(Pos.CENTER);
        getChildren().add(wrapper);
    }

    public void applySettings(GameSettings settings) {
        musicSlider.setValue(settings.getMusicVolume());
        sfxSlider.setValue(settings.getSfxVolume());
        fullscreenToggle.setSelected(settings.isFullscreen());
        showFpsToggle.setSelected(settings.isShowFps());
        debugGridToggle.setSelected(settings.isDebugGrid());
    }

    public void copyValuesInto(GameSettings settings) {
        settings.setMusicVolume(musicSlider.getValue());
        settings.setSfxVolume(sfxSlider.getValue());
        settings.setFullscreen(fullscreenToggle.isSelected());
        settings.setShowFps(showFpsToggle.isSelected());
        settings.setDebugGrid(debugGridToggle.isSelected());
    }

    public Slider getMusicSlider() {
        return musicSlider;
    }

    public Slider getSfxSlider() {
        return sfxSlider;
    }

    public CheckBox getFullscreenToggle() {
        return fullscreenToggle;
    }

    public CheckBox getShowFpsToggle() {
        return showFpsToggle;
    }

    public CheckBox getDebugGridToggle() {
        return debugGridToggle;
    }

    public Button getBackButton() {
        return backButton;
    }

    private Slider buildSlider() {
        Slider slider = new Slider(0, 1, 0.5);
        slider.getStyleClass().add("settings-slider");
        return slider;
    }

    private CheckBox buildToggle(String text) {
        CheckBox checkBox = new CheckBox(text);
        checkBox.getStyleClass().add("settings-toggle");
        checkBox.setMaxWidth(Double.MAX_VALUE);
        return checkBox;
    }

    private HBox buildSliderRow(String title, Slider slider) {
        Label label = new Label(title);
        label.getStyleClass().add("hud-value");
        label.setMinWidth(110);

        HBox.setHgrow(slider, Priority.ALWAYS);
        HBox row = new HBox(12, label, slider);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
