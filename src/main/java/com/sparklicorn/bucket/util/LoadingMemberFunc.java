package com.sparklicorn.bucket.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class LoadingMemberFunc<T,V> {
  protected AtomicBoolean isLoading;
  protected AtomicBoolean hasLoaded;
  protected Function<T,V> getter;
  protected V value;

  public LoadingMemberFunc(Function<T,V> getter) {
    this.getter = getter;
    this.isLoading = new AtomicBoolean();
    this.hasLoaded = new AtomicBoolean();
    this.value = null;
  }

  public synchronized V get(T input) {
    if (!hasLoaded.get()) {
      isLoading.set(true);
      value = getter.apply(input);
      isLoading.set(false);
      hasLoaded.set(true);
    }

    return value;
  }

  public synchronized V reload(T input) {
    hasLoaded.set(false);
    return get(input);
  }

  public boolean isLoading() {
    return isLoading.get();
  }

  public boolean hasLoaded() {
    return hasLoaded.get();
  }
}
