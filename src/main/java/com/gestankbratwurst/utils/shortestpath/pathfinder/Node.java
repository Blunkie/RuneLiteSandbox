package com.gestankbratwurst.utils.shortestpath.pathfinder;

import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Node implements Comparable<Node> {
    public final WorldPoint position;
    public final Node previous;
    public final long heuristic;
    private final int wait;
    private final int distance;

    public Node(WorldPoint position, Node previous, WorldPoint target, int wait) {
        this.position = position;
        this.previous = previous;
        this.wait = (previous != null ? previous.wait : 0) + wait;
        distance = previous != null ? position.distanceTo(previous.position) : 0;
        this.heuristic = getHeuristic(target);
    }

    public Node(WorldPoint position, Node previous, WorldPoint target) {
        this(position, previous, target, 0);
    }

    public List<WorldPoint> getPath() {
        List<WorldPoint> path = new LinkedList<>();
        Node node = this;

        while (node != null) {
            path.add(0, node.position);
            node = node.previous;
        }

        return new ArrayList<>(path);
    }

    public boolean isTransport() {
        return distance > 1;
    }

    /**
     * The pathfinding heuristic is an optimistic distance (number of tiles) consisting of:
     * - 2D Chebyshev distance from the current position to the path destination
     * - Additional transport travel time (unavoidable cutscene, stall, ...)
     *   considered as a distance (1 tick = 1 tile) instead of ticks
     * @param target  the destination of the path
     * @return  distance to target including additional travel time
     */
    private long getHeuristic(WorldPoint target) {
        long h = (long) position.distanceTo(target) + wait;
        if (previous != null && isTransport() && previous.heuristic < h) {
            return previous.heuristic;
        }
        return h;
    }

    @Override
    public int compareTo(Node other) {
        return Long.compare(heuristic, other.heuristic);
    }
}
