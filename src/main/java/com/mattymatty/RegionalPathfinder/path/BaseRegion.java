package com.mattymatty.RegionalPathfinder.path;

import com.mattymatty.RegionalPathfinder.RegionalPathfinder;
import com.mattymatty.RegionalPathfinder.graph.Graph;
import com.mattymatty.RegionalPathfinder.path.entity.Entity;
import com.mattymatty.RegionalPathfinder.path.entity.PlayerEntity;
import com.mattymatty.RegionalPathfinder.path.loader.LoadData;
import com.mattymatty.RegionalPathfinder.path.loader.Loader;
import com.mattymatty.RegionalPathfinder.path.loader.SynchronousLoader;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

public class BaseRegion extends InternalRegion {

    private final int ID;

    private String Name;

    private List<Integer> errors = new ArrayList<>();

    private LoadData loadData = new LoadData(null,null,new Graph(),new MovementCost());

    private Loader loader = new SynchronousLoader();


    //METHODS FROM INTERFACE


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
        return (loadData.lowerCorner==null)?null:loadData.lowerCorner.getWorld();
    }

    @Override
    public Location[] getCorners() {
        return null;
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
        return loadData.getStatus() == LoadData.Status.VALIDATED;
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

    //METHODS FROM INTERNAL
    @Override
    public void delete() {}



    //LOCAL METHODS

    private void load(){
        loader.load(loadData);
    }

    private void evaluate(){
        loader.evaluate(loadData);
    }

    public void validate(){
        loader.validate(loadData);
    }

    public Location[] setCorners(Location c1,Location c2){
        if(c1.getWorld() == c2.getWorld()){
            loadData.lowerCorner= new Location(c1.getWorld(),Math.min(c1.getX(),c2.getX()),Math.min(c1.getY(),c2.getY()),Math.min(c1.getZ(),c2.getZ()));
            loadData.upperCorner= new Location(c1.getWorld(),Math.max(c1.getX(),c2.getX()),Math.max(c1.getY(),c2.getY()),Math.max(c1.getZ(),c2.getZ()));
            load();
            return new Location[]{loadData.lowerCorner,loadData.upperCorner};
        }
        return null;
    }

    public Location setSamplePoint(Location sa){
        if(sa.getWorld() == loadData.upperCorner.getWorld()){
            loadData.samplePoint = sa;
            evaluate();
        }
        return null;
    }

    BaseRegion(String name) {
        this.ID = InternalRegion.nextID.getAndIncrement();
        Name = name;
    }



    //internal methods here


    //internal classes here
}
