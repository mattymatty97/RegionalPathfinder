package com.mattymatty.RegionalPathfinder.core.entity;

import com.mattymatty.RegionalPathfinder.api.entity.Entity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

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
        Block block = loc.getBlock();
        if (!(
                block.isPassable() ||
                        (block.getBlockData() instanceof Door) ||
                        (block.getBlockData() instanceof TrapDoor)
        ))
            return false;

        block = cloneLoc(loc).add(0, 1, 0).getBlock();
        if (!(
                block.isPassable() ||
                        (block.getBlockData() instanceof Door) ||
                        (block.getBlockData() instanceof TrapDoor)
        ))
            return false;

        block = cloneLoc(loc).add(0, -1, 0).getBlock();
        return (
                !block.isPassable() &&
                        (!(block.getBlockData() instanceof Door)) &&
                        (!(block.getBlockData() instanceof TrapDoor))
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

        return isValidLocation(t1) && isValidLocation(t2) && isValidLocation(t3) && isValidLocation(t4);
    }

    @Override
    public double movementCost(Location start, Location end) {
        return cost(movementCost, cloneLoc(start).add(0, -1, 0), cloneLoc(end).add(0, -1, 0));
    }

    private double cost(MovementCost cost, Location source, Location dest) {
        double result = 0;

        int dx = dest.getBlockX() - source.getBlockX();
        int dy = dest.getBlockY() - source.getBlockY();
        int dz = dest.getBlockZ() - source.getBlockZ();

        if (Math.abs(dy) == 0) {
            result += cost.getDefaultMovement();
        } else {
            if (isStairMovement(source, dest)) {
                result += cost.getStair_slab();
            } else {
                result += cost.getJump();
            }
        }

        if (dx != 0 && dz != 0)
            result += cost.getDiagonalAddition();

        result += cost.getBlockCost(dest.getBlock().getType());

        return result;
    }

    private boolean isStairMovement(Location start, Location end) {
        int dx = end.getBlockX() - start.getBlockX();
        int dy = end.getBlockY() - start.getBlockY();
        int dz = end.getBlockZ() - start.getBlockZ();
        Block block;
        if (dy == 1) {
            block = end.getBlock();
            if (block.getBlockData() instanceof Slab)
                return true;
            if (block.getBlockData() instanceof Stairs) {
                Stairs stair = (Stairs) block.getBlockData();
                if (stair.getHalf() == Bisected.Half.TOP)
                    return false;
                Vector direction = stair.getFacing().getOppositeFace().getDirection();
                if (stair.getShape() == Stairs.Shape.STRAIGHT)
                    return direction.equals(new Vector(dx, 0, dz));
                if (stair.getShape() == Stairs.Shape.OUTER_LEFT)
                    return direction.equals(new Vector(dx, 0, dz)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(-90)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(-45));
                if (stair.getShape() == Stairs.Shape.OUTER_RIGHT)
                    return direction.equals(new Vector(dx, 0, dz)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(90)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(45));
                if (stair.getShape() == Stairs.Shape.INNER_LEFT)
                    return direction.equals(new Vector(dx, 0, dz).rotateAroundY(-45));
                if (stair.getShape() == Stairs.Shape.INNER_RIGHT)
                    return direction.equals(new Vector(dx, 0, dz).rotateAroundY(45));
            }
        } else if (dy == -1) {
            block = start.getBlock();
            if (block.getBlockData() instanceof Slab)
                return true;
            if (block.getBlockData() instanceof Stairs) {
                Stairs stair = (Stairs) block.getBlockData();
                if (stair.getHalf() == Bisected.Half.TOP)
                    return false;
                Vector direction = stair.getFacing().getDirection();
                if (stair.getShape() == Stairs.Shape.STRAIGHT)
                    return direction.equals(new Vector(dx, 0, dz));
                if (stair.getShape() == Stairs.Shape.OUTER_LEFT)
                    return direction.equals(new Vector(dx, 0, dz)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(-90)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(-45));
                if (stair.getShape() == Stairs.Shape.OUTER_RIGHT)
                    return direction.equals(new Vector(dx, 0, dz)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(90)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(45));
                if (stair.getShape() == Stairs.Shape.INNER_LEFT)
                    return direction.equals(new Vector(dx, 0, dz).rotateAroundY(-45));
                if (stair.getShape() == Stairs.Shape.INNER_RIGHT)
                    return direction.equals(new Vector(dx, 0, dz).rotateAroundY(45));
            }
        }
        return false;
    }

    private Location cloneLoc(Location loc) {
        return new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());
    }

    private class MovementCost {

        private final double defaultMovement;
        private final double diagonalAddition;
        private final double jump;
        private final double stair_slab;
        private final Map<String, Double> blockCosts;

        public MovementCost() {
            this.defaultMovement = 1;
            this.diagonalAddition = 0.5;
            this.jump = 3;
            this.stair_slab = 2;
            this.blockCosts = new HashMap<>();
            blockCosts.put("minecraft:soul_sand", 7.0);
            blockCosts.put("minecraft:grass_block", 5.0);
            blockCosts.put("minecraft:dirt", 5.0);
        }

        public double getDefaultMovement() {
            return defaultMovement;
        }

        public double getDiagonalAddition() {
            return diagonalAddition;
        }

        public double getJump() {
            return jump;
        }

        public double getStair_slab() {
            return stair_slab;
        }

        public double getBlockCost(Material material) {
            return blockCosts.getOrDefault(material.getKey().toString(), 0.0);
        }

    }
}
