package com.gestankbratwurst.pathfinder;

import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PathTreeNode {

  @Getter
  private final String nodeId;
  @Getter
  private final Waypoint waypoint;
  private final Set<String> connectedNodes = new HashSet<>();

  public PathTreeNode(String nodeId, Waypoint waypoint) {
    this.nodeId = nodeId;
    this.waypoint = waypoint;
  }

  public List<String> getConnectedNodes() {
    return List.copyOf(connectedNodes);
  }

  public void addConnectedNode(String nodeId) {
    connectedNodes.add(nodeId);
  }

  public void removeConnectedNodes(String nodeId) {
    connectedNodes.remove(nodeId);
  }

}
