package wallbuilder.core;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import wallbuilder.asset.AssetManager;
import wallbuilder.build.BuildManager;
import wallbuilder.item.Item;
import wallbuilder.item.WallItem;
import wallbuilder.ui.Hotbar;
import wallbuilder.ui.HotbarSlot;
import wallbuilder.world.GameWorld;
import wallbuilder.world.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * GameApp:
 * - Entry point chinh cua mini game build wall.
 * - Noi hotbar, world, input va build mode thanh mot flow hoan chinh.
 */
public class GameApp extends Application {
    private final List<KeyCode> movementKeys = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        AssetManager assetManager = new AssetManager();
        GameWorld gameWorld = new GameWorld(assetManager);
        Player player = gameWorld.getPlayer();

        List<Item> hotbarItems = createDefaultHotbarItems(assetManager);
        Hotbar hotbar = new Hotbar(hotbarItems);
        BuildManager buildManager = new BuildManager(gameWorld, gameWorld.getCollisionManager());
        buildManager.selectItem(hotbar.getSelectedItem());
        gameWorld.setBuildPreview(buildManager.getPreviewRectangle());

        Label titleLabel = new Label("JavaFX Survival Build Demo");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label statusLabel = new Label("BUILD_WALL_MODE dang bat voi Stone Wall.");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #f5f5f5;");

        VBox topBox = new VBox(8, titleLabel, statusLabel);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(18, 0, 12, 0));

        BorderPane contentPane = new BorderPane();
        contentPane.setTop(topBox);
        contentPane.setCenter(gameWorld);
        BorderPane.setAlignment(gameWorld, Pos.CENTER);

        StackPane root = new StackPane(contentPane, hotbar);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #72bbff, #9fda73);");
        StackPane.setAlignment(hotbar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(hotbar, new Insets(0, 0, 18, 0));

        Scene scene = new Scene(root, 1040, 760, Color.BLACK);

        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (!movementKeys.contains(code)) {
                movementKeys.add(code);
            }

            int hotbarIndex = parseHotbarIndex(code);
            if (hotbarIndex >= 0) {
                hotbar.updateSelection(hotbarIndex);
                buildManager.selectItem(hotbar.getSelectedItem());
                statusLabel.setText(buildManager.getBuildMode() == wallbuilder.build.BuildMode.BUILD_WALL_MODE
                        ? "BUILD_WALL_MODE dang bat."
                        : "Dang o che do thuong.");
            }

            if (code == KeyCode.ESCAPE) {
                buildManager.cancelBuildMode();
                statusLabel.setText("Da thoat BUILD_WALL_MODE.");
            }
        });

        scene.setOnKeyReleased(event -> movementKeys.remove(event.getCode()));

        for (HotbarSlot slot : hotbar.getSlots()) {
            slot.setOnMouseClicked(event -> {
                hotbar.updateSelection(slot.getSlotIndex());
                buildManager.selectItem(hotbar.getSelectedItem());
                statusLabel.setText(buildManager.getBuildMode() == wallbuilder.build.BuildMode.BUILD_WALL_MODE
                        ? "BUILD_WALL_MODE dang bat."
                        : "Dang o che do thuong.");
                event.consume();
            });
        }

        gameWorld.setOnMouseMoved(event -> buildManager.updatePreview(event.getX(), event.getY(), player));
        gameWorld.setOnMouseDragged(event -> buildManager.updatePreview(event.getX(), event.getY(), player));
        gameWorld.setOnMouseExited(event -> buildManager.getPreviewRectangle().setVisible(false));
        gameWorld.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                buildManager.updatePreview(event.getX(), event.getY(), player);
                if (buildManager.tryPlaceSelectedItem(player)) {
                    statusLabel.setText("Da dat Stone Wall thanh cong.");
                } else {
                    statusLabel.setText("Khong the dat wall tai vi tri nay.");
                }
            }
        });

        AnimationTimer loop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double deltaX = 0;
                double deltaY = 0;
                if (movementKeys.contains(KeyCode.A)) {
                    deltaX -= 1;
                }
                if (movementKeys.contains(KeyCode.D)) {
                    deltaX += 1;
                }
                if (movementKeys.contains(KeyCode.W)) {
                    deltaY -= 1;
                }
                if (movementKeys.contains(KeyCode.S)) {
                    deltaY += 1;
                }
                if (deltaX != 0 || deltaY != 0) {
                    player.move(deltaX, deltaY, gameWorld.getCollisionManager());
                }
            }
        };
        loop.start();

        stage.setTitle("JavaFX Wall Builder");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private List<Item> createDefaultHotbarItems(AssetManager assetManager) {
        List<Item> items = new ArrayList<>();
        items.add(new WallItem("stone_wall", "Stone Wall", assetManager.getWallImage("stoneWall_S"), 1, 1));
        for (int index = 1; index < 9; index++) {
            items.add(null);
        }
        return items;
    }

    private int parseHotbarIndex(KeyCode code) {
        return switch (code) {
            case DIGIT1, NUMPAD1 -> 0;
            case DIGIT2, NUMPAD2 -> 1;
            case DIGIT3, NUMPAD3 -> 2;
            case DIGIT4, NUMPAD4 -> 3;
            case DIGIT5, NUMPAD5 -> 4;
            case DIGIT6, NUMPAD6 -> 5;
            case DIGIT7, NUMPAD7 -> 6;
            case DIGIT8, NUMPAD8 -> 7;
            case DIGIT9, NUMPAD9 -> 8;
            default -> -1;
        };
    }
}
