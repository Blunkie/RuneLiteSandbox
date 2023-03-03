package com.gestankbratwurst.autoharvester;

import com.gestankbratwurst.RuneLiteAddons;
import com.gestankbratwurst.mousemovement.MouseAgent;
import com.gestankbratwurst.utils.EnvironmentUtils;
import com.gestankbratwurst.utils.InventoryUtils;
import com.gestankbratwurst.utils.ShapeUtils;
import net.runelite.api.AnimationID;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.widgets.WidgetInfo;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PlankGatherer {

  private final RuneLiteAddons plugin;
  private final Client client;
  private final MouseAgent mouseAgent;
  private final WorldPoint startPoint;
  private final WorldPoint bankPoint;
  private boolean started = false;

  public PlankGatherer(RuneLiteAddons plugin) {
    this.plugin = plugin;
    this.client = plugin.getClient();
    this.mouseAgent = plugin.getMouseAgent();
    this.startPoint = new WorldPoint(2548, 3575, 0);
    this.bankPoint = new WorldPoint(2535, 3575, 0);
  }

  public void start() {
    if (started) {
      return;
    }
    System.out.println("> Started gathering planks");
    started = true;
    CompletableFuture.runAsync(this::loop);
  }

  public void stop() {
    if (!started) {
      return;
    }
    System.out.println("> Stopped gathering planks");
    started = false;
  }

  private void loop() {
    if (!started) {
      return;
    }
    runToStart().join();
    if (!started) {
      return;
    }

    while (!plugin.supplySync(() -> InventoryUtils.isFull(client)).join()) {
      gatherPlanks();
      if (!started) {
        return;
      }
    }

    runToBank().join();
    if (!started) {
      return;
    }
    emptyIntoBank().join();

    loop();
  }

  public void nextTick() {
    if (!started) {
      return;
    }
  }

  private CompletableFuture<Void> runToStart() {
    System.out.println("> Running to start");
    return plugin.getPathTravel().travelTo(startPoint);
  }

  private CompletableFuture<Void> runToBank() {
    System.out.println("> Running to bank");
    return plugin.getPathTravel().travelTo(bankPoint);
  }

  private void gatherPlanks() {
    System.out.println("> Gathering planks");
    while (client.getLocalPlayer().getAnimation() != AnimationID.IDLE) {
      try {
        Thread.sleep(333);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    List<Tile> plankTiles = new ArrayList<>();

    EnvironmentUtils.getAllItems(client, 32).forEach((tile, itemlist) -> {
      if (itemlist.stream().anyMatch(item -> item.getId() == ItemID.PLANK)) {
        plankTiles.add(tile);
      }
    });

    if (plankTiles.isEmpty()) {
      System.out.println("> Waiting for plank respawns.");
      this.runToStart().join();
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return;
    }

    plankTiles.sort(Comparator.comparingInt(tile -> tile.getLocalLocation().distanceTo(client.getLocalPlayer().getLocalLocation())));

    for (Tile tile : plankTiles) {
      Point point = ShapeUtils.selectRandomPointIn(tile.getItemLayer().getClickbox());
      try {
        mouseAgent.moveMouseTo(point).get(5, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        throw new RuntimeException(e);
      }
      mouseAgent.leftClick();
      try {
        plugin.waitForEvent(ItemContainerChanged.class, event -> event.getContainerId() == WidgetInfo.INVENTORY.getId(), 5000).get(5, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        System.out.println("> Timed out on plank pickup!");
      }
      try {
        Thread.sleep(ThreadLocalRandom.current().nextInt(500, 750));
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }


  private CompletableFuture<Void> emptyIntoBank() {
    System.out.println("> Opening nearby bank");
    return EnvironmentUtils.openNearbyBankBooth(client, plugin, mouseAgent, 10).thenRun(() -> {
      try {
        Thread.sleep(750);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      InventoryUtils.emptyIntoBank(client, mouseAgent, ItemID.PLANK);
      try {
        Thread.sleep(ThreadLocalRandom.current().nextInt(333, 500));
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
  }

}
