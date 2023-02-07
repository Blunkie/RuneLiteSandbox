package com.gestankbratwurst.simplewalk;

import com.gestankbratwurst.RuneLiteAddons;
import com.gestankbratwurst.utils.EnvironmentUtils;
import com.gestankbratwurst.utils.ShapeUtils;
import lombok.AllArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.awt.Point;
import java.awt.Shape;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimpleWalker {

  @AllArgsConstructor
  private static class SimpleTravel {
    private WorldArea targetArea;
    private WorldArea nextArea;
    private CompletableFuture<Boolean> future;
    private int stepsPerWalk;
  }

  private final Client client;
  private final RuneLiteAddons addons;
  private SimpleTravel currentTravel = null;

  public SimpleWalker(RuneLiteAddons addons) {
    this.client = addons.getClient();
    this.addons = addons;
  }

  public void stop() {
    currentTravel = null;
  }

  public CompletableFuture<Boolean> walkTo(WorldArea worldArea) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    WorldArea clientArea = client.getLocalPlayer().getWorldArea();
    WorldArea nextArea = clientArea.calculateNextTravellingPoint(client, worldArea, true);
    currentTravel = new SimpleTravel(worldArea, nextArea, future, 16);
    return future;
  }

  public CompletableFuture<Boolean> walkTo(WorldPoint worldPoint, int range) {
    return walkTo(new WorldArea(worldPoint, range, range));
  }

  public void nextTick() {
    if (currentTravel == null) {
      return;
    }
    System.out.println("TICK");
    if (currentTravel.targetArea.contains(client.getLocalPlayer().getWorldLocation())) {
      currentTravel.future.complete(true);
      currentTravel = null;
    } else if (currentTravel.nextArea.distanceTo((client.getLocalPlayer().getWorldLocation())) <= 1) {
      for (int i = 0; i < currentTravel.stepsPerWalk; i++) {
        currentTravel.nextArea = currentTravel.nextArea.calculateNextTravellingPoint(client, currentTravel.targetArea, true);
        System.out.println("Distance to next point[ " + i + "] : " + currentTravel.nextArea.distanceTo(client.getLocalPlayer().getWorldArea()));
      }
      //System.out.println("Distance to next point: " + currentTravel.nextArea.distanceTo(client.getLocalPlayer().getWorldArea()));
      clickNextArea();
    }
  }

  private void clickNextArea() {
    int x = currentTravel.nextArea.getX();
    int y = currentTravel.nextArea.getY();
    EnvironmentUtils.findExactTile(client, tile -> tile.getWorldLocation().getX() == x && tile.getWorldLocation().getY() == y)
            .ifPresentOrElse((tile) -> {

              Shape shape;
              TileObject groundObject = tile.getGroundObject();
              if (groundObject == null) {
                for (GameObject gameObject : tile.getGameObjects()) {
                  if (gameObject != null) {
                    groundObject = gameObject;
                    break;
                  }
                }
              }
              if (groundObject == null) {
                throw new IllegalStateException("No object to click on");
              }
              shape = groundObject.getClickbox();
              Point point = ShapeUtils.selectRandomPointIn(shape);
              CompletableFuture.runAsync(() -> {

                addons.getMouseAgent().moveMouseTo(point);
                try {
                  addons.getMouseAgent().leftClick().get(5, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                  throw new RuntimeException(e);
                }
                /*
                try {
                  addons.getMouseAgent().rightClick().get(5, TimeUnit.SECONDS);
                  Thread.sleep(250);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                  throw new RuntimeException(e);
                }

                addons.runSync(() -> {
                  List<MenuUtils.MenuEntryArea> entries = MenuUtils.getMenuEntries(client);
                  for (MenuUtils.MenuEntryArea entryArea : entries) {
                    if (entryArea.getEntry().getType() == MenuAction.WALK) {
                      addons.getMouseAgent().moveMouseTo(ShapeUtils.selectRandomPointIn(entryArea.getArea()));
                      addons.getMouseAgent().leftClick();
                      return;
                    }
                  }
                }).join();
                 */
              });
            }, () -> System.out.println("! Didnt find next tile in scene..."));
  }

}
