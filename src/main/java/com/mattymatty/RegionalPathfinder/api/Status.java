package com.mattymatty.RegionalPathfinder.api;

import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public interface Status<T> {
    boolean isScheduled();

    boolean isRunning();

    boolean isDone();

    boolean hasException();

    Exception getException();

    long getTimeSync();

    long getTimeTotal();

    float getPercentage();

    T getProduct();

    Plugin setPlugin(Plugin plugin);

    Status<T> setOnSchedule(Runnable onSchedule);
    Status<T> setOnSyncSchedule(Runnable onSyncSchedule);

    Status<T> setOnProgress(Consumer<Float> onProgress);
    Status<T> setOnSyncProgress(Consumer<Float> onSyncProgress);

    Status<T> setOnDone(Consumer<T> onDone);
    Status<T> setOnSyncDone(Consumer<T> onSyncDone);

    Status<T> setOnException(Consumer<Exception> onException);
    Status<T> setOnSyncException(Consumer<Exception> onSyncException);


}
