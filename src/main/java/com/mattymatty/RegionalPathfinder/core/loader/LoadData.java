package com.mattymatty.RegionalPathfinder.core.loader;

import com.mattymatty.RegionalPathfinder.core.graph.Edge;
import com.mattymatty.RegionalPathfinder.core.graph.Node;
import org.jgrapht.*;
import com.mattymatty.RegionalPathfinder.core.region.BaseRegionImpl;
import org.bukkit.Location;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;

import java.util.HashMap;
import java.util.Map;

public class LoadData {
    //data from Region
    public Location upperCorner;
    public Location lowerCorner;
    public Location samplePoint;
    public final BaseRegionImpl region;

    //generated datas;
    Graph<Node, Edge> graph;
    Graph<Node, Edge> reachableGraph;
    ShortestPathAlgorithm<Node, Edge> shortestPath;
    Map<Integer,Node> nodesMap;
    Status status;
    int x_size;
    int y_size;
    int z_size;

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

    public Node getNode(Location loc){
        int x = loc.getBlockX() - lowerCorner.getBlockX();
        int y = loc.getBlockY() - lowerCorner.getBlockY();
        int z = loc.getBlockZ() - lowerCorner.getBlockZ();
        int id = x + z * x_size + y * x_size * z_size;
        return nodesMap.get(id);
    }

    public Map<Integer, Node> getNodesMap() {
        return nodesMap;
    }

    public Graph<Node, Edge> getGraph() {
        return graph;
    }

    public Graph<Node, Edge> getReachableGraph() {
        return reachableGraph;
    }

    public ShortestPathAlgorithm<Node, Edge> getShortestPath() {
        return shortestPath;
    }

    public LoadData(BaseRegionImpl region, Location upperCorner, Location lowerCorner) {
        this.region = region;
        this.upperCorner = upperCorner;
        this.lowerCorner = lowerCorner;
        this.x_size = upperCorner.getBlockX() - lowerCorner.getBlockX();
        this.y_size = upperCorner.getBlockY() - lowerCorner.getBlockY();
        this.z_size = upperCorner.getBlockZ() - lowerCorner.getBlockZ();
        this.status = null;
        this.nodesMap = new HashMap<>();
    }

    public enum Status implements Comparable<Status>{
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
