package com.mattymatty.RegionalPathfinder.core.region;

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
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExtendedRegionImpl implements ExtendedRegion, RegionImpl {

    private final int id;

    private final String name;

    Map<RegionImpl, RegionWrapper> regions = new HashMap<RegionImpl, RegionWrapper>();

    Map<Node, Integer> waypointMap = new HashMap<>();

    Graph<Node, Edge> graph = GraphTypeBuilder.<Node, Edge>directed().edgeClass(Edge.class).weighted(true).buildGraph();

    private Lock lock = new ReentrantLock();

    private boolean valid = false;

    @Override
    public Status<Region[]> addRegion(Region region) {

        long tic = System.currentTimeMillis();
        StatusImpl<Region[]> status = new StatusImpl<>();
        RegionImpl reg = (RegionImpl) region;
        Bukkit.getScheduler().runTaskAsynchronously(RegionalPathfinder.getInstance(), () -> {
            boolean locked = false;
            try {
                status.setStatus(1);
                lock.lockInterruptibly();
                locked = true;
                status.setStatus(2);

                RegionWrapper rw = new RegionWrapper();
                rw.region = region;

                int size = regions.size();
                int i = 0;
                for (RegionImpl act : regions.keySet()) {
                    i++;
                    RegionWrapper actwr = regions.get(act);
                    List<Node> waypoints = act.getIntersection(reg).stream().map(Node::new).peek(n -> {
                        makeEdges(act, actwr, n);
                        makeEdges(act, rw, n);
                    }).collect(Collectors.toList());
                    addWaypoints(actwr, waypoints);
                    addWaypoints(rw, waypoints);
                    status.percentage = (float) i / size;
                    status.setStatus(2);
                }

                regions.put(reg, rw);

                status.setProduct(regions.keySet().toArray(new Region[]{}));
                status.totTime = (System.currentTimeMillis() - tic);
                status.setStatus(3);

                lock.unlock();

            } catch (Exception ex) {
                status.ex = ex;
                status.setStatus(4);
                if (locked)
                    lock.unlock();
            }
        });
        return status;
    }

    private void addWaypoints(RegionWrapper rw, List<Node> waypoints) {
        rw.waypoints.addAll(waypoints.stream().filter(w -> !rw.waypoints.contains(w)).peek(n->{
            Integer count = waypointMap.getOrDefault(n,null);
            if(count==null)
                count = 0;
            waypointMap.put(n,++count);
        }).collect(Collectors.toList()));
    }

    private void makeEdges(RegionImpl act, RegionWrapper actwr, Node n) {
        if (!actwr.waypoints.contains(n)) {
            if (!graph.containsVertex(n))
                graph.addVertex(n);
            actwr.waypoints.forEach(w -> {
                Path go = act._getPath(n.getLoc(), w.getLoc());
                Path ret = act._getPath(w.getLoc(), n.getLoc());
                Edge goE = graph.addEdge(n, w).setPath(go.getPath());
                graph.setEdgeWeight(goE, go.getWeight());
                Edge retE = graph.addEdge(w, n).setPath(ret.getPath());
                graph.setEdgeWeight(retE, ret.getWeight());
            });
        }
    }

    @Override
    public Status<Region[]> removeRegion(Region region) {
        long tic = System.currentTimeMillis();
        StatusImpl<Region[]> status = new StatusImpl<>();
        RegionImpl reg = (RegionImpl) region;
        Bukkit.getScheduler().runTaskAsynchronously(RegionalPathfinder.getInstance(), () -> {
            boolean locked = false;
            try {
                status.setStatus(1);
                lock.lockInterruptibly();
                locked = true;
                status.setStatus(2);

                RegionWrapper rw = regions.get(reg);
                regions.remove(reg);
                if (reg != null) {

                    int size = rw.waypoints.size();
                    int i = 0;
                    for (Node waypoint :
                            rw.waypoints) {
                        i++;
                        Integer count = waypointMap.getOrDefault(waypoint,null);
                        if(--count<2){
                            graph.removeVertex(waypoint);
                            waypointMap.remove(waypoint);
                        }else{
                            waypointMap.put(waypoint,count);
                            rw.waypoints.forEach(w->graph.removeEdge(waypoint,w));
                        }
                        status.percentage = (float) i / size;
                        status.setStatus(2);
                    }

                    regions.put(reg, rw);

                }
                status.setProduct(regions.keySet().toArray(new Region[]{}));
                status.totTime = (System.currentTimeMillis() - tic);
                status.setStatus(3);

                lock.unlock();

            } catch (Exception ex) {
                status.ex = ex;
                status.setStatus(4);
                if (locked)
                    lock.unlock();
            }
        });
        return status;
    }

    @Override
    public Path _getPath(Location start, Location end) {

        lock.lock();
        RegionImpl sreg = regions.keySet().stream().filter(r -> isReachableLocation(start)).findFirst().get();
        RegionImpl ereg = regions.keySet().stream().filter(r -> isReachableLocation(end)).findFirst().get();

        if (sreg == ereg)
            return sreg._getPath(start, end);

        RegionWrapper swr = regions.get(sreg);
        RegionWrapper ewr = regions.get(ereg);

        Node snode = new Node(start);
        Node enode = new Node(end);

        boolean sadded = false;
        boolean eadded = false;

        if (!swr.waypoints.contains(snode)) {
            makeEdges(sreg, swr, snode);
            sadded = true;
        }

        if (!ewr.waypoints.contains(enode)) {
            makeEdges(ereg, ewr, enode);
            eadded = true;
        }

        ShortestPathAlgorithm<Node, Edge> spa = new DijkstraShortestPath<>(graph);

        GraphPath<Node, Edge> path = spa.getPath(snode, enode);

        Stream<Location> locationStream = path.getEdgeList().stream().map(Edge::getPath).flatMap(l -> l.stream().skip(1));

        Path ret = new Path(Stream.concat(Stream.of(start), locationStream).collect(Collectors.toList()), path.getWeight());

        if (sadded)
            graph.removeVertex(snode);
        if (eadded)
            graph.removeVertex(enode);

        lock.unlock();
        return ret;
    }

    @Override
    public void delete() {

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
        return regions.keySet().stream().mapToInt(Region::getLevel).max().orElse(0);
    }

    @Override
    public World getWorld() {
        return regions.keySet().stream().map(Region::getWorld).findAny().orElse(null);
    }

    @Override
    public Location[] getCorners() {
        return (Location[]) regions.keySet().stream().flatMap(r -> Arrays.stream(r.getCorners())).toArray();
    }

    @Override
    public List<Location> getValidLocations() {
        return regions.keySet().stream().flatMap(r -> r.getValidLocations().stream()).distinct().collect(Collectors.toList());
    }

    @Override
    public List<Location> getValidLocations(Location center, int radius) {
        return null;
    }

    @Override
    public List<Location> getReachableLocations() {
        return regions.keySet().stream().flatMap(r -> r.getReachableLocations().stream()).distinct().collect(Collectors.toList());
    }

    @Override
    public List<Location> getReachableLocations(Location center, int radius) {
        return null;
    }

    @Override
    public boolean isInRegion(Location location) {
        return regions.keySet().stream().anyMatch(r -> r.isInRegion(location));
    }

    @Override
    public boolean isValidLocation(Location location) {
        return regions.keySet().stream().anyMatch(r -> r.isValidLocation(location));
    }

    @Override
    public boolean isReachableLocation(Location location) {
        return regions.keySet().stream().anyMatch(r -> r.isReachableLocation(location));
    }

    @Override
    public Entity setEntity(Entity entity) {
        regions.keySet().forEach(r -> r.setEntity(entity));
        return getEntity();
    }

    @Override
    public Entity getEntity() {
        return regions.keySet().stream().map(Region::getEntity).findAny().orElse(null);
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public Status<Boolean> validate() {
        long tic = System.currentTimeMillis();
        StatusImpl<Boolean> status = new StatusImpl<>();
        Bukkit.getScheduler().runTaskAsynchronously(RegionalPathfinder.getInstance(), () -> {
            boolean locked = false;
            try {
                status.setStatus(1);
                lock.lockInterruptibly();
                locked = true;
                status.setStatus(2);
                StrongConnectivityAlgorithm<Node, Edge> scAlg =
                        new KosarajuStrongConnectivityInspector<>(graph);
                status.setProduct(scAlg.isStronglyConnected());
                valid = status.getProduct();
                status.totTime = (System.currentTimeMillis() - tic);
                status.setStatus(3);
                lock.unlock();
            } catch (Exception ex) {
                status.ex = ex;
                status.setStatus(4);
                if (locked)
                    lock.unlock();
            }
        });
        return status;
    }

    @Override
    public Status<Path> getPath(Location start, Location end) {
        long tic = System.currentTimeMillis();
        StatusImpl<Path> status = new StatusImpl<>();
        Bukkit.getScheduler().runTaskAsynchronously(RegionalPathfinder.getInstance(), () -> {
            status.setStatus(1);
            boolean locked = false;
            try {
                status.setStatus(1);
                lock.lockInterruptibly();
                locked = true;
                status.setStatus(2);

                RegionImpl sreg = regions.keySet().stream().filter(r -> isReachableLocation(start)).findFirst().orElse(null);
                RegionImpl ereg = regions.keySet().stream().filter(r -> isReachableLocation(end)).findFirst().orElse(null);

                if (sreg == null || ereg == null) {
                    status.totTime = (System.currentTimeMillis() - tic);
                    status.setStatus(3);
                    lock.unlock();
                    return;
                }

                status.percentage = 0.001f;
                status.setStatus(2);

                if (sreg == ereg) {
                    status.setProduct(sreg._getPath(start, end));
                    status.totTime = (System.currentTimeMillis() - tic);
                    status.setStatus(3);
                    lock.unlock();
                    return;
                }

                RegionWrapper swr = regions.get(sreg);
                RegionWrapper ewr = regions.get(ereg);

                Node snode = new Node(start);
                Node enode = new Node(end);

                boolean sadded = false;
                boolean eadded = false;

                status.percentage = 0.01f;
                status.setStatus(2);

                if (!swr.waypoints.contains(snode)) {
                    makeEdges(sreg, swr, snode);
                    sadded = true;
                }


                status.percentage = 0.2f;
                status.setStatus(2);

                if (!ewr.waypoints.contains(enode)) {
                    makeEdges(ereg, ewr, enode);
                    eadded = true;
                }

                status.percentage = 0.5f;
                status.setStatus(2);

                ShortestPathAlgorithm<Node, Edge> spa = new DijkstraShortestPath<>(graph);

                GraphPath<Node, Edge> path = spa.getPath(snode, enode);

                Stream<Location> locationStream = path.getEdgeList().stream().map(Edge::getPath).flatMap(l -> l.stream().skip(1));

                Path ret = new Path(Stream.concat(Stream.of(start), locationStream).collect(Collectors.toList()), path.getWeight());

                status.totTime = (System.currentTimeMillis() - tic);
                status.setProduct(ret);
                status.setStatus(3);

                if (sadded)
                    graph.removeVertex(snode);
                if (eadded)
                    graph.removeVertex(enode);

                lock.unlock();
            } catch (Exception ex) {
                status.ex = ex;
                status.setStatus(4);
                if (locked)
                    lock.unlock();
            }
        });
        return status;
    }

    private static class RegionWrapper {
        Region region;
        List<Node> waypoints = new LinkedList<>();
    }

    public ExtendedRegionImpl(String name) {
        id = RegionImpl.nextID.getAndIncrement();
        this.name = name;
    }
}
