package com.gestankbratwurst.pathfinder;

import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PathfindingModule {

  private final PathTree pathTree = new PathTree();

  public CompletableFuture<List<Waypoint>> generatePathToLocation(WorldPoint current, WorldPoint target) {
    return pathTree.fetchNearestNode(current).thenApply(nearest -> {
      PathTreeNode next = nearest;
      List<Waypoint> waypoints = new ArrayList<>();



      return waypoints;
    });
  }

}
