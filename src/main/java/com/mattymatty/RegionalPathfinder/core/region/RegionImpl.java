package com.mattymatty.RegionalPathfinder.core.region;

import com.mattymatty.RegionalPathfinder.api.region.Region;
import com.mattymatty.RegionalPathfinder.api.region.RegionType;
import org.bukkit.Location;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/*
    THIS ARE THE METHODS THAT EVERY REGION SHOULD HAVE
 */
public interface RegionImpl extends Region {

    AtomicInteger nextID = new AtomicInteger(1);

    //static creator
    static Region createRegion(String name, RegionType type) {
        if (type == RegionType.BASE)
            return new BaseRegionImpl(name);
        if (type == RegionType.EXTENDED)
            return new ExtendedRegionImpl(name);
        return null;
    }

    default List<Location> _getIntersection(Region region) {
        List<Location> common = new LinkedList<Location>(region.getReachableLocations());
        common.retainAll(this.getReachableLocations());
        return common;
    }

    Path _getPath(Location start, Location end);

    //a cancellation method
    void delete();

    void invalidate();

    void referencer(RegionImpl region);
}
