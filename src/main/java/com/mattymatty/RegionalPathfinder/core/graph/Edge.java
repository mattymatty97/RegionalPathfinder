package com.mattymatty.RegionalPathfinder.core.graph;

import org.bukkit.Location;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.List;

public class Edge extends DefaultWeightedEdge {

    private Node source;

    private Node dest;

    private List<Location> path;

    @Override
    public Node getSource() {
        return source;
    }

    public Edge setSource(Node source) {
        this.source = source;
        return this;
    }

    public Node getDest() {
        return dest;
    }

    public Edge setDest(Node dest) {
        this.dest = dest;
        return this;
    }

    public List<Location> getPath() {
        return path;
    }

    public Edge setPath(List<Location> path) {
        this.path = path;
        return this;
    }
}
