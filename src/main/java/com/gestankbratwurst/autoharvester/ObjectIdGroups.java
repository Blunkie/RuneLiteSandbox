package com.gestankbratwurst.autoharvester;

import java.util.HashMap;
import java.util.Map;

public class ObjectIdGroups {

  private static final Map<Integer, int[]> groups = new HashMap<>();

  static {
    create(10943, 11161);
    create(11365, 11364);
    create(11360, 11361);
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
