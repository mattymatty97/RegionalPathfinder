package com.mattymatty.RegionalPathfinder.api;

import org.bukkit.Location;

public class Status {
    int status = 0;
    Iterable<Location> path = null;

    public boolean isScheduled() {
        return status == 1;
    }

    public boolean isRunning() {
        return status == 2;
    }

    public boolean isDone() {
        return status == 3;
    }

    public Iterable<Location> getPath() {
        return path;
    }

    Status setStatus(int status) {
        this.status = status;
        return this;
    }

    Status setPath(Iterable<Location> path) {
        this.path = path;
        return this;
    }
}
