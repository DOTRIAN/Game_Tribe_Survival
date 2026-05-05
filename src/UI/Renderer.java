package ui;

import core.GameState;
import entity.Player;
import entity.Wolf;
import entity.Tree;
import input.InputHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import  core.GameConfig;
import javafx.scene.image.Image;
import java.util.List;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class Renderer {



    private final Canvas canvas;  // Là vùng để vẽ game
    private final GraphicsContext graphicsContext;   // là cây bút để vẽ canvas
    private final Hud hud;
    private final Image welcomeBackgroundImage;
    private final Image gameBackgroundImage;


    public Renderer(Stage stage, InputHandler inputHandler) {
        this.canvas = new Canvas(GameConfig.WIDTH, GameConfig.HEIGHT);
        this.graphicsContext = canvas.getGraphicsContext2D();
        this.hud= new Hud();
        this.welcomeBackgroundImage = new Image("file:assets/backgrounds/menu_bg1.png");
        this.gameBackgroundImage = new Image("file:assets/backgrounds/grass03.png");


        StackPane root = new StackPane(canvas);// tạo 1 StackPane và đặt canvas trong nó
        // StackPane là 1 layout container của JVFX để quản lý các node con
        // ko đưa canvas vào thẳng scene vì scene thường nhận 1 root node, và StackPane là root đó

        Scene scene = new Scene(root, GameConfig.WIDTH,GameConfig.HEIGHT);
        inputHandler.attach(scene);
        //gán input vào scene, kiểu hãy đọc input trên scene này

        stage.setScene(scene); // đặt scene làm nội dung của cửa sổ stage
        //->> kể từ giờ cửa sổ sẽ hiển thị scene vừa tạo
        stage.show();
        canvas.setFocusTraversable(true);
        canvas.requestFocus();
        stage.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                canvas.requestFocus();
            }
        });
    }

    public void render(GameState gameState, Player player, Wolf wolf,long now,boolean wolfMoving, List<Tree> trees,double cameraX, double cameraY, int menuIndex) {
        if (gameState == GameState.WELCOME) {
            if (welcomeBackgroundImage.isError()) {
                graphicsContext.setFill(Color.web("#2a3a2a"));
                graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
            } else {
                graphicsContext.drawImage(welcomeBackgroundImage, 0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
            }

            graphicsContext.setFill(Color.color(0, 0, 0, 0.5));
            graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);

            double panelX = 280;
            double panelY = 120;
            double panelW = 400;
            double panelH = 300;
            graphicsContext.setFill(Color.color(0.95, 0.92, 0.78, 0.9));
            graphicsContext.fillRoundRect(panelX, panelY, panelW, panelH, 20, 20);
            graphicsContext.setStroke(Color.web("#5b4a2e"));
            graphicsContext.setLineWidth(3);
            graphicsContext.strokeRoundRect(panelX, panelY, panelW, panelH, 20, 20);

            graphicsContext.setFill(Color.web("#2f2618"));
            graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 34));
            graphicsContext.fillText("TRIBE SURVIVAL", 334, 186);

            String[] menuItems = {"PLAY", "GUIDE", "EXIT"};
            double itemY = 270;
            for (int i = 0; i < menuItems.length; i++) {
                boolean selected = i == menuIndex;
                graphicsContext.setFill(selected ? Color.web("#7b5b2e") : Color.web("#d6c29b"));
                graphicsContext.fillRoundRect(350, itemY - 24, 260, 42, 12, 12);
                graphicsContext.setStroke(Color.web("#4a3a20"));
                graphicsContext.strokeRoundRect(350, itemY - 24, 260, 42, 12, 12);

                graphicsContext.setFill(selected ? Color.web("#fff4d8") : Color.web("#3a2f1d"));
                graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
                if ("PLAY".equals(menuItems[i])) {
                    graphicsContext.fillText(menuItems[i], 456, itemY + 2);
                } else if ("GUIDE".equals(menuItems[i])) {
                    graphicsContext.fillText(menuItems[i], 448, itemY + 2);
                } else {
                    graphicsContext.fillText(menuItems[i], 460, itemY + 2);
                }

                itemY += 58;
            }

            return;
        }

        if (gameState == GameState.GUIDE) {
            graphicsContext.setFill(Color.LIGHTYELLOW);
            graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);

            graphicsContext.setFill(Color.DARKGREEN);
            graphicsContext.fillText("HOW TO PLAY", 420, 180);
            graphicsContext.fillText("W A S D: Move", 390, 230);
            graphicsContext.fillText("J: Take damage (test)", 390, 260);
            graphicsContext.fillText("K: Heal (test)", 390, 290);
            graphicsContext.fillText("ENTER: Play", 390, 340);
            graphicsContext.fillText("ESC: Back to Welcome", 390, 370);
            return;
        }

        if (gameBackgroundImage.isError()) {
            graphicsContext.setFill(Color.BEIGE);
            graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
        } else {
            graphicsContext.drawImage(
                    gameBackgroundImage,
                    0, 0,
                    GameConfig.WIDTH, GameConfig.HEIGHT
            );
        }

        for (Tree tree : trees) {
            tree.draw(graphicsContext,cameraX,cameraY);  // tu goij de ve cay
        }
        //4 dongf treen la ve cay
        graphicsContext.setFill(Color.DARKGREEN);
        graphicsContext.fillText("State: " + gameState, 20, 30);
        graphicsContext.fillText("WASD: move", 20, 55);
        graphicsContext.fillText("J: take damage | K: heal", 20, 80);

        player.draw(graphicsContext,cameraX,cameraY);

        wolf.draw(graphicsContext,cameraX,cameraY,now,wolfMoving,player);

        hud.render(graphicsContext,player);

        if (gameState == GameState.GAME_OVER) {
            graphicsContext.setFill(Color.DARKRED);
            graphicsContext.fillText("GAME OVER", 400, 250);

            graphicsContext.setFill(Color.BLACK);
            graphicsContext.fillText("The wolf caught you.", 380, 280);
            graphicsContext.fillText("Press R to restart.", 385, 310);
        }
    }
}
