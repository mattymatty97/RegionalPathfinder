package com.mattymatty.RegionalPathfinder.Path;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

class BaseRegion extends InternalRegion {
    private final int ID;
    private String Name;
    private World World;
    private Location[] Corners;
    private boolean valid=false;
    private List<Integer> erors = new ArrayList<>();

    @Override
    public int getID() {
        return ID;
    }

    @Override
    public String getName() {
        return Name;
    }

    @Override
    public int getLevel() {
        return 1;
    }

    @Override
    public World getWorld() {
        return World;
    }

    @Override
    public Location[] getCorners() {
        return Corners;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public String[] getErrors() {
        return new String[0];
    }

    @Override
    public Iterable<Location> getPath(Location start, Location end) {
        return null;
    }

    @Override
    public Status getAsyncPath(Location start, Location end, Callable<Iterable<Location>> callback) {
        return null;
    }

    @Override
    List<Location> getAllowed() {
        return null;
    }

    @Override
    List<Location> getAllowed(Location center, int radius) {
        return null;
    }

    @Override
    boolean tryValidate() {
        return false;
    }

    @Override
    void delete() {

    }

    public BaseRegion(String name) {
        this.ID = InternalRegion.nextID.getAndIncrement();
        Name = name;
    }
}
