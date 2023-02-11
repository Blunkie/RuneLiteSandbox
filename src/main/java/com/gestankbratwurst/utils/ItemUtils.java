package com.gestankbratwurst.utils;

import net.runelite.api.ItemID;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ItemUtils {

  private static final Set<Integer> ORE_IDS = Set.of(
          ItemID.COAL,
          ItemID.COPPER_ORE,
          ItemID.TIN_ORE,
          ItemID.IRON_ORE,
          ItemID.GOLD_ORE,
          ItemID.MITHRIL_ORE,
          ItemID.ADAMANTITE_ORE,
          ItemID.RUNITE_ORE,
          ItemID.EMERALD,
          ItemID.UNCUT_SAPPHIRE,
          ItemID.UNCUT_EMERALD,
          ItemID.UNCUT_RUBY,
          ItemID.UNCUT_DIAMOND,
          ItemID.UNCUT_ONYX,
          ItemID.UNCUT_DRAGONSTONE,
          ItemID.UNCUT_JADE,
          ItemID.UNCUT_OPAL,
          ItemID.UNCUT_RED_TOPAZ,
          ItemID.UNCUT_ZENYTE,
          ItemID.CLAY
  );
  private static final Set<Integer> LOG_IDS = Set.of(
          ItemID.LOGS,
          ItemID.OAK_LOGS,
          ItemID.WILLOW_LOGS,
          ItemID.MAPLE_LOGS,
          ItemID.YEW_LOGS,
          ItemID.MAGIC_LOGS,
          ItemID.REDWOOD_LOGS,
          ItemID.TEAK_LOGS,
          ItemID.MAHOGANY_LOGS,
          ItemID.ACHEY_TREE_LOGS,
          ItemID.ARCTIC_PINE_LOGS,
          ItemID.ARCTIC_PYRE_LOGS,
          ItemID.BARK
  );
  private static final Set<Integer> GRIMY_IDS;
  private static final Set<Integer> BIRD_NEST_IDS;
  private static final Set<Integer> FOOD_IDS = Set.of(
          ItemID.SALMON,
          ItemID.COOKED_CHICKEN,
          ItemID.TROUT,
          ItemID.SHRIMPS,
          ItemID.SARDINE,
          ItemID.COOKED_MEAT
  );

  static {
    List<Integer> grimyList = new ArrayList<>();
    List<Integer> birdNestList = new ArrayList<>();

    Class<ItemID> idClass = ItemID.class;

    for (Field field : idClass.getDeclaredFields()) {
      if (field.getName().contains("GRIMY")) {
        try {
          grimyList.add(field.getInt(null));
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }
      if (field.getName().contains("BIRD_NEST")) {
        try {
          birdNestList.add(field.getInt(null));
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    }

    GRIMY_IDS = Set.copyOf(grimyList);
    BIRD_NEST_IDS = Set.copyOf(birdNestList);
  }

  public static boolean isGrimyLeaf(int itemId) {
    return GRIMY_IDS.contains(itemId);
  }

  public static boolean isFood(int itemId) {
    return FOOD_IDS.contains(itemId);
  }

  public static boolean isBirdsNest(int id) {
    return BIRD_NEST_IDS.contains(id);
  }

  public static List<Integer> getLogIds() {
    ArrayList<Integer> ids = new ArrayList<>(LOG_IDS);
    ids.addAll(BIRD_NEST_IDS);
    return ids;
  }

  public static List<Integer> getOreIds() {
    return new ArrayList<>(ORE_IDS);
  }
}
