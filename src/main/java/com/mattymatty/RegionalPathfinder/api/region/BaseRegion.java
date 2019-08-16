package com.mattymatty.RegionalPathfinder.api.region;

import com.mattymatty.RegionalPathfinder.api.entity.Entity;
import org.bukkit.Location;

public interface BaseRegion extends Region {

    Location[] setCorners(Location c1, Location c2);


    Location setSamplePoint(Location sa);

    Location getSamplePoint();

    Entity setEntity(Entity entity);
    Entity getEntity();




}
