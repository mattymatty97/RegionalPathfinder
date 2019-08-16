package com.mattymatty.RegionalPathfinder.api.entity;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public interface Entity {

    //checks if the entity can stand onto this location
    boolean isValidLocation(Location loc);

    //returns a list of possible movements
    Vector[] getAllowedMovements();

    //performs extra checks for non standard movements
    boolean extraMovementChecks(Location start, Location end);

    //returns the cost of a move
    double movementCost(Location start, Location end);
}
