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
import com.mattymatty.RegionalPathfinder.exeptions.AsyncExecption;
import com.mattymatty.RegionalPathfinder.exeptions.RegionException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class BaseRegionImpl implements RegionImpl, BaseRegion {

    public static Loader loader = new SynchronousLoader();
    public final Lock lock = new ReentrantLock();
    private final int ID;
    private String Name;
    private Entity entity = new PlayerEntity();
    private LoadData loadData;
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
        if (!lock.tryLock())
            throw new AsyncExecption("Async operation still running on this region", this);
        World world = (loadData == null) ? null : loadData.lowerCorner.getWorld();
        lock.unlock();
        return world;
    }

    private Location lowerCorner;

    @Override
    public List<Location> getValidLocations() {
        if (!lock.tryLock())
            throw new AsyncExecption("Async operation still running on this region", this);
        List<Location> ret = (loadData == null) ? null : loader.getValid(loadData);
        lock.unlock();
        return ret;
    }

    @Override
    public List<Location> getValidLocations(Location center, int radius) {
        throw new RuntimeException("Not Yet implemented");
    }

    @Override
    public List<Location> getReachableLocations() {
        if (!lock.tryLock())
            throw new AsyncExecption("Async operation still running on this region", this);
        List<Location> ret = (loadData == null) ? null : loader.getReachable(loadData);
        lock.unlock();
        return ret;
    }

    @Override
    public List<Location> getReachableLocations(Location center, int radius) {
        throw new RuntimeException("Not Yet implemented");
    }

    private Location upperCorner;
    private Location samplepoint;
    private List<WeakReference<RegionImpl>> backreferences = new LinkedList<>();

    @Override
    public boolean isValid() {
        if (!lock.tryLock())
            throw new AsyncExecption("Async operation still running on this region", this);
        boolean ret = (loadData != null) && (loadData.getStatus() == LoadData.Status.VALIDATED);
        lock.unlock();
        return ret;
    }

    @Override
    public Location[] getCorners() {
        if (!lock.tryLock())
            throw new AsyncExecption("Async operation still running on this region", this);
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
        lock.unlock();
        return ret;
    }

    @Override
    public boolean isInRegion(Location location) {
        boolean ret = false;
        if (!lock.tryLock())
            throw new AsyncExecption("Async operation still running on this region", this);
        if (loadData != null && loadData.getStatus().getValue() > LoadData.Status.LOADING.getValue()) {

            int dx = location.getBlockX() - loadData.lowerCorner.getBlockX();
            int dy = location.getBlockY() - loadData.lowerCorner.getBlockY();
            int dz = location.getBlockZ() - loadData.lowerCorner.getBlockZ();

            ret = dx >= 0 && dx < loadData.getX_size() && dy >= 0 && dy < loadData.getY_size() && dz >= 0 && dz < loadData.getZ_size();
        }
        lock.unlock();
        return ret;
    }

    @Override
    public boolean isValidLocation(Location location) {
        boolean ret = false;
        if (!isInRegion(location))
            return false;
        if (!lock.tryLock())
            throw new AsyncExecption("Async operation still running on this region", this);
        if (loadData != null && loadData.getStatus().getValue() > LoadData.Status.LOADING.getValue()) {
            Location actual_loc = new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ()).add(0.5, 0.5, 0.5);
            Node node = loadData.getNode(actual_loc);
            ret = node != null;
        }
        lock.unlock();
        return ret;
    }

    @Override
    public void delete() {
        sourceCache.invalidateAll();
    }

    @Override
    public boolean isReachableLocation(Location location) {
        boolean ret = false;
        if (!isInRegion(location))
            return false;
        if (!isValidLocation(location))
            return false;
        if (!lock.tryLock())
            throw new AsyncExecption("Async operation still running on this region", this);
        if (loadData != null && loadData.getStatus().getValue() > LoadData.Status.EVALUATING.getValue()) {
            Location actual_loc = new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ()).add(0.5, 0.5, 0.5);
            Node node = loadData.getNode(actual_loc);
            ret = loadData.getReachableGraph().containsVertex(node);
        }
        lock.unlock();
        return ret;
    }

    @Override
    public Entity setEntity(Entity entity) {
        return this.entity = entity;
    }

    //LOCAL METHODS

    @Override
    public Entity getEntity() {
        return this.entity;
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
        new Thread(() -> {
            boolean locked = false;
            long tic = System.currentTimeMillis();
            try {
                status.setStatus(1);
                lock.lockInterruptibly();
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
                        lock.unlock();
                        return;
                    }

                    Node sNode = loadData.getNode(actual_s);
                    Node eNode = loadData.getNode(actual_e);
                    GraphPath<Node, Edge> path;

                    status.setStatus(2);

                    path = getNodeEdgeGraphPath(sNode, eNode);

                    status.totTime = (System.currentTimeMillis() - tic);
                    status.setProduct((path != null) ? new Path(path.getVertexList().stream().map(Node::getLoc).collect(Collectors.toList()), path.getWeight()) : null);

                    long toc = System.currentTimeMillis();
                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () -> {
                        Logger.info(((path != null) ? "Found path" : "Failed pathfinding") + " in region: " + getName());
                        Logger.fine("Took: " + (toc - tic) + " ms");
                    });
                } else {
                    throw new RegionException("Region is not valid", this);
                }
                status.setStatus(3);
                lock.unlock();
            } catch (Exception ex) {
                status.ex = ex;
                status.totTime = (System.currentTimeMillis() - tic);
                if (locked)
                    lock.unlock();
                status.setStatus(4);
            }
        }).start();
        return status;
    }

    @Override
    public Status<Boolean> validate() {
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
        loadData.invalidate();
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
}
