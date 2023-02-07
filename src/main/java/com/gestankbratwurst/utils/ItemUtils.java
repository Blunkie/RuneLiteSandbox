package com.gestankbratwurst.utils;

import net.runelite.api.ItemID;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ItemUtils {

  private static final Set<Integer> GRIMY_IDS;
  private static final Set<Integer> BIRD_NEST_IDS;
  private static final Set<Integer> FOOD_IDS = Set.of(
          ItemID.SALMON,
          ItemID.COOKED_CHICKEN,
          ItemID.TROUT
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
}
