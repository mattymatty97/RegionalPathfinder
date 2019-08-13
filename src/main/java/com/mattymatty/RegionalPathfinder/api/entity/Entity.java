package com.mattymatty.RegionalPathfinder.api.entity;

import org.bukkit.Location;
import org.bukkit.block.Block;

public interface Entity {
    boolean isValidLocation(Location loc);

    int allowedMovement(int X, int Y, int Z);

    Block evaluableBlock(Location valid);
}
