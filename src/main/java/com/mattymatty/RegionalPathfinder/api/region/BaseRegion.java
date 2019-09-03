package com.mattymatty.RegionalPathfinder.api.region;

import com.mattymatty.RegionalPathfinder.api.Status;
import org.bukkit.Location;

public interface BaseRegion extends Region {

    //set the corners of a cubic sized zone containing the region
    Location[] setCorners(Location c1, Location c2);

    //set the point used as sample to prune unreachable locations
    Location setSamplePoint(Location sa);

    //load the cubic zone extracting all the valid regions ( based on current Entity )
    Status<Location[]> load();

    //prune all unreachable locations using as start point the "samplePoint"
    Status<Location> evaluate();

}
