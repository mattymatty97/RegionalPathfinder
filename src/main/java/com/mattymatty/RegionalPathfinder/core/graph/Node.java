package com.mattymatty.RegionalPathfinder.core.graph;


import org.bukkit.Location;

public class Node {
    Location loc;
    int i;

    public Node(Location loc, int i) {
        this.loc = loc;
        this.i = i;
    }

    public Node(Location loc) {
        this.loc = loc;
    }

    public Location getLoc() {
        return loc;
    }

    public int getI() {
        return i;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Node))
            return false;
        Node comp = (Node)obj;

        return comp.loc == this.loc;
    }
}
