package wallbuilder.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import wallbuilder.model.TileType;
import wallbuilder.service.BuildBoard;

/**
 * BuildBoardView:
 * - Ve luoi xay dung va cac block da dat len Canvas.
 * - Muc tieu la minh hoa ro cach render 2D co ban bang JavaFX.
 */
public class BuildBoardView extends Canvas {
    private static final double CELL_SIZE = 40;
    private static final double BOARD_PADDING = 18;

    private final BuildBoard buildBoard;
    private int hoverRow;
    private int hoverColumn;

    public BuildBoardView(BuildBoard buildBoard) {
        this.buildBoard = buildBoard;
        this.hoverRow = -1;
        this.hoverColumn = -1;

        double canvasWidth = buildBoard.getColumns() * CELL_SIZE + BOARD_PADDING * 2;
        double canvasHeight = buildBoard.getRows() * CELL_SIZE + BOARD_PADDING * 2;
        setWidth(canvasWidth);
        setHeight(canvasHeight);
        redraw();
    }

    public void setHoverCell(int row, int column) {
        this.hoverRow = row;
        this.hoverColumn = column;
        redraw();
    }

    public void clearHoverCell() {
        this.hoverRow = -1;
        this.hoverColumn = -1;
        redraw();
    }

    /**
     * toBoardColumn:
     * - Input: toa do X cua chuot tren canvas.
     * - Output: cot cua o luoi, hoac -1 neu nam ngoai board.
     */
    public int toBoardColumn(double mouseX) {
        double localX = mouseX - BOARD_PADDING;
        if (localX < 0) {
            return -1;
        }
        int column = (int) (localX / CELL_SIZE);
        return column >= 0 && column < buildBoard.getColumns() ? column : -1;
    }

    /**
     * toBoardRow:
     * - Input: toa do Y cua chuot tren canvas.
     * - Output: hang cua o luoi, hoac -1 neu nam ngoai board.
     */
    public int toBoardRow(double mouseY) {
        double localY = mouseY - BOARD_PADDING;
        if (localY < 0) {
            return -1;
        }
        int row = (int) (localY / CELL_SIZE);
        return row >= 0 && row < buildBoard.getRows() ? row : -1;
    }

    public void redraw() {
        GraphicsContext graphicsContext = getGraphicsContext2D();

        graphicsContext.setFill(Color.web("#8cc36f"));
        graphicsContext.fillRect(0, 0, getWidth(), getHeight());

        graphicsContext.setFill(Color.web("#6f9b57"));
        graphicsContext.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

        drawTiles(graphicsContext);
        drawGrid(graphicsContext);
        drawHover(graphicsContext);
        drawLegend(graphicsContext);
    }

    private void drawTiles(GraphicsContext graphicsContext) {
        for (int row = 0; row < buildBoard.getRows(); row++) {
            for (int column = 0; column < buildBoard.getColumns(); column++) {
                TileType tileType = buildBoard.getTile(row, column);
                if (tileType == TileType.EMPTY) {
                    continue;
                }

                double x = BOARD_PADDING + column * CELL_SIZE;
                double y = BOARD_PADDING + row * CELL_SIZE;

                if (tileType == TileType.SMALL_STONE) {
                    graphicsContext.setFill(Color.web("#7a7a7a"));
                    graphicsContext.fillOval(x + 9, y + 9, 22, 22);
                    graphicsContext.setStroke(Color.web("#4f4f4f"));
                    graphicsContext.strokeOval(x + 9, y + 9, 22, 22);
                } else if (tileType == TileType.STONE_WALL) {
                    graphicsContext.setFill(Color.web("#898989"));
                    graphicsContext.fillRoundRect(x + 4, y + 6, 32, 28, 6, 6);
                    graphicsContext.setStroke(Color.web("#565656"));
                    graphicsContext.strokeRoundRect(x + 4, y + 6, 32, 28, 6, 6);
                    graphicsContext.strokeLine(x + 20, y + 6, x + 20, y + 34);
                    graphicsContext.strokeLine(x + 4, y + 20, x + 36, y + 20);
                }
            }
        }
    }

    private void drawGrid(GraphicsContext graphicsContext) {
        graphicsContext.setStroke(Color.color(0, 0, 0, 0.20));
        for (int row = 0; row <= buildBoard.getRows(); row++) {
            double y = BOARD_PADDING + row * CELL_SIZE;
            graphicsContext.strokeLine(BOARD_PADDING, y, BOARD_PADDING + buildBoard.getColumns() * CELL_SIZE, y);
        }
        for (int column = 0; column <= buildBoard.getColumns(); column++) {
            double x = BOARD_PADDING + column * CELL_SIZE;
            graphicsContext.strokeLine(x, BOARD_PADDING, x, BOARD_PADDING + buildBoard.getRows() * CELL_SIZE);
        }
    }

    private void drawHover(GraphicsContext graphicsContext) {
        if (hoverRow < 0 || hoverColumn < 0) {
            return;
        }
        double x = BOARD_PADDING + hoverColumn * CELL_SIZE;
        double y = BOARD_PADDING + hoverRow * CELL_SIZE;
        graphicsContext.setStroke(Color.WHITE);
        graphicsContext.setLineWidth(2);
        graphicsContext.strokeRect(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2);
        graphicsContext.setLineWidth(1);
    }

    private void drawLegend(GraphicsContext graphicsContext) {
        graphicsContext.setFill(Color.color(0, 0, 0, 0.58));
        graphicsContext.fillRoundRect(14, getHeight() - 56, 330, 34, 10, 10);
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.setFont(Font.font(13));
        graphicsContext.fillText("Left Click: Dat block | Right Click: Xoa block | 1-9: Chon o hotbar", 24, getHeight() - 34);
    }
}
