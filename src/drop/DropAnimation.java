package drop;

import javafx.scene.image.Image;

import java.util.List;

public final class DropAnimation {
    private final List<Image> frames;
    private final long frameDurationNs;

    public DropAnimation(List<Image> frames, long frameDurationNs) {
        this.frames = frames == null ? List.of() : List.copyOf(frames);
        this.frameDurationNs = Math.max(1L, frameDurationNs);
    }

    public List<Image> getFrames() {
        return frames;
    }

    public long getFrameDurationNs() {
        return frameDurationNs;
    }

    public int getFrameCount() {
        return frames.size();
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    public Image getFrame(long nowNs, long animationStartedAtNs) {
        if (frames.isEmpty()) {
            return null;
        }
        long elapsedNs = Math.max(0L, nowNs - animationStartedAtNs);
        int frameIndex = (int) ((elapsedNs / frameDurationNs) % frames.size());
        return frames.get(Math.max(0, Math.min(frameIndex, frames.size() - 1)));
    }
}
