package com.mattymatty.RegionalPathfinder.api;

import java.util.function.Consumer;

public interface Status<T> {
    boolean isScheduled();

    boolean isRunning();

    boolean isDone();

    boolean hasException();

    Exception getException();

    long getTimeSync();

    long getTimeTotal();

    T getProduct();

    Status<T> addListener(Consumer<Status<T>> callback);

    Status<T> clearListeners();
}
