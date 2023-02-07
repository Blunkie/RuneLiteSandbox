package com.gestankbratwurst.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class MenuUtils {

  private static final int MENU_TOP_BORDER = 19;
  private static final int MENU_ENTRY_HEIGHT = 15;

  @Data
  @AllArgsConstructor
  public static class MenuEntryArea {
    private final MenuEntry entry;
    private final Rectangle area;
  }

  public static List<MenuEntryArea> getMenuEntries(Client client) {
    List<MenuEntryArea> entryAreaList = new ArrayList<>();
    MenuEntry[] entries = client.getMenuEntries();
    if (client.isMenuOpen()) {
      int mx = client.getMenuX();
      int my = client.getMenuY();
      int width = client.getMenuWidth();

      int count = entries.length;

      for (int i = 0; i < count; i++) {
        int y = my + MENU_TOP_BORDER + (MENU_ENTRY_HEIGHT * (count - i - 1));
        Rectangle entryRect = new Rectangle(mx + 2, y, width - 5, MENU_ENTRY_HEIGHT);
        entryAreaList.add(new MenuEntryArea(entries[i], entryRect));
      }
    }
    return entryAreaList;
  }

}
