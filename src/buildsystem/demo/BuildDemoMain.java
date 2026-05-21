package buildsystem.demo;

import build.AssetManager;
import build.CollisionManager;
import buildsystem.core.BuildController;
import buildsystem.core.BuildManager;
import buildsystem.core.BuildPreview;
import buildsystem.object.BuildObject;
import inventory.Inventory;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * BuildDemoMain:
 * - Demo smoke-test dung CHINH BuildManager/BuildController/BuildToolbar/PlacementValidator cua module buildsystem.
 * - Khong copy gameplay logic rieng:
 *   1) Demo chi seed inventory va runtime world nho.
 *   2) Input Q / 1..9 / left click di qua BuildController giong game chinh.
 *   3) Render doc placed objects + preview tu BuildManager.
 */
public class BuildDemoMain extends Application {
    private static final double DEMO_WORLD_WIDTH = 960;
    private static final double DEMO_WORLD_HEIGHT = 640;

    @Override
    public void start(Stage stage) {
        AssetManager assetManager = new AssetManager();
        CollisionManager collisionManager = new CollisionManager(null, null, null, null, DEMO_WORLD_WIDTH, DEMO_WORLD_HEIGHT);
        BuildManager buildManager = new BuildManager(assetManager, collisionManager);
        BuildController buildController = new BuildController(buildManager);
        Inventory inventory = new Inventory();
        inventory.addItem("stone_wall", 24);
        inventory.addItem("wood_wall", 20);
        inventory.addItem("torch", 12);
        inventory.addItem("spike_trap", 8);
        inventory.addItem("chest", 3);
        inventory.addItem("workbench", 2);
        buildManager.syncToolbar(inventory.snapshot());
        buildController.onToolbarSlotSelected(0, inventory);

        Canvas canvas = new Canvas(DEMO_WORLD_WIDTH, DEMO_WORLD_HEIGHT);
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, DEMO_WORLD_WIDTH, DEMO_WORLD_HEIGHT);
        GraphicsContext graphics = canvas.getGraphicsContext2D();

        final double[] mouseX = {0.0};
        final double[] mouseY = {0.0};

        scene.setOnMouseMoved(event -> {
            mouseX[0] = event.getX();
            mouseY[0] = event.getY();
        });
        scene.setOnMouseDragged(event -> {
            mouseX[0] = event.getX();
            mouseY[0] = event.getY();
        });
        scene.setOnMouseClicked(event -> buildController.onPrimaryClickPlace(null, inventory));
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.Q) {
                buildController.onRotatePressed();
                return;
            }
            int digitIndex = keyToToolbarIndex(event.getCode());
            if (digitIndex >= 0) {
                buildController.onToolbarSlotSelected(digitIndex, inventory);
            }
        });

        stage.setScene(scene);
        stage.setTitle("BuildSystem Demo");
        stage.show();

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                buildController.onCursorMoved(mouseX[0], mouseY[0], 0, 0, 1.0, false, null, inventory);
                buildManager.syncToolbar(inventory.snapshot());
                renderDemo(graphics, assetManager, buildManager, inventory);
            }
        }.start();
    }

    private void renderDemo(GraphicsContext graphics,
                            AssetManager assetManager,
                            BuildManager buildManager,
                            Inventory inventory) {
        graphics.setFill(Color.web("#1b1f24"));
        graphics.fillRect(0, 0, DEMO_WORLD_WIDTH, DEMO_WORLD_HEIGHT);

        graphics.setStroke(Color.color(1, 1, 1, 0.08));
        for (double x = 0; x <= DEMO_WORLD_WIDTH; x += 32) {
            graphics.strokeLine(x, 0, x, DEMO_WORLD_HEIGHT);
        }
        for (double y = 0; y <= DEMO_WORLD_HEIGHT; y += 32) {
            graphics.strokeLine(0, y, DEMO_WORLD_WIDTH, y);
        }

        for (BuildObject object : buildManager.getPlacedObjects()) {
            if (object == null) {
                continue;
            }
            Image image = assetManager.getSprite(object.getSpriteKey());
            if (image != null && !image.isError()) {
                drawRotatedImage(graphics, image, object.getRenderX(), object.getRenderY(), object.getRenderWidth(), object.getRenderHeight(), object.getRotationDegrees());
            } else {
                graphics.setFill(Color.web("#b5b7bb"));
                graphics.fillRect(object.getRenderX(), object.getRenderY(), object.getRenderWidth(), object.getRenderHeight());
            }
        }

        BuildPreview preview = buildManager.getPreview();
        if (preview != null && preview.isVisible()) {
            graphics.save();
            graphics.setGlobalAlpha(preview.getOpacity());
            Image image = preview.getImage();
            if (image != null && !image.isError()) {
                drawRotatedImage(graphics, image, preview.getRenderX(), preview.getRenderY(), preview.getWidth(), preview.getHeight(), preview.getRotationDegrees());
            } else {
                graphics.setFill(Color.color(0.9, 0.9, 0.9, 0.55));
                graphics.fillRect(preview.getRenderX(), preview.getRenderY(), preview.getWidth(), preview.getHeight());
            }
            graphics.restore();

            if (!preview.isValid()) {
                graphics.setFill(Color.color(1.0, 0.15, 0.15, 0.20));
                graphics.fillRect(preview.getRenderX(), preview.getRenderY(), preview.getWidth(), preview.getHeight());
                graphics.setStroke(Color.color(1.0, 0.12, 0.12, 0.95));
                graphics.strokeRect(preview.getRenderX(), preview.getRenderY(), preview.getWidth(), preview.getHeight());
            }
        }

        graphics.setFill(Color.web("#f4efe1"));
        graphics.setFont(Font.font("Consolas", 14));
        graphics.fillText("1..9 select build | Q rotate | Click place | BuildSystem demo dung chung BuildManager", 20, 24);
        graphics.fillText("Selected: " + (buildManager.getSelectedDefinition() == null ? "none" : buildManager.getSelectedDefinition().getDisplayName()), 20, 48);
        graphics.fillText("Stone Wall: " + inventory.getAmount("stone_wall")
                + " | Wood Wall: " + inventory.getAmount("wood_wall")
                + " | Torch: " + inventory.getAmount("torch")
                + " | Trap: " + inventory.getAmount("spike_trap"), 20, 72);
        if (preview != null && preview.isVisible()) {
            graphics.fillText("Preview valid: " + preview.isValid() + " | reason: " + preview.getValidationMessage(), 20, 96);
        }
    }

    private void drawRotatedImage(GraphicsContext graphics,
                                  Image image,
                                  double x,
                                  double y,
                                  double width,
                                  double height,
                                  double rotationDegrees) {
        graphics.save();
        graphics.translate(x + width / 2.0, y + height / 2.0);
        graphics.rotate(rotationDegrees);
        graphics.drawImage(image, -width / 2.0, -height / 2.0, width, height);
        graphics.restore();
    }

    private int keyToToolbarIndex(KeyCode keyCode) {
        return switch (keyCode) {
            case DIGIT1 -> 0;
            case DIGIT2 -> 1;
            case DIGIT3 -> 2;
            case DIGIT4 -> 3;
            case DIGIT5 -> 4;
            case DIGIT6 -> 5;
            case DIGIT7 -> 6;
            case DIGIT8 -> 7;
            case DIGIT9 -> 8;
            default -> -1;
        };
    }

    public static void main(String[] args) {
        launch(args);
    }
}
