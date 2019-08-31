package com.mattymatty.RegionalPathfinder.api.region;

import com.mattymatty.RegionalPathfinder.api.Status;

public interface ExtendedRegion extends Region {

    Status<Region[]> addRegion(Region region);

    Status<Region[]> removeRegion(Region region);

}
