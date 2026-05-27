package entity;

import javafx.geometry.Point2D;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class PathfindingManager {
    public interface WalkValidator {
        boolean canOccupy(double x, double y, double width, double height);
    }

    public record PathResult(List<Point2D> waypoints, boolean success, long computedAtNs, double targetX, double targetY) {
    }

    private record Node(int x, int y) {
    }

    private record PathNode(Node node, double score) {
    }

    private record PathRequest(Enemy requester,
                               double startX,
                               double startY,
                               double targetX,
                               double targetY,
                               double width,
                               double height,
                               WalkValidator validator,
                               long requestedAtNs) {
    }

    private final int tileWidth;
    private final int tileHeight;
    private final int maxPathRequestsPerFrame;
    private final int goalSearchRadius;
    private final int maxExpansions;
    private final Queue<PathRequest> queue;
    private final Set<Enemy> queuedEnemies;
    private final Map<Enemy, PathResult> readyResults;
    private final EnemyAiDebug debug;

    public PathfindingManager(int tileWidth,
                              int tileHeight,
                              int maxPathRequestsPerFrame,
                              int goalSearchRadius,
                              int maxExpansions,
                              EnemyAiDebug debug) {
        this.tileWidth = Math.max(1, tileWidth);
        this.tileHeight = Math.max(1, tileHeight);
        this.maxPathRequestsPerFrame = Math.max(1, maxPathRequestsPerFrame);
        this.goalSearchRadius = Math.max(1, goalSearchRadius);
        this.maxExpansions = Math.max(64, maxExpansions);
        this.queue = new ArrayDeque<>();
        this.queuedEnemies = new HashSet<>();
        this.readyResults = new HashMap<>();
        this.debug = debug;
    }

    public void requestPath(Enemy requester,
                            double startX,
                            double startY,
                            double targetX,
                            double targetY,
                            double width,
                            double height,
                            WalkValidator validator,
                            long nowNs) {
        if (requester == null || validator == null) {
            return;
        }
        if (queuedEnemies.contains(requester)) {
            return;
        }
        queue.offer(new PathRequest(requester, startX, startY, targetX, targetY, width, height, validator, nowNs));
        queuedEnemies.add(requester);
        if (debug != null) {
            debug.recordPathRequest();
        }
    }

    public PathResult consumeResult(Enemy requester) {
        if (requester == null) {
            return null;
        }
        return readyResults.remove(requester);
    }

    public void update(long nowNs) {
        for (int processed = 0; processed < maxPathRequestsPerFrame && !queue.isEmpty(); processed++) {
            PathRequest request = queue.poll();
            if (request == null) {
                continue;
            }
            queuedEnemies.remove(request.requester());
            List<Point2D> path = computePath(request);
            boolean success = !path.isEmpty();
            readyResults.put(request.requester(), new PathResult(path, success, nowNs, request.targetX(), request.targetY()));
            if (!success && debug != null) {
                debug.recordPathFail();
            }
        }
    }

    private List<Point2D> computePath(PathRequest request) {
        Node start = toNode(request.startX(), request.startY());
        Set<Node> goals = buildGoalNodes(request.targetX(), request.targetY(), request.width(), request.height(), request.validator());
        if (goals.isEmpty()) {
            return List.of();
        }

        PriorityQueue<PathNode> open = new PriorityQueue<>(Comparator.comparingDouble(PathNode::score));
        Map<Node, Node> cameFrom = new HashMap<>();
        Map<Node, Double> gScore = new HashMap<>();
        Set<Node> closed = new HashSet<>();
        Node bestGoal = null;
        int expansions = 0;

        open.add(new PathNode(start, heuristic(start, goals)));
        gScore.put(start, 0.0);

        while (!open.isEmpty() && expansions < maxExpansions) {
            PathNode currentRecord = open.poll();
            Node current = currentRecord.node();
            if (!closed.add(current)) {
                continue;
            }
            expansions++;
            if (goals.contains(current)) {
                bestGoal = current;
                break;
            }
            for (Node neighbor : neighbors(current)) {
                if (closed.contains(neighbor)
                        || !isWalkableNode(neighbor, request.width(), request.height(), request.validator())
                        || isDiagonalCornerCut(current, neighbor, request.width(), request.height(), request.validator())) {
                    continue;
                }
                double tentative = gScore.getOrDefault(current, Double.POSITIVE_INFINITY)
                        + distance(current.x(), current.y(), neighbor.x(), neighbor.y());
                if (tentative >= gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    continue;
                }
                cameFrom.put(neighbor, current);
                gScore.put(neighbor, tentative);
                open.add(new PathNode(neighbor, tentative + heuristic(neighbor, goals)));
            }
        }

        if (bestGoal == null) {
            return List.of();
        }

        Deque<Point2D> reversed = new ArrayDeque<>();
        Node cursor = bestGoal;
        while (cursor != null && !Objects.equals(cursor, start)) {
            reversed.addFirst(new Point2D(cursor.x() * tileWidth + tileWidth * 0.5, cursor.y() * tileHeight + tileHeight * 0.5));
            cursor = cameFrom.get(cursor);
        }
        return new ArrayList<>(reversed);
    }

    private Set<Node> buildGoalNodes(double targetX, double targetY, double width, double height, WalkValidator validator) {
        Set<Node> goals = new HashSet<>();
        Node direct = toNode(targetX, targetY);
        if (isWalkableNode(direct, width, height, validator)) {
            goals.add(direct);
        }
        for (int dy = -goalSearchRadius; dy <= goalSearchRadius; dy++) {
            for (int dx = -goalSearchRadius; dx <= goalSearchRadius; dx++) {
                if (Math.abs(dx) + Math.abs(dy) > goalSearchRadius + 1) {
                    continue;
                }
                Node candidate = new Node(direct.x() + dx, direct.y() + dy);
                if (isWalkableNode(candidate, width, height, validator)) {
                    goals.add(candidate);
                }
            }
        }
        return goals;
    }

    private List<Node> neighbors(Node node) {
        return List.of(
                new Node(node.x() + 1, node.y()),
                new Node(node.x() - 1, node.y()),
                new Node(node.x(), node.y() + 1),
                new Node(node.x(), node.y() - 1),
                new Node(node.x() + 1, node.y() + 1),
                new Node(node.x() - 1, node.y() + 1),
                new Node(node.x() + 1, node.y() - 1),
                new Node(node.x() - 1, node.y() - 1)
        );
    }

    private boolean isWalkableNode(Node node, double width, double height, WalkValidator validator) {
        double centerX = node.x() * tileWidth + tileWidth * 0.5;
        double centerY = node.y() * tileHeight + tileHeight * 0.5;
        double candidateX = centerX - width * 0.5;
        double candidateY = centerY - height * 0.5;
        return validator.canOccupy(candidateX, candidateY, width, height);
    }

    private boolean isDiagonalCornerCut(Node current, Node neighbor, double width, double height, WalkValidator validator) {
        int dx = neighbor.x() - current.x();
        int dy = neighbor.y() - current.y();
        if (Math.abs(dx) != 1 || Math.abs(dy) != 1) {
            return false;
        }
        return !isWalkableNode(new Node(current.x() + dx, current.y()), width, height, validator)
                || !isWalkableNode(new Node(current.x(), current.y() + dy), width, height, validator);
    }

    private Node toNode(double worldX, double worldY) {
        return new Node((int) Math.floor(worldX / tileWidth), (int) Math.floor(worldY / tileHeight));
    }

    private double heuristic(Node current, Set<Node> goals) {
        double best = Double.POSITIVE_INFINITY;
        for (Node goal : goals) {
            best = Math.min(best, distance(current.x(), current.y(), goal.x(), goal.y()));
        }
        return best;
    }

    private double distance(double ax, double ay, double bx, double by) {
        double dx = ax - bx;
        double dy = ay - by;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
