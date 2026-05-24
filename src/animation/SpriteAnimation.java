package animation;
import javafx.scene.image.Image;


public class SpriteAnimation {
    private final Image[] frames; // các ảnh animation
    private final long frameDurationNs; // mỗi frame giữ bao lâu
    private int currentFrameIndex;  //
    private long lastFrameTime;

    public SpriteAnimation(Image[] frames, long frameDurationNs) {
        this.frames = frames;
        this.frameDurationNs = frameDurationNs;
        this.currentFrameIndex = 0;
        this.lastFrameTime = 0;
    }

    public void update(long now, boolean moving) {
        if (!moving) {
            currentFrameIndex = 0;
            return;
        }

        if (now - lastFrameTime >= frameDurationNs) {
            currentFrameIndex = (currentFrameIndex + 1) % frames.length;
            lastFrameTime = now;
        }
    }

    public Image getCurrentFrame() {
        return frames[currentFrameIndex];
    }

    public int getCurrentFrameIndex() {
        return currentFrameIndex;
    }

    public boolean updateOnce(long now) {
        if (frames.length <= 1) {
            currentFrameIndex = 0;
            return true;
        }
        if (currentFrameIndex >= frames.length - 1) {
            return true;
        }
        if (now - lastFrameTime < frameDurationNs) {
            return false;
        }
        currentFrameIndex = Math.min(currentFrameIndex + 1, frames.length - 1);
        lastFrameTime = now;
        return currentFrameIndex >= frames.length - 1;
    }

    // Reset animation ve frame dau, dung cho state transition (vd: bat dau slash moi).
    public void reset() {
        currentFrameIndex = 0;
        lastFrameTime = 0;
    }

    public int getFrameCount() {
        return frames.length;
    }
}
