package system;

public final class MovementSlideSystem {
    private static final double EPSILON = 0.0001;

    private MovementSlideSystem() {
    }

    @FunctionalInterface
    public interface OccupancyTester {
        boolean canOccupy(double x, double y, double width, double height);
    }

    public static MoveResult move(double startX,
                                  double startY,
                                  double width,
                                  double height,
                                  double dx,
                                  double dy,
                                  OccupancyTester tester) {
        if (tester == null || (Math.abs(dx) < EPSILON && Math.abs(dy) < EPSILON)) {
            return new MoveResult(startX, startY, false, false);
        }
        MoveResult xFirst = moveAxisOrder(startX, startY, width, height, dx, dy, tester, true);
        if (Math.abs(dx) < EPSILON || Math.abs(dy) < EPSILON) {
            return xFirst;
        }
        MoveResult yFirst = moveAxisOrder(startX, startY, width, height, dx, dy, tester, false);
        return score(yFirst, startX, startY) > score(xFirst, startX, startY) ? yFirst : xFirst;
    }

    private static MoveResult moveAxisOrder(double startX,
                                            double startY,
                                            double width,
                                            double height,
                                            double dx,
                                            double dy,
                                            OccupancyTester tester,
                                            boolean xFirst) {
        double x = startX;
        double y = startY;
        boolean movedX = false;
        boolean movedY = false;

        if (xFirst) {
            if (Math.abs(dx) >= EPSILON && tester.canOccupy(startX + dx, startY, width, height)) {
                x = startX + dx;
                movedX = true;
            }
            if (Math.abs(dy) >= EPSILON && tester.canOccupy(x, startY + dy, width, height)) {
                y = startY + dy;
                movedY = true;
            }
            return new MoveResult(x, y, movedX, movedY);
        }

        if (Math.abs(dy) >= EPSILON && tester.canOccupy(startX, startY + dy, width, height)) {
            y = startY + dy;
            movedY = true;
        }
        if (Math.abs(dx) >= EPSILON && tester.canOccupy(startX + dx, y, width, height)) {
            x = startX + dx;
            movedX = true;
        }
        return new MoveResult(x, y, movedX, movedY);
    }

    private static double score(MoveResult result, double startX, double startY) {
        if (result == null) {
            return -1.0;
        }
        double movedDx = result.x() - startX;
        double movedDy = result.y() - startY;
        double distanceScore = movedDx * movedDx + movedDy * movedDy;
        double axisBonus = (result.movedX() ? 0.001 : 0.0) + (result.movedY() ? 0.001 : 0.0);
        return distanceScore + axisBonus;
    }

    public record MoveResult(double x, double y, boolean movedX, boolean movedY) {
    }
}
