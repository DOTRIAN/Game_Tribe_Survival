package world;

import system.resource.ResourceType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * InfiniteWorldManager:
 * - Quan ly chunk map vo han theo vi tri player.
 * - Sinh procedural resource khi chunk lan dau duoc load.
 */
public class InfiniteWorldManager {
    // Kich thuoc 1 chunk theo pixel world.
    private static final int CHUNK_SIZE = 768;
    // Ban kinh load chunk quanh player (2 = 5x5 chunk active).
    private static final int ACTIVE_RADIUS = 2;

    // worldSeed:
    // - Seed giup procedural generation on dinh giua nhieu lan save/load.
    private final long worldSeed;
    private final Map<ChunkCoord, WorldChunk> loadedChunks;

    // Constructor:
    // - Input: worldSeed de procedural generation reproducible.
    public InfiniteWorldManager(long worldSeed) {
        this.worldSeed = worldSeed;
        this.loadedChunks = new LinkedHashMap<>();
    }

    // updateAndGetNewChunks:
    // - Input: vi tri player hien tai.
    // - Output: danh sach chunk moi vua duoc load trong frame nay.
    // - Tac dong: mo rong world theo buoc di cua player.
    public List<WorldChunk> updateAndGetNewChunks(double playerX, double playerY) {
        List<WorldChunk> newlyLoaded = new ArrayList<>();
        int centerChunkX = floorDiv((int) Math.floor(playerX), CHUNK_SIZE);
        int centerChunkY = floorDiv((int) Math.floor(playerY), CHUNK_SIZE);

        for (int offsetY = -ACTIVE_RADIUS; offsetY <= ACTIVE_RADIUS; offsetY++) {
            for (int offsetX = -ACTIVE_RADIUS; offsetX <= ACTIVE_RADIUS; offsetX++) {
                ChunkCoord coord = new ChunkCoord(centerChunkX + offsetX, centerChunkY + offsetY);
                if (loadedChunks.containsKey(coord)) {
                    continue;
                }
                WorldChunk chunk = generateChunk(coord);
                loadedChunks.put(coord, chunk);
                newlyLoaded.add(chunk);
            }
        }

        return newlyLoaded;
    }

    // generateChunk:
    // - Input: toa do chunk can tao.
    // - Output: chunk da co danh sach resource procedural.
    // - Tac dong: bo sung tai nguyen cho gameplay sinh ton ban ngay.
    private WorldChunk generateChunk(ChunkCoord coord) {
        WorldChunk chunk = new WorldChunk(coord);

        long localSeed = worldSeed ^ (coord.x * 73856093L) ^ (coord.y * 19349663L);
        Random random = new Random(localSeed);

        int resourceCount = 8 + random.nextInt(8);
        for (int i = 0; i < resourceCount; i++) {
            double px = coord.x * CHUNK_SIZE + 32 + random.nextInt(CHUNK_SIZE - 64);
            double py = coord.y * CHUNK_SIZE + 32 + random.nextInt(CHUNK_SIZE - 64);

            String kind;
            ResourceType type;
            int roll = random.nextInt(100);
            if (roll < 50) {
                kind = "tree_oak";
                type = ResourceType.TREE;
            } else if (roll < 85) {
                kind = "rock_small";
                type = ResourceType.ROCK;
            } else {
                kind = "grass";
                type = ResourceType.GRASS;
            }

            int objectId = Math.abs((coord.x * 92821) ^ (coord.y * 68917) ^ (i * 131)) + 1_000_000;
            chunk.addResource(new WorldChunk.GeneratedResource(objectId, kind, type, px, py, 48, 48));
        }

        return chunk;
    }

    public int getChunkSize() {
        return CHUNK_SIZE;
    }

    public long getWorldSeed() {
        return worldSeed;
    }

    // floorDiv:
    // - Input: value pixel, divisor chunk size.
    // - Output: chunk index dung cho ca gia tri am.
    private int floorDiv(int value, int divisor) {
        int result = value / divisor;
        if ((value ^ divisor) < 0 && (result * divisor != value)) {
            result--;
        }
        return result;
    }
}
