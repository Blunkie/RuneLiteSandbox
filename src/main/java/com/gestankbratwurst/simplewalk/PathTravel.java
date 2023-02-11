package com.gestankbratwurst.simplewalk;

import com.gestankbratwurst.RuneLiteAddons;
import com.gestankbratwurst.utils.ShapeUtils;
import com.gestankbratwurst.utils.shortestpath.Transport;
import com.gestankbratwurst.utils.shortestpath.pathfinder.CollisionMap;
import com.gestankbratwurst.utils.shortestpath.pathfinder.Pathfinder;
import com.gestankbratwurst.utils.shortestpath.pathfinder.PathfinderConfig;
import lombok.Getter;
import lombok.Value;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PathTravel {

  @Value
  public static class TravelCallback {
    WorldPoint target;
    CompletableFuture<Void> callBacK;
  }

  private static final int[] SKIPS_PER_ITERATION = {5, 7};

  private final RuneLiteAddons addons;
  private final PathfinderConfig pathfinderConfig;
  @Getter
  private boolean traveling = false;
  private CompletableFuture<Void> currentWalkCallback = null;
  @Getter
  private WorldPoint currentTarget = null;
  private boolean stuck = false;

  public PathTravel(RuneLiteAddons addons) {
    this.addons = addons;
    CollisionMap collisionMap = CollisionMap.fromResources();
    Map<WorldPoint, List<Transport>> transports = Transport.fromResources();
    this.pathfinderConfig = new PathfinderConfig(collisionMap, transports, addons);
  }

  public void stop() {
    traveling = false;
    if (currentWalkCallback != null && !currentWalkCallback.isDone()) {
      currentWalkCallback.complete(null);
    }
    currentTarget = null;
  }

  public CompletableFuture<Void> travelTo(WorldPoint target) {
    if (traveling) {
      return CompletableFuture.completedFuture(null);
    }
    traveling = true;
    WorldPoint start = addons.getClient().getLocalPlayer().getWorldLocation();
    Pathfinder pathfinder = new Pathfinder(pathfinderConfig, start, target);
    return pathfinder.calculatePath().thenRun(() -> this.walkPath(pathfinder)).whenComplete((v, t) -> {
      if (t != null) {
        t.printStackTrace();
      }
    });
  }

  private void walkPath(Pathfinder pathfinder) {
    if (!traveling) {
      return;
    }
    Deque<WorldPoint> path = new ArrayDeque<>(pathfinder.getPath());
    System.out.println("> Path length: " + path.size());
    WorldPoint worldPoint = null;
    while (!path.isEmpty()) {
      int skips = stuck ? 1 : ThreadLocalRandom.current().nextInt(SKIPS_PER_ITERATION[0], SKIPS_PER_ITERATION[1]);
      for (int i = 0; i < skips; i++) {
        stuck = false;
        worldPoint = path.poll();

        // TODO: Remove. Debug.
        currentTarget = worldPoint;
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }

        if (path.isEmpty()) {
          break;
        }
      }
      walkUntilConvergence(worldPoint);
    }
    traveling = false;
  }

  private void walkUntilConvergence(WorldPoint worldPoint) {
    walkUntilConvergence(worldPoint, 3);
  }

  private void walkUntilConvergence(WorldPoint worldPoint, int retryCounter) {
    if (!traveling) {
      return;
    }
    if(retryCounter <= 0) {
      stuck = true;
      return;
    }
    try {
      walkToPoint(worldPoint).get(4, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    } catch (TimeoutException e) {
      System.out.println("> Didnt reach target in time. Trying again.");
      walkUntilConvergence(worldPoint, retryCounter - 1);
    }
  }

  public void nextTick() {
    if (currentWalkCallback == null || currentTarget == null || !traveling || currentWalkCallback.isDone()) {
      return;
    }
    if (addons.getClient().getLocalPlayer().getWorldLocation().distanceTo2D(currentTarget) <= 1) {
      currentWalkCallback.complete(null);
    }
  }

  private CompletableFuture<Void> walkToPoint(WorldPoint point) {
    currentWalkCallback = new CompletableFuture<>();
    currentTarget = point;
    clickOnTileInLocalScene();
    return currentWalkCallback;
  }

  private void clickOnTileInLocalScene() {
    if (!traveling) {
      return;
    }
    LocalPoint localPoint = LocalPoint.fromWorld(addons.getClient(), currentTarget);

    if (localPoint == null) {
      currentWalkCallback.complete(null);
      moveCam().thenRun(this::clickOnTileInLocalScene);
      return;
    }

    Polygon poly = Perspective.getCanvasTilePoly(addons.getClient(), localPoint);

    Point point = ShapeUtils.selectRandomPointIn(poly);
    addons.getMouseAgent().moveMouseTo(point);
    try {
      addons.getMouseAgent().leftClick().get(5, TimeUnit.SECONDS);
    } catch (InterruptedException | TimeoutException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private CompletableFuture<Void> moveCam() {
    return CompletableFuture.runAsync(() -> addons.getMouseAgent().randomCamMove(3.0));
  }

}
