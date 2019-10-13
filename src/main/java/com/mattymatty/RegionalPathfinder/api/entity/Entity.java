package com.mattymatty.RegionalPathfinder.api.entity;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.util.Vector;

import javax.validation.constraints.Positive;
import java.util.HashMap;
import java.util.Map;


public interface Entity {

    //checks if the entity can stand onto this location
    boolean isValidLocation(Location loc);

    //returns a list of possible movements
    Vector[] getAllowedMovements();

    //performs extra checks for non standard movements
    boolean extraMovementChecks(Location start, Location end);

    //returns the cost of a move
    @Positive
    double movementCost(Location start, Location end);

    //this is a default implementation of movementCost, source and dest typically are NOT the start and end location
    //for Players or NPC are the block under start or end
    @Positive
    default double cost(MovementCost cost, Location source, Location dest) {
        double result = 0;

        int dx = dest.getBlockX() - source.getBlockX();
        int dy = dest.getBlockY() - source.getBlockY();
        int dz = dest.getBlockZ() - source.getBlockZ();

        if (Math.abs(dy) == 0) {
            result += cost.getDefaultMovement();
        } else {
            if (isStairMovement(source.clone(), dest.clone())) {
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

    default boolean isStairMovement(Location start, Location end) {
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

    class MovementCost {
        //cost of a normal move
        @Positive
        private final double defaultMovement;
        //additional cost of a diagonal move ( never be 0 or it will pathfind at ZigZag)
        @Positive
        private final double diagonalAddition;
        //the cost of climbing or descending a block
        @Positive
        private final double jump;
        //the cost of climbing or descending a block that's a stair took right or a slab
        @Positive
        private final double stair_slab;
        //additional costs to add if dest is a specific block ( strings are Block Keys )
        private final Map<String, Double> blockCosts;

        //default implementation normal Player
        public MovementCost() {
            this.defaultMovement = 1;
            this.diagonalAddition = 0.5;
            this.jump = 5;
            this.stair_slab = 3;
            this.blockCosts = new HashMap<>();
            blockCosts.put("minecraft:soul_sand", 7.0);
            blockCosts.put("minecraft:grass_block", 5.0);
            blockCosts.put("minecraft:dirt", 5.0);
        }

        public MovementCost(double defaultMovement, double diagonalAddition, double jump, double stair_slab, Map<String, Double> blockCosts) {
            this.defaultMovement = defaultMovement;
            this.diagonalAddition = diagonalAddition;
            this.jump = jump;
            this.stair_slab = stair_slab;
            this.blockCosts = blockCosts;
        }

        @Positive
        public double getDefaultMovement() {
            return defaultMovement;
        }

        @Positive
        public double getDiagonalAddition() {
            return diagonalAddition;
        }

        @Positive
        public double getJump() {
            return jump;
        }

        @Positive
        public double getStair_slab() {
            return stair_slab;
        }

        @Positive
        public double getBlockCost(Material material) {
            return blockCosts.getOrDefault(material.getKey().toString(), 0.0);
        }

    }
}
