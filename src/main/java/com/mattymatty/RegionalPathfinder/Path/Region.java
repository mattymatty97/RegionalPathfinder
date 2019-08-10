package com.mattymatty.RegionalPathfinder.Path;

import org.bukkit.Location;
import org.bukkit.World;

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

    //the corners of this region ( 4 if lvl 1, more if higher lvl but always multiples of 4 )
    Location[] getCorners();

    //if this region is ready to be used
    boolean isValid();

    //if it is not valid this will provide a list of the reasons
    String[] getErrors();


    Iterable<Location> getPath(Location start, Location end);

    Status getAsyncPath(Location start, Location end, Callable<Iterable<Location>> callback);

}
