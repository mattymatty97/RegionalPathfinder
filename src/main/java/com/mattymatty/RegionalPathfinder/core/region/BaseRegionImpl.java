package com.mattymatty.RegionalPathfinder.core.region;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mattymatty.RegionalPathfinder.Logger;
import com.mattymatty.RegionalPathfinder.RegionalPathfinder;
import com.mattymatty.RegionalPathfinder.api.Status;
import com.mattymatty.RegionalPathfinder.api.entity.Entity;
import com.mattymatty.RegionalPathfinder.api.region.BaseRegion;
import com.mattymatty.RegionalPathfinder.core.StatusImpl;
import com.mattymatty.RegionalPathfinder.core.entity.PlayerEntity;
import com.mattymatty.RegionalPathfinder.core.graph.Edge;
import com.mattymatty.RegionalPathfinder.core.graph.Node;
import com.mattymatty.RegionalPathfinder.core.loader.LoadData;
import com.mattymatty.RegionalPathfinder.core.loader.Loader;
import com.mattymatty.RegionalPathfinder.core.loader.SynchronousLoader;
import com.mattymatty.RegionalPathfinder.exeptions.RegionException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class BaseRegionImpl implements RegionImpl, BaseRegion {

    public static Loader loader = new SynchronousLoader();
    public final Lock lock = new ReentrantLock();
    private final AtomicInteger readers = new AtomicInteger();
    private final int ID;
    private String Name;
    private Entity entity = new PlayerEntity();
    LoadData loadData;
    private Cache<Node, ShortestPathAlgorithm.SingleSourcePaths<Node, Edge>> sourceCache = CacheBuilder.newBuilder().softValues()
            .maximumSize(15).build();

    //METHODS FROM Region


    BaseRegionImpl(String name) {
        this.ID = RegionImpl.nextID.getAndIncrement();
        Name = name;
    }

    @Override
    public int getID() {
        return ID;
    }

    @Override
    public String getName() {
        return Name;
    }

    @Override
    public int getLevel() {
        return 1;
    }

    @Override
    public World getWorld() {

        World world = (loadData == null) ? null : loadData.lowerCorner.getWorld();

        return world;
    }

    private Location lowerCorner;

    @Override
    public Set<Location> getValidLocations() {


        Set<Location> ret = (loadData == null) ? null : new HashSet<>(loader.getValid(loadData));

        return ret;
    }

    @Override
    public Set<Location> getValidLocations(Location center, int range) {
        throw new RuntimeException("Not Yet implemented");
    }

    @Override
    public Set<Location> getReachableLocations() {

        Set<Location> ret = (loadData == null) ? null : new HashSet<>(loader.getReachable(loadData));

        return ret;
    }

    @Override
    public Set<Location> getReachableLocations(Location center, int range) {

        if (loadData == null) {
            return null;
        }
        Set<Location> result = new HashSet<>();
        for (int y = center.getBlockY() - range; y < center.getBlockY() + range; y++) {
            Optional.ofNullable(loadData.reachableLocationsMap.get(y)).ifPresent(
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


        if (loadData == null) {
            return null;
        }
        Set<Location> result = new HashSet<>();
        for (int y = center.getBlockY() - y_range; y < center.getBlockY() + y_range; y++) {
            Optional.ofNullable(loadData.reachableLocationsMap.get(y)).ifPresent(
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

    private Location upperCorner;
    private Location samplepoint;
    private List<WeakReference<RegionImpl>> backreferences = new LinkedList<>();

    @Override
    public boolean isValid() {


        boolean ret = (loadData != null) && (loadData.getStatus() == LoadData.Status.VALIDATED);
        return ret;
    }

    @Override
    public Location[] getCorners() {


        Location[] ret = null;
        if (loadData != null)
            ret = new Location[]{
                    loadData.lowerCorner,
                    new Location(getWorld(),
                            loadData.lowerCorner.getX(),
                            loadData.lowerCorner.getY(),
                            loadData.upperCorner.getZ()),
                    new Location(getWorld(),
                            loadData.upperCorner.getX(),
                            loadData.lowerCorner.getY(),
                            loadData.lowerCorner.getZ()),
                    new Location(getWorld(),
                            loadData.upperCorner.getX(),
                            loadData.lowerCorner.getY(),
                            loadData.upperCorner.getZ()),
                    new Location(getWorld(),
                            loadData.lowerCorner.getX(),
                            loadData.upperCorner.getY(),
                            loadData.lowerCorner.getZ()),
                    loadData.upperCorner
            };
        else if (lowerCorner != null)
            ret = new Location[]{
                    lowerCorner,
                    new Location(getWorld(),
                            lowerCorner.getX(),
                            lowerCorner.getY(),
                            upperCorner.getZ()),
                    new Location(getWorld(),
                            upperCorner.getX(),
                            lowerCorner.getY(),
                            lowerCorner.getZ()),
                    new Location(getWorld(),
                            upperCorner.getX(),
                            lowerCorner.getY(),
                            upperCorner.getZ()),
                    new Location(getWorld(),
                            lowerCorner.getX(),
                            upperCorner.getY(),
                            lowerCorner.getZ()),
                    upperCorner
            };
        return ret;
    }

    @Override
    public Location getMinCorner() {
        return (loadData == null) ? null : loadData.lowerCorner;
    }

    @Override
    public Location getMaxCorner() {

        return (loadData == null) ? null : loadData.upperCorner;
    }

    @Override
    public boolean isInRegion(Location location) {
        boolean ret = false;


        if (loadData != null && loadData.getStatus().getValue() > LoadData.Status.LOADING.getValue()) {

            int dx = location.getBlockX() - loadData.lowerCorner.getBlockX();
            int dy = location.getBlockY() - loadData.lowerCorner.getBlockY();
            int dz = location.getBlockZ() - loadData.lowerCorner.getBlockZ();

            ret = dx >= 0 && dx < loadData.getX_size() && dy >= 0 && dy < loadData.getY_size() && dz >= 0 && dz < loadData.getZ_size();
        }

        return ret;
    }

    @Override
    public boolean isValidLocation(Location location) {
        boolean ret = false;
        if (!isInRegion(location))
            return false;


        if (loadData != null && loadData.getStatus().getValue() > LoadData.Status.LOADING.getValue()) {
            Location actual_loc = new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ()).add(0.5, 0.5, 0.5);
            Node node = loadData.getNode(actual_loc);
            ret = node != null;
        }

        return ret;
    }

    @Override
    public void delete() {
        sourceCache.invalidateAll();
        this.loadData.delete();
        this.loadData = null;
        this.lowerCorner = null;
        this.upperCorner = null;
        this.samplepoint = null;
        this.backreferences.clear();
        this.backreferences = null;
        this.entity = null;
        this.Name = null;
        this.sourceCache = null;
    }

    @Override
    public boolean isReachableLocation(Location location) {


        boolean ret = false;
        if (loadData == null)
            ret = false;
        else if (loadData.getStatus().getValue() < LoadData.Status.EVALUATED.getValue())
            ret = false;
        else {
            Map<Integer, Map<Integer, Location>> Z_map = loadData.reachableLocationsMap.get(location.getBlockY());
            if (Z_map == null)
                ret = false;
            else {
                Map<Integer, Location> X_map = Z_map.get(location.getBlockZ());
                if (X_map == null)
                    ret = false;
                else
                    ret = X_map.containsKey(location.getBlockX());
            }
        }

        return ret;
    }

    @Override
    public Entity setEntity(Entity entity) {
        invalidate();
        loadData = null;
        return this.entity = entity;
    }

    //LOCAL METHODS

    @Override
    public Entity getEntity() {
        return this.entity;
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

        Node sNode = loadData.getNode(actual_s);
        Node eNode = loadData.getNode(actual_e);
        GraphPath<Node, Edge> path = null;

        try {
            path = getNodeEdgeGraphPath(sNode, eNode);
        } catch (Exception ignored) {
        }

        return (path != null) ? new Path(path.getVertexList().stream().map(Node::getLoc).collect(Collectors.toList()), path.getWeight()) : null;
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
                locked = true;
                if (loadData.getStatus() == LoadData.Status.VALIDATED) {

                    Semaphore tmp = new Semaphore(0);
                    AtomicBoolean allowed = new AtomicBoolean(false);

                    Location actual_s = new Location(start.getWorld(), start.getBlockX(), start.getBlockY(), start.getBlockZ()).add(0.5, 0.5, 0.5);
                    Location actual_e = new Location(end.getWorld(), end.getBlockX(), end.getBlockY(), end.getBlockZ()).add(0.5, 0.5, 0.5);

                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () -> {
                        Logger.info("Pathfinding in region: " + getName());
                        Logger.fine("from: X=" + actual_s.getBlockX() + " Y=" + actual_s.getBlockY() + " Z=" + actual_s.getBlockZ());
                        Logger.fine("to: X=" + actual_e.getBlockX() + " Y=" + actual_e.getBlockY() + " Z=" + actual_e.getBlockZ());
                    });

                    RegionalPathfinder.getInstance().getServer().getScheduler()
                            .runTask(RegionalPathfinder.getInstance(), () -> {
                                long tic2 = System.currentTimeMillis();
                                List<Location> reachable = loader.getReachable(loadData);
                                if (reachable == null || start.getWorld() != end.getWorld() || !reachable.contains(actual_s) || !reachable.contains(actual_e)) {
                                    allowed.set(false);
                                } else {
                                    allowed.set(true);
                                }
                                status.syncTime += (System.currentTimeMillis() - tic2);
                                tmp.release();
                            });

                    tmp.acquire();

                    if (!allowed.get()) {
                        status.totTime = (System.currentTimeMillis() - tic);
                        status.setStatus(3);
                        RegionalPathfinder.getInstance().runningThreads.remove(Thread.currentThread());
                        return;
                    }

                    Node sNode = loadData.getNode(actual_s);
                    Node eNode = loadData.getNode(actual_e);
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
                } else {
                    throw new RegionException("Region is not valid", this);
                }
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

    @Override
    public Status<Boolean> validate() {
        invalidate();
        StatusImpl<Boolean> ret = new StatusImpl<>();
        ret.setProduct(false);
        if (loadData != null)
            loader.validate(loadData, ret);
        return ret;
    }

    private GraphPath<Node, Edge> getNodeEdgeGraphPath(Node sNode, Node eNode) {
        GraphPath<Node, Edge> path;
        ShortestPathAlgorithm.SingleSourcePaths<Node, Edge> iPaths;
        iPaths = sourceCache.getIfPresent(sNode);
        if (iPaths == null) {
            iPaths = loadData.getShortestPath().getPaths(sNode);
            sourceCache.put(sNode, iPaths);
        }
        path = iPaths.getPath(eNode);
        return path;
    }

    @Override
    public Status<Location[]> load() {
        invalidate();
        StatusImpl<Location[]> ret = new StatusImpl<>();
        if (loadData != null && loadData.lowerCorner == lowerCorner && loadData.upperCorner == upperCorner) {
            loader.load(loadData, ret);
        } else {
            loadData = new LoadData(this, upperCorner, lowerCorner);
            loader.load(loadData, ret);
        }
        sourceCache.invalidateAll();
        return ret;
    }

    @Override
    public Status<Location> evaluate() {
        invalidate();
        StatusImpl<Location> ret = new StatusImpl<>();
        if (loadData != null) {
            loadData.samplePoint = samplepoint;
            loader.evaluate(loadData, ret);
            sourceCache.invalidateAll();
        }
        return ret;
    }

    @Override
    public Location[] setCorners(Location c1, Location c2) {
        if (c1.getWorld() == c2.getWorld()) {
            Location actual_c1 = new Location(c1.getWorld(), c1.getBlockX(), c1.getBlockY(), c1.getBlockZ()).add(0.5, 0.5, 0.5);
            Location actual_c2 = new Location(c2.getWorld(), c2.getBlockX(), c2.getBlockY(), c2.getBlockZ()).add(0.5, 0.5, 0.5);
            lowerCorner = new Location(actual_c1.getWorld(), Math.min(actual_c1.getX(), actual_c2.getX()), Math.min(actual_c1.getY(), actual_c2.getY()), Math.min(actual_c1.getZ(), actual_c2.getZ()));
            upperCorner = new Location(actual_c1.getWorld(), Math.max(actual_c1.getX(), actual_c2.getX()) + 1, Math.max(actual_c1.getY(), actual_c2.getY()) + 1, Math.max(actual_c1.getZ(), actual_c2.getZ()) + 1);
            return new Location[]{lowerCorner, upperCorner};
        }
        return null;
    }

    @Override
    public Location setSamplePoint(Location sa) {
        samplepoint = new Location(sa.getWorld(), sa.getBlockX(), sa.getBlockY(), sa.getBlockZ()).add(0.5, 0.5, 0.5);
        return samplepoint;
    }

    @Override
    public void invalidate() {
        if (loadData != null)
            loadData.invalidate();
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
}
