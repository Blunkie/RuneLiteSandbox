package com.gestankbratwurst.utils;

import com.gestankbratwurst.RuneLiteAddons;
import com.gestankbratwurst.mousemovement.MouseAgent;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.MenuAction;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class EnvironmentUtils {

  @Value
  private static class ItemPickup {
    Tile tile;
    TileItem items;
  }

  private static final int MAX_PICKUP_DIST = 20;
  private static final int TICK_DELAY_BETWEEN_PICKUPS = 2;
  private static final ConcurrentLinkedQueue<ItemPickup> itemQueue = new ConcurrentLinkedQueue<>();
  private static final AtomicBoolean currentlyPickingUp = new AtomicBoolean(false);
  private static final Semaphore pickupLock = new Semaphore(0);
  private static int currentPickupId = -1;

  public static void startPickupLoop(RuneLiteAddons addons) {
    addons.addTask(TICK_DELAY_BETWEEN_PICKUPS, () -> checkForPickupItems(addons).whenComplete((a, t) -> {
      if (t != null) {
        t.printStackTrace();
      }
      startPickupLoop(addons);
    }));
  }

  public static List<ItemPickup> getItemsToPickup() {
    return List.copyOf(itemQueue);
  }

  public static CompletableFuture<Void> haulAllItems(RuneLiteAddons addons) {
    return CompletableFuture.runAsync(() -> {
      boolean itemsLeft = true;
      while (itemsLeft) {
        try {
          itemsLeft = checkForPickupItems(addons).get();
          Thread.sleep(ThreadLocalRandom.current().nextInt(400, 650));
        } catch (InterruptedException | ExecutionException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  public static CompletableFuture<Boolean> checkForPickupItems(RuneLiteAddons addons) {
    if (itemQueue.isEmpty() || currentlyPickingUp.get()) {
      return CompletableFuture.completedFuture(false);
    }

    ItemPickup pickup = itemQueue.poll();
    if (pickup == null) {
      return CompletableFuture.completedFuture(false);
    }

    System.out.println("> Trying to pickup item " + pickup.items.getId());

    currentlyPickingUp.set(true);

    return pickupItem(pickup.tile, pickup.items, addons.getClient(), addons.getMouseAgent()).whenComplete((a, t) -> {
      if (t != null) {
        t.printStackTrace();
      }
      currentlyPickingUp.set(false);
    }).thenApply(any -> true);
  }

  public static Optional<Tile> findExactTile(Client client, Predicate<Tile> predicate) {
    for (Tile[][] plane : client.getScene().getTiles()) {
      for (Tile[] row : plane) {
        for (Tile tile : row) {
          if (tile != null && predicate.test(tile)) {
            return Optional.of(tile);
          }
        }
      }
    }
    return Optional.empty();
  }


  public static CompletableFuture<Boolean> openNearbyBankBooth(Client client, RuneLiteAddons addons, MouseAgent mouseAgent, int maxDist) {
    return CompletableFuture.supplyAsync(() -> {
      List<GameObject> booths = EnvironmentUtils.findObjects(client, maxDist, obj -> ObjectUtils.isBankBooth(obj.getId()));
      if (booths.isEmpty()) {
        new IllegalStateException("Could not find any nearby booths...").printStackTrace();
        return false;
      }
      GameObject bankObject = booths.get(ThreadLocalRandom.current().nextInt(booths.size()));
      Point point = ShapeUtils.selectRandomPointIn(bankObject.getClickbox());
      mouseAgent.moveMouseTo(point);
      try {
        mouseAgent.leftClick().get(5, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        throw new RuntimeException(e);
      }

      for (int i = 0; i < 200; i++) {
        Widget inventory = client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER);
        if (i > 0 && i % 25 == 0) {
          point = ShapeUtils.selectRandomPointIn(bankObject.getClickbox());
          if(point == null) {
            continue;
          }
          mouseAgent.moveMouseTo(point);
          try {
            mouseAgent.leftClick().get(5, TimeUnit.SECONDS);
          } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
          }
        }
        if (inventory == null || addons.supplySync(inventory::isHidden).join()) {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        } else {
          System.out.println("> Opened bank booth!");
          return true;
        }
      }

      return false;
    }).whenComplete((v, t) -> {
      if (t != null) {
        t.printStackTrace();
      }
    });
  }

  // TODO: Better performance by getting delta position of player until max distance
  public static List<GameObject> findObjects(Client client, int maxDist, Predicate<GameObject> filter) {
    WorldPoint currentLocation = client.getLocalPlayer().getWorldLocation();
    List<GameObject> objectList = new ArrayList<>();
    for (Tile[][] plane : client.getScene().getTiles()) {
      for (Tile[] row : plane) {
        for (Tile tile : row) {
          if (tile != null && tile.getPlane() == currentLocation.getPlane() && tile.getWorldLocation().distanceTo2D(currentLocation) < maxDist) {
            GameObject[] objects = tile.getGameObjects();
            if (objects != null) {
              Arrays.stream(objects).filter(Objects::nonNull).filter(filter).forEach(objectList::add);
            }
          }
        }
      }
    }
    return objectList;
  }

  public static void releasePickup() {
    pickupLock.release();
  }

  public static CompletableFuture<Void> pickupItem(Tile tile, TileItem item, Client client, MouseAgent agent) {
    return CompletableFuture.runAsync(() -> {
      int distance = tile.getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation());
      if (distance > MAX_PICKUP_DIST || tile.getPlane() != client.getLocalPlayer().getWorldLocation().getPlane()) {
        System.out.println("> Item is not in range. Skipping.");
        return;
      }
      currentPickupId = item.getId();
      Shape itemLayerClickBox = tile.getItemLayer().getClickbox();
      Point point = ShapeUtils.selectRandomPointIn(itemLayerClickBox);
      try {
        agent.moveMouseTo(point);
        agent.rightClick().get(5, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        throw new RuntimeException(e);
      }

      try {
        Thread.sleep(ThreadLocalRandom.current().nextLong(200, 300));
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      List<MenuUtils.MenuEntryArea> entryAreaList = MenuUtils.getMenuEntries(client);

      for (MenuUtils.MenuEntryArea entry : entryAreaList) {
        if (entry.getEntry().getType() == MenuAction.EXAMINE_ITEM_GROUND) {
          continue;
        }
        if (entry.getEntry().getIdentifier() == currentPickupId && entry.getEntry().getOption().equals("Take")) {
          Rectangle entryArea = entry.getArea();

          Point entryPoint = ShapeUtils.selectRandomPointIn(entryArea);
          try {
            agent.moveMouseTo(entryPoint);
            agent.leftClick().get(5, TimeUnit.SECONDS);
          } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
          }
          try {
            if (pickupLock.availablePermits() > 0) {
              pickupLock.acquire(pickupLock.availablePermits());
            }
            if (!pickupLock.tryAcquire(5, TimeUnit.SECONDS)) {
              System.out.println("!! Semaphore acquired negative permit (?)");
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          return;
        }
      }
    });
  }

  private static boolean isEnqueued(TileItem item) {
    return itemQueue.stream().anyMatch(entry -> entry.items == item);
  }

  // TODO: Better performance by getting delta position of player until max distance
  public static void enqueueNearbyGroundItems(Client client, Predicate<TileItem> selector) {
    int counter = 0;
    Map<Tile, List<TileItem>> items = getAllItems(client, MAX_PICKUP_DIST);
    for (Map.Entry<Tile, List<TileItem>> entry : items.entrySet()) {
      for (TileItem item : entry.getValue()) {
        if (selector.test(item) && !isEnqueued(item)) {
          itemQueue.add(new ItemPickup(entry.getKey(), item));
          counter++;
        }
      }
    }
    System.out.println("> Enqueued " + counter + " nearby items.");
  }

  public static Map<Tile, List<TileItem>> getAllItems(Client client, int maxDist) {
    WorldPoint currentLocation = client.getLocalPlayer().getWorldLocation();
    Map<Tile, List<TileItem>> items = new HashMap<>();
    for (Tile[][] plane : client.getScene().getTiles()) {
      for (Tile[] row : plane) {
        for (Tile tile : row) {
          if (tile != null && tile.getPlane() == currentLocation.getPlane() && tile.getWorldLocation().distanceTo2D(currentLocation) < maxDist) {
            List<TileItem> itemList = tile.getGroundItems();
            if (itemList != null && !itemList.isEmpty()) {
              items.put(tile, itemList);
            }
          }
        }
      }
    }
    return items;
  }

}
