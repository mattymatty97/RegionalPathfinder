package com.mattymatty.RegionalPathfinder.core.region;

import com.mattymatty.RegionalPathfinder.api.region.Region;
import com.mattymatty.RegionalPathfinder.api.region.RegionType;
import com.mattymatty.RegionalPathfinder.core.loader.Loader;
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
        return null;
    }

    //a check for intersection locations
    default Iterable<Location> getIntersection(RegionImpl region) {
        List<Location> common = new LinkedList<Location>(region.getReachableLocations());
        common.retainAll(this.getReachableLocations());
        return common;
    }

    Loader setLoader(Loader loader);

    Loader getLoader();

    Path _getPath(Location start, Location end);

    //a cancellation method
    void delete();
}
