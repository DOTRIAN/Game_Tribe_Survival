package event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * GameEventBus:
 * - Event system don gian cho game loop don luong.
 * - Giup tach coupling giua Game va cac he thong save/UI/log.
 */
public class GameEventBus {
    private final List<Consumer<GameEvent>> listeners;

    // Constructor:
    // - Tao danh sach listener rong.
    public GameEventBus() {
        this.listeners = new ArrayList<>();
    }

    // subscribe:
    // - Input: ham nhan event.
    // - Output: khong tra ve, dang ky listener vao bus.
    public void subscribe(Consumer<GameEvent> listener) {
        if (listener == null) {
            return;
        }
        listeners.add(listener);
    }

    // publish:
    // - Input: event can phat.
    // - Tac dong: gui event den toan bo listener theo thu tu dang ky.
    public void publish(GameEvent event) {
        if (event == null) {
            return;
        }
        for (Consumer<GameEvent> listener : listeners) {
            listener.accept(event);
        }
    }
}
