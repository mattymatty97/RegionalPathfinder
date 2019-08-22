package com.mattymatty.RegionalPathfinder.api.region;

import com.mattymatty.RegionalPathfinder.api.Status;
import com.mattymatty.RegionalPathfinder.core.region.RegionImpl;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;

/*
    THIS CLASS IS THE PUBLIC INTERFACE THAT USERS SHOULD USE
 */

public interface Region {

    static Region createRegion(String name, RegionType type) {
        return RegionImpl.createRegion(name, type);
    }

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

    //a method to redo all the loading,evaluating and verifing
    Status load();

    Status evaluate();

    Status validate();

    Status<List<Location>> getPath(Location start, Location end);

}
