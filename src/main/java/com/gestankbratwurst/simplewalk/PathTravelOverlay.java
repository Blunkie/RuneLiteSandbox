package com.gestankbratwurst.simplewalk;

import com.gestankbratwurst.RuneLiteAddons;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ColorUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;

public class PathTravelOverlay extends Overlay {

  private final PathTravel pathTravel;
  private final RuneLiteAddons addons;

  public PathTravelOverlay(RuneLiteAddons addons) {
    this.pathTravel = addons.getPathTravel();
    this.addons = addons;
  }

  @Override
  public Dimension render(Graphics2D graphics2D) {
    if (!pathTravel.isTraveling()) {
      return null;
    }

    WorldPoint globalPoint = pathTravel.getCurrentTarget();

    if (globalPoint == null) {
      return null;
    }

    LocalPoint localPoint = LocalPoint.fromWorld(addons.getClient(), globalPoint);

    if (localPoint == null) {
      return null;
    }

    Polygon tilePolygon = Perspective.getCanvasTilePoly(addons.getClient(), localPoint);

    renderTile(graphics2D, tilePolygon);
    return null;
  }

  private void renderTile(Graphics2D graphics, Polygon tilePolygon) {
    Color color = Color.GREEN;
    Stroke stroke = new BasicStroke(2);
    Color clickBoxColor = ColorUtil.colorWithAlpha(color, color.getAlpha() / 12);

    if (tilePolygon == null) {
      return;
    }

    OverlayUtil.renderPolygon(graphics, tilePolygon, color, clickBoxColor, stroke);
  }

}
