package com.mattymatty.RegionalPathfinder.api.region;

import com.mattymatty.RegionalPathfinder.api.Status;
import org.bukkit.Location;

public interface BaseRegion extends Region {

    Location[] setCorners(Location c1, Location c2);


    Location setSamplePoint(Location sa);

    Location getSamplePoint();

    //a method to redo all the loading,evaluating and verifing
    Status<Location[]> load();

    Status<Location> evaluate();


}
