package com.gestankbratwurst.autoharvester;

import com.gestankbratwurst.RuneLiteAddons;
import com.gestankbratwurst.mousemovement.MouseAgent;
import com.gestankbratwurst.utils.EnvironmentUtils;
import com.gestankbratwurst.utils.InventoryUtils;
import com.gestankbratwurst.utils.ItemUtils;
import com.gestankbratwurst.utils.ShapeUtils;
import net.runelite.api.AnimationID;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.SoundEffectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoMiner {

  private static boolean created = false;

  private final Set<Integer> currentObjectTargets = new HashSet<>();
  private final RuneLiteAddons addons;
  private final Client client;
  private final AtomicBoolean active = new AtomicBoolean(false);
  private int initialId = -1;
  private WorldPoint startPoint = null;
  private long lastAction = System.currentTimeMillis();
  private int retries = 0;

  public AutoMiner(RuneLiteAddons addons) {
    if(created) {
      throw new RuntimeException("Instantiated multiple auto miners.");
    }
    created = true;
    this.client = addons.getClient();
    this.addons = addons;
  }

  public void start(int id) {
    if (!active.get()) {
      System.out.println("> Miner started");
    } else {
      return;
    }
    active.set(true);
    initialId = id;
    currentObjectTargets.clear();
    Arrays.stream(ObjectIdGroups.groupOrId(id)).forEach(currentObjectTargets::add);
    addons.runSync(this::mineNextRock);
  }

  public void injectWoodcuttingOption(MenuEntryAdded event) {
    if (event.getOption().equals("Mine") && client.isKeyPressed(KeyCode.KC_SHIFT)) {
      MenuEntry entry = event.getMenuEntry();
      int id = entry.getIdentifier();
      client.createMenuEntry(-1)
              .setOption("Starte auto mining [" + id + "]")
              .setTarget(event.getTarget())
              .setParam0(event.getActionParam0())
              .setParam1(event.getActionParam1())
              .setIdentifier(event.getIdentifier())
              .setType(MenuAction.RUNELITE)
              .onClick(menuEntry -> start(id));
    }
  }

  public void stop() {
    if (active.get()) {
      System.out.println("> Miner stopped");
    } else {
      return;
    }
    active.set(false);
  }

  public void nextTick() {
    if (!active.get()) {
      return;
    }
    if (addons.getClient().getLocalPlayer().getAnimation() != AnimationID.IDLE) {
      lastAction = System.currentTimeMillis();
    }
    if (System.currentTimeMillis() - lastAction > addons.getConfig().maxIdleForRetryMining()) {
      checkForRetry();
    }
  }

  private void checkForRetry() {
    inventoryCheck();
    lastAction = System.currentTimeMillis();
  }

  public void notifyInventoryUpdate() {
    if (!active.get()) {
      return;
    }
    lastAction = System.currentTimeMillis();
    inventoryCheck();
  }

  private void inventoryCheck() {
    if (InventoryUtils.isFull(client)) {
      client.playSoundEffect(SoundEffectID.UI_BOOP);
      client.addChatMessage(ChatMessageType.GAMEMESSAGE, "FlosSandbox", "Laufe zur Bank...", "FlosSandbox");
      stop();
      startPoint = client.getLocalPlayer().getWorldLocation();
      // 3183 3420
      addons.getPathTravel().travelTo(new WorldPoint(3183, 3436, 0)).thenRun(() -> {
        try {
          Thread.sleep(2500);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        if (EnvironmentUtils.openNearbyBankBooth(client, addons, addons.getMouseAgent(), 12).join()) {
          try {
            Thread.sleep(750);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          InventoryUtils.emptyIntoBank(addons.getClient(), addons.getMouseAgent(), ItemUtils.getOreIds()).join();
        } else {
          throw new IllegalStateException("Could not open nearby bank booths");
        }
      }).thenRun(() -> {
        addons.getPathTravel().travelTo(startPoint).join();
        try {
          Thread.sleep(ThreadLocalRandom.current().nextInt(2150, 3350));
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        start(initialId);
      }).whenComplete((v, t) -> {
        if (t != null) {
          t.printStackTrace();
        }
      });
    } else {
      mineNextRock();
    }
  }

  public void notifyActionChange(AnimationChanged event) {
    if (!active.get()) {
      return;
    }
    if (addons.getClient().getLocalPlayer().getAnimation() != AnimationID.IDLE) {
      lastAction = System.currentTimeMillis();
      // addons.addTask(2, this::mineNextRock);
    }
  }

  private void mineNextRock() {
    if (!active.get()) {
      return;
    }

    System.out.println("> Mine next rock: " + Arrays.toString(this.currentObjectTargets.toArray()));

    WorldPoint currentPoint = client.getLocalPlayer().getWorldLocation();
    int oreDist = (int) addons.getConfig().maxOreDistance();

    EnvironmentUtils.findObjects(client, oreDist, obj -> currentObjectTargets.contains(obj.getId()))
            .stream()
            .min(Comparator.comparingInt(obj -> obj.getWorldLocation().distanceTo(currentPoint)))
            .ifPresentOrElse(this::mineRock, this::lookAroundAndTryAgain);
  }

  private void lookAroundAndTryAgain() {
    if (!active.get()) {
      return;
    }
    System.out.println("> Look around and try again");
    CompletableFuture.runAsync(() -> {
      try {
        addons.getMouseAgent().randomCamMove(2.0).get(5, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        e.printStackTrace();
      } finally {
        addons.addTask(1 + Math.min(10, retries++), this::mineNextRock);
      }
    }).whenComplete((v, t) -> {
      if (t != null) {
        t.printStackTrace();
      }
    });
  }

  private void mineRock(GameObject rock) {
    if (!active.get()) {
      return;
    }
    retries = 0;
    addons.runSync(() -> {
      Point point = ShapeUtils.selectRandomPointIn(rock.getClickbox());
      MouseAgent agent = addons.getMouseAgent();
      CompletableFuture.runAsync(() -> {
        agent.moveMouseTo(point);
        try {
          lastAction = System.currentTimeMillis();
          agent.leftClick().get(5, TimeUnit.SECONDS);
          Thread.sleep(ThreadLocalRandom.current().nextInt(200, 300));
          agent.randomCamMove().get();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
          throw new RuntimeException(e);
        }
      }).whenComplete((v, t) -> {
        if (t != null) {
          t.printStackTrace();
        }
      });
    });
  }

}
