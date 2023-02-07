package com.gestankbratwurst.autoharvester;

import com.gestankbratwurst.CompletionTask;
import com.gestankbratwurst.RuneLiteAddons;
import com.gestankbratwurst.mousemovement.MouseAgent;
import com.gestankbratwurst.utils.EnvironmentUtils;
import com.gestankbratwurst.utils.InventoryUtils;
import com.gestankbratwurst.utils.ItemUtils;
import com.gestankbratwurst.utils.ShapeUtils;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ItemID;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ObjectID;
import net.runelite.api.SoundEffectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuEntryAdded;

import java.awt.Point;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AutoWoodcutter {

  private static final int MAX_DISTANCE = 16;

  private final Set<Integer> currentObjectTargets = new HashSet<>();
  private final RuneLiteAddons addons;
  private final Client client;
  private boolean active = false;

  public AutoWoodcutter(RuneLiteAddons addons) {
    this.client = addons.getClient();
    this.addons = addons;
  }

  public void start(int id) {
    currentObjectTargets.clear();
    Arrays.stream(ObjectIdGroups.groupOrId(id)).forEach(currentObjectTargets::add);
    active = true;
    chopNextTree();
  }

  public void injectWoodcuttingOption(MenuEntryAdded event) {
    if (event.getOption().equals("Chop down") && client.isKeyPressed(KeyCode.KC_SHIFT)) {
      MenuEntry entry = event.getMenuEntry();
      int id = entry.getIdentifier();
      client.createMenuEntry(-1)
              .setOption("Starte auto woodcutting [" + id + "]")
              .setTarget(event.getTarget())
              .setParam0(event.getActionParam0())
              .setParam1(event.getActionParam1())
              .setIdentifier(event.getIdentifier())
              .setType(MenuAction.RUNELITE)
              .onClick(menuEntry -> start(id));
    }
  }

  public void stop() {
    active = false;
  }

  public void notifyInventoryUpdate() {
    if (!active) {
      return;
    }
    if (InventoryUtils.isFull(client)) {
      client.playSoundEffect(SoundEffectID.UI_BOOP);
      client.addChatMessage(ChatMessageType.GAMEMESSAGE, "FlosSandbox", "Inventar ist voll. Autobank funkt noch nicht :(", "FlosSandbox");
      stop();
      debugWalkToChest().thenRun(() -> {
        debugDeposit().thenRun(() -> {
          debugWalkToTree().thenRun(() -> {
            active = true;
            chopNextTree();
          });
        });
      });
    }
  }

  public void notifyActionChange() {
    if (!active) {
      return;
    }
    addons.addTask(2, this::chopNextTree);
  }

  private CompletableFuture<Void> checkForBirdsNest() {
    EnvironmentUtils.enqueueNearbyGroundItems(client, item -> ItemUtils.isBirdsNest(item.getId()));
    return EnvironmentUtils.haulAllItems(addons);
  }

  private CompletableFuture<Void> debugWalkToChest() {
    WorldPoint worldPoint = new WorldPoint(2443, 3083, client.getPlane());
    return addons.getSimpleWalker().walkTo(worldPoint, 4).thenApply(any -> null);
  }

  public CompletableFuture<Void> debugDeposit() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    EnvironmentUtils.findObjects(client, 8, obj -> obj.getId() == ObjectID.BANK_CHEST_4483).stream().findAny().ifPresentOrElse(chest -> {
      Point point = ShapeUtils.selectRandomPointIn(chest.getClickbox());
      CompletableFuture.runAsync(() -> {
        addons.getMouseAgent().moveMouseTo(point);
        try {
          addons.getMouseAgent().leftClick().get(5, TimeUnit.SECONDS);
          Thread.sleep(ThreadLocalRandom.current().nextInt(1200, 1500));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
          throw new RuntimeException(e);
        }
        InventoryUtils.emptyIntoBank(client, addons.getMouseAgent(), ItemID.TEAK_LOGS).thenRun(() -> future.complete(null));
      });
    }, () -> System.out.println("! Could not find chest..."));
    return future;
  }

  private CompletableFuture<Void> debugWalkToTree() {
    WorldPoint worldPoint = new WorldPoint(2335, 3048, client.getPlane());
    return addons.getSimpleWalker().walkTo(worldPoint, 4).thenApply(any -> null);
  }

  private void chopNextTree() {
    if (!active) {
      return;
    }

    System.out.println("> Cut next tree: " + Arrays.toString(this.currentObjectTargets.toArray()));

    checkForBirdsNest().thenRun(() -> addons.runSync(() -> {
      WorldPoint currentPoint = client.getLocalPlayer().getWorldLocation();
      EnvironmentUtils.findObjects(client, MAX_DISTANCE, obj -> currentObjectTargets.contains(obj.getId()))
              .stream()
              .min(Comparator.comparingInt(obj -> obj.getWorldLocation().distanceTo(currentPoint)))
              .ifPresentOrElse(this::chopTree, this::lookAroundAndTryAgain);
    }));
  }

  private void lookAroundAndTryAgain() {
    if (!active) {
      return;
    }
    CompletableFuture.runAsync(() -> {
      try {
        addons.getMouseAgent().randomCamMove(2.0).get(5, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        e.printStackTrace();
      } finally {
        addons.addTask(2, this::chopNextTree);
      }
    });
  }

  private void chopTree(GameObject tree) {
    if (!active) {
      return;
    }
    addons.runSync(() -> {
      Point point = ShapeUtils.selectRandomPointIn(tree.getClickbox());
      MouseAgent agent = addons.getMouseAgent();
      CompletableFuture.runAsync(() -> {
        agent.moveMouseTo(point);
        try {
          agent.leftClick().get(5, TimeUnit.SECONDS);
          Thread.sleep(ThreadLocalRandom.current().nextInt(200, 300));
          agent.randomCamMove().get();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
          throw new RuntimeException(e);
        }
      });
    });
  }

}
