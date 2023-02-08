package com.gestankbratwurst.autoharvester;

import com.gestankbratwurst.RuneLiteAddons;
import com.gestankbratwurst.mousemovement.MouseAgent;
import com.gestankbratwurst.utils.EnvironmentUtils;
import com.gestankbratwurst.utils.InventoryUtils;
import com.gestankbratwurst.utils.ShapeUtils;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
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

public class AutoMiner {

  private static final int MAX_DISTANCE = 16;

  private final Set<Integer> currentObjectTargets = new HashSet<>();
  private final RuneLiteAddons addons;
  private final Client client;
  private boolean active = false;

  public AutoMiner(RuneLiteAddons addons) {
    this.client = addons.getClient();
    this.addons = addons;
  }

  public void start(int id) {
    currentObjectTargets.clear();
    Arrays.stream(ObjectIdGroups.groupOrId(id)).forEach(currentObjectTargets::add);
    active = true;
    mineNextRock();
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
    }
  }

  public void notifyActionChange() {
    if (!active) {
      return;
    }
    addons.addTask(2, this::mineNextRock);
  }

  private void mineNextRock() {
    if (!active) {
      return;
    }

    System.out.println("> Cut next tree: " + Arrays.toString(this.currentObjectTargets.toArray()));

    WorldPoint currentPoint = client.getLocalPlayer().getWorldLocation();
    EnvironmentUtils.findObjects(client, MAX_DISTANCE, obj -> currentObjectTargets.contains(obj.getId()))
            .stream()
            .min(Comparator.comparingInt(obj -> obj.getWorldLocation().distanceTo(currentPoint)))
            .ifPresentOrElse(this::mineRock, this::lookAroundAndTryAgain);
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
        addons.addTask(2, this::mineNextRock);
      }
    });
  }

  private void mineRock(GameObject rock) {
    if (!active) {
      return;
    }
    addons.runSync(() -> {
      Point point = ShapeUtils.selectRandomPointIn(rock.getClickbox());
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
