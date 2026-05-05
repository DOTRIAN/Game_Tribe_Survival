package map;

public class TileLayerData {
    // Ten layer trong Tiled (Grounds, Objects, Foreground...).
    private final String name;
    // Kich thuoc theo so o tile.
    private final int width;
    private final int height;
    // Mang 1 chieu chua global tile id (GID) theo CSV.
    private final int[] gids;

    public TileLayerData(String name, int width, int height, int[] gids) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.gids = gids;
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getGidAt(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return 0;
        }
        return gids[y * width + x];
    }
}
