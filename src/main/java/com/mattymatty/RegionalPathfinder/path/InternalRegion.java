package com.mattymatty.RegionalPathfinder.path;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/*
    THIS ARE THE METHODS THAT EVERY REGION SHOULD HAVE
 */
public abstract class InternalRegion implements Region {

    static AtomicInteger nextID = new AtomicInteger(1);

    //a check for intersection locations
    public List<Location> getIntersection(InternalRegion region) {
        List<Location> common = new ArrayList<>(region.getReachableLocations());
        common.retainAll(this.getReachableLocations());
        return common;
    }

    //a cancellation method
    public abstract void delete();
}
