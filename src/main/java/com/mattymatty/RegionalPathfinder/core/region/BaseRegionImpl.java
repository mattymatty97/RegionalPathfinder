package com.mattymatty.RegionalPathfinder.core.region;

import com.mattymatty.RegionalPathfinder.Logger;
import com.mattymatty.RegionalPathfinder.RegionalPathfinder;
import com.mattymatty.RegionalPathfinder.api.Status;
import com.mattymatty.RegionalPathfinder.api.entity.Entity;
import com.mattymatty.RegionalPathfinder.api.region.BaseRegion;
import com.mattymatty.RegionalPathfinder.api.region.Region;
import com.mattymatty.RegionalPathfinder.core.StatusImpl;
import com.mattymatty.RegionalPathfinder.core.entity.PlayerEntity;
import com.mattymatty.RegionalPathfinder.core.graph.Edge;
import com.mattymatty.RegionalPathfinder.core.graph.Node;
import com.mattymatty.RegionalPathfinder.core.loader.LoadData;
import com.mattymatty.RegionalPathfinder.core.loader.Loader;
import com.mattymatty.RegionalPathfinder.exeptions.AsyncExecption;
import com.mattymatty.RegionalPathfinder.exeptions.RegionException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class BaseRegionImpl implements RegionImpl, BaseRegion {

    private final int ID;

    private String Name;

    private Entity entity = new PlayerEntity();

    private LoadData loadData;

    private Loader<Location> loader;

    public final Lock lock = new ReentrantLock();

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
        lock.unlock();
        return ret;
    }

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
        return null;
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
        return null;
    }

    @Override
    public boolean isValid() {
        if (!lock.tryLock())
            throw new AsyncExecption("Async operation still running on this region", this);
        boolean ret = (loadData != null) && (loadData.getStatus() == LoadData.Status.VALIDATED);
        lock.unlock();
        return ret;
    }

    @Override
    public Status<List<Location>> getPath(Location start, Location end) {
        StatusImpl<List<Location>> status = new StatusImpl<>();
        new Thread(() -> {
            boolean locked = false;
            long tic = System.currentTimeMillis();
            try {
                status.setStatus(1);
                lock.lockInterruptibly();
                locked=true;
                if (loadData.getStatus() == LoadData.Status.VALIDATED) {

                    Semaphore tmp = new Semaphore(0);
                    AtomicBoolean allowed = new AtomicBoolean(false);

                    Location actual_s = new Location(start.getWorld(), start.getBlockX(), start.getBlockY(), start.getBlockZ()).add(0.5, 0.5, 0.5);
                    Location actual_e = new Location(end.getWorld(), end.getBlockX(), end.getBlockY(), end.getBlockZ()).add(0.5, 0.5, 0.5);

                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(),()->{
                        Logger.info("Pathfinding in region: "+ getName());
                        Logger.fine("from: X="+actual_s.getBlockX() + " Y="+actual_s.getBlockY() +" Z="+actual_s.getBlockZ());
                        Logger.fine("to: X="+actual_e.getBlockX() + " Y="+actual_e.getBlockY() +" Z="+actual_e.getBlockZ());
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
                                status.syncTime+=(System.currentTimeMillis()-tic2);
                                tmp.release();
                            });

                    tmp.acquire();

                    if (!allowed.get()) {
                        status.totTime=(System.currentTimeMillis()-tic);
                        status.setStatus(3);
                        lock.unlock();
                        return;
                    }

                    Node sNode = loadData.getNode(actual_s);
                    Node eNode = loadData.getNode(actual_e);
                    List<Node> path;

                    status.setStatus(2);

                    ShortestPathAlgorithm.SingleSourcePaths<Node, Edge> iPaths = loadData.getShortestPath().getPaths(sNode);

                    path = iPaths.getPath(eNode).getVertexList();

                    status.totTime=(System.currentTimeMillis()-tic);
                    status.setProduct(path.stream().map(Node::getLoc).collect(Collectors.toList()));

                    long toc = System.currentTimeMillis();
                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(),()->{
                        Logger.info(((!path.isEmpty())?"Found path":"Failed pathfinding") + " in region: "+ getName());
                        Logger.fine("Took: "+(toc-tic) +" ms");
                    });
                } else {
                    throw new RegionException("Region is not valid", this);
                }
                status.setStatus(3);
                lock.unlock();
            } catch (Exception ex) {
                status.ex = ex;
                status.totTime=(System.currentTimeMillis()-tic);
                if(locked)
                    lock.unlock();
                status.setStatus(4);
            }
        }).start();
        return status;
    }

    @Override
    public Loader setLoader(Loader loader) {
        return this.loader=loader;
    }

    @Override
    public Loader getLoader() {
        return loader;
    }

    @Override
    public void delete() {
    }

    @Override
    public Location getSamplePoint() {
        return (loadData == null) ? null : this.loadData.samplePoint;
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
    public Status<Location[]> load() {
        StatusImpl<Location[]> ret = new StatusImpl<>();
        if (loadData != null)
            loader.load(loadData,ret);
        return ret;
    }

    @Override
    public Status<Location> evaluate() {
        StatusImpl<Location> ret = new StatusImpl<>();
        if (loadData != null)
            loader.evaluate(loadData,ret);
        return ret;
    }

    @Override
    public Status<Object> validate() {
        StatusImpl<Object> ret = new StatusImpl<Object>();
        if (loadData != null)
            loader.validate(loadData,ret);
        return ret;
    }

    @Override
    public Status<Location[]> setCorners(Location c1, Location c2) {
        if (c1.getWorld() == c2.getWorld()) {
            Location actual_c1 = new Location(c1.getWorld(), c1.getBlockX(), c1.getBlockY(), c1.getBlockZ()).add(0.5, 0.5, 0.5);
            Location actual_c2 = new Location(c2.getWorld(), c2.getBlockX(), c2.getBlockY(), c2.getBlockZ()).add(0.5, 0.5, 0.5);
            Location lowerCorner = new Location(actual_c1.getWorld(), Math.min(actual_c1.getX(), actual_c2.getX()), Math.min(actual_c1.getY(), actual_c2.getY()), Math.min(actual_c1.getZ(), actual_c2.getZ()));
            Location upperCorner = new Location(actual_c1.getWorld(), Math.max(actual_c1.getX(), actual_c2.getX()), Math.max(actual_c1.getY(), actual_c2.getY()), Math.max(actual_c1.getZ(), actual_c2.getZ()));
            loadData = new LoadData(this, upperCorner, lowerCorner);
            return load();
        }
        return null;
    }

    @Override
    public Status<Location> setSamplePoint(Location sa) {
        if (loadData != null)
            if (sa.getWorld() == loadData.upperCorner.getWorld()) {
                Location actual_sa = new Location(sa.getWorld(), sa.getBlockX(), sa.getBlockY(), sa.getBlockZ()).add(0.5, 0.5, 0.5);
                if (getValidLocations().contains(actual_sa)) {
                    loadData.samplePoint = actual_sa;
                    return evaluate();
                }
            }
        return null;
    }
}
