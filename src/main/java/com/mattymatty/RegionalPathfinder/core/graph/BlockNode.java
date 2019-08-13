package com.mattymatty.RegionalPathfinder.core.graph;

import com.mattymatty.RegionalPathfinder.core.graph.Graph;

import org.bukkit.Location;

public class BlockNode extends Graph.Node {

    private final Location location;

    public Location getLocation() {
        return location;
    }

    /**
     * Node class
     * Each node is a point of interest on the map
     *
     * @param graph
     * @param n_id  local id used for pathfinding algorithm
     */
    public BlockNode(Graph graph, int n_id,Location location) {
        super(graph, n_id);
        this.location = location;
    }
}
