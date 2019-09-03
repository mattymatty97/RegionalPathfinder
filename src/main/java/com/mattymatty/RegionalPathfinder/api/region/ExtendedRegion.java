package com.mattymatty.RegionalPathfinder.api.region;

import com.mattymatty.RegionalPathfinder.api.Status;
import org.bukkit.Location;

import java.util.List;

public interface ExtendedRegion extends Region {

    Status<Region[]> addRegion(Region region);

    Status<Region[]> removeRegion(Region region);

    List<Location> getIntersections();
}
