package com.gestankbratwurst.autofight;

import com.gestankbratwurst.RuneLiteAddons;
import com.gestankbratwurst.mousemovement.MouseAgent;
import com.gestankbratwurst.utils.EnvironmentUtils;
import com.gestankbratwurst.utils.InventoryUtils;
import com.gestankbratwurst.utils.ItemUtils;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.AnimationID;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.SoundEffectID;
import net.runelite.api.coords.WorldPoint;

import java.util.concurrent.ThreadLocalRandom;

public class SandcrabFighter {

  private final RuneLiteAddons addons;
  private final Client client;
  private final MouseAgent mouseAgent;
  private boolean active = false;
  private long lastActionTime = System.currentTimeMillis();
  @Getter
  @Setter
  private double minHealthPercentage = 0.5;
  private boolean eating = false;
  private WorldPoint startPoint;

  public SandcrabFighter(RuneLiteAddons addons) {
    this.addons = addons;
    this.client = addons.getClient();
    this.mouseAgent = addons.getMouseAgent();
  }

  public void start() {
    if(active) {
      return;
    }
    System.out.println("> Started crab fighter");
    active = true;
    lastActionTime = System.currentTimeMillis();
    foodLoop();
  }

  private double getHealth() {
    return 1.0 / client.getLocalPlayer().getHealthScale() * client.getLocalPlayer().getHealthRatio();
  }

  private void foodLoop() {
    if (!active) {
      return;
    }
    if (getHealth() < minHealthPercentage && !eating) {
      System.out.println("> Try eating");
      eating = true;
      InventoryUtils.eatFood(client, mouseAgent, addons).thenRun(() -> eating = false).whenComplete((a, t) -> {
        if (t != null) {
          t.printStackTrace();
        }
      });
    }
    addons.addTask(2, this::foodLoop);
  }

  public void nextTick() {
    if (!active) {
      return;
    }
    if (client.getLocalPlayer().getAnimation() != AnimationID.IDLE) {
      lastActionTime = System.currentTimeMillis();
    }

    long actionDelta = System.currentTimeMillis() - lastActionTime;

    if (actionDelta > 3000 && active) {
      lastActionTime = System.currentTimeMillis();
      System.out.println("> Idled on crabs. Running for reset location.");
      walkAndBack();
    }
  }

  public void stop() {
    this.active = false;
  }

  private void walkAndBack() {
    client.playSoundEffect(SoundEffectID.UI_BOOP);
    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "FlosSandbox", "Resette crabs", "FlosSandbox");
    stop();
    startPoint = client.getLocalPlayer().getWorldLocation();
    int prePrecision = addons.getPathTravel().getPrecision();
    addons.getPathTravel().setPrecision(0);
    addons.getPathTravel().travelTo(new WorldPoint(1761, 3493, 0)).thenRun(() -> {
      if(!active) {
        addons.getPathTravel().setPrecision(prePrecision);
        return;
      }
      try {
        Thread.sleep(2500);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }).thenRun(() -> {
      addons.getPathTravel().travelTo(startPoint).join();
      if(!active) {
        addons.getPathTravel().setPrecision(prePrecision);
        return;
      }
      try {
        Thread.sleep(ThreadLocalRandom.current().nextInt(2150, 3350));
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      while (client.getLocalPlayer().getWorldLocation().distanceTo(startPoint) > 0) {
        addons.getPathTravel().travelTo(startPoint).join();
      }
      addons.getPathTravel().setPrecision(prePrecision);
      if(!active) {
        return;
      }
      start();
    }).whenComplete((v, t) -> {
      if (t != null) {
        t.printStackTrace();
      }
    });
  }

}
