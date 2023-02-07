package com.gestankbratwurst;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class CompletionTask<T> {

  private final CompletableFuture<T> future;
  private final Supplier<T> supplier;

  public CompletionTask(CompletableFuture<T> future, Supplier<T> supplier) {
    this.future = future;
    this.supplier = supplier;
  }

  public void completeOnCurrentThread() {
    future.complete(supplier.get());
  }

}
