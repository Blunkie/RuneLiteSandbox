package com.gestankbratwurst.utils;

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.Duration;
import java.util.Map;

import static net.runelite.api.ObjectID.ASH_PILE;
import static net.runelite.api.ObjectID.DAEYALT_ESSENCE_39095;
import static net.runelite.api.ObjectID.ROCKS_10943;
import static net.runelite.api.ObjectID.ROCKS_11161;
import static net.runelite.api.ObjectID.ROCKS_11360;
import static net.runelite.api.ObjectID.ROCKS_11361;
import static net.runelite.api.ObjectID.ROCKS_11364;
import static net.runelite.api.ObjectID.ROCKS_11365;
import static net.runelite.api.ObjectID.ROCKS_11366;
import static net.runelite.api.ObjectID.ROCKS_11367;
import static net.runelite.api.ObjectID.ROCKS_11368;
import static net.runelite.api.ObjectID.ROCKS_11369;
import static net.runelite.api.ObjectID.ROCKS_11370;
import static net.runelite.api.ObjectID.ROCKS_11371;
import static net.runelite.api.ObjectID.ROCKS_11372;
import static net.runelite.api.ObjectID.ROCKS_11373;
import static net.runelite.api.ObjectID.ROCKS_11374;
import static net.runelite.api.ObjectID.ROCKS_11375;
import static net.runelite.api.ObjectID.ROCKS_11376;
import static net.runelite.api.ObjectID.ROCKS_11377;
import static net.runelite.api.ObjectID.ROCKS_11380;
import static net.runelite.api.ObjectID.ROCKS_11381;
import static net.runelite.api.ObjectID.ROCKS_11386;
import static net.runelite.api.ObjectID.ROCKS_11387;
import static net.runelite.api.ObjectID.ROCKS_28596;
import static net.runelite.api.ObjectID.ROCKS_28597;
import static net.runelite.api.ObjectID.ROCKS_33254;
import static net.runelite.api.ObjectID.ROCKS_33255;
import static net.runelite.api.ObjectID.ROCKS_33256;
import static net.runelite.api.ObjectID.ROCKS_33257;
import static net.runelite.api.ObjectID.ROCKS_36203;
import static net.runelite.api.ObjectID.ROCKS_36204;
import static net.runelite.api.ObjectID.ROCKS_36205;
import static net.runelite.api.ObjectID.ROCKS_36206;
import static net.runelite.api.ObjectID.ROCKS_36207;
import static net.runelite.api.ObjectID.ROCKS_36208;
import static net.runelite.api.ObjectID.ROCKS_36209;
import static net.runelite.client.util.RSTimeUnit.GAME_TICKS;

public enum Rock {
  TIN(Duration.of(4, GAME_TICKS), 0, ROCKS_11360, ROCKS_11361),
  COPPER(Duration.of(4, GAME_TICKS), 0, ROCKS_10943, ROCKS_11161),
  IRON(Duration.of(9, GAME_TICKS), 0, ROCKS_11364, ROCKS_11365, ROCKS_36203) {
    @Override
    Duration getRespawnTime(int region) {
      return region == MINING_GUILD ? Duration.of(4, GAME_TICKS) : super.respawnTime;
    }
  },
  COAL(Duration.of(49, GAME_TICKS), 0, ROCKS_11366, ROCKS_11367, ROCKS_36204) {
    @Override
    Duration getRespawnTime(int region) {
      switch (region) {
        case MINING_GUILD:
          return Duration.of(24, GAME_TICKS);
        case MISCELLANIA:
          return Duration.of(11, GAME_TICKS);
        default:
          return super.respawnTime;
      }
    }
  },
  SILVER(Duration.of(100, GAME_TICKS), 0, ROCKS_11368, ROCKS_11369, ROCKS_36205),
  SANDSTONE(Duration.of(9, GAME_TICKS), 0, ROCKS_11386),
  GOLD(Duration.of(100, GAME_TICKS), 0, ROCKS_11370, ROCKS_11371, ROCKS_36206),
  GRANITE(Duration.of(9, GAME_TICKS), 0, ROCKS_11387),
  MITHRIL(Duration.of(200, GAME_TICKS), 0, ROCKS_11372, ROCKS_11373, ROCKS_36207) {
    @Override
    Duration getRespawnTime(int region) {
      return region == MINING_GUILD ? Duration.of(100, GAME_TICKS) : super.respawnTime;
    }
  },
  LOVAKITE(Duration.of(10, GAME_TICKS), 0, ROCKS_28596, ROCKS_28597),
  ADAMANTITE(Duration.of(400, GAME_TICKS), 0, ROCKS_11374, ROCKS_11375, ROCKS_36208) {
    @Override
    Duration getRespawnTime(int region) {
      return region == MINING_GUILD || region == WILDERNESS_RESOURCE_AREA ? Duration.of(200, GAME_TICKS) : super.respawnTime;
    }
  },
  RUNITE(Duration.of(1200, GAME_TICKS), 0, ROCKS_11376, ROCKS_11377, ROCKS_36209) {
    @Override
    Duration getRespawnTime(int region) {
      return region == MINING_GUILD ? Duration.of(600, GAME_TICKS) : super.respawnTime;
    }
  },
  ORE_VEIN(Duration.of(10, GAME_TICKS), 150),
  AMETHYST(Duration.of(125, GAME_TICKS), 120),
  ASH_VEIN(Duration.of(50, GAME_TICKS), 0, ASH_PILE),
  GEM_ROCK(Duration.of(99, GAME_TICKS), 0, ROCKS_11380, ROCKS_11381),
  URT_SALT(Duration.of(9, GAME_TICKS), 0, ROCKS_33254),
  EFH_SALT(Duration.of(9, GAME_TICKS), 0, ROCKS_33255),
  TE_SALT(Duration.of(9, GAME_TICKS), 0, ROCKS_33256),
  BASALT(Duration.of(9, GAME_TICKS), 0, ROCKS_33257),
  DAEYALT_ESSENCE(Duration.of(10, GAME_TICKS), 0, DAEYALT_ESSENCE_39095),
  BARRONITE(Duration.of(89, GAME_TICKS), 140),
  MINERAL_VEIN(Duration.of(100, GAME_TICKS), 150);

  private static final int WILDERNESS_RESOURCE_AREA = 12605;
  private static final int MISCELLANIA = 10044;
  private static final int MINING_GUILD = 12183;
  private static final Map<Integer, Rock> ROCKS;

  static {
    ImmutableMap.Builder<Integer, Rock> builder = new ImmutableMap.Builder<>();
    for (Rock rock : values()) {
      for (int id : rock.ids) {
        builder.put(id, rock);
      }
    }
    ROCKS = builder.build();
  }

  private final Duration respawnTime;
  @Getter
  private final int zOffset;
  private final int[] ids;

  Rock(Duration respawnTime, int zOffset, int... ids) {
    this.respawnTime = respawnTime;
    this.zOffset = zOffset;
    this.ids = ids;
  }

  Duration getRespawnTime(int region) {
    return respawnTime;
  }

  public static Rock getRockFromObjectId(int id) {
    return ROCKS.get(id);
  }
}
