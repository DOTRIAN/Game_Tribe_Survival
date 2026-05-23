package main;

import core.Game;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Tribe Survival Game");

        Game game = new Game(stage);  // biến cục bộ dùng trong methods
        game.start();
    }

    /**
     đây là phương thức start tự định nghĩa để tbao bắt đầu vòng đời game
     khác với Main.start(Stage stage) vì đây1 là method của JavaFx Applicatio
     JAVAFX tự gọi nó khi khởi động
     */

    public static void main(String[] args) {
        launch();
    }
}
