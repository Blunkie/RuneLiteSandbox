package com.gestankbratwurst;

public class FutureTickTask {

  private int ticksLeft;
  private final Runnable action;

  public FutureTickTask(int delayTicks, Runnable action) {
    this.ticksLeft = delayTicks;
    this.action = action;
  }

  public boolean tick() {
    if(--ticksLeft == 0) {
      action.run();
      return true;
    }
    return false;
  }

}
