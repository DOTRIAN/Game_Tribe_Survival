package main;

import core.Game;
import javafx.application.Application;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        prepareWorkingDirectory();
        stage.setTitle("Tribe Survival Game");

        Game game = new Game(stage);
        game.start();
    }

    public static void main(String[] args) {
        launch();
    }

    /**
     * Ensure working directory points to project root so all relative
     * asset paths like "assets/..." resolve consistently.
     */
    private static void prepareWorkingDirectory() {
        Path current = Path.of("").toAbsolutePath().normalize();
        Path resolved = findProjectRoot(current);
        if (resolved != null) {
            System.setProperty("user.dir", resolved.toString());
            System.out.println("[BOOT] user.dir=" + System.getProperty("user.dir"));
        } else {
            System.out.println("[BOOT] keep user.dir=" + System.getProperty("user.dir"));
        }
    }

    private static Path findProjectRoot(Path start) {
        Path cursor = start;
        while (cursor != null) {
            if (looksLikeProjectRoot(cursor)) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return null;
    }

    private static boolean looksLikeProjectRoot(Path dir) {
        return Files.isRegularFile(dir.resolve("assets").resolve("Map_Game").resolve("map.tmx"))
                && Files.isDirectory(dir.resolve("src"))
                && Files.isDirectory(dir.resolve("assets"));
    }
}
