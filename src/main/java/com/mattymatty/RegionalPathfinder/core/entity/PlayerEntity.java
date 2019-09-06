package com.mattymatty.RegionalPathfinder.core.entity;

import com.mattymatty.RegionalPathfinder.api.entity.Entity;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.util.Vector;

import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

public class PlayerEntity implements Entity {

    private final Vector[] allowedMovements = new Vector[]{
            //front
            new Vector(1, 0, 0),
            //left
            new Vector(0, 0, 1),
            //back
            new Vector(-1, 0, 0),
            //right
            new Vector(0, 0, -1),
            //front-left
            new Vector(1, 0, 1),
            //front-right
            new Vector(1, 0, -1),
            //back-right
            new Vector(-1, 0, -1),
            //back-left
            new Vector(-1, 0, 1),
            //up-front
            new Vector(1, 1, 0),
            //up-left
            new Vector(0, 1, 1),
            //up-back
            new Vector(-1, 1, 0),
            //up-right
            new Vector(0, 1, -1),
            //down
            new Vector(1, -1, 0),
            new Vector(0, -1, 1),
            new Vector(-1, -1, 0),
            new Vector(0, -1, -1)
    };
    private final MovementCost movementCost = new MovementCost();

    @Override
    public boolean isValidLocation(Location loc) {
        if (isValidFlyLocation(loc)) return false;
        Block block;

        block = cloneLoc(loc).add(0, -1, 0).getBlock();
        return (
                !block.isPassable() &&
                        (!(block.getBlockData() instanceof Door)) &&
                        (!(block.getBlockData() instanceof TrapDoor))
        );
    }

    private boolean isValidFlyLocation(Location loc) {
        Block block = loc.getBlock();
        if (!(
                block.isPassable() ||
                        (block.getBlockData() instanceof Door) ||
                        (block.getBlockData() instanceof TrapDoor)
        ))
            return true;

        block = cloneLoc(loc).add(0, 1, 0).getBlock();
        return !(
                block.isPassable() ||
                        (block.getBlockData() instanceof Door) ||
                        (block.getBlockData() instanceof TrapDoor)
        );
    }

    @Override
    public Vector[] getAllowedMovements() {
        return allowedMovements;
    }

    @Override
    public boolean extraMovementChecks(Location start, Location end) {
        int dx = end.getBlockX() - start.getBlockX();
        int dy = end.getBlockY() - start.getBlockY();
        int dz = end.getBlockZ() - start.getBlockZ();

        if ((Math.abs(dx) + Math.abs(dy) + Math.abs(dz)) == 1)
            return true;

        if (dy > 0) {
            Block block = cloneLoc(start).add(0, 2, 0).getBlock();
            return (block.isPassable() || (block.getBlockData() instanceof Door) || (block.getBlockData() instanceof TrapDoor));
        } else if (dy < 0) {
            Block block = cloneLoc(end).add(0, 2, 0).getBlock();
            return (block.isPassable() || (block.getBlockData() instanceof Door) || (block.getBlockData() instanceof TrapDoor));
        }

        Location t1 = new Location(start.getWorld(), Math.max(start.getBlockX(), end.getBlockX()), start.getBlockY(), Math.max(start.getBlockZ(), end.getBlockZ()));
        Location t2 = new Location(start.getWorld(), Math.min(start.getBlockX(), end.getBlockX()), start.getBlockY(), Math.min(start.getBlockZ(), end.getBlockZ()));
        Location t3 = new Location(start.getWorld(), Math.max(start.getBlockX(), end.getBlockX()), start.getBlockY(), Math.min(start.getBlockZ(), end.getBlockZ()));
        Location t4 = new Location(start.getWorld(), Math.min(start.getBlockX(), end.getBlockX()), start.getBlockY(), Math.max(start.getBlockZ(), end.getBlockZ()));

        return isValidFlyLocation(t1) && isValidFlyLocation(t2) && isValidFlyLocation(t3) && isValidFlyLocation(t4);
    }

    @Positive
    @PositiveOrZero
    @Override
    public double movementCost(Location start, Location end) {
        return cost(movementCost, cloneLoc(start).add(0, -1, 0), cloneLoc(end).add(0, -1, 0));
    }

    private Location cloneLoc(Location loc) {
        return new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());
    }


}
