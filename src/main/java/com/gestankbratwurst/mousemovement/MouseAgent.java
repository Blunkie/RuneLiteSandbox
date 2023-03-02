package com.gestankbratwurst.mousemovement;

import com.gestankbratwurst.RuneLiteAddons;
import com.github.joonasvali.naturalmouse.api.MouseMotionFactory;
import com.github.joonasvali.naturalmouse.support.MouseMotionNature;
import com.github.joonasvali.naturalmouse.support.ScreenAdjustedNature;
import com.github.joonasvali.naturalmouse.util.FactoryTemplates;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;

import java.awt.AWTException;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

public class MouseAgent {

  private static final long MIN_CLICK_DOWN_MS = 40;
  private static final long MAX_CLICK_DOWN_MS = 120;

  private final MouseMotionFactory fastMotionFactory;
  private final MouseMotionFactory motionFactory;
  private final Robot robot;
  private ExecutorService mouseExecutor = Executors.newSingleThreadExecutor();
  private final RuneLiteAddons addons;

  public MouseAgent(RuneLiteAddons addons) {
    this.addons = addons;
    Canvas canvas = addons.getClient().getCanvas();
    Dimension screenSize = canvas.getSize();
    Point screenLoc = canvas.getLocationOnScreen();
    System.out.println("ScreenLoc: " + screenLoc.x + " / " + screenLoc.y);
    MouseMotionNature nature = new ScreenAdjustedNature(screenSize, screenLoc);
    this.motionFactory = FactoryTemplates.createAverageComputerUserMotionFactory(nature);
    this.fastMotionFactory = FactoryTemplates.createFastGamerMotionFactory(nature);
    try {
      this.robot = new Robot();
    } catch (AWTException e) {
      throw new RuntimeException(e);
    }
  }

  private MouseMotionFactory getMotionFactory() {
    return addons.getConfig().useFastMouse() ? fastMotionFactory : motionFactory;
  }

  public synchronized Future<Boolean> randomCamMove(double magnitude) {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    Point current = getMotionFactory().getMouseInfo().getMousePosition();
    Point point = new Point();
    point.x = current.x + random.nextInt((int) (-48 * magnitude), (int) (48 * magnitude));
    point.y = current.y + random.nextInt((int) (-12 * magnitude), (int) (16 * magnitude));

    return mouseExecutor.submit(() -> {
      try {
        robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
        getMotionFactory().move(point.x, point.y);
        robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
        return true;
      } catch (InterruptedException e) {
        return false;
      }
    });
  }

  public Future<Boolean> randomCamMove() {
    return randomCamMove(1.0);
  }

  public synchronized void interruptMouse() {
    mouseExecutor.shutdownNow();
    mouseExecutor = Executors.newSingleThreadExecutor();
  }

  public Future<Boolean> moveMouseTo(Point point) {
    return mouseExecutor.submit(() -> {
      try {
        getMotionFactory().move(point.x, point.y);
        return true;
      } catch (InterruptedException e) {
        return false;
      }
    });
  }

  public Future<Boolean> leftClick() {
    return mouseExecutor.submit(() -> {
      long holdClick = ThreadLocalRandom.current().nextLong(MIN_CLICK_DOWN_MS, MAX_CLICK_DOWN_MS);
      robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      try {
        Thread.sleep(holdClick);
      } catch (InterruptedException e) {
        return false;
      }
      robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
      return true;
    });
  }

  public Future<Boolean> rightClick() {
    return mouseExecutor.submit(() -> {
      long holdClick = ThreadLocalRandom.current().nextLong(MIN_CLICK_DOWN_MS, MAX_CLICK_DOWN_MS);
      robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
      try {
        Thread.sleep(holdClick);
      } catch (InterruptedException e) {
        return false;
      }
      robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
      return true;
    });
  }

  public Future<Boolean> pressKey(int keyCode) {
    return mouseExecutor.submit(() -> {
      long holdClick = ThreadLocalRandom.current().nextLong(MIN_CLICK_DOWN_MS * 2, MAX_CLICK_DOWN_MS * 2);
      robot.keyPress(keyCode);
      try {
        Thread.sleep(holdClick);
      } catch (InterruptedException e) {
        return false;
      }
      robot.keyRelease(keyCode);
      return true;
    });
  }
}
