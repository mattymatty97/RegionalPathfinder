package com.mattymatty.RegionalPathfinder.core.graph;

import org.jgrapht.graph.DefaultWeightedEdge;

public class Edge extends DefaultWeightedEdge {

    private double weight;

    @Override
    protected double getWeight() {
        return weight;
    }

    public Edge setWeight(double weight) {
        this.weight = weight;
        return this;
    }
}
