package com.mattymatty.RegionalPathfinder.core.loader;

import com.mattymatty.RegionalPathfinder.core.StatusImpl;
import com.mattymatty.RegionalPathfinder.core.graph.Node;
import org.bukkit.Location;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public interface Loader<T> {
    //max dimensions 1290 blocks each size


    void load(LoadData data, StatusImpl<T[]> status);

    void evaluate(LoadData data, StatusImpl<T> status);

    void validate(LoadData data, StatusImpl<Boolean> status);

    default List<Location> getValid(LoadData data) {

        if (data.graph == null)
            return null;

        List<Location> result = data.graph.vertexSet().parallelStream().map(Node::getLoc).collect(Collectors.toCollection(LinkedList::new));

        return (result.isEmpty()) ? null : result;
    }

    default List<Location> getReachable(LoadData data) {
        if (data.reachableGraph == null)
            return null;

        List<Location> result = data.reachableGraph.vertexSet().parallelStream().map(Node::getLoc).collect(Collectors.toCollection(LinkedList::new));

        return (result.isEmpty()) ? null : result;
    }

    default Location cloneLoc(Location loc) {
        return new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());
    }
}
