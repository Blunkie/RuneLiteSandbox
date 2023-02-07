package com.gestankbratwurst.pathfinder;

import net.runelite.api.coords.WorldPoint;

public interface Waypoint {

  String getId();

  void moveTowards();

  double distanceTo(WorldPoint worldPoint);

}
