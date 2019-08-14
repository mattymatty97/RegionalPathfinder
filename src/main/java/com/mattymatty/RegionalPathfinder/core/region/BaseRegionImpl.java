package com.mattymatty.RegionalPathfinder.core.region;

import com.mattymatty.RegionalPathfinder.RegionalPathfinder;
import com.mattymatty.RegionalPathfinder.api.Status;
import com.mattymatty.RegionalPathfinder.api.cost.MovementCost;
import com.mattymatty.RegionalPathfinder.api.region.BaseRegion;
import com.mattymatty.RegionalPathfinder.api.entity.Entity;
import com.mattymatty.RegionalPathfinder.core.entity.PlayerEntity;
import com.mattymatty.RegionalPathfinder.core.graph.BlockNode;
import com.mattymatty.RegionalPathfinder.core.graph.Graph;
import com.mattymatty.RegionalPathfinder.core.loader.LoadData;
import com.mattymatty.RegionalPathfinder.core.loader.Loader;
import com.mattymatty.RegionalPathfinder.core.loader.SynchronousLoader;
import com.mattymatty.RegionalPathfinder.exeptions.GraphExeption;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BaseRegionImpl implements RegionImpl, BaseRegion {

    private final int ID;

    private String Name;

    private List<Integer> errors = new ArrayList<>();

    private Graph graph = new Graph();

    private MovementCost cost = new MovementCost();

    private Entity entity = new PlayerEntity();

    private LoadData loadData;

    private Loader loader = new SynchronousLoader();


    //METHODS FROM Region


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
        return (loadData==null)?null:loadData.lowerCorner.getWorld();
    }

    @Override
    public Location[] getCorners() {
        if(loadData!=null)
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
        return (loadData==null)?null:loader.getValid(loadData);
    }

    @Override
    public List<Location> getValidLocations(Location center, int radius) {
        return null;
    }

    @Override
    public List<Location> getReachableLocations() {
        return (loadData==null)?null:loader.getReachable(loadData);
    }

    @Override
    public List<Location> getReachableLocations(Location center, int radius) {
        return null;
    }

    @Override
    public boolean isValid() {
        return (loadData != null) && (loadData.getStatus() == LoadData.Status.VALIDATED);
    }

    @Override
    public String[] getErrors() {
        return new String[0];
    }

    @Override
    public List<Location> getPath(Location start, Location end) {
        List<Location> reachable = getReachableLocations();
        Location actual_s = new Location(start.getWorld(),start.getBlockX(),start.getBlockY(),start.getBlockZ()).add(0.5,0.5,0.5);
        Location actual_e = new Location(end.getWorld(),end.getBlockX(),end.getBlockY(),end.getBlockZ()).add(0.5,0.5,0.5);

        if(reachable==null || start.getWorld() != end.getWorld() || !reachable.contains(actual_s) || !reachable.contains(actual_e))
            return null;

        BlockNode sNode = loadData.getNodesMap().get(actual_s);
        BlockNode eNode = loadData.getNodesMap().get(actual_e);
        List<Graph.Node> path;
        try {
            path = graph.shortestPath(sNode,eNode);
        } catch (GraphExeption graphExeption) {
            throw new RuntimeException(graphExeption);
        }
        return path.stream().map((n)->((BlockNode)n).getLocation()).collect(Collectors.toList());
    }

    @Override
    public Status getAsyncPath(Location start, Location end, Consumer<List<Location>> callback) {
        List<Location> reachable = getReachableLocations();
        Location actual_s = new Location(start.getWorld(),start.getBlockX(),start.getBlockY(),start.getBlockZ()).add(0.5,0.5,0.5);
        Location actual_e = new Location(end.getWorld(),end.getBlockX(),end.getBlockY(),end.getBlockZ()).add(0.5,0.5,0.5);

        if(reachable==null || start.getWorld() != end.getWorld() || !reachable.contains(actual_s) || !reachable.contains(actual_e))
            return null;

        BlockNode sNode = loadData.getNodesMap().get(actual_s);
        BlockNode eNode = loadData.getNodesMap().get(actual_e);

        new Thread(()->{
            List<Graph.Node> path;
            try {
                path = graph.shortestPath(sNode,eNode);
            } catch (GraphExeption graphExeption) {
                throw new RuntimeException(graphExeption);
            }

            RegionalPathfinder.getInstance().getServer().getScheduler()
                    .runTask(RegionalPathfinder.getInstance(),
                            ()->callback.accept(path.stream().map((n)->((BlockNode)n).getLocation()).collect(Collectors.toList())));

        }).start();

        return null;
    }

    //METHODS FROM RegionImpl

    @Override
    public void delete() {}

    //METHODS FROM BaseRegion

    @Override
    public Location getSamplePoint() {
        return (loadData==null)?null:this.loadData.samplePoint;
    }

    @Override
    public Loader setLoader(Loader loader) {
        return this.loader=loader;
    }

    @Override
    public Loader getLoader() {
        return this.loader;
    }

    @Override
    public MovementCost setMovementCost(MovementCost cost) {
        this.cost=cost;
        if(loadData!=null)
            loadData.cost=cost;
        return this.cost;
    }

    @Override
    public MovementCost getMovementCost() {
        return this.cost;
    }

    @Override
    public Entity setEntity(Entity entity) {
        this.entity = entity;
        if(loadData!=null)
            loadData.entity=entity;
        return this.entity;
    }

    @Override
    public Entity getEntity() {
        return this.entity;
    }

    //LOCAL METHODS

    public void load(){
        if(loadData!=null)
            loader.load(loadData);
    }

    public void evaluate(){

        if(loadData!=null)
            loader.evaluate(loadData);
    }

    public void validate(){

        if(loadData!=null)
            loader.validate(loadData);
    }

    @Override
    public Location[] setCorners(Location c1,Location c2){
        if(c1.getWorld() == c2.getWorld()){
            Location actual_c1 = new Location(c1.getWorld(),c1.getBlockX(),c1.getBlockY(),c1.getBlockZ()).add(0.5,0.5,0.5);
            Location actual_c2 = new Location(c2.getWorld(),c2.getBlockX(),c2.getBlockY(),c2.getBlockZ()).add(0.5,0.5,0.5);
            Location lowerCorner= new Location(actual_c1.getWorld(),Math.min(actual_c1.getX(),actual_c2.getX()),Math.min(actual_c1.getY(),actual_c2.getY()),Math.min(actual_c1.getZ(),actual_c2.getZ()));
            Location upperCorner= new Location(actual_c1.getWorld(),Math.max(actual_c1.getX(),actual_c2.getX()),Math.max(actual_c1.getY(),actual_c2.getY()),Math.max(actual_c1.getZ(),actual_c2.getZ()));
            loadData = new LoadData(upperCorner,lowerCorner,this.graph,this.cost,this.entity);
            load();
            if(loadData.getStatus() == LoadData.Status.LOADED)
                return new Location[]{loadData.lowerCorner,loadData.upperCorner};
        }
        return null;
    }

    @Override
    public Location setSamplePoint(Location sa){
        if(loadData!=null)
            if(sa.getWorld() == loadData.upperCorner.getWorld()){
                Location actual_sa = new Location(sa.getWorld(),sa.getBlockX(),sa.getBlockY(),sa.getBlockZ()).add(0.5,0.5,0.5);
                if(getValidLocations().contains(actual_sa)) {
                    loadData.samplePoint = actual_sa;
                    evaluate();
                    if(loadData.getStatus() == LoadData.Status.EVALUATED)
                        return actual_sa;
                }
            }
        return null;
    }

    BaseRegionImpl(String name) {
        this.ID = RegionImpl.nextID.getAndIncrement();
        Name = name;
    }
}
