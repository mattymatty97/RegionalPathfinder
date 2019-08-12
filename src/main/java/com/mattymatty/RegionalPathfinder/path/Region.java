package com.mattymatty.RegionalPathfinder.path;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.concurrent.Callable;

/*
    THIS CLASS IS THE PUBLIC INTERFACE THAT USERS SHOULD USE
 */

public interface Region {

    int getID();

    String getName();

    //from lvl 1 ( block driven pathfinding ) to higher levels
    int getLevel();

    //the world this region is into
    World getWorld();

    //the corners of this region ( 6 if lvl 1, more if higher lvl but always multiples of 6 )
    Location[] getCorners();


    //a getter for all the locations where the entity can stand
    List<Location> getValidLocations();

    //a getter for all the locations where the entity can stand near a point
    List<Location> getValidLocations(Location center, int radius);


    //a getter for all the locations where the entity can stand
    List<Location> getReachableLocations();

    //a getter for all the locations where the entity can stand near a point
    List<Location> getReachableLocations(Location center, int radius);


    //if this region is ready to be used
    boolean isValid();

    //if it is not valid this will provide a list of the reasons
    String[] getErrors();


    Iterable<Location> getPath(Location start, Location end);

    Status getAsyncPath(Location start, Location end, Callable<Iterable<Location>> callback);

    static Region createRegion(String name,RegionType type){
        if(type==RegionType.BASE)
            return new BaseRegion(name);
        return null;
    }

}
