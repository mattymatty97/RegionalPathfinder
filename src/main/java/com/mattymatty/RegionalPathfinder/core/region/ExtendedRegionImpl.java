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

    public static boolean sync = true;

    private final int id;

    private final String name;

    private Map<RegionImpl, RegionWrapper> regions = new HashMap<RegionImpl, RegionWrapper>();

    private Map<Node, Integer> waypointMap = new HashMap<>();

    private Graph<Node, Edge> graph = GraphTypeBuilder.<Node, Edge>directed().edgeClass(Edge.class).weighted(true).buildGraph();

    private Lock lock = new ReentrantLock();

    private boolean valid = false;

    private final ShortestPathAlgorithm<Node, Edge> spa = new DijkstraShortestPath<>(graph);
    private List<WeakReference<RegionImpl>> backreferences = new LinkedList<>();
    private long max_z = Long.MIN_VALUE;

    @Override
    public Status<Region[]> addRegion(Region region) {
        return addRegion(region, 1.0);
    }

    private Set<Location> reachableLocations = new HashSet<>();

    @Override
    public Status<Region[]> addRegion(Region region, @Positive double weightMultiplier) {
        return addRegion(region, weightMultiplier, new ArrayList<>());
    }

    private void addWaypoints(RegionWrapper rw, List<Node> waypoints) {
        rw.waypoints.addAll(waypoints.stream().filter(w -> !rw.waypoints.contains(w)).peek(n -> {
            Integer count = waypointMap.getOrDefault(n, null);
            if (count == null)
                count = 0;
            waypointMap.put(n, ++count);
        }).collect(Collectors.toList()));
    }

    @Override
    public Status<Region[]> addRegion(Region region, List<Location> excludedWaypoints) {
        return addRegion(region, 1.0, excludedWaypoints);
    }

    @Override
    public Status<Region[]> addRegion(Region region, @Positive double weightMultiplier, List<Location> excludedWaypoints) {
        if (regions.containsKey(region))
            return null;
        if (excludedWaypoints == null)
            excludedWaypoints = new ArrayList<>();
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
            List<Location> finalExcludedWaypoints = excludedWaypoints;
            Bukkit.getScheduler().runTaskAsynchronously(RegionalPathfinder.getInstance(), () -> {
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
            });
        }
        return status;
    }

    private void _addRegion(long tic, StatusImpl<Region[]> status, RegionImpl reg, double multiplier, List<Location> excludedWaypoints) {
        status.setStatus(2);

        RegionWrapper rw = new RegionWrapper();
        rw.region = reg;
        rw.multiplier = multiplier;

        int size = regions.size();
        int i = 0;
        for (RegionImpl act : regions.keySet()) {
            i++;
            RegionWrapper actrw = regions.get(act);
            Map<Region, Long> Regions = act._getIntersection(reg).stream().map(this::getRegion).collect(Collectors.groupingBy((Region r) -> r, Collectors.counting()));
            List<Node> waypoints = act._getIntersection(reg).stream().filter(
                    (l) -> {
                        if (excludedWaypoints.contains(l)) {
                            Region loc_r = getRegion(l);
                            long count = Regions.get(loc_r);
                            if (count > 1) {
                                Regions.put(loc_r, --count);
                                return false;
                            }
                        }
                        return true;
                    }
            ).map(Node::new).collect(Collectors.toList());
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
        if (!rw.waypoints.contains(n)) {
            if (!graph.containsVertex(n))
                graph.addVertex(n);
            rw.waypoints.forEach(w -> {
                Path go = null, ret = null;
                if (direction == 0 || direction == 1)
                    go = region._getPath(n.getLoc(), w.getLoc());
                if (direction == 0 || direction == 2)
                    ret = region._getPath(w.getLoc(), n.getLoc());

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
            Bukkit.getScheduler().runTaskAsynchronously(RegionalPathfinder.getInstance(), () -> {
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
            });
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
        RegionImpl sreg = regions.keySet().stream().sorted(Comparator.comparing(Region::getLevel)).filter(r -> r.isReachableLocation(start)).findFirst().get();
        RegionImpl ereg = regions.keySet().stream().sorted(Comparator.comparing(Region::getLevel)).filter(r -> r.isReachableLocation(end)).findFirst().get();

        if (sreg == ereg)
            return sreg._getPath(start, end);

        RegionWrapper swr = regions.get(sreg);
        RegionWrapper ewr = regions.get(ereg);

        Node snode = new Node(start);
        Node enode = new Node(end);

        boolean sadded = false;
        boolean eadded = false;

        if (!swr.waypoints.contains(snode)) {
            makeEdges(sreg, swr, snode, 1);
            sadded = true;
        }

        if (!ewr.waypoints.contains(enode)) {
            makeEdges(ereg, ewr, enode, 2);
            eadded = true;
        }

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
    public World getWorld() {
        return regions.keySet().stream().map(Region::getWorld).findAny().orElse(null);
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
    public Set<Location> getValidLocations(Location center, int radius) {
        throw new RuntimeException("Not Yet implemented");
    }

    private Region getRegion(Location l) {
        for (Region region : this.regions.keySet()) {
            if (region.isReachableLocation(l))
                return region;
        }
        return null;
    }

    private void _removeRegion(long tic, StatusImpl<Region[]> status, RegionImpl reg) {
        status.setStatus(2);

        RegionWrapper rw = regions.get(reg);
        regions.remove(reg);
        if (rw != null) {

            int size = rw.waypoints.size();
            int i = 0;
            for (Node waypoint :
                    rw.waypoints) {
                i++;
                Integer count = waypointMap.getOrDefault(waypoint, null);
                if (--count < 2) {
                    graph.removeVertex(waypoint);
                    waypointMap.remove(waypoint);
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
            reachableLocations = regions.keySet().stream().flatMap(r -> r.getReachableLocations().stream()).distinct().collect(Collectors.toSet());
        return new HashSet<>(reachableLocations);
    }

    @Override
    public Status<Set<Location>> getIntersection(Region region) {
        StatusImpl<Set<Location>> result = new StatusImpl<>();
        if (sync) {
            result.setProduct(_getIntersection(region));
            result.setStatus(3);
        } else {
            result.setStatus(1);
            new Thread(() -> {
                result.setStatus(2);
                result.setProduct(_getIntersection(region));
                result.setStatus(3);
            }).start();
        }
        return result;
    }

    @Override
    public Set<Location> _getIntersection(Region region) {
        Set<Location> common = new HashSet<>(region.getReachableLocations());
        Location min = region.getMinCorner();
        Location max = region.getMaxCorner();
        Location center = new Location(getWorld(), (min.getBlockX() + max.getBlockX()) / 2.0, (min.getBlockY() + max.getBlockY()) / 2.0, (min.getBlockZ() + max.getBlockZ()) / 2.0);
        int radius = (int) Math.floor((Math.max(max.getBlockX() - min.getBlockX(), Math.max(max.getBlockY() - min.getBlockY(), max.getBlockZ() - min.getBlockZ())) / 2.0));

        common.retainAll(this.getReachableLocations(center, radius));
        return common;
    }

    @Override
    public int getLevel() {
        return regions.keySet().stream().mapToInt(Region::getLevel).max().orElse(-1) + 1;
    }

    @Override
    public Set<Location> getReachableLocations(Location center, int radius) {
        if (reachableLocationsMap.isEmpty())
            return null;
        Set<Location> result = new HashSet<>();
        for (int y = center.getBlockY() - radius; y < center.getBlockY() + radius; y++) {
            for (int z = center.getBlockZ() - radius; z < center.getBlockZ() + radius; z++) {
                for (int x = center.getBlockX() - radius; x < center.getBlockX() + radius; x++) {
                    int finalZ = z;
                    int finalX = x;
                    Optional.ofNullable(reachableLocationsMap.get(y))
                            .flatMap(z_map -> Optional.ofNullable(z_map.get(finalZ)))
                            .flatMap(x_map -> Optional.ofNullable(x_map.get(finalX)))
                            .ifPresent(result::add);
                }
            }
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
        if (sync) {
            try {
                status.setStatus(1);
                Logger.info("Extended region " + this.getName() + " started validating");
                _validate(tic, status);
                Logger.info("Extended region " + this.getName() + (isValid() ? "successfully validated" : "failed validating"));
            } catch (Exception ex) {
                Logger.info("Extended region " + this.getName() + "failed validating");
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
                            Logger.info("Extended region " + this.getName() + (isValid() ? "successfully validated" : "failed validating")));
                    lock.unlock();
                } catch (Exception ex) {
                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () ->
                            Logger.info("Extended region " + this.getName() + "failed validating"));
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
        if (ESubRegions().anyMatch(r -> r == this)) {
            status.setProduct(false);
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

                RegionImpl sreg = regions.keySet().stream().sorted(Comparator.comparing(Region::getLevel)).filter(r -> r.isReachableLocation(start)).findFirst().orElse(null);
                RegionImpl ereg = regions.keySet().stream().sorted(Comparator.comparing(Region::getLevel)).filter(r -> r.isReachableLocation(end)).findFirst().orElse(null);

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
                    makeEdges(sreg, swr, snode, 1);
                    sadded = true;
                }


                status.percentage = 0.2f;
                status.setStatus(2);

                if (!ewr.waypoints.contains(enode)) {
                    makeEdges(ereg, ewr, enode, 2);
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
        List<Node> waypoints = new LinkedList<>();
    }

    public ExtendedRegionImpl(String name) {
        id = RegionImpl.nextID.getAndIncrement();
        this.name = name;
    }
}
