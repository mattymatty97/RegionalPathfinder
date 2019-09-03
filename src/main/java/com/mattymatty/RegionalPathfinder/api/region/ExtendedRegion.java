package com.mattymatty.RegionalPathfinder.api.region;

import com.mattymatty.RegionalPathfinder.api.Status;
import org.bukkit.Location;

import java.util.List;

public interface ExtendedRegion extends Region {

    //adds a sub-region to this extended region
    Status<Region[]> addRegion(Region region);

    //removes a sub-region from this extended region
    Status<Region[]> removeRegion(Region region);

    //gets the intersection points between each sub-region
    List<Location> getIntersections();
}
