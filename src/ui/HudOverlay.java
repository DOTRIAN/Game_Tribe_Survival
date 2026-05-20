package ui;

import entity.Player;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * HudOverlay:
 * - Redesigned survival status overlay in the top-left corner.
 * - Displays character name, compact status bars (HP, Energy, XP), level, and a styled quick Shop button.
 */
public class HudOverlay extends VBox {
    private final Label nameLabel;
    private final ProgressBar hpBar;
    private final ProgressBar energyBar;
    private final ProgressBar xpBar;
    private final Label hpLabel;
    private final Label energyLabel;
    private final Label xpLabel;
    private final Label levelLabel;
    private final Button shopButton;

    public HudOverlay() {
        getStyleClass().add("hud-panel");
        setSpacing(8);
        setPadding(new Insets(12, 14, 12, 14));
        setPrefWidth(240);

        this.nameLabel = new Label("SURVIVOR");
        nameLabel.getStyleClass().add("hud-title");
        nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #eecf97;");

        this.hpBar = createBar("hp-bar");
        this.energyBar = createBar("energy-bar");
        this.xpBar = createBar("xp-bar");

        this.hpLabel = new Label("HP 0/0");
        this.energyLabel = new Label("EN 0/0");
        this.xpLabel = new Label("XP 0/0");
        this.levelLabel = new Label("LV. 1");

        hpLabel.getStyleClass().add("hud-value");
        energyLabel.getStyleClass().add("hud-value");
        xpLabel.getStyleClass().add("hud-value");
        levelLabel.getStyleClass().add("hud-value");

        hpLabel.setStyle("-fx-font-size: 11px;");
        energyLabel.setStyle("-fx-font-size: 11px;");
        xpLabel.setStyle("-fx-font-size: 11px;");
        levelLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #eecf97; -fx-font-size: 13px;");

        Label hpIcon = createStatIcon("❤");
        Label energyIcon = createStatIcon("⚡");
        Label xpIcon = createStatIcon("✦");

        HBox hpRow = buildBarRow(hpIcon, hpBar, hpLabel, "hp-row");
        HBox energyRow = buildBarRow(energyIcon, energyBar, energyLabel, "energy-row");
        HBox xpRow = buildBarRow(xpIcon, xpBar, xpLabel, "xp-row");

        HBox footerRow = new HBox(8);
        footerRow.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.shopButton = new Button("Shop");
        shopButton.getStyleClass().add("overlay-small-button");

        footerRow.getChildren().addAll(levelLabel, spacer, shopButton);
        getChildren().addAll(nameLabel, hpRow, energyRow, xpRow, footerRow);
    }

    public void update(Player player) {
        if (player == null) {
            return;
        }

        // Live name update
        String playerName = player.getPlayerName();
        if (playerName == null || playerName.isBlank()) {
            nameLabel.setText("SURVIVOR");
        } else {
            nameLabel.setText(playerName.toUpperCase());
        }

        double hpProgress = player.getMaxHp() <= 0 ? 0.0 : (double) player.getHp() / player.getMaxHp();
        double energyProgress = player.getMaxEnergy() <= 0 ? 0.0 : player.getEnergy() / player.getMaxEnergy();
        double xpProgress = player.getExperienceToNextLevel() <= 0 ? 0.0 : (double) player.getExperience() / player.getExperienceToNextLevel();

        hpBar.setProgress(clamp01(hpProgress));
        energyBar.setProgress(clamp01(energyProgress));
        xpBar.setProgress(clamp01(xpProgress));

        hpLabel.setText("HP " + player.getHp() + "/" + player.getMaxHp());
        energyLabel.setText("EN " + (int) player.getEnergy() + "/" + (int) player.getMaxEnergy());
        xpLabel.setText("XP " + player.getExperience() + "/" + player.getExperienceToNextLevel());
        levelLabel.setText("LV. " + player.getLevel());
    }

    public Button getShopButton() {
        return shopButton;
    }

    private ProgressBar createBar(String styleClass) {
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add("status-bar");
        progressBar.getStyleClass().add(styleClass);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        return progressBar;
    }

    private Label createStatIcon(String iconText) {
        Label label = new Label(iconText);
        label.getStyleClass().add("stat-icon");
        return label;
    }

    private HBox buildBarRow(Label icon, ProgressBar bar, Label valueLabel, String rowStyleClass) {
        HBox row = new HBox(6);
        row.getStyleClass().add(rowStyleClass);
        row.setAlignment(Pos.CENTER_LEFT);
        valueLabel.setMinWidth(72);
        row.getChildren().addAll(icon, bar, valueLabel);
        return row;
    }

    private double clamp01(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }
}
