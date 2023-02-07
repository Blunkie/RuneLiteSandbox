package com.gestankbratwurst;

import com.gestankbratwurst.autofight.AutoFighter;
import com.gestankbratwurst.autoharvester.AutoWoodcutter;
import com.gestankbratwurst.mousemovement.MouseAgent;
import com.gestankbratwurst.simplewalk.SimpleWalker;
import com.gestankbratwurst.utils.EnvironmentUtils;
import com.gestankbratwurst.utils.Rock;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemID;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import shortestpath.ShortestPathPlugin;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

@Slf4j
@PluginDescriptor(
        name = "Flos RuneLiteAddons"
)
public class RuneLiteAddons extends Plugin {

  @Getter
  private final Map<WorldPoint, AvailableOre> ores = new HashMap<>();
  private final LinkedList<FutureTickTask> tasks = new LinkedList<>();
  private final List<FutureTickTask> pendingAddTasks = new ArrayList<>();
  private final ConcurrentLinkedQueue<CompletionTask<?>> completionTasks = new ConcurrentLinkedQueue<>();

  @Getter
  @Inject
  private Client client;

  @Inject
  private RuneLiteAdddonsConfig config;

  @Inject
  private InventoryGridOverlay inventoryOverlay;

  @Inject
  private OreDetectOverlay oreOverlay;

  @Inject
  private OverlayManager overlayManager;

  @Getter
  private MouseAgent mouseAgent;

  @Getter
  private AutoFighter autoFighter;

  @Getter
  private AutoWoodcutter autoWoodcutter;

  @Getter
  private SimpleWalker simpleWalker;

  private void initAfterLogin() {
    mouseAgent = new MouseAgent(client);
    autoFighter = new AutoFighter(this);
    autoWoodcutter = new AutoWoodcutter(this);
    simpleWalker = new SimpleWalker(this);
    // EnvironmentUtils.startPickupLoop(this);
  }

  public <T> CompletableFuture<T> supplySync(Supplier<T> supplier) {
    CompletableFuture<T> future = new CompletableFuture<>();
    completionTasks.add(new CompletionTask<>(future, supplier));
    return future;
  }

  public CompletableFuture<Void> runSync(Runnable runnable) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    completionTasks.add(new CompletionTask<>(future, () -> {
      runnable.run();
      return null;
    }));
    return future;
  }

  @Override
  protected void startUp() {
    overlayManager.add(inventoryOverlay);
    overlayManager.add(oreOverlay);
    log.info("> RuneLiteSandbox started!");
  }

  @Override
  protected void shutDown() {
    overlayManager.remove(oreOverlay);
    overlayManager.remove(inventoryOverlay);
    log.info("> RuneLiteSandbox stopped!");
  }

  public void addTask(int delay, Runnable action) {
    pendingAddTasks.add(new FutureTickTask(delay, action));
  }

  private boolean allowPick = true;

  @Subscribe
  public void onItemContainerChanged(ItemContainerChanged event) {
    EnvironmentUtils.releasePickup();
    if (event.getItemContainer().getId() == InventoryID.INVENTORY.getId() && autoWoodcutter != null) {
      autoWoodcutter.notifyInventoryUpdate();
    }
  }

  @Subscribe
  public void onAnimationChanged(AnimationChanged event) {
    if (autoWoodcutter != null && event.getActor().equals(client.getLocalPlayer())) {
      autoWoodcutter.notifyActionChange();
    }
  }

  private boolean stored = false;

  @Subscribe
  public void onClientTick(ClientTick tick) {
    int left = completionTasks.size();
    while (!completionTasks.isEmpty() && left > 0) {
      CompletionTask<?> task = completionTasks.poll();
      --left;
      if (task == null) {
        continue;
      }
      task.completeOnCurrentThread();
    }

    if (client.isKeyPressed(KeyCode.KC_V)) {
      if (stored) {
        return;
      }
      stored = true;
      autoWoodcutter.debugDeposit().thenRun(() -> System.out.println("Stored everything"));
    }

    if (client.isKeyPressed(KeyCode.KC_X)) {
      autoFighter.stop();
      autoWoodcutter.stop();
      simpleWalker.stop();
    }

    if (client.isKeyPressed(KeyCode.KC_V) && allowPick) {
      allowPick = false;
      EnvironmentUtils.enqueueNearbyGroundItems(client, item -> item.getId() == ItemID.CABBAGE);
      CompletableFuture.runAsync(() -> {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        } finally {
          allowPick = true;
        }
      });
    }
  }

  @Subscribe
  public void onGameTick(GameTick tick) {
    tasks.addAll(pendingAddTasks);
    pendingAddTasks.clear();
    if (simpleWalker != null) {
      simpleWalker.nextTick();
    }
    tasks.removeIf(task -> {
      if (task == null) {
        return true;
      }
      return task.tick();
    });
  }

  @Subscribe
  public void onActorDeath(ActorDeath actorDeath) {
    autoFighter.onActorDeath(actorDeath.getActor());
  }

  @Subscribe
  public void onGameStateChanged(GameStateChanged gameStateChanged) {
    if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {

      addTask(1, this::initAfterLogin);

      addTask(2, () -> {
        for (Tile[][] plane : client.getScene().getTiles()) {
          for (Tile[] row : plane) {
            for (Tile tile : row) {
              if (tile == null) {
                continue;
              }
              GameObject[] objects = tile.getGameObjects();
              for (GameObject object : objects) {
                addIfRock(object);
              }
            }
          }
        }
      });
      client.addChatMessage(ChatMessageType.GAMEMESSAGE, "FlosSandbox", "> Flos Addon ist aktiv :D", "FlosSandbox");
    }
  }

  @Subscribe
  public void onMenuEntryAdded(MenuEntryAdded event) {
    if (autoFighter != null) {
      autoFighter.injectNpcAction(event);
    } else {
      return;
    }

    if (autoWoodcutter != null) {
      autoWoodcutter.injectWoodcuttingOption(event);
    } else {
      return;
    }

    if (event.getType() != MenuAction.EXAMINE_OBJECT.getId() || !client.isKeyPressed(KeyCode.KC_SHIFT)) {
      return;
    }

    final Tile tile = client.getScene().getTiles()[client.getPlane()][event.getActionParam0()][event.getActionParam1()];
    final TileObject tileObject = findTileObject(tile, event.getIdentifier());

    if (tileObject == null) {
      return;
    }

    client.createMenuEntry(-1)
            .setOption("-- TEST --")
            .setTarget(event.getTarget())
            .setParam0(event.getActionParam0())
            .setParam1(event.getActionParam1())
            .setIdentifier(event.getIdentifier())
            .setType(MenuAction.RUNELITE)
            .onClick(this::mineClickAction);
  }

  private void mineClickAction(MenuEntry entry) {

  }

  private TileObject findTileObject(Tile tile, int id) {
    if (tile == null) {
      return null;
    }

    final GameObject[] tileGameObjects = tile.getGameObjects();
    final DecorativeObject tileDecorativeObject = tile.getDecorativeObject();
    final WallObject tileWallObject = tile.getWallObject();
    final GroundObject groundObject = tile.getGroundObject();

    if (objectIdEquals(tileWallObject, id)) {
      return tileWallObject;
    }

    if (objectIdEquals(tileDecorativeObject, id)) {
      return tileDecorativeObject;
    }

    if (objectIdEquals(groundObject, id)) {
      return groundObject;
    }

    for (GameObject object : tileGameObjects) {
      if (objectIdEquals(object, id)) {
        return object;
      }
    }

    return null;
  }

  private boolean objectIdEquals(TileObject tileObject, int id) {
    if (tileObject == null) {
      return false;
    }

    if (tileObject.getId() == id) {
      return true;
    }

    // Menu action EXAMINE_OBJECT sends the transformed object id, not the base id, unlike
    // all the GAME_OBJECT_OPTION actions, so check the id against the impostor ids
    final ObjectComposition comp = client.getObjectDefinition(tileObject.getId());

    if (comp.getImpostorIds() != null) {
      for (int impostorId : comp.getImpostorIds()) {
        if (impostorId == id) {
          return true;
        }
      }
    }

    return false;
  }

  @Subscribe
  public void onGameObjectDespawned(GameObjectDespawned event) {
    //ores.remove(event.getGameObject().getWorldLocation());
  }

  @Subscribe
  public void onGameObjectSpawned(GameObjectSpawned event) {
    if (client.getGameState() != GameState.LOGGED_IN) {
      return;
    }

    //addIfRock(event.getGameObject());
  }

  private void addIfRock(GameObject gameObject) {
    if (gameObject == null) {
      return;
    }
    Rock rock = Rock.getRockFromObjectId(gameObject.getId());
    if (rock == null) {
      return;
    }

    ores.put(gameObject.getWorldLocation(), new AvailableOre(rock, gameObject));
  }

  @Provides
  RuneLiteAdddonsConfig provideConfig(ConfigManager configManager) {
    return configManager.getConfig(RuneLiteAdddonsConfig.class);
  }
}
