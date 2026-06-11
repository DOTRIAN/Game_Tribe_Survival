package core;

import javafx.animation.AnimationTimer;
//AnimationTimer của JavaFX đã là một cơ chế chạy lặp theo từng frame, rất hợp để làm game loop đơn giản.


public class GameLoop extends AnimationTimer {

    private final Game game;
    private boolean frameFailureReported;

    public GameLoop(Game game) {
        this.game = game;
        this.frameFailureReported = false;
    }

    @Override
    public void handle(long now) { // long là kiểu so nguyên rất lớn, now là thời điểm hiện tại do Animatioon Timer truyền vào mỗi frame
        /*Nên sau này người ta dùng now để tính delta time, nghĩa là thời gian trôi qua giữa hai frame,
        rồi di chuyển theo thời gian thật thay vì theo số frame*/
        try {
            game.update(now);
            game.render(now);
        } catch (Exception exception) {
            if (!frameFailureReported) {
                frameFailureReported = true;
                System.err.println("[GameLoop] Frame failed: " + exception.getMessage());
                exception.printStackTrace();
            }
        }
    }
}

