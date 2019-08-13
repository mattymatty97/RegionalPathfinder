package com.mattymatty.RegionalPathfinder.api.cost;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class MovementCost {

    private final double defaultMovement;
    private final double diagonalAddition ;
    private final double jump;
    private final double stair_slab ;
    private final Map<Material,Double> blockCosts;

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

    public double getBlockCost(Material material){
        return blockCosts.getOrDefault(material,0.0);
    }

    public MovementCost() {
        this.defaultMovement = 1;
        this.diagonalAddition = 0;
        this.jump = 1;
        this.stair_slab = 1;
        this.blockCosts = new HashMap<>();
    }

    public MovementCost(double defaultMovement, double diagonalAddition, double jump, double stair_slab, Map<Material, Double> blockCosts) {
        this.defaultMovement = defaultMovement;
        this.diagonalAddition = diagonalAddition;
        this.jump = jump;
        this.stair_slab = stair_slab;
        this.blockCosts = blockCosts;
    }

}
