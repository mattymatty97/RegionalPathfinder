package com.mattymatty.RegionalPathfinder.api.region;

import com.mattymatty.RegionalPathfinder.api.cost.MovementCost;
import com.mattymatty.RegionalPathfinder.api.entity.Entity;
import com.mattymatty.RegionalPathfinder.core.loader.Loader;
import org.bukkit.Location;

public interface BaseRegion extends Region {

    Location[] setCorners(Location c1, Location c2);


    Location setSamplePoint(Location sa);

    Location getSamplePoint();


    Loader setLoader(Loader loader);

    Loader getLoader();


    MovementCost setMovementCost(MovementCost cost);

    MovementCost getMovementCost();

    Entity setEntity(Entity entity);
    Entity getEntity();




}
