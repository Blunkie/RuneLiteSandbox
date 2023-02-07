package com.gestankbratwurst.pathfinder;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

public class PathLayer {

  private final WorldArea area;
  private final List<WorldPoint> entrances = new ArrayList<>();
  private final List<WorldPoint> exits = new ArrayList<>();

  public PathLayer(WorldArea area) {
    this.area = area;
  }

  public boolean contains(WorldPoint worldPoint) {
    return area.contains(worldPoint);
  }

}