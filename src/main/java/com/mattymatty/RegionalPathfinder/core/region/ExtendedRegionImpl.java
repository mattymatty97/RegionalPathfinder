package com.mattymatty.RegionalPathfinder.core.region;

import com.mattymatty.RegionalPathfinder.Logger;
import com.mattymatty.RegionalPathfinder.RegionalPathfinder;
import com.mattymatty.RegionalPathfinder.api.Status;
import com.mattymatty.RegionalPathfinder.api.entity.Entity;
import com.mattymatty.RegionalPathfinder.api.region.ExtendedRegion;
import com.mattymatty.RegionalPathfinder.api.region.Region;
import com.mattymatty.RegionalPathfinder.core.StatusImpl;
import com.mattymatty.RegionalPathfinder.core.graph.Edge;
import com.mattymatty.RegionalPathfinder.core.graph.Node;
import com.mattymatty.RegionalPathfinder.exeptions.RegionException;
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

import javax.validation.constraints.Positive;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExtendedRegionImpl implements ExtendedRegion, RegionImpl {

    public final Map<Integer, Map<Integer, Map<Integer, Location>>> reachableLocationsMap = new HashMap<>();
    private long min_x = Long.MAX_VALUE;
    private long min_y = Long.MAX_VALUE;
    private long min_z = Long.MAX_VALUE;
    private long max_x = Long.MIN_VALUE;
    private long max_y = Long.MIN_VALUE;
    private long max_z = Long.MIN_VALUE;

    public static boolean sync = true;

    private final int id;

    private final String name;

    private Map<RegionImpl, RegionWrapper> regions = new HashMap<RegionImpl, RegionWrapper>();

    private Map<Node, Integer> waypointMap = new HashMap<>();

    private Graph<Node, Edge> graph = GraphTypeBuilder.<Node, Edge>directed().edgeClass(Edge.class).allowingSelfLoops(true).weighted(true).buildGraph();

    private Map<Location, Node> nodeMap = new HashMap<>();

    private Lock lock = new ReentrantLock();

    private boolean valid = false;

    private final ShortestPathAlgorithm<Node, Edge> spa = new DijkstraShortestPath<>(graph);
    private List<WeakReference<RegionImpl>> backreferences = new LinkedList<>();

    @Override
    public Status<Region[]> addRegion(Region region) {
        return addRegion(region, 1.0);
    }

    private Set<Location> reachableLocations = new HashSet<>();

    @Override
    public Status<Region[]> addRegion(Region region, @Positive double weightMultiplier) {
        return addRegion(region, weightMultiplier, new HashSet<>());
    }

    private void addWaypoints(RegionWrapper rw, Set<Node> waypoints) {
        waypoints.stream().filter((n) -> !rw.waypoints.contains(n)).forEach(n -> {
            if (!graph.containsVertex(n)) {
                graph.addVertex(n);
            }
            Integer count = waypointMap.getOrDefault(n, null);
            if (count == null)
                count = 0;
            waypointMap.put(n, ++count);
        });
        rw.waypoints.addAll(waypoints);
    }

    private void addWaypoint(RegionWrapper rw, Node waypoint) {
        Integer count = waypointMap.getOrDefault(waypoint, null);
        if (count == null)
            count = 0;
        waypointMap.put(waypoint, ++count);
        rw.waypoints.add(waypoint);
    }

    @Override
    public Status<Region[]> addRegion(Region region, Set<Location> excludedWaypoints) {
        return addRegion(region, 1.0, excludedWaypoints);
    }

    @Override
    public Status<Region[]> addRegion(Region region, @Positive double weightMultiplier, Set<Location> excludedWaypoints) {
        if (regions.containsKey(region))
            return null;
        if (excludedWaypoints == null)
            excludedWaypoints = new HashSet<>();
        long tic = System.currentTimeMillis();
        StatusImpl<Region[]> status = new StatusImpl<>();
        RegionImpl reg = (RegionImpl) region;
        if (sync) {
            try {
                status.setStatus(1);
                Logger.info("Adding region " + region.getName() + " to " + this.getName());
                _addRegion(tic, status, reg, weightMultiplier, excludedWaypoints);
                Logger.info("Successfully added region " + region.getName() + " to " + this.getName());
            } catch (Exception ex) {
                Logger.info("Failed adding region " + region.getName() + " to " + this.getName());
                status.ex = ex;
                status.setStatus(4);
            }
        } else {
            Set<Location> finalExcludedWaypoints = excludedWaypoints;
            new Thread(() -> {
                boolean locked = false;
                try {
                    status.setStatus(1);
                    lock.lockInterruptibly();
                    locked = true;
                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () ->
                            Logger.info("Adding region " + region.getName() + " to " + this.getName()));
                    _addRegion(tic, status, reg, weightMultiplier, finalExcludedWaypoints);
                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () ->
                            Logger.info("Successfully added region " + region.getName() + " to " + this.getName()));
                    lock.unlock();
                } catch (Exception ex) {
                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () ->
                            Logger.info("Failed adding region " + region.getName() + " to " + this.getName()));
                    status.ex = ex;
                    status.setStatus(4);
                    if (locked)
                        lock.unlock();
                }
            }).start();
        }
        return status;
    }

    private void _addRegion(long tic, StatusImpl<Region[]> status, RegionImpl reg, double multiplier, Set<Location> excludedWaypoints) {
        status.setStatus(2);
        RegionWrapper rw = new RegionWrapper();
        rw.region = reg;
        rw.multiplier = multiplier;

        Map<RegionImpl, Set<Location>> intersectionMap = new HashMap<>();

        this._getIntersection(reg)
                .forEach(
                        (l) -> regions.keySet().stream().filter((r) -> r.isReachableLocation(l.clone())).forEach(
                        (r) -> {
                            intersectionMap.computeIfAbsent(r, (k) -> new HashSet<>()).add(l.clone());
                        }
                )
        );

        invalidate();

        int size = intersectionMap.size();
        int i = 0;
        for (RegionImpl act : intersectionMap.keySet()) {
            i++;
            RegionWrapper actrw = regions.get(act);
            Set<Node> waypoints = intersectionMap.get(act).stream().map(Location::clone).filter(
                    l -> !excludedWaypoints.contains(l)
            ).map(Node::new).collect(Collectors.toSet());
            waypoints.add(new Node(intersectionMap.get(act).toArray(new Location[]{})[0]));


            waypoints.forEach(w -> {
                makeEdges(act, actrw, w);
                makeEdges(reg, rw, w);
            });

            addWaypoints(actrw, waypoints);
            addWaypoints(rw, waypoints);

            status.percentage = (float) i / size;
            status.setStatus(2);
        }

        regions.put(reg, rw);
        reg.referencer(this);

        reg.getReachableLocations().forEach(this::computeLocations);

        reachableLocations.addAll(reg.getReachableLocations());
        status.setProduct(regions.keySet().toArray(new Region[]{}));
        status.totTime = (System.currentTimeMillis() - tic);
        status.setStatus(3);
    }

    private void makeEdges(RegionImpl region, RegionWrapper rw, Node n) {
        makeEdges(region, rw, n, 0);
    }

    private void makeEdges(RegionImpl region, RegionWrapper rw, Node n, int direction) {
        if (!graph.containsVertex(n)) {
            graph.addVertex(n);
        }
        rw.waypoints.stream().filter(w -> !w.equals(n)).forEach(w -> {
                Path go = null, ret = null;
                if (direction == 0 || direction == 1)
                    go = region._getPath(n.getLoc().clone(), w.getLoc().clone());
                if (direction == 0 || direction == 2)
                    ret = region._getPath(w.getLoc().clone(), n.getLoc().clone());

                if (direction == 0 || direction == 1) {
                    Edge goE = graph.addEdge(n, w);
                    if (goE == null)
                        goE = graph.getEdge(n, w);
                    goE.setPath(go.getPath());
                    graph.setEdgeWeight(goE, go.getWeight() * rw.multiplier);
                }

                if (direction == 0 || direction == 2) {
                    Edge retE = graph.addEdge(w, n);
                    if (retE == null)
                        retE = graph.getEdge(w, n);
                    retE.setPath(ret.getPath());
                    graph.setEdgeWeight(retE, ret.getWeight() * rw.multiplier);
                }
            });
    }

    @Override
    public Status<Region[]> removeRegion(Region region) {
        long tic = System.currentTimeMillis();
        StatusImpl<Region[]> status = new StatusImpl<>();
        RegionImpl reg = (RegionImpl) region;
        if (sync) {
            try {
                status.setStatus(1);
                Logger.info("Removing region " + region.getName() + " from " + this.getName());
                _removeRegion(tic, status, reg);
                Logger.info("Successfully removed region " + region.getName() + " from " + this.getName());
            } catch (Exception ex) {
                Logger.info("Failed removing region " + region.getName() + " from " + this.getName());
                status.ex = ex;
                status.setStatus(4);
            }
        } else
            new Thread(() -> {
                boolean locked = false;
                try {
                    status.setStatus(1);
                    lock.lockInterruptibly();
                    locked = true;
                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () ->
                            Logger.info("Removing region " + region.getName() + " from " + this.getName()));
                    _removeRegion(tic, status, reg);
                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () ->
                            Logger.info("Successfully removed region " + region.getName() + " from " + this.getName()));
                    lock.unlock();
                } catch (Exception ex) {
                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () ->
                            Logger.info("Failed removing region " + region.getName() + " from " + this.getName()));
                    status.ex = ex;
                    status.setStatus(4);
                    if (locked)
                        lock.unlock();
                }
            }).start();
        return status;
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
    public Path _getPath(Location start, Location end) {

        lock.lock();

        if (!isReachableLocation(start) || !isReachableLocation(end)) {
            return null;
        }

        RegionImpl sreg = (RegionImpl) getRegion(start.clone());
        RegionImpl ereg = (RegionImpl) getRegion(end.clone());

        if (sreg == ereg)
            return sreg._getPath(start.clone(), end.clone());

        RegionWrapper swr = regions.get(sreg);
        RegionWrapper ewr = regions.get(ereg);

        Location act_s = new Location(
                start.getWorld(),
                start.getBlockX() + 0.5,
                start.getBlockY() + 0.5,
                start.getBlockZ() + 0.5
        );

        Location act_e = new Location(
                end.getWorld(),
                end.getBlockX() + 0.5,
                end.getBlockY() + 0.5,
                end.getBlockZ() + 0.5
        );

        boolean sadded = false;
        boolean eadded = false;

        Node snode = nodeMap.get(act_s);

        if (snode == null) {
            snode = new Node(act_s.clone());
            sadded = true;
        }

        Node enode = nodeMap.get(act_e);

        if (enode == null) {
            enode = new Node(act_e.clone());
            eadded = true;
        }

        boolean sradded = false;
        boolean eradded = false;

        if (!swr.waypoints.contains(snode)) {
            makeEdges(sreg, swr, snode, 1);
            sradded = true;
        }

        if (!ewr.waypoints.contains(enode)) {
            makeEdges(ereg, ewr, enode, 2);
            eradded = true;
        }

        GraphPath<Node, Edge> path = spa.getPath(snode, enode);

        Stream<Location> locationStream = path.getEdgeList().stream().map(Edge::getPath).flatMap(l -> l.stream().skip(1));

        Path ret = new Path(Stream.concat(Stream.of(start), locationStream).map(Location::clone).collect(Collectors.toList()), path.getWeight());

        if (sadded)
            graph.removeVertex(snode);
        if (eadded)
            graph.removeVertex(enode);
        if (sradded && !sadded)
            removeEdges(swr, snode);
        if (eradded && !eadded)
            removeEdges(ewr, enode);

        lock.unlock();
        return ret;
    }

    @Override
    public World getWorld() {
        return regions.keySet().stream().map(Region::getWorld).findFirst().orElse(null);
    }

    @Override
    public Location[] getCorners() {
        return (Location[]) regions.keySet().stream().flatMap(r -> Arrays.stream(r.getCorners())).toArray();
    }

    @Override
    public Location getMinCorner() {
        return (min_x == Long.MAX_VALUE) ? null : new Location(getWorld(), min_x, min_y, min_z);
    }

    @Override
    public Location getMaxCorner() {
        return (max_x == Long.MIN_VALUE) ? null : new Location(getWorld(), max_x, max_y, max_z);
    }

    @Override
    public Set<Location> getValidLocations() {
        return regions.keySet().stream().flatMap(r -> r.getValidLocations().stream()).distinct().collect(Collectors.toSet());
    }

    @Override
    public Set<Location> getValidLocations(Location center, int range) {
        throw new RuntimeException("Not Yet implemented");
    }

    private Region getRegion(Location l) {
        return regions.keySet().stream().filter((r) -> r.isReachableLocation(l)).findFirst().orElse(null);
    }

    private void _removeRegion(long tic, StatusImpl<Region[]> status, RegionImpl reg) {
        status.setStatus(2);
        RegionWrapper rw = regions.get(reg);
        regions.remove(reg);
        if (rw != null) {
            invalidate();
            int size = rw.waypoints.size();
            int i = 0;
            for (Node waypoint :
                    rw.waypoints) {
                i++;
                Integer count = waypointMap.getOrDefault(waypoint, null);
                if (--count < 2) {
                    graph.removeVertex(waypoint);
                    waypointMap.remove(waypoint);
                    nodeMap.remove(waypoint.getLoc());
                } else {
                    waypointMap.put(waypoint, count);
                    rw.waypoints.forEach(w -> graph.removeEdge(waypoint, w));
                }
                status.percentage = (float) i / size;
                status.setStatus(2);
            }

            regions.put(reg, rw);
        }
        status.setProduct(regions.keySet().toArray(new Region[]{}));
        status.totTime = (System.currentTimeMillis() - tic);
        reachableLocations.clear();
        getReachableLocations();
        reachableLocationsMap.clear();
        min_x = Long.MAX_VALUE;
        min_y = Long.MAX_VALUE;
        min_z = Long.MAX_VALUE;
        max_x = Long.MIN_VALUE;
        max_y = Long.MIN_VALUE;
        max_z = Long.MIN_VALUE;
        reachableLocations.forEach(this::computeLocations);
        status.setStatus(3);
    }

    @Override
    public Set<Location> getReachableLocations() {
        if (reachableLocations.isEmpty())
            reachableLocations = regions.keySet().stream().flatMap(r -> r.getReachableLocations().stream()).collect(Collectors.toSet());
        return new HashSet<>(reachableLocations);
    }

    @Override
    public int getLevel() {
        return regions.keySet().stream().mapToInt(Region::getLevel).max().orElse(-1) + 1;
    }

    @Override
    public Set<Location> getReachableLocations(Location center, int range) {
        if (reachableLocationsMap.isEmpty())
            return null;
        Set<Location> result = new HashSet<>();
        for (int y = center.getBlockY() - range; y < center.getBlockY() + range; y++) {
            Optional.ofNullable(reachableLocationsMap.get(y)).ifPresent(
                    (z_map) -> {
                        for (int z = center.getBlockZ() - range; z < center.getBlockZ() + range; z++) {
                            Optional.ofNullable(z_map.get(z)).ifPresent(
                                    (x_map) -> {
                                        for (int x = center.getBlockX() - range; x < center.getBlockX() + range; x++) {
                                            Optional.ofNullable(x_map.get(x))
                                                    .ifPresent(result::add);
                                        }
                                    }
                            );
                        }
                    }
            );
        }
        return result;
    }

    @Override
    public Set<Location> getReachableLocations(Location center, int x_range, int y_range, int z_range) {
        if (reachableLocationsMap.isEmpty())
            return null;
        Set<Location> result = new HashSet<>();
        for (int y = center.getBlockY() - y_range; y < center.getBlockY() + y_range; y++) {
            Optional.ofNullable(reachableLocationsMap.get(y)).ifPresent(
                    (z_map) -> {
                        for (int z = center.getBlockZ() - z_range; z < center.getBlockZ() + z_range; z++) {
                            Optional.ofNullable(z_map.get(z)).ifPresent(
                                    (x_map) -> {
                                        for (int x = center.getBlockX() - x_range; x < center.getBlockX() + x_range; x++) {
                                            Optional.ofNullable(x_map.get(x))
                                                    .ifPresent(result::add);
                                        }
                                    }
                            );
                        }
                    }
            );
        }
        return result;
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
        Map<Integer, Map<Integer, Location>> Z_map = reachableLocationsMap.get(location.getBlockY());
        if (Z_map == null)
            return false;
        Map<Integer, Location> X_map = Z_map.get(location.getBlockZ());
        if (X_map == null)
            return false;
        return X_map.containsKey(location.getBlockX());
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
        if (sync) {
            try {
                status.setStatus(1);
                Logger.info("Extended region " + this.getName() + " started validating");
                _validate(tic, status);
                Logger.info("Extended region " + this.getName() + (isValid() ? " successfully validated" : " failed validating"));
            } catch (Exception ex) {
                Logger.info("Extended region " + this.getName() + " Exception while validating");
                status.ex = ex;
                status.setStatus(4);
            }
        } else
            Bukkit.getScheduler().runTaskAsynchronously(RegionalPathfinder.getInstance(), () -> {
                boolean locked = false;
                try {
                    status.setStatus(1);
                    lock.lockInterruptibly();
                    locked = true;
                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () ->
                            Logger.info("Extended region " + this.getName() + " started validating"));
                    _validate(tic, status);
                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () ->
                            Logger.info("Extended region " + this.getName() + (isValid() ? " successfully validated" : " failed validating")));
                    lock.unlock();
                } catch (Exception ex) {
                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () ->
                            Logger.info("Extended region " + this.getName() + " Exception while validating"));
                    status.ex = ex;
                    status.setStatus(4);
                    if (locked)
                        lock.unlock();
                }
            });
        return status;
    }

    private void _validate(long tic, StatusImpl<Boolean> status) {
        status.setStatus(2);
        invalidate();
        if (regions.size() == 0) {
            status.setProduct(false);
            status.totTime = (System.currentTimeMillis() - tic);
            status.setStatus(3);
            return;
        }
        if (ESubRegions().anyMatch(r -> r == this)) {
            status.setProduct(false);
            status.totTime = (System.currentTimeMillis() - tic);
            status.setStatus(3);
            return;
        }

        if (regions.size() == 2) {
            status.setProduct(getIntersections().size() > 0);
            status.totTime = (System.currentTimeMillis() - tic);
            status.setStatus(3);
            return;
        }

        StrongConnectivityAlgorithm<Node, Edge> scAlg =
                new KosarajuStrongConnectivityInspector<>(graph);
        status.setProduct(scAlg.isStronglyConnected());
        valid = status.getProduct();
        status.totTime = (System.currentTimeMillis() - tic);
        status.setStatus(3);
    }

    private Stream<ExtendedRegion> ESubRegions() {
        return regions.keySet().stream().map(Region::asExtendedRegion).filter(Objects::nonNull).map(r -> (ExtendedRegionImpl) r).flatMap(ExtendedRegionImpl::ESubRegions);
    }

    @Override
    public List<Location> getIntersections() {
        return regions.values().stream().flatMap(r -> r.waypoints.stream().map(Node::getLoc)).distinct().collect(Collectors.toList());
    }

    @Override
    public List<Region> getUnconnectedRegions() {
        return regions.entrySet().stream().filter((e) -> e.getValue().waypoints.isEmpty()).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public Status<Path> getPath(Location start, Location end) {
        long tic = System.currentTimeMillis();
        StatusImpl<Path> status = new StatusImpl<>();
        Bukkit.getScheduler().runTaskAsynchronously(RegionalPathfinder.getInstance(), () -> {
            status.setStatus(1);
            boolean locked = false;
            try {
                if (!isValid()) {
                    status.totTime = (System.currentTimeMillis() - tic);
                    status.ex = new RegionException("Region is not Valid", this);
                    status.setStatus(4);
                    return;
                }
                status.setStatus(1);
                lock.lockInterruptibly();
                locked = true;
                status.setStatus(2);

                Location act_s = new Location(
                        start.getWorld(),
                        start.getBlockX() + 0.5,
                        start.getBlockY() + 0.5,
                        start.getBlockZ() + 0.5
                );

                Location act_e = new Location(
                        end.getWorld(),
                        end.getBlockX() + 0.5,
                        end.getBlockY() + 0.5,
                        end.getBlockZ() + 0.5
                );

                if (!isReachableLocation(act_s.clone()) || !isReachableLocation(act_e.clone())) {
                    status.totTime = (System.currentTimeMillis() - tic);
                    status.setStatus(3);
                    lock.unlock();
                    return;
                }

                RegionImpl sreg = (RegionImpl) getRegion(act_s.clone());
                RegionImpl ereg = (RegionImpl) getRegion(act_e.clone());

                if (sreg == null || ereg == null) {
                    status.totTime = (System.currentTimeMillis() - tic);
                    status.setStatus(3);
                    lock.unlock();
                    return;
                }

                status.percentage = 0.001f;
                status.setStatus(2);

                if (sreg == ereg) {
                    status.setProduct(sreg._getPath(start.clone(), end.clone()));
                    status.totTime = (System.currentTimeMillis() - tic);
                    status.setStatus(3);
                    lock.unlock();
                    return;
                }

                RegionWrapper swr = regions.get(sreg);
                RegionWrapper ewr = regions.get(ereg);



                boolean sadded = false;
                boolean eadded = false;

                Node snode = nodeMap.get(act_s);

                if (snode == null) {
                    snode = new Node(act_s.clone());
                    sadded = true;
                }

                Node enode = nodeMap.get(act_e);

                if (enode == null) {
                    enode = new Node(act_e.clone());
                    eadded = true;
                }

                status.percentage = 0.01f;
                status.setStatus(2);


                boolean sradded = false;
                boolean eradded = false;

                if (!swr.waypoints.contains(snode)) {
                    makeEdges(sreg, swr, snode, 1);
                    sradded = true;
                }

                status.percentage = 0.2f;
                status.setStatus(2);

                if (!ewr.waypoints.contains(enode)) {
                    makeEdges(ereg, ewr, enode, 2);
                    eradded = true;
                }

                status.percentage = 0.5f;
                status.setStatus(2);

                ShortestPathAlgorithm<Node, Edge> spa = new DijkstraShortestPath<>(graph);

                GraphPath<Node, Edge> path = spa.getPath(snode, enode);

                Stream<Location> locationStream = path.getEdgeList().stream().map(Edge::getPath).flatMap(l -> l.stream().skip(1));

                Path ret = new Path(Stream.concat(Stream.of(start), locationStream).map(Location::clone).collect(Collectors.toList()), path.getWeight());

                status.totTime = (System.currentTimeMillis() - tic);
                status.setProduct(ret);
                status.setStatus(3);

                if (sadded)
                    graph.removeVertex(snode);
                if (eadded)
                    graph.removeVertex(enode);
                if (sradded && !sadded)
                    removeEdges(swr, snode);
                if (eradded && !eadded)
                    removeEdges(ewr, enode);

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

    private void removeEdges(RegionWrapper rw, Node n) {
        rw.waypoints.remove(n);
        rw.waypoints.forEach((w) -> {
            graph.removeEdge(w, n);
            graph.removeEdge(n, w);
        });
    }

    @Override
    public void invalidate() {
        this.valid = false;
        for (WeakReference<RegionImpl> reference :
                backreferences) {
            RegionImpl region = reference.get();
            if (region == null)
                backreferences.remove(reference);
            else
                region.invalidate();
        }
        RegionImpl.intersectionCacheMap.clear();
    }

    @Override
    public void referencer(RegionImpl region) {
        backreferences.add(new WeakReference<>(region));
    }

    private void computeLocations(Location l) {
        if (l.getBlockY() < min_y)
            min_y = l.getBlockY();
        if (l.getBlockZ() < min_z)
            min_z = l.getBlockZ();
        if (l.getBlockX() < min_x)
            min_x = l.getBlockX();
        if (l.getBlockY() > max_y)
            max_y = l.getBlockY();
        if (l.getBlockZ() > max_z)
            max_z = l.getBlockZ();
        if (l.getBlockX() > max_x)
            max_x = l.getBlockX();
        Map<Integer, Map<Integer, Location>> z_map = reachableLocationsMap.computeIfAbsent(l.getBlockY(), k -> new HashMap<>());
        Map<Integer, Location> x_set = z_map.computeIfAbsent(l.getBlockZ(), k -> new HashMap<>());
        x_set.put(l.getBlockX(), l);
    }

    private static class RegionWrapper {
        Region region;
        double multiplier;
        Set<Node> waypoints = new HashSet<>();
    }

    public ExtendedRegionImpl(String name) {
        id = RegionImpl.nextID.getAndIncrement();
        this.name = name;
    }
}
