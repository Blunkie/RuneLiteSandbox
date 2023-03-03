package com.gestankbratwurst.autoharvester;

import com.gestankbratwurst.RuneLiteAddons;
import com.gestankbratwurst.utils.ArrayUtils;
import com.gestankbratwurst.utils.EnvironmentUtils;
import com.gestankbratwurst.utils.InventoryUtils;
import com.gestankbratwurst.utils.ShapeUtils;
import com.google.common.base.Preconditions;
import net.runelite.api.AnimationID;
import net.runelite.api.GameObject;
import net.runelite.api.ItemID;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetInfo;

import java.awt.Point;
import java.awt.Shape;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DwarvenAutoCoal {

  private static final int[] ITEMS = {
          ItemID.COAL,
          ItemID.UNCUT_DIAMOND,
          ItemID.UNCUT_EMERALD,
          ItemID.UNCUT_RUBY,
          ItemID.UNCUT_SAPPHIRE
  };

  private final RuneLiteAddons plugin;
  private boolean active = false;
  private Runnable nextAction = null;
  private WorldPoint startPoint = null;

  public DwarvenAutoCoal(RuneLiteAddons plugin) {
    this.plugin = plugin;
  }

  public void start() {
    if (!active) {
      active = true;
      startPoint = plugin.getClient().getLocalPlayer().getWorldLocation();
      nextAction = this::mineCoalOres;
      CompletableFuture.runAsync(this::loop);
      System.out.println("> Dwarven coal miner started");
    }
  }

  public void stop() {
    if (active) {
      active = false;
      nextAction = null;
      System.out.println("> Dwarven coal miner stopped");
    }
  }

  private void loop() {
    while (nextAction != null && active) {
      nextAction.run();
    }
  }

  private void mineCoalOres() {
    System.out.println("> Mining ores...");

    while (!plugin.supplySync(() -> InventoryUtils.isFull(plugin.getClient())).join()) {
      if (!active) {
        return;
      }
      Optional<GameObject> nextBlock = EnvironmentUtils.findObjects(plugin.getClient(), 32, object -> {
        if (!ArrayUtils.contains(ObjectIdGroups.coalRocks(), object.getId())) {
          return false;
        }
        return object.getWorldLocation().getX() <= 3025;
      }).stream().min(Comparator.comparingInt(obj -> obj.getWorldLocation().distanceTo(plugin.getClient().getLocalPlayer().getWorldLocation())));

      if (nextBlock.isPresent()) {
        Shape clickBox = nextBlock.get().getClickbox();
        if (clickBox == null) {
          plugin.getPathTravel().travelTo(nextBlock.get().getWorldLocation()).join();
          clickBox = nextBlock.get().getClickbox();
          if (clickBox == null) {
            continue;
          }
        }
        Point point = ShapeUtils.selectMiddle(clickBox, 0.5);
        plugin.getMouseAgent().moveMouseTo(point);
        plugin.getMouseAgent().leftClick();
        while (plugin.waitForEvent(ItemContainerChanged.class, event -> true, 6000).join() == null) {
          point = ShapeUtils.selectMiddle(nextBlock.get().getClickbox(), 0.5);
          plugin.getMouseAgent().moveMouseTo(point);
          plugin.getMouseAgent().leftClick();
        }
      } else {
        nextAction = this::walkToBank;
        return;
      }
    }

    nextAction = this::walkToBank;
  }


  private void walkToBank() {
    WorldPoint exactBankPoint = new WorldPoint(3012, 9718, 0);
    LocalPoint localPoint = LocalPoint.fromWorld(plugin.getClient(), exactBankPoint);

    if (localPoint != null) {
      Tile tile = plugin.getClient().getScene().getTiles()[0][localPoint.getSceneX()][localPoint.getSceneY()];
      GameObject[] gameObjects = tile.getGameObjects();
      if (gameObjects != null && gameObjects.length > 0) {
        GameObject base = gameObjects[0];
        Shape clickBox = base.getClickbox();
        if (clickBox != null) {
          System.out.println("> Found bank on screen...");
          nextAction = this::clickBankExact;
          return;
        }
      }
    }

    WorldPoint bankPoint = new WorldPoint(3015, 9718, 0);
    plugin.getPathTravel().travelTo(bankPoint).join();
    plugin.waitForEvent(AnimationChanged.class, event -> {
      return event.getActor().equals(plugin.getClient().getLocalPlayer()) && event.getActor().getAnimation() == AnimationID.IDLE;
    }, 2500).join();
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
    if (plugin.waitForEvent(WidgetLoaded.class, event -> event.getGroupId() == WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getGroupId(), 12000).join() == null) {
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
    InventoryUtils.emptyIntoBank(plugin.getClient(), plugin.getMouseAgent(), ITEMS).join();
    try {
      Thread.sleep(666);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    nextAction = this::mineCoalOres;
  }

}
