package world;

import system.resource.ResourceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WorldChunk:
 * - Du lieu runtime cua 1 chunk map: resource seed va spawn points.
 */
public class WorldChunk {
    public static class GeneratedResource {
        public final int objectId;
        public final String kind;
        public final ResourceType type;
        public final double x;
        public final double y;
        public final double width;
        public final double height;

        // Constructor resource:
        // - Input: thong tin resource da procedural-generate tu chunk.
        public GeneratedResource(int objectId, String kind, ResourceType type, double x, double y, double width, double height) {
            this.objectId = objectId;
            this.kind = kind;
            this.type = type;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private final ChunkCoord coord;
    private final List<GeneratedResource> resources;

    // Constructor:
    // - Input: toa do chunk.
    // - Tac dong: tao chunk rong, sau do manager se bo sung resource spawn.
    public WorldChunk(ChunkCoord coord) {
        this.coord = coord;
        this.resources = new ArrayList<>();
    }

    public ChunkCoord getCoord() {
        return coord;
    }

    public void addResource(GeneratedResource resource) {
        if (resource == null) {
            return;
        }
        resources.add(resource);
    }

    public List<GeneratedResource> getResources() {
        return Collections.unmodifiableList(resources);
    }
}
