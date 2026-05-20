package event;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GameEvent:
 * - Dong goi 1 su kien game + payload kem theo.
 */
public class GameEvent {
    private final GameEventType type;
    private final long createdAtNs;
    private final Map<String, Object> payload;

    // Constructor:
    // - Input: type va payload tuy y theo ngu canh su kien.
    // - Tac dong: tao object immutable de dispatch an toan.
    public GameEvent(GameEventType type, Map<String, Object> payload) {
        this.type = type;
        this.createdAtNs = System.nanoTime();
        this.payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }

    public GameEventType getType() {
        return type;
    }

    public long getCreatedAtNs() {
        return createdAtNs;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
