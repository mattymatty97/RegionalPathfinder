package com.mattymatty.RegionalPathfinder.core.graph;


import org.bukkit.Location;

public class Node {
    Location loc;
    int i;

    public Node(Location loc, int i) {
        this.loc = loc;
        this.i = i;
    }

    public Location getLoc() {
        return loc;
    }

    public int getI() {
        return i;
    }
}
