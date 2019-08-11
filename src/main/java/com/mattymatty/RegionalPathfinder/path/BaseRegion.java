package com.mattymatty.RegionalPathfinder.path;

import com.mattymatty.RegionalPathfinder.graph.Graph;
import com.mattymatty.RegionalPathfinder.path.entity.Entity;
import com.mattymatty.RegionalPathfinder.path.entity.PlayerEntity;
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

    private List<Integer> errors = new ArrayList<>();

    private Entity entity = new PlayerEntity();

    private Graph graph = new Graph();









    //PUBLIC METHOD HERE


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
    public List<Location> getValidLocations() {
        return null;
    }

    @Override
    public List<Location> getValidLocations(Location center, int radius) {
        return null;
    }

    @Override
    public List<Location> getReachableLocations() {
        return null;
    }

    @Override
    public List<Location> getReachableLocations(Location center, int radius) {
        return null;
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








    //PROTECTED METHODS HERE

    @Override
    boolean tryValidate() {
        return false;
    }

    @Override
    void delete() {

    }

    BaseRegion(String name) {
        this.ID = InternalRegion.nextID.getAndIncrement();
        Name = name;
    }



    //internal methods here


    //internal classes here
}
