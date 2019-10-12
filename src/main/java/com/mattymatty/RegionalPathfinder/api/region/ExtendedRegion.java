package com.mattymatty.RegionalPathfinder.api.region;

import com.mattymatty.RegionalPathfinder.api.Status;
import org.bukkit.Location;

import javax.validation.constraints.Positive;
import java.util.List;
import java.util.Set;

public interface ExtendedRegion extends Region {

    //adds a sub-region to this extended region
    Status<Region[]> addRegion(Region region, @Positive double weightMultiplier, Set<Location> excludedWayPoints);

    //adds a sub-region to this extended region
    Status<Region[]> addRegion(Region region, @Positive double weightMultiplier);

    //wrapper for backwise compatibility ( equals to addRegion( region , 1.0 ); )
    Status<Region[]> addRegion(Region region, Set<Location> excludedWayPoints);

    //wrapper for backwise compatibility ( equals to addRegion( region , 1.0 ); )
    Status<Region[]> addRegion(Region region);

    //removes a sub-region from this extended region
    Status<Region[]> removeRegion(Region region);

    //gets the intersection points between each sub-region
    List<Location> getIntersections();

    List<Region> getUnconnectedRegions();
}
