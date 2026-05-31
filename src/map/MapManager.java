package map;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MapManager {
    private static final long FADE_DURATION_NS = 500_000_000L;

    private enum TransitionPhase {
        NONE,
        FADING_OUT,
        FADING_IN
    }

    private final Map<MapType, GameMapDefinition> definitions;
    private MapType currentMapType;
    private MapType pendingMapType;
    private TransitionPhase transitionPhase;
    private long phaseStartedAtNs;
    private double fadeAlpha;

    public MapManager(MapType initialMapType, Iterable<GameMapDefinition> definitions) {
        this.definitions = new LinkedHashMap<>();
        if (definitions != null) {
            for (GameMapDefinition definition : definitions) {
                if (definition != null) {
                    this.definitions.put(definition.type(), definition);
                }
            }
        }
        this.currentMapType = initialMapType;
        this.pendingMapType = initialMapType;
        this.transitionPhase = TransitionPhase.NONE;
        this.phaseStartedAtNs = 0L;
        this.fadeAlpha = 0.0;
    }

    public MapType getCurrentMapType() {
        return currentMapType;
    }

    public GameMapDefinition getCurrentDefinition() {
        return getDefinition(currentMapType);
    }

    public GameMapDefinition getDefinition(MapType mapType) {
        return definitions.get(mapType);
    }

    public boolean isTransitioning() {
        return transitionPhase != TransitionPhase.NONE;
    }

    public double getFadeAlpha() {
        return fadeAlpha;
    }

    public void setCurrentMap(MapType mapType) {
        if (mapType == null) {
            return;
        }
        currentMapType = mapType;
        pendingMapType = mapType;
        transitionPhase = TransitionPhase.NONE;
        phaseStartedAtNs = 0L;
        fadeAlpha = 0.0;
    }

    public void changeMap(MapType targetMapType) {
        if (targetMapType == null || targetMapType == currentMapType || !definitions.containsKey(targetMapType)) {
            return;
        }
        if (isTransitioning()) {
            return;
        }
        pendingMapType = targetMapType;
        transitionPhase = TransitionPhase.FADING_OUT;
        phaseStartedAtNs = 0L;
        fadeAlpha = 0.0;
    }

    public void update(long nowNs, Consumer<GameMapDefinition> mapLoader) {
        if (transitionPhase == TransitionPhase.NONE) {
            fadeAlpha = 0.0;
            return;
        }
        if (phaseStartedAtNs <= 0L) {
            phaseStartedAtNs = nowNs;
        }

        double progress = Math.max(0.0, Math.min(1.0, (double) (nowNs - phaseStartedAtNs) / FADE_DURATION_NS));
        if (transitionPhase == TransitionPhase.FADING_OUT) {
            fadeAlpha = progress;
            if (progress >= 1.0) {
                GameMapDefinition definition = getDefinition(pendingMapType);
                if (definition != null && mapLoader != null) {
                    mapLoader.accept(definition);
                }
                currentMapType = pendingMapType;
                transitionPhase = TransitionPhase.FADING_IN;
                phaseStartedAtNs = nowNs;
                fadeAlpha = 1.0;
            }
            return;
        }

        fadeAlpha = 1.0 - progress;
        if (progress >= 1.0) {
            transitionPhase = TransitionPhase.NONE;
            phaseStartedAtNs = 0L;
            fadeAlpha = 0.0;
        }
    }
}
