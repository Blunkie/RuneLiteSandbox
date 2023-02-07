package com.gestankbratwurst.utils;

import com.gestankbratwurst.RuneLiteAddons;
import com.gestankbratwurst.mousemovement.MouseAgent;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class InventoryUtils {

  private static final int INVENTORY_SLOTS = 28;
  private static final int EMPTY_ITEM_ID = 6512;
  private static final AtomicBoolean active = new AtomicBoolean(false);

  public static boolean isFull(Client client) {
    Widget inventory = client.getWidget(WidgetInfo.INVENTORY);

    if (inventory == null || inventory.isHidden()) {
      return false;
    }

    for (int i = 0; i < INVENTORY_SLOTS; i++) {
      Widget child = inventory.getChild(i);
      if (child.getItemId() == EMPTY_ITEM_ID) {
        return false;
      }
    }

    return true;
  }

  public static CompletableFuture<Void> emptyIntoBank(Client client, MouseAgent agent, int... itemIds) {
    Set<Integer> ids = new HashSet<>(itemIds.length);
    Map<Integer, Point> firstCaught = new HashMap<>();
    Arrays.stream(itemIds).forEach(ids::add);
    Widget inventory = client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER);
    if (inventory == null) {
      return CompletableFuture.completedFuture(null);
    }
    for (int i = 0; i < INVENTORY_SLOTS; i++) {
      Widget slot = inventory.getChild(i);
      if (ids.contains(slot.getItemId()) && !firstCaught.containsKey(slot.getItemId())) {
        firstCaught.put(slot.getItemId(), ShapeUtils.selectRandomPointIn(slot.getBounds()));
      }
    }
    return CompletableFuture.runAsync(() -> {
      for (Point point : firstCaught.values()) {
        agent.moveMouseTo(point);
        agent.leftClick();
      }
      try {
        agent.pressKey(KeyEvent.VK_ESCAPE).get(10, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        e.printStackTrace();
      }
    });
  }

  public static CompletableFuture<Void> emptyIntoBank(Client client, MouseAgent mouseAgent) {
    return emptyIntoBank(client, mouseAgent, widget -> false);
  }

  public static boolean isInventoryOpen(Client client, RuneLiteAddons addons) {
    Widget inventory = client.getWidget(WidgetInfo.INVENTORY);
    return inventory != null && !addons.supplySync(inventory::isHidden).join();
  }

  public static CompletableFuture<Void> eatFood(Client client, MouseAgent mouseAgent, RuneLiteAddons addons) {
    return CompletableFuture.runAsync(() -> {
      try {
        openInventory(client, mouseAgent, addons).get(5, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        throw new RuntimeException(e);
      }
      Widget inventory = client.getWidget(WidgetInfo.INVENTORY);

      if (inventory == null) {
        System.out.println("> Inv is not here");
        return;
      }

      for (int i = 0; i < INVENTORY_SLOTS; i++) {
        Widget item = inventory.getChild(i);
        if (ItemUtils.isFood(item.getItemId())) {
          System.out.println("> Found food");
          try {
            mouseAgent.moveMouseTo(ShapeUtils.selectRandomPointIn(item.getBounds())).get(5, TimeUnit.SECONDS);
            mouseAgent.leftClick().get(5, TimeUnit.SECONDS);
          } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
          }
          return;
        }
      }
      System.out.println("> Found no food");
    });
  }

  public static Future<Boolean> openInventory(Client client, MouseAgent mouseAgent, RuneLiteAddons addons) {
    if (isInventoryOpen(client, addons)) {
      return CompletableFuture.completedFuture(true);
    }
    Widget inventoryIcon = client.getWidget(WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB);
    if (inventoryIcon == null) {
      System.out.println("> Could not find inventory tab icon!");
      return CompletableFuture.completedFuture(false);
    }

    if (active.get()) {
      return CompletableFuture.completedFuture(null);
    }

    active.set(true);

    return CompletableFuture.supplyAsync(() -> {
      try {
        mouseAgent.moveMouseTo(ShapeUtils.selectRandomPointIn(inventoryIcon.getBounds())).get(5, TimeUnit.SECONDS);
        mouseAgent.leftClick().get(5, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        return false;
      } finally {
        active.set(false);
      }
      active.set(false);
      return true;
    });
  }

  public static CompletableFuture<Void> emptyIntoBank(Client client, MouseAgent mouseAgent, Predicate<Widget> filter) {
    Predicate<Widget> endFilter = filter.or(widget -> widget.getItemId() == EMPTY_ITEM_ID);
    Widget inventory = client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER);

    if (inventory == null) {
      return CompletableFuture.completedFuture(null);
    }

    if (inventory.isHidden()) {
      return null;
    }

    if (active.get()) {
      return CompletableFuture.completedFuture(null);
    }

    active.set(true);

    return CompletableFuture.runAsync(() -> {
      int j = 0;
      for (int i = 0; i < INVENTORY_SLOTS; i++) {
        Widget child = inventory.getChild(i);
        if (endFilter.test(child)) {
          continue;
        }

        if (j++ == INVENTORY_SLOTS) {
          break;
        }

        Rectangle area = child.getBounds();
        Point point = ShapeUtils.selectRandomPointIn(area);
        try {
          mouseAgent.moveMouseTo(point).get(5, TimeUnit.SECONDS);
          mouseAgent.leftClick().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
          throw new RuntimeException(e);
        }
        i = 0;
      }
      active.set(false);
    });
  }

}
