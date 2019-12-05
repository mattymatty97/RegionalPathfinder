package com.mattymatty.RegionalPathfinder.core.region;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mattymatty.RegionalPathfinder.Logger;
import com.mattymatty.RegionalPathfinder.RegionalPathfinder;
import com.mattymatty.RegionalPathfinder.api.Status;
import com.mattymatty.RegionalPathfinder.api.entity.Entity;
import com.mattymatty.RegionalPathfinder.api.region.ExtendedRegion;
import com.mattymatty.RegionalPathfinder.api.region.Region;
import com.mattymatty.RegionalPathfinder.core.StatusImpl;
import com.mattymatty.RegionalPathfinder.core.graph.Edge;
import com.mattymatty.RegionalPathfinder.core.graph.Node;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.json.JSONObject;

import javax.validation.constraints.Positive;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class MergedRegionImpl implements ExtendedRegion, RegionImpl {


    private final int id;

    private final String name;

    private Graph<Node, Edge> graph = GraphTypeBuilder.<Node, Edge>directed().weighted(true).edgeClass(Edge.class).allowingSelfLoops(true).buildGraph();

    private Map<Location, Node> nodeMap = new HashMap<>();

    private Set<RegionImpl> regions = new HashSet<>();

    private boolean changed = false;

    private Set<Location> reachable = new HashSet<>();

    private Cache<Node, ShortestPathAlgorithm.SingleSourcePaths<Node, Edge>> sourceCache = CacheBuilder.newBuilder().softValues()
            .maximumSize(15).build();

    public MergedRegionImpl(String name) {
        this.name = name;
        this.id = RegionImpl.nextID.getAndIncrement();
    }

    @Override
    public Status<Region[]> addRegion(Region region, @Positive double weightMultiplier, Set<Location> excludedWayPoints) {
        StatusImpl<Region[]> status = new StatusImpl<>();
        status.setStatus(1);
        RegionalPathfinder.getInstance().executor.execute(() -> {
                    if (region instanceof BaseRegionImpl) {
                        BaseRegionImpl baseRegion = (BaseRegionImpl) region;
                        RegionalPathfinder.getInstance().runningThreads.add(Thread.currentThread());
                        long tic = System.currentTimeMillis();
                        try {
                            status.setStatus(2);
                            if (regions.contains(region) || !region.isValid()) {
                                status.setProduct(regions.toArray(new Region[]{}));
                                status.totTime = (System.currentTimeMillis() - tic);
                                status.setStatus(3);
                                return;
                            }

                            Graph<Node, Edge> sub_graph = baseRegion.loadData.getGraph();
                            sub_graph.vertexSet().forEach(node -> nodeMap.put(node.getLoc(), node));
                            sourceCache.invalidateAll();
                            Graphs.addGraph(graph, sub_graph);
                            regions.add(baseRegion);
                            changed = true;
                        } catch (Exception ex) {
                            status.ex = ex;
                            status.totTime = (System.currentTimeMillis() - tic);
                            status.setStatus(4);
                        } finally {
                            RegionalPathfinder.getInstance().runningThreads.remove(Thread.currentThread());
                        }
                    } else {
                        status.setProduct(regions.toArray(new Region[]{}));
                        status.setStatus(3);
                    }
                }
        );
        return status;
    }

    @Override
    public Status<Region[]> addRegion(Region region, @Positive double weightMultiplier) {
        return addRegion(region, weightMultiplier, new HashSet<>());
    }

    @Override
    public Status<Region[]> addRegion(Region region, Set<Location> excludedWayPoints) {
        return addRegion(region, 1, excludedWayPoints);
    }

    @Override
    public Status<Region[]> addRegion(Region region) {
        return addRegion(region, 1, new HashSet<>());
    }

    @Override
    public Status<Region[]> removeRegion(Region region) {
        return null;
    }

    @Override
    public List<Location> getIntersections() {
        return new ArrayList<>();
    }

    @Override
    public List<Region> getUnconnectedRegions() {
        return new ArrayList<>();
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getLevel() {
        return regions.stream().mapToInt(Region::getID).max().orElse(-1) + 1;
    }

    @Override
    public World getWorld() {
        if (regions.iterator().hasNext())
            return regions.iterator().next().getWorld();
        else
            return null;
    }

    @Override
    public Location[] getCorners() {
        return (Location[]) regions.stream().flatMap(r -> Arrays.stream(r.getCorners())).toArray();
    }

    @Override
    public Location getMinCorner() {
        return regions.stream().flatMap(r -> Arrays.stream(r.getCorners())).min(Comparator.comparing(Location::hashCode)).orElse(null);
    }

    @Override
    public Location getMaxCorner() {
        return regions.stream().flatMap(r -> Arrays.stream(r.getCorners())).max(Comparator.comparing(Location::hashCode)).orElse(null);
    }

    @Override
    public Set<Location> getValidLocations() {
        return regions.stream().flatMap(region -> region.getValidLocations().stream()).collect(Collectors.toSet());
    }

    @Override
    public Set<Location> getValidLocations(Location center, int range) {
        return regions.stream().flatMap(region -> region.getValidLocations(center, range).stream()).collect(Collectors.toSet());
    }

    @Override
    public Set<Location> getReachableLocations() {
        if (!changed)
            return new HashSet<>(reachable);
        changed = false;
        StrongConnectivityAlgorithm<Node, Edge> sca = new KosarajuStrongConnectivityInspector<>(graph);
        AtomicReference<Node> to_reach = null;
        regions.stream().findFirst().ifPresent(region -> to_reach.set(((BaseRegionImpl) region).loadData.getNode(((BaseRegionImpl) region).loadData.samplePoint)));
        if (to_reach.get() != null) {
            reachable = new HashSet<>();
            Optional<Graph<Node, Edge>> opt = sca.getStronglyConnectedComponents().stream().filter(nodeEdgeGraph -> nodeEdgeGraph.containsVertex(to_reach.get())).findAny();
            opt.ifPresent(nodeEdgeGraph -> reachable = nodeEdgeGraph.vertexSet().stream().map(Node::getLoc).collect(Collectors.toSet()));
        }
        return new HashSet<>(reachable);
    }

    @Override
    public Set<Location> getReachableLocations(Location center, int range) {
        return regions.stream().flatMap(region -> region.getReachableLocations(center, range).stream()).collect(Collectors.toSet());
    }

    @Override
    public Set<Location> getReachableLocations(Location center, int x_range, int y_range, int z_range) {
        return regions.stream().flatMap(region -> region.getReachableLocations(center, x_range, y_range, z_range).stream()).collect(Collectors.toSet());
    }

    @Override
    public boolean isInRegion(Location location) {
        return getValidLocations().contains(location);
    }

    @Override
    public boolean isValidLocation(Location location) {
        return isInRegion(location);
    }

    @Override
    public boolean isReachableLocation(Location location) {
        return getReachableLocations().contains(location);
    }

    @Override
    public Entity setEntity(Entity entity) {
        throw new RuntimeException("Not yet implemented for Merged Regions");
    }

    @Override
    public Entity getEntity() {
        if (regions.iterator().hasNext())
            return regions.iterator().next().getEntity();
        else
            return null;
    }

    @Override
    public boolean isValid() {
        return !regions.isEmpty();
    }

    @Override
    public Status<Boolean> validate() {
        StatusImpl<Boolean> status = new StatusImpl<>();
        status.setStatus(3);
        status.setProduct(true);
        return status;
    }

    @Override
    public Status<Path> getPath(Location start, Location end) {
        StatusImpl<Path> status = new StatusImpl<>();
        RegionalPathfinder.getInstance().executor.execute(() -> {
            RegionalPathfinder.getInstance().runningThreads.add(Thread.currentThread());
            boolean locked = false;
            long tic = System.currentTimeMillis();
            try {
                status.setStatus(1);

                Location actual_s = new Location(start.getWorld(), start.getBlockX(), start.getBlockY(), start.getBlockZ()).add(0.5, 0.5, 0.5);
                Location actual_e = new Location(end.getWorld(), end.getBlockX(), end.getBlockY(), end.getBlockZ()).add(0.5, 0.5, 0.5);

                Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () -> {
                    Logger.info("Pathfinding in region: " + getName());
                    Logger.fine("from: X=" + actual_s.getBlockX() + " Y=" + actual_s.getBlockY() + " Z=" + actual_s.getBlockZ());
                    Logger.fine("to: X=" + actual_e.getBlockX() + " Y=" + actual_e.getBlockY() + " Z=" + actual_e.getBlockZ());
                });

                Set<Location> valid = getValidLocations();

                if (!valid.contains(actual_s) || !valid.contains(actual_e)) {
                    status.totTime = (System.currentTimeMillis() - tic);
                    status.setStatus(3);
                    RegionalPathfinder.getInstance().runningThreads.remove(Thread.currentThread());
                    return;
                }

                Node sNode = nodeMap.get(actual_s);
                Node eNode = nodeMap.get(actual_e);
                GraphPath<Node, Edge> path;

                status.setStatus(2);

                path = getNodeEdgeGraphPath(sNode, eNode);

                status.totTime = (System.currentTimeMillis() - tic);
                status.setProduct((path != null) ? new Path(path.getVertexList().stream().map(Node::getLoc).map(Location::clone).collect(Collectors.toList()), path.getWeight()) : null);

                long toc = System.currentTimeMillis();
                Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () -> {
                    Logger.info(((path != null) ? "Found path" : "Failed pathfinding") + " in region: " + getName());
                    Logger.fine("Took: " + (toc - tic) + " ms");
                });
                status.setStatus(3);
            } catch (Exception ex) {
                status.ex = ex;
                status.totTime = (System.currentTimeMillis() - tic);
                status.setStatus(4);
            }
            RegionalPathfinder.getInstance().runningThreads.remove(Thread.currentThread());
        });
        return status;
    }

    private GraphPath<Node, Edge> getNodeEdgeGraphPath(Node sNode, Node eNode) {
        GraphPath<Node, Edge> path;
        ShortestPathAlgorithm.SingleSourcePaths<Node, Edge> iPaths;
        DijkstraShortestPath<Node, Edge> dijkstraShortestPath = new DijkstraShortestPath<>(graph);
        iPaths = sourceCache.getIfPresent(sNode);
        if (iPaths == null) {
            iPaths = dijkstraShortestPath.getPaths(sNode);
            sourceCache.put(sNode, iPaths);
        }
        path = iPaths.getPath(eNode);
        return path;
    }

    @Override
    public void fromJson(JSONObject json) {

    }

    @Override
    public void toJson(File baseCache, File extendedCache) throws IOException {

    }

    @Override
    public Path _getPath(Location start, Location end) {
        Location actual_s = new Location(start.getWorld(), start.getBlockX(), start.getBlockY(), start.getBlockZ()).add(0.5, 0.5, 0.5);
        Location actual_e = new Location(end.getWorld(), end.getBlockX(), end.getBlockY(), end.getBlockZ()).add(0.5, 0.5, 0.5);

        Node sNode = nodeMap.get(actual_s);
        Node eNode = nodeMap.get(actual_e);
        GraphPath<Node, Edge> path = null;

        try {
            path = getNodeEdgeGraphPath(sNode, eNode);
        } catch (Exception ignored) {
        }

        return (path != null) ? new Path(path.getVertexList().stream().map(Node::getLoc).collect(Collectors.toList()), path.getWeight()) : null;
    }

    @Override
    public void delete() {

    }

    @Override
    public void invalidate() {

    }

    @Override
    public void referencer(RegionImpl region) {

    }

    @Override
    public ExtendedRegion asExtendedRegion() {
        return this;
    }
}
