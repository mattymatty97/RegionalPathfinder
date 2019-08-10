package com.mattymatty.RegionalPathfinder.Path;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/*
    THIS ARE THE METHODS THAT EVERY REGION SHOULD HAVE
 */
abstract class InternalRegion implements Region {

        static AtomicInteger nextID = new AtomicInteger(1);

        //a check for intersection locations
        List<Location> getIntersection(InternalRegion region){
            List<Location> common = new ArrayList<>(region.getAllowed());
            common.retainAll(this.getAllowed());
            return common;
        };

        //a getter for all the "pathfindable" locations
        abstract List<Location> getAllowed();

        //a getter for all the "pathfindable" locations near a point
        abstract List<Location> getAllowed(Location center, int radius);

        //a validate command
        abstract boolean tryValidate();

        //a cancellation method
        abstract void delete();
}
