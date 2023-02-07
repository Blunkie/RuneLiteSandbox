package com.gestankbratwurst.autofight;

import net.runelite.api.NpcID;

import java.util.HashMap;
import java.util.Map;

public class NpcIdGroups {

  private static final Map<Integer, int[]> groups = new HashMap<>();

  static {
    int goblin3k = NpcID.GOBLIN_3076 - NpcID.GOBLIN_3028 + 1;
    int[] goblinIds = new int[goblin3k];
    for (int i = 0; i < goblin3k; i++) {
      goblinIds[i] = NpcID.GOBLIN_3028 + i;
      System.out.println(NpcID.GOBLIN_3028 + i);
    }
    create(goblinIds);
  }

  public static int[] groupOrId(int id) {
    if (groups.containsKey(id)) {
      return groups.get(id);
    } else {
      return new int[]{id};
    }
  }

  private static void create(int... ids) {
    for (int id : ids) {
      groups.put(id, ids);
    }
  }

}
