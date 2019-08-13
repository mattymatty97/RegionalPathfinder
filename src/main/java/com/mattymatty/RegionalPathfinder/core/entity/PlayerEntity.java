package com.mattymatty.RegionalPathfinder.core.entity;

import com.google.common.collect.Lists;
import com.mattymatty.RegionalPathfinder.api.entity.Entity;
import org.bukkit.Location;
import org.bukkit.Material;

import static java.lang.Math.abs;
import static org.bukkit.Material.*;
import org.bukkit.block.Block;

import java.util.List;

public class PlayerEntity implements Entity {
    
    private final List<Material> blockExceptions = Lists.newArrayList(
            ACACIA_DOOR,
            BIRCH_DOOR,
            DARK_OAK_DOOR,
            IRON_DOOR,
            JUNGLE_DOOR,
            JUNGLE_DOOR,
            OAK_DOOR,
            SPRUCE_DOOR,
            ACACIA_TRAPDOOR,
            BIRCH_TRAPDOOR,
            DARK_OAK_TRAPDOOR,
            IRON_TRAPDOOR,
            JUNGLE_TRAPDOOR,
            JUNGLE_TRAPDOOR,
            OAK_TRAPDOOR,
            SPRUCE_TRAPDOOR
    );

    private final int[][][] allowedMovement = {
            {{0,1,0},{1,1,1},{0,1,0}},
            {{1,0,1},{1,0,1},{1,0,1}},
            {{0,1,0},{1,1,1},{0,1,0}}
    };
    
    @Override
    public boolean isValidLocation(Location loc) {
        Block block = loc.getBlock();
        if (!(block.isPassable() && !blockExceptions.contains(block.getType())))
            return false;
        
        block = cloneLoc(loc).add(0,1,0).getBlock();
        if (!(block.isPassable() && !blockExceptions.contains(block.getType())))
            return false;

        block = cloneLoc(loc).add(0,-1,0).getBlock();
        if (block.isPassable() && !blockExceptions.contains(block.getType()))
            return false;
        
        return true;
    }

    @Override
    public int allowedMovement(int X, int Y, int Z) {
        if(abs(X)>1 || abs(Y)>1 || abs(Z)>1)
            return 0;
        int x = X+1;
        int y = Y+1;
        int z = Z+1;
        return allowedMovement[x][y][z];
    }

    @Override
    public Block evaluableBlock(Location valid) {
        return cloneLoc(valid).add(0,-1,0).getBlock();
    }

    private Location cloneLoc(Location loc){
        return new Location(loc.getWorld(),loc.getX(),loc.getY(),loc.getZ());
    }
}
