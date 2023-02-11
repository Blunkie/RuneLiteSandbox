package com.gestankbratwurst.utils;

import net.runelite.api.ObjectID;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ObjectUtils {

  private static final Set<Integer> BANK_BOOTHS;

  static {
    List<Integer> bankBoothList = new ArrayList<>();

    Class<ObjectID> idClass = ObjectID.class;

    for (Field field : idClass.getDeclaredFields()) {
      if (field.getName().startsWith("BANK_BOOTH")) {
        try {
          bankBoothList.add(field.getInt(null));
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    }

    BANK_BOOTHS = Set.copyOf(bankBoothList);
  }

  public static boolean isBankBooth(int objectID) {
    return BANK_BOOTHS.contains(objectID);
  }

}
