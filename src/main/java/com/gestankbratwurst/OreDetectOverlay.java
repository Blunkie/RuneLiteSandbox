package com.gestankbratwurst;

import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;

public class OreDetectOverlay extends Overlay {

  private final Client client;
  private final ModelOutlineRenderer modelOutlineRenderer;
  private final RuneLiteAddons runeLiteAddons;

  @Inject
  private OreDetectOverlay(Client client, RuneLiteAddons runeLiteAddons, ModelOutlineRenderer modelOutlineRenderer) {
    this.client = client;
    this.modelOutlineRenderer = modelOutlineRenderer;
    this.runeLiteAddons = runeLiteAddons;

    setPosition(OverlayPosition.DYNAMIC);
    setLayer(OverlayLayer.ABOVE_SCENE);
  }

  @Override
  public Dimension render(Graphics2D graphics) {
    for (AvailableOre ore : runeLiteAddons.getOres().values()) {
      renderClickBox(graphics, ore);
    }

    return null;
  }

  private void renderCircleMiddle(Graphics2D graphics, AvailableOre ore) {
    LocalPoint localPoint = LocalPoint.fromWorld(client, ore.getTileObject().getWorldLocation());

    if (localPoint == null) {
      return;
    }

    Point point = Perspective.localToCanvas(client, localPoint, client.getPlane(), ore.getRock().getZOffset());
    if (point == null) {
      return;
    }

    graphics.setColor(new Color(0, 255, 255, 50));
    graphics.fillOval(point.getX(), point.getY(), 7, 7);

    graphics.setColor(Color.CYAN);
    graphics.setStroke(new BasicStroke(2));
    graphics.drawOval(point.getX(), point.getY(), 8, 8);
  }

  private void renderClickBox(Graphics2D graphics, AvailableOre ore) {
    Color color = Color.CYAN;
    Stroke stroke = new BasicStroke(2);
    Color clickBoxColor = ColorUtil.colorWithAlpha(color, color.getAlpha() / 12);
    Shape clickBox = ore.getTileObject().getClickbox();
    if(clickBox == null) {
      return;
    }
    OverlayUtil.renderPolygon(graphics, clickBox, color, clickBoxColor, stroke);
  }

  private void renderConvexHull(Graphics2D graphics, TileObject object, Color color, Stroke stroke) {
    final Shape polygon;
    Shape polygon2 = null;

    if (object instanceof GameObject) {
      polygon = ((GameObject) object).getConvexHull();
    } else if (object instanceof WallObject) {
      polygon = ((WallObject) object).getConvexHull();
      polygon2 = ((WallObject) object).getConvexHull2();
    } else if (object instanceof DecorativeObject) {
      polygon = ((DecorativeObject) object).getConvexHull();
      polygon2 = ((DecorativeObject) object).getConvexHull2();
    } else if (object instanceof GroundObject) {
      polygon = ((GroundObject) object).getConvexHull();
    } else {
      polygon = object.getCanvasTilePoly();
    }

    if (polygon != null) {
      OverlayUtil.renderPolygon(graphics, polygon, color, stroke);
    }

    if (polygon2 != null) {
      OverlayUtil.renderPolygon(graphics, polygon2, color, stroke);
    }
  }
}
