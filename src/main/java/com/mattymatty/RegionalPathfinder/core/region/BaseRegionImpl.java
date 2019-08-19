package com.mattymatty.RegionalPathfinder.core.region;

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
import org.bukkit.Location;
import org.bukkit.World;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class BaseRegionImpl implements RegionImpl, BaseRegion {

    private final int ID;

    private String Name;

    private Entity entity = new PlayerEntity();

    private LoadData loadData;

    private Loader loader = new SynchronousLoader();

    private Semaphore sem = new Semaphore(1);


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
        if (sem.availablePermits() == 0)
            throw new AsyncExecption("Async operation still running on this region", this);
        return (loadData == null) ? null : loadData.lowerCorner.getWorld();
    }

    @Override
    public Location[] getCorners() {
        if (sem.availablePermits() == 0)
            throw new AsyncExecption("Async operation still running on this region", this);
        if (loadData != null)
            return new Location[]{
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
        return null;
    }

    @Override
    public List<Location> getValidLocations() {
        if (sem.availablePermits() == 0)
            throw new AsyncExecption("Async operation still running on this region", this);
        return (loadData == null) ? null : loader.getValid(loadData);
    }

    @Override
    public List<Location> getValidLocations(Location center, int radius) {
        return null;
    }

    @Override
    public List<Location> getReachableLocations() {
        if (sem.availablePermits() == 0)
            throw new AsyncExecption("Async operation still running on this region", this);
        return (loadData == null) ? null : loader.getReachable(loadData);
    }

    @Override
    public List<Location> getReachableLocations(Location center, int radius) {
        return null;
    }

    @Override
    public boolean isValid() {
        if (sem.availablePermits() == 0)
            throw new AsyncExecption("Async operation still running on this region", this);
        return (loadData != null) && (loadData.getStatus() == LoadData.Status.VALIDATED);
    }

    @Override
    public List<Location> getPath(Location start, Location end) {
        if (sem.availablePermits() == 0)
            throw new AsyncExecption("Async operation still running on this region", this);
        if (isValid()) {
            List<Location> reachable = getReachableLocations();
            Location actual_s = new Location(start.getWorld(), start.getBlockX(), start.getBlockY(), start.getBlockZ()).add(0.5, 0.5, 0.5);
            Location actual_e = new Location(end.getWorld(), end.getBlockX(), end.getBlockY(), end.getBlockZ()).add(0.5, 0.5, 0.5);

            if (reachable == null || start.getWorld() != end.getWorld() || !reachable.contains(actual_s) || !reachable.contains(actual_e))
                return null;

            Node sNode = loadData.getNode(actual_s);
            Node eNode = loadData.getNode(actual_e);
            List<Node> path;

            ShortestPathAlgorithm.SingleSourcePaths<Node, Edge> iPaths = loadData.getShortestPath().getPaths(sNode);

            path = iPaths.getPath(eNode).getVertexList();

            return path.stream().map(Node::getLoc).collect(Collectors.toList());

        }
        return null;
    }

    @Override
    public Status getAsyncPath(Location start, Location end) {
        StatusImpl status = new StatusImpl();
        new Thread(() -> {
            try {
                status.setStatus(1);
                sem.acquire();
                if (loadData.getStatus() == LoadData.Status.VALIDATED) {
                    Semaphore tmp = new Semaphore(0);
                    AtomicBoolean allowed = new AtomicBoolean(false);

                    Location actual_s = new Location(start.getWorld(), start.getBlockX(), start.getBlockY(), start.getBlockZ()).add(0.5, 0.5, 0.5);
                    Location actual_e = new Location(end.getWorld(), end.getBlockX(), end.getBlockY(), end.getBlockZ()).add(0.5, 0.5, 0.5);

                    RegionalPathfinder.getInstance().getServer().getScheduler()
                            .runTask(RegionalPathfinder.getInstance(), () -> {
                                List<Location> reachable = loader.getReachable(loadData);
                                if (reachable == null || start.getWorld() != end.getWorld() || !reachable.contains(actual_s) || !reachable.contains(actual_e)) {
                                    allowed.set(false);
                                } else {
                                    allowed.set(true);
                                }
                                tmp.release();
                            });

                    tmp.acquire();

                    if (!allowed.get()) {
                        status.setStatus(3);
                        sem.release();
                        return;
                    }

                    Node sNode = loadData.getNode(actual_s);
                    Node eNode = loadData.getNode(actual_e);
                    List<Node> path;

                    status.setStatus(2);

                    ShortestPathAlgorithm.SingleSourcePaths<Node, Edge> iPaths = loadData.getShortestPath().getPaths(sNode);

                    path = iPaths.getPath(eNode).getVertexList();

                    status.setPath(path.stream().map(Node::getLoc).collect(Collectors.toList()));
                } else {
                    status.setStatus(3);
                    sem.release();
                    throw new RegionException("Region is not valid", this);
                }
                status.setStatus(3);
                sem.release();
            } catch (InterruptedException ignored) {
            }
        }).start();
        return status;
    }

    //METHODS FROM BaseRegion

    @Override
    public void delete() {
    }

    @Override
    public Location getSamplePoint() {
        return (loadData == null) ? null : this.loadData.samplePoint;
    }

    @Override
    public Loader setLoader(Loader loader) {
        return this.loader = loader;
    }

    @Override
    public Loader getLoader() {
        return this.loader;
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

    public void load() {
        if (loadData != null)
            loader.load(loadData);
    }

    public void evaluate() {

        if (loadData != null)
            loader.evaluate(loadData);
    }

    public void validate() {

        if (loadData != null)
            loader.validate(loadData);
    }

    @Override
    public Location[] setCorners(Location c1, Location c2) {
        if (c1.getWorld() == c2.getWorld()) {
            Location actual_c1 = new Location(c1.getWorld(), c1.getBlockX(), c1.getBlockY(), c1.getBlockZ()).add(0.5, 0.5, 0.5);
            Location actual_c2 = new Location(c2.getWorld(), c2.getBlockX(), c2.getBlockY(), c2.getBlockZ()).add(0.5, 0.5, 0.5);
            Location lowerCorner = new Location(actual_c1.getWorld(), Math.min(actual_c1.getX(), actual_c2.getX()), Math.min(actual_c1.getY(), actual_c2.getY()), Math.min(actual_c1.getZ(), actual_c2.getZ()));
            Location upperCorner = new Location(actual_c1.getWorld(), Math.max(actual_c1.getX(), actual_c2.getX()), Math.max(actual_c1.getY(), actual_c2.getY()), Math.max(actual_c1.getZ(), actual_c2.getZ()));
            loadData = new LoadData(this, upperCorner, lowerCorner);
            load();
            if (loadData.getStatus() == LoadData.Status.LOADED)
                return new Location[]{loadData.lowerCorner, loadData.upperCorner};
        }
        return null;
    }

    @Override
    public Location setSamplePoint(Location sa) {
        if (loadData != null)
            if (sa.getWorld() == loadData.upperCorner.getWorld()) {
                Location actual_sa = new Location(sa.getWorld(), sa.getBlockX(), sa.getBlockY(), sa.getBlockZ()).add(0.5, 0.5, 0.5);
                if (getValidLocations().contains(actual_sa)) {
                    loadData.samplePoint = actual_sa;
                    evaluate();
                    if (loadData.getStatus() == LoadData.Status.EVALUATED)
                        return actual_sa;
                }
            }
        return null;
    }
}
