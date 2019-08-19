package com.mattymatty.RegionalPathfinder.api;

import org.bukkit.Location;

import java.util.Observable;

public abstract class Status extends Observable {
    public abstract boolean isScheduled();

    public abstract boolean isRunning();

    public abstract boolean isDone();

    public abstract Iterable<Location> getPath();
}
