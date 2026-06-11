package system;

public final class MovementSlideSystem {
    private static final double EPSILON = 0.0001;
    private static final double MAX_SUBSTEP = 2.0;
    private static final double SIDE_NUDGE = 1.35;
    private static final double[] STEER_ANGLES_DEGREES = {0.0, 28.0, -28.0, 55.0, -55.0, 82.0, -82.0, 110.0, -110.0};

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
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / MAX_SUBSTEP));
        double stepDx = dx / steps;
        double stepDy = dy / steps;
        double x = startX;
        double y = startY;
        boolean movedX = false;
        boolean movedY = false;

        for (int index = 0; index < steps; index++) {
            MoveResult stepResult = moveSingleStep(x, y, width, height, stepDx, stepDy, tester);
            if (Math.abs(stepResult.x() - x) >= EPSILON) {
                movedX = true;
            }
            if (Math.abs(stepResult.y() - y) >= EPSILON) {
                movedY = true;
            }
            x = stepResult.x();
            y = stepResult.y();
        }
        return new MoveResult(x, y, movedX, movedY);
    }

    public static MoveResult steerToward(double startX,
                                         double startY,
                                         double width,
                                         double height,
                                         double desiredDx,
                                         double desiredDy,
                                         OccupancyTester tester) {
        if (tester == null || (Math.abs(desiredDx) < EPSILON && Math.abs(desiredDy) < EPSILON)) {
            return new MoveResult(startX, startY, false, false);
        }
        double bestScore = Double.NEGATIVE_INFINITY;
        MoveResult bestResult = new MoveResult(startX, startY, false, false);
        double desiredLength = Math.sqrt(desiredDx * desiredDx + desiredDy * desiredDy);

        for (double angleDegrees : STEER_ANGLES_DEGREES) {
            double radians = Math.toRadians(angleDegrees);
            double rotatedDx = desiredDx * Math.cos(radians) - desiredDy * Math.sin(radians);
            double rotatedDy = desiredDx * Math.sin(radians) + desiredDy * Math.cos(radians);
            MoveResult candidate = move(startX, startY, width, height, rotatedDx, rotatedDy, tester);
            double score = steeringScore(candidate, startX, startY, desiredDx, desiredDy, desiredLength, Math.abs(angleDegrees));
            if (score > bestScore) {
                bestScore = score;
                bestResult = candidate;
            }
        }

        return bestResult;
    }

    private static MoveResult moveSingleStep(double startX,
                                             double startY,
                                             double width,
                                             double height,
                                             double dx,
                                             double dy,
                                             OccupancyTester tester) {
        if (tester.canOccupy(startX + dx, startY + dy, width, height)) {
            return new MoveResult(startX + dx, startY + dy, Math.abs(dx) >= EPSILON, Math.abs(dy) >= EPSILON);
        }

        MoveResult xFirst = moveAxisOrder(startX, startY, width, height, dx, dy, tester, true);
        MoveResult best = xFirst;
        if (Math.abs(dx) >= EPSILON && Math.abs(dy) >= EPSILON) {
            MoveResult yFirst = moveAxisOrder(startX, startY, width, height, dx, dy, tester, false);
            if (score(yFirst, startX, startY) > score(best, startX, startY)) {
                best = yFirst;
            }
        }

        if (Math.abs(dx) >= EPSILON) {
            MoveResult nudged = tryPerpendicularNudge(startX, startY, width, height, dx, dy, tester, true);
            if (score(nudged, startX, startY) > score(best, startX, startY)) {
                best = nudged;
            }
        }
        if (Math.abs(dy) >= EPSILON) {
            MoveResult nudged = tryPerpendicularNudge(startX, startY, width, height, dx, dy, tester, false);
            if (score(nudged, startX, startY) > score(best, startX, startY)) {
                best = nudged;
            }
        }
        return best;
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

    private static MoveResult tryPerpendicularNudge(double startX,
                                                    double startY,
                                                    double width,
                                                    double height,
                                                    double dx,
                                                    double dy,
                                                    OccupancyTester tester,
                                                    boolean movingHorizontally) {
        MoveResult best = new MoveResult(startX, startY, false, false);
        double primary = movingHorizontally ? dx : dy;
        if (Math.abs(primary) < EPSILON) {
            return best;
        }
        for (double direction : new double[]{1.0, -1.0}) {
            double probeDx = movingHorizontally ? dx : direction * SIDE_NUDGE;
            double probeDy = movingHorizontally ? direction * SIDE_NUDGE : dy;
            if (!tester.canOccupy(startX + probeDx, startY + probeDy, width, height)) {
                continue;
            }
            MoveResult candidate = new MoveResult(
                    startX + probeDx,
                    startY + probeDy,
                    Math.abs(probeDx) >= EPSILON,
                    Math.abs(probeDy) >= EPSILON
            );
            if (score(candidate, startX, startY) > score(best, startX, startY)) {
                best = candidate;
            }
        }
        return best;
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

    private static double steeringScore(MoveResult result,
                                        double startX,
                                        double startY,
                                        double desiredDx,
                                        double desiredDy,
                                        double desiredLength,
                                        double steeringAngleAbs) {
        if (result == null) {
            return Double.NEGATIVE_INFINITY;
        }
        double movedDx = result.x() - startX;
        double movedDy = result.y() - startY;
        double movedDistanceSq = movedDx * movedDx + movedDy * movedDy;
        if (movedDistanceSq < EPSILON) {
            return Double.NEGATIVE_INFINITY;
        }
        double projection = desiredLength < EPSILON ? 0.0 : ((movedDx * desiredDx) + (movedDy * desiredDy)) / desiredLength;
        return projection * 10.0 + movedDistanceSq - steeringAngleAbs * 0.01;
    }

    public record MoveResult(double x, double y, boolean movedX, boolean movedY) {
    }
}
