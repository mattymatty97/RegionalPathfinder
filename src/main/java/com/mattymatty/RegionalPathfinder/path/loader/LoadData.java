package com.mattymatty.RegionalPathfinder.path.loader;

import com.mattymatty.RegionalPathfinder.graph.Graph;
import com.mattymatty.RegionalPathfinder.path.BlockNode;
import com.mattymatty.RegionalPathfinder.path.MovementCost;
import com.mattymatty.RegionalPathfinder.path.entity.Entity;
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

    public HashMap<Location, BlockNode> getNodesMap() {
        return nodesMap;
    }

    public LoadData(Location upperCorner, Location lowerCorner, Graph graph, MovementCost cost) {
        this.upperCorner = upperCorner;
        this.lowerCorner = lowerCorner;
        this.graph = graph;
        this.cost = cost;
        this.x_size = upperCorner.getBlockX() - lowerCorner.getBlockX();
        this.y_size = upperCorner.getBlockY() - lowerCorner.getBlockY();
        this.z_size = upperCorner.getBlockZ() - lowerCorner.getBlockZ();
        this.status = null;
        this.nodesMap = new HashMap<>();
        this.map = new int[x_size][y_size][z_size];
        for (int i = 0; i < (x_size * y_size * z_size); i++) {
            map
                    [(((i % z_size) % y_size) / x_size)]
                    [((i % z_size) / y_size)]
                    [(i / z_size)] = 1;
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
