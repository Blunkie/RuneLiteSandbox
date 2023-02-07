package com.gestankbratwurst;

import com.gestankbratwurst.utils.EnvironmentUtils;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class InventoryGridOverlay extends Overlay {
  private static final int INVENTORY_SIZE = 28;


  private final Client client;

  @Inject
  private InventoryGridOverlay(Client client) {
    this.client = client;

    setPosition(OverlayPosition.DYNAMIC);
    setLayer(OverlayLayer.ALWAYS_ON_TOP);
  }

  @Override
  public Dimension render(Graphics2D graphics) {
    final Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);

    if (inventoryWidget == null) {
      return null;
    }

    if (inventoryWidget.isHidden()) {
      return null;
    }

    for (int i = 0; i < INVENTORY_SIZE; ++i) {
      final Widget targetWidgetItem = inventoryWidget.getChild(i);
      final Rectangle bounds = targetWidgetItem.getBounds();
      graphics.setColor(Color.RED);
      graphics.draw(bounds);
    }

    return null;
  }
}
