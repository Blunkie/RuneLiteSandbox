package com.gestankbratwurst.pathfinder;

import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PathTree {

  private final Map<String, PathTreeNode> waypointMap = new ConcurrentHashMap<>();

  public void addPathTreeNode(PathTreeNode node) {
    waypointMap.put(node.getNodeId(), node);
  }

  public CompletableFuture<PathTreeNode> fetchNearestNode(WorldPoint location) {
    final List<PathTreeNode> nodeList = List.copyOf(waypointMap.values());
    return CompletableFuture.supplyAsync(() -> {
      double shortestDist = Double.MAX_VALUE;
      PathTreeNode nearestNode = null;
      for (PathTreeNode node : nodeList) {
        double nextDist = node.getWaypoint().distanceTo(location);
        if (nextDist < shortestDist) {
          shortestDist = nextDist;
          nearestNode = node;
        }
      }
      return nearestNode;
    });
  }

}
