package com.gestankbratwurst.utils;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.ThreadLocalRandom;

public class ShapeUtils {

  public static Point selectRandomPointIn(Shape region) {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    if (region == null) {
      return null;
    }

    if (region instanceof Rectangle) {
      Rectangle boundRect = (Rectangle) region;
      double x = boundRect.x + random.nextDouble(0.0001, boundRect.width);
      double y = boundRect.y + random.nextDouble(0.0001, boundRect.height);
      return new Point((int) (x + 0.5), (int) (y + 0.5));
    }

    Rectangle r = region.getBounds();

    double x, y;
    do {
      x = r.getX() + r.getWidth() * random.nextDouble();
      y = r.getY() + r.getHeight() * random.nextDouble();
    } while (!region.contains(x, y));

    return new Point((int) (x + 0.5), (int) (y + 0.5));
  }

  public static Point selectMiddle(Shape clickbox, double offset) {
    double dx = ThreadLocalRandom.current().nextDouble(-offset, offset);
    double dy = ThreadLocalRandom.current().nextDouble(-offset, offset);
    Rectangle2D bounds = clickbox.getBounds2D();
    return new Point((int) (bounds.getCenterX() + dx), (int) (bounds.getCenterY() + dy));
  }
}
