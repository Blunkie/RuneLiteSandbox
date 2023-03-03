package com.gestankbratwurst.autoharvester;

import com.gestankbratwurst.RuneLiteAddons;
import com.gestankbratwurst.utils.ArrayUtils;
import com.gestankbratwurst.utils.EnvironmentUtils;
import com.gestankbratwurst.utils.InventoryUtils;
import com.gestankbratwurst.utils.ShapeUtils;
import com.google.common.base.Preconditions;
import net.runelite.api.AnimationID;
import net.runelite.api.GameObject;
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

public class GemMiner {

  private final RuneLiteAddons plugin;
  private final WorldPoint bankEntry;
  private final WorldPoint mineEntry;
  private final WorldPoint bankBooth;
  private boolean active = false;
  private Runnable nextAction = null;

  public GemMiner(RuneLiteAddons plugin) {
    this.plugin = plugin;
    this.bankEntry = new WorldPoint(2852, 2956, 0);
    this.mineEntry = new WorldPoint(2826, 2997, 0);
    this.bankBooth = new WorldPoint(2852, 2951, 0);
  }

  public void start() {
    if (!active) {
      active = true;
      nextAction = this::mineGems;
      CompletableFuture.runAsync(this::loop);
      System.out.println("> Gem miner started");
    }
  }

  public void stop() {
    if (active) {
      active = false;
      nextAction = null;
      System.out.println("> Gem miner stopped");
    }
  }

  private void loop() {
    while (nextAction != null && active) {
      nextAction.run();
    }
  }

  private void mineGems() {
    System.out.println("> Mining gems...");

    while (!plugin.supplySync(() -> InventoryUtils.isFull(plugin.getClient())).join()) {
      if (!active) {
        return;
      }
      Optional<GameObject> nextBlock = EnvironmentUtils.findObjects(plugin.getClient(), 32, object -> {
        if (!ArrayUtils.contains(ObjectIdGroups.gemRocks(), object.getId())) {
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
        Point point = ShapeUtils.selectRandomPointIn(clickBox);
        plugin.getMouseAgent().moveMouseTo(point);
        plugin.getMouseAgent().leftClick();
        while (plugin.waitForEvent(ItemContainerChanged.class, event -> true, 6000).join() == null) {
          point = ShapeUtils.selectRandomPointIn(nextBlock.get().getClickbox());
          plugin.getMouseAgent().moveMouseTo(point);
          plugin.getMouseAgent().leftClick();
        }
      } else {
        nextAction = this::runToMine;
        System.out.println("> Didnt find any gems... Running to mine");
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        return;
      }
    }

    nextAction = this::runToBank;
  }

  private void runToBank() {
    System.out.println("> Running to bank...");
    plugin.getPathTravel().travelTo(bankEntry).join();
    plugin.waitForEvent(AnimationChanged.class, event -> {
      return event.getActor().equals(plugin.getClient().getLocalPlayer()) && event.getActor().getAnimation() == AnimationID.IDLE;
    }, 3750).join();
    nextAction = this::clickOnBankExact;
  }

  private void clickOnBankExact() {
    System.out.println("> Clicking bank...");
    Optional<GameObject> box = EnvironmentUtils.findObjects(plugin.getClient(), 32, obj -> obj.getId() == 10529).stream().findAny();

    Preconditions.checkState(box.isPresent(), "Bank box is not nearby");

    Shape clickBox = box.get().getClickbox();
    Preconditions.checkState(clickBox != null, "ClickBox cant be null.");
    Point clickPoint = ShapeUtils.selectMiddle(clickBox, 0.5);
    plugin.getMouseAgent().moveMouseTo(clickPoint);

    try {
      plugin.getMouseAgent().leftClick().get(5, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }

    if (plugin.waitForEvent(WidgetLoaded.class, event -> event.getGroupId() == WidgetInfo.DEPOSIT_BOX_INVENTORY_ITEMS_CONTAINER.getGroupId(), 8000).join() == null) {
      nextAction = this::clickOnBankExact;
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
    System.out.println("> Deposit into bank...");
    InventoryUtils.dumpIntoBankBox(plugin.getClient(), plugin.getMouseAgent()).join();
    nextAction = this::runToMine;
  }

  private void runToMine() {
    System.out.println("> Running to gems...");
    plugin.getPathTravel().travelTo(mineEntry).join();
    nextAction = this::mineGems;
  }

}
