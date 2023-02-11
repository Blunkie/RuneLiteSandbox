package com.gestankbratwurst.utils.shortestpath.pathfinder;

import com.gestankbratwurst.RuneLiteAddons;
import com.gestankbratwurst.utils.shortestpath.ShortestPathConfig;
import com.gestankbratwurst.utils.shortestpath.ShortestPathPlugin;
import com.gestankbratwurst.utils.shortestpath.Transport;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathfinderConfig {
    public static final Duration CALCULATION_CUTOFF = Duration.ofSeconds(2);
    private static final WorldArea WILDERNESS_ABOVE_GROUND = new WorldArea(2944, 3523, 448, 448, 0);
    private static final WorldArea WILDERNESS_UNDERGROUND = new WorldArea(2944, 9918, 320, 442, 0);

    @Getter
    private final CollisionMap map;
    @Getter
    private final Map<WorldPoint, List<Transport>> transports;
    private final Client client;

    private boolean avoidWilderness;
    private boolean useAgilityShortcuts;
    private boolean useGrappleShortcuts;
    private boolean useBoats;
    private boolean useFairyRings;
    private boolean useTeleports;
    private int agilityLevel;
    private int rangedLevel;
    private int strengthLevel;
    private int prayerLevel;
    private int woodcuttingLevel;
    private final Map<Quest, QuestState> questStates = new HashMap<>();
    private final RuneLiteAddons addons;

    public PathfinderConfig(CollisionMap map, Map<WorldPoint, List<Transport>> transports, RuneLiteAddons addons) {
        this.map = map;
        this.transports = transports;
        this.addons = addons;
        this.client = addons.getClient();
        refresh();
    }

    public void refresh() {
        if (!GameState.LOGGED_IN.equals(client.getGameState())) {
            return;
        }
        avoidWilderness = true;
        useAgilityShortcuts = false;
        useGrappleShortcuts = false;
        useBoats = false;
        useFairyRings = false;
        useTeleports = false;
        agilityLevel = client.getBoostedSkillLevel(Skill.AGILITY);
        rangedLevel = client.getBoostedSkillLevel(Skill.RANGED);
        strengthLevel = client.getBoostedSkillLevel(Skill.STRENGTH);
        prayerLevel = client.getBoostedSkillLevel(Skill.PRAYER);
        woodcuttingLevel = client.getBoostedSkillLevel(Skill.WOODCUTTING);
        addons.runSync(this::refreshQuests);
    }

    private void refreshQuests() {
        useFairyRings &= !QuestState.NOT_STARTED.equals(Quest.FAIRYTALE_II__CURE_A_QUEEN.getState(client));
        for (Map.Entry<WorldPoint, List<Transport>> entry : transports.entrySet()) {
            for (Transport transport : entry.getValue()) {
                if (transport.isQuestLocked()) {
                    try {
                        questStates.put(transport.getQuest(), transport.getQuest().getState(client));
                    } catch (NullPointerException ignored) {
                    }
                }
            }
        }
    }

    private boolean isInWilderness(WorldPoint p) {
        return WILDERNESS_ABOVE_GROUND.distanceTo(p) == 0 || WILDERNESS_UNDERGROUND.distanceTo(p) == 0;
    }

    public boolean avoidWilderness(WorldPoint position, WorldPoint neighbor, WorldPoint target) {
        return avoidWilderness && !isInWilderness(position) && isInWilderness(neighbor) && !isInWilderness(target);
    }

    public boolean isNear(WorldPoint location) {
        if (client.getLocalPlayer() == null) {
            return true;
        }
        int recalculateDistance = 10;
        return client.getLocalPlayer().getWorldLocation().distanceTo2D(location) <= recalculateDistance;
    }

    public boolean useTransport(Transport transport) {
        final int transportAgilityLevel = transport.getRequiredLevel(Skill.AGILITY);
        final int transportRangedLevel = transport.getRequiredLevel(Skill.RANGED);
        final int transportStrengthLevel = transport.getRequiredLevel(Skill.STRENGTH);
        final int transportPrayerLevel = transport.getRequiredLevel(Skill.PRAYER);
        final int transportWoodcuttingLevel = transport.getRequiredLevel(Skill.WOODCUTTING);

        final boolean isAgilityShortcut = transport.isAgilityShortcut();
        final boolean isGrappleShortcut = transport.isGrappleShortcut();
        final boolean isBoat = transport.isBoat();
        final boolean isFairyRing = transport.isFairyRing();
        final boolean isTeleport = transport.isTeleport();
        final boolean isCanoe = isBoat && transportWoodcuttingLevel > 1;
        final boolean isPrayerLocked = transportPrayerLevel > 1;
        final boolean isQuestLocked = transport.isQuestLocked();

        if (isAgilityShortcut) {
            if (!useAgilityShortcuts || agilityLevel < transportAgilityLevel) {
                return false;
            }

            if (isGrappleShortcut && (!useGrappleShortcuts || rangedLevel < transportRangedLevel || strengthLevel < transportStrengthLevel)) {
                return false;
            }
        }

        if (isBoat) {
            if (!useBoats) {
                return false;
            }

            if (isCanoe && woodcuttingLevel < transportWoodcuttingLevel) {
                return false;
            }
        }

        if (isFairyRing && !useFairyRings) {
            return false;
        }

        if (isTeleport && !useTeleports) {
            return false;
        }

        if (isPrayerLocked && prayerLevel < transportPrayerLevel) {
            return false;
        }

        if (isQuestLocked && !QuestState.FINISHED.equals(questStates.getOrDefault(transport.getQuest(), QuestState.NOT_STARTED))) {
            return false;
        }

        return true;
    }
}
