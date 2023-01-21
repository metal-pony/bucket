package com.sparklicorn.bucket.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class LoadingMember<T> {
  protected AtomicBoolean isLoading;
  protected AtomicBoolean hasLoaded;
  protected Supplier<T> getter;
  protected T value;

  public LoadingMember(Supplier<T> getter) {
    this.getter = getter;
    this.isLoading = new AtomicBoolean();
    this.hasLoaded = new AtomicBoolean();
    this.value = null;
  }

  public synchronized T get() {
    if (!hasLoaded.get()) {
      isLoading.set(true);
      value = getter.get();
      isLoading.set(false);
      hasLoaded.set(true);
    }

    return value;
  }

  public synchronized T reload() {
    hasLoaded.set(false);
    return get();
  }

  public boolean isLoading() {
    return isLoading.get();
  }

  public boolean hasLoaded() {
    return hasLoaded.get();
  }
}
