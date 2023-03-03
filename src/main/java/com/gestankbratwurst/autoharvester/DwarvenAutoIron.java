package com.gestankbratwurst.autoharvester;

import com.gestankbratwurst.RuneLiteAddons;
import com.gestankbratwurst.utils.InventoryUtils;
import com.gestankbratwurst.utils.ShapeUtils;
import com.google.common.base.Preconditions;
import net.runelite.api.AnimationID;
import net.runelite.api.GameObject;
import net.runelite.api.ItemID;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetInfo;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DwarvenAutoIron {

  private final RuneLiteAddons plugin;
  private boolean active = false;
  private Runnable nextAction = null;
  private WorldPoint startPoint = null;

  public DwarvenAutoIron(RuneLiteAddons plugin) {
    this.plugin = plugin;
  }

  public void start() {
    if (!active) {
      active = true;
      startPoint = plugin.getClient().getLocalPlayer().getWorldLocation();
      nextAction = this::mineIronOres;
      CompletableFuture.runAsync(this::loop);
      System.out.println("> Dwarven iron miner started");
    }
  }

  public void stop() {
    if (active) {
      active = false;
      nextAction = null;
      System.out.println("> Dwarven iron miner stopped");
    }
  }

  private void loop() {
    while (nextAction != null && active) {
      nextAction.run();
    }
  }

  private void mineIronOres() {
    System.out.println("> Mining ores...");
    WorldPoint[] ores = {
            new WorldPoint(3028, 9720, 0),
            new WorldPoint(3029, 9721, 0),
            new WorldPoint(3030, 9720, 0)
    };
    int index = 0;

    while (!plugin.supplySync(() -> InventoryUtils.isFull(plugin.getClient())).join()) {
      if(!active) {
        return;
      }
      if(plugin.getClient().getLocalPlayer().getWorldLocation().distanceTo(startPoint) > 0) {
        System.out.println("> Distance to start is " + plugin.getClient().getLocalPlayer().getWorldLocation().distanceTo(startPoint));
        System.out.println("> Run further");
        nextAction = this::clickSpotExact;
        return;
      }
      WorldPoint nextOre = ores[index];
      LocalPoint localPoint = LocalPoint.fromWorld(plugin.getClient(), nextOre);
      Preconditions.checkState(localPoint != null, "Local point not found");
      Tile tile = plugin.getClient().getScene().getTiles()[0][localPoint.getSceneX()][localPoint.getSceneY()];
      GameObject[] gameObjects = tile.getGameObjects();
      Preconditions.checkState(gameObjects != null, "Game object was null");
      GameObject base = gameObjects[0];
      Preconditions.checkState(base != null, "Base cant be null.");
      Shape clickBox = base.getClickbox();
      Preconditions.checkState(clickBox != null, "ClickBox cant be null.");
      Point clickPoint = ShapeUtils.selectMiddle(clickBox, 0.5);
      plugin.getMouseAgent().moveMouseTo(clickPoint);
      plugin.getMouseAgent().leftClick();
      if(plugin.waitForEvent(ItemContainerChanged.class, event -> true, 4500).join() == null) {
        System.out.println("> Didnt mine for 4.5s");
        System.out.println("> Try to run to spot and retry");
        nextAction = this::clickSpotExact;
        return;
      }
      if (++index == ores.length) {
        index = 0;
      }
    }

    nextAction = this::clickBankExact;
  }

  private void clickBankExact() {
    System.out.println("> Clicking bank...");
    WorldPoint bankPoint = new WorldPoint(3012, 9718, 0);
    LocalPoint localPoint = LocalPoint.fromWorld(plugin.getClient(), bankPoint);

    Preconditions.checkState(localPoint != null, "Bank not on the screen");

    Tile tile = plugin.getClient().getScene().getTiles()[0][localPoint.getSceneX()][localPoint.getSceneY()];
    GameObject[] gameObjects = tile.getGameObjects();
    Preconditions.checkState(gameObjects != null);
    GameObject base = gameObjects[0];
    Preconditions.checkState(base != null, "Base cant be null.");
    Shape clickBox = base.getClickbox();
    Preconditions.checkState(clickBox != null, "ClickBox cant be null.");
    Point clickPoint = ShapeUtils.selectMiddle(clickBox, 0.5);
    plugin.getMouseAgent().moveMouseTo(clickPoint);
    try {
      plugin.getMouseAgent().leftClick().get(5, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
    if(plugin.waitForEvent(WidgetLoaded.class, event -> event.getGroupId() == WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getGroupId(), 12000).join() == null) {
      nextAction = this::clickBankExact;
      return;
    }
    try {
      Thread.sleep(666);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    nextAction = this::deposit;
  }

  private void deposit() {
    System.out.println("> Empty into bank...");
    InventoryUtils.emptyIntoBank(plugin.getClient(), plugin.getMouseAgent(), ItemID.IRON_ORE).join();
    nextAction = this::clickSpotExact;
  }

  private void clickSpotExact() {
    System.out.println("> Clicking exact spot...");
    LocalPoint localPoint = LocalPoint.fromWorld(plugin.getClient(), startPoint);
    Preconditions.checkState(localPoint != null, "ExactSpot LocalPoint was null.");
    Polygon tilePolygon = Perspective.getCanvasTilePoly(plugin.getClient(), localPoint);
    Point middle = ShapeUtils.selectMiddle(tilePolygon, 0.5);
    plugin.getMouseAgent().moveMouseTo(middle);
    try {
      plugin.getMouseAgent().leftClick().get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    plugin.waitForEvent(AnimationChanged.class, event -> {
      if (!event.getActor().equals(plugin.getClient().getLocalPlayer())) {
        return false;
      }
      return event.getActor().getAnimation() == AnimationID.IDLE;
    }, 12000).join();

    if(plugin.getClient().getLocalPlayer().getWorldLocation().distanceTo(startPoint) > 0) {
      System.out.println("> Distance to start is " + plugin.getClient().getLocalPlayer().getWorldLocation().distanceTo(startPoint));
      System.out.println("> Run further");
      nextAction = this::clickSpotExact;
    } else {
      nextAction = this::mineIronOres;
    }
  }

}
