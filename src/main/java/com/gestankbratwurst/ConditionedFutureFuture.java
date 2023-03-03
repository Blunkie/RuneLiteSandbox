package com.gestankbratwurst;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Builder
@AllArgsConstructor
public class ConditionedFutureFuture<T> {

  private final Predicate<T> predicate;
  private final CompletableFuture<T> future;

  public void completeTimeOut() {
    if (!future.isDone()) {
      future.complete(null);
    }
  }

  public boolean checkCompletion(T element) {
    if(future.isDone()) {
      return true;
    }
    if (predicate.test(element)) {
      future.complete(element);
      return true;
    } else {
      return false;
    }
  }

}
