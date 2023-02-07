package com.gestankbratwurst.autofight;

import com.gestankbratwurst.RuneLiteAddons;
import com.gestankbratwurst.mousemovement.MouseAgent;
import com.gestankbratwurst.utils.EnvironmentUtils;
import com.gestankbratwurst.utils.InventoryUtils;
import com.gestankbratwurst.utils.ItemUtils;
import com.gestankbratwurst.utils.ShapeUtils;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuEntryAdded;

import java.awt.Point;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AutoFighter {

  private static final int KILLS_BEFORE_HAUL = 2;

  private int killCounter = 0;
  private boolean active = false;
  private final Set<Integer> selectedTypes = new HashSet<>();
  private final RuneLiteAddons addons;
  private final Client client;
  private final MouseAgent mouseAgent;
  @Getter
  @Setter
  private double minHealthPercentage = 0.5;
  private boolean eating = false;


  public AutoFighter(RuneLiteAddons addons) {
    this.addons = addons;
    this.client = addons.getClient();
    this.mouseAgent = addons.getMouseAgent();
  }

  public void start() {
    active = true;
    attackNextTick();
    foodLoop();
    System.out.println("> Auto fighter started");
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

  public void stop() {
    if (!active) {
      return;
    }
    active = false;
    selectedTypes.clear();
    mouseAgent.interruptMouse();
    System.out.println("> Auto fighter stopped");
  }

  public void attackNextNpc() {
    if (!active) {
      return;
    }
    WorldPoint current = client.getLocalPlayer().getWorldLocation();
    client.getNpcs().stream()
            .filter(Objects::nonNull)
            .filter(npc -> !npc.isDead())
            .filter(npc -> selectedTypes.contains(npc.getId()))
            .min(Comparator.comparingInt(npc -> npc.getWorldLocation().distanceTo(current)))
            .ifPresentOrElse(this::attackNpc, this::attackNextTick);
  }

  private void attackNpc(NPC npc) {
    if (!active) {
      return;
    }
    Shape shape = npc.getConvexHull();
    Point point = ShapeUtils.selectRandomPointIn(shape);
    mouseAgent.moveMouseTo(point);
    mouseAgent.leftClick();
    checkForFightNextTick();

    System.out.println("> Attacking " + npc.getId());
  }

  private boolean isValuable(TileItem item) {
    int id = item.getId();
    return ItemUtils.isGrimyLeaf(id) || id == ItemID.BIG_BONES;
  }

  public void onActorDeath(Actor actor) {
    if (!active) {
      return;
    }
    if (client.getLocalPlayer().getInteracting() == actor) {
      EnvironmentUtils.enqueueNearbyGroundItems(client, this::isValuable);
      if(++killCounter == KILLS_BEFORE_HAUL) {
        killCounter = 0;
        EnvironmentUtils.haulAllItems(addons).whenComplete((a, t) -> {
          if (t != null) {
            t.printStackTrace();
          }
          attackNextTick();
        });
      } else {
        attackNextTick();
      }
      System.out.println("> Fight ended");
    }
  }

  public void attackNextTick() {
    if (!active) {
      return;
    }
    System.out.println("> Attacking next tick");
    addons.addTask(2, this::attackNextNpc);
  }

  public void attackLater() {
    if (!active) {
      return;
    }
    System.out.println("> Attacking next tick");
    addons.addTask(5, this::attackNextNpc);
  }

  public void checkForFightNextTick() {
    if (!active) {
      return;
    }
    addons.addTask(3, () -> {
      if (client.getLocalPlayer().getInteracting() == null) {
        attackNextNpc();
        System.out.println("> No combat detected");
      } else {
        mouseAgent.randomCamMove();
      }
    });
  }

  private void initId(int id) {
    List<Integer> ids = new ArrayList<>();
    for (int groupedId : NpcIdGroups.groupOrId(id)) {
      ids.add(groupedId);
    }
    selectedTypes.addAll(ids);
  }

  public void injectNpcAction(MenuEntryAdded event) {
    if (event.getType() != MenuAction.EXAMINE_NPC.getId() || !client.isKeyPressed(KeyCode.KC_SHIFT)) {
      return;
    }

    NPC npc = event.getMenuEntry().getNpc();

    client.createMenuEntry(-1)
            .setOption("Starte auto fighter [" + (npc == null ? "-" : npc.getId()) + "]")
            .setTarget(event.getTarget())
            .setParam0(event.getActionParam0())
            .setParam1(event.getActionParam1())
            .setIdentifier(event.getIdentifier())
            .setType(MenuAction.RUNELITE)
            .onClick(menuEntry -> {
              if (npc != null) {
                initId(npc.getId());
                start();
              } else {
                System.out.println("NPC is null");
              }
            });
  }

}
