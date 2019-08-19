package com.mattymatty.RegionalPathfinder.core;

import com.mattymatty.RegionalPathfinder.api.Status;
import org.bukkit.Location;

import java.util.List;
import java.util.Observable;

public class StatusImpl extends Status{
    int status=0;

    List<Location> path;

    @Override
    public boolean isScheduled() {
        return status==1;
    }

    @Override
    public boolean isRunning() {
        return status==2;
    }

    @Override
    public boolean isDone() {
        return status==3;
    }

    @Override
    public Iterable<Location> getPath() {
        return path;
    }

    public StatusImpl setStatus(int status) {
        this.status = status;
        changed();
        return this;
    }

    public StatusImpl setPath(List<Location> path) {
        this.path = path;
        return this;
    }

    private void changed(){
        this.setChanged();
        this.notifyObservers();
    }
}
