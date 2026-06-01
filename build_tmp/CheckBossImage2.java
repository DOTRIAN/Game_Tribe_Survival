import javafx.scene.image.Image;
import java.nio.file.Path;
public class CheckBossImage2 {
  public static void main(String[] args) {
    Image img = new Image(Path.of("assets/map_boss.png").toUri().toString(), false);
    System.out.println("progress=" + img.getProgress());
    System.out.println("error=" + img.isError());
    System.out.println("w=" + img.getWidth() + ", h=" + img.getHeight());
    if (img.isError()) {
      System.out.println(img.getException());
    }
  }
}
