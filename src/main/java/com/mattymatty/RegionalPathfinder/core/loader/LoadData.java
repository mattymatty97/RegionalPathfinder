package com.mattymatty.RegionalPathfinder.core.loader;

import com.mattymatty.RegionalPathfinder.core.graph.Graph;
import com.mattymatty.RegionalPathfinder.core.graph.BlockNode;
import com.mattymatty.RegionalPathfinder.api.cost.MovementCost;
import com.mattymatty.RegionalPathfinder.api.entity.Entity;
import org.bukkit.Location;

import java.util.HashMap;

public class LoadData {
    //data from Region
    public Location upperCorner;
    public Location lowerCorner;
    public Location samplePoint;
    public Entity entity;
    Graph graph;
    public MovementCost cost;

    //generated datas;
    HashMap<Location, BlockNode> nodesMap;
    Status status;
    int x_size;
    int y_size;
    int z_size;
    int[][][] map;

    public Status getStatus() {
        return status;
    }

    public int getX_size() {
        return x_size;
    }

    public int getY_size() {
        return y_size;
    }

    public int getZ_size() {
        return z_size;
    }

    public int[][][] getMap() {
        return map;
    }

    public Graph getGraph() {
        return graph;
    }

    public HashMap<Location, BlockNode> getNodesMap() {
        return nodesMap;
    }

    public LoadData(Location upperCorner, Location lowerCorner, Graph graph, MovementCost cost, Entity entity) {
        this.upperCorner = upperCorner;
        this.lowerCorner = lowerCorner;
        this.graph = graph;
        this.cost = cost;
        this.entity = entity;
        this.x_size = upperCorner.getBlockX() - lowerCorner.getBlockX();
        this.y_size = upperCorner.getBlockY() - lowerCorner.getBlockY();
        this.z_size = upperCorner.getBlockZ() - lowerCorner.getBlockZ();
        this.status = null;
        this.nodesMap = new HashMap<>();
        this.map = new int[x_size][y_size][z_size];
        for (int i = 0; i < (x_size * y_size * z_size); i++) {
            int y = (( i / x_size ) / z_size ) % y_size;
            int z = ( i / x_size ) % z_size;
            int x = i % x_size;
            map[x][y][z] = 1;
        }
    }

    public enum Status implements Comparable<Status> {
        LOADING(0),
        LOADED(1),
        EVALUATING(2),
        EVALUATED(3),
        VALIDATING(4),
        VALIDATED(5);

        final int value;

        Status(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
