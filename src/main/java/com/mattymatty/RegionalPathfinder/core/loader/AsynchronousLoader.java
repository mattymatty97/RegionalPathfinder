package com.mattymatty.RegionalPathfinder.core.loader;

import com.mattymatty.RegionalPathfinder.Logger;
import com.mattymatty.RegionalPathfinder.RegionalPathfinder;
import com.mattymatty.RegionalPathfinder.api.Status;
import com.mattymatty.RegionalPathfinder.api.region.Region;
import com.mattymatty.RegionalPathfinder.core.StatusImpl;
import com.mattymatty.RegionalPathfinder.core.graph.Edge;
import com.mattymatty.RegionalPathfinder.core.graph.Node;
import com.mattymatty.RegionalPathfinder.exeptions.AsyncExecption;
import com.mattymatty.RegionalPathfinder.exeptions.LoaderException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.shortestpath.JohnsonShortestPaths;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class AsynchronousLoader implements Loader<Location> {

    //getting the valid locations inside the region
    @Override
    public void load(LoadData data, StatusImpl<Location[]> status) {
        new Thread(() -> {
            boolean locked = false;
            long tic = System.currentTimeMillis();
            try {
                status.setStatus(1);
                data.region.lock.lockInterruptibly();
                locked=true;
                status.setStatus(2);
                data.status = LoadData.Status.LOADING;
                Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(),
                        () -> Logger.info("Started loading region: " + data.region.getName()));
                preLoad(data);
                Semaphore tmp = new Semaphore(0);
                Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(),
                        () -> _load(0, 0, data, status, tmp));
                tmp.acquire();
                long toc = System.currentTimeMillis();
                status.totTime = (toc - tic);
                data.region.lock.unlock();
                status.setStatus(3);
            } catch (Exception ex) {
                status.ex = ex;
                if(locked)
                    data.region.lock.unlock();
                status.setStatus(4);
            }
        }).start();
    }

    private void _load(int start_i, int count, LoadData data, StatusImpl<Location[]> status, Semaphore tmp) {
        long tic = System.currentTimeMillis();
        long max_time = RegionalPathfinder.getInstance().getConfig().getInt("async-max-halt-time");
        long tick_skip = RegionalPathfinder.getInstance().getConfig().getInt("async-tick-delay");
        //visit all the points inside the region
        int i = 0;
        for (i = start_i;
             (i < data.z_size * data.y_size * data.x_size)
                     && (System.currentTimeMillis() < (tic + (max_time * 0.9)))
                ; i++) {
            int y = ((i / data.x_size) / data.z_size) % data.y_size;
            int z = (i / data.x_size) % data.z_size;
            int x = i % data.x_size;
            Location actual = cloneLoc(data.lowerCorner).add(x, y, z);
            //test if the point is a valid point
            if (data.region.getEntity().isValidLocation(actual)) {
                count++;
                Node node = new Node(actual, i);
                data.graph.addVertex(node);
                data.nodesMap.put(i, node);
            }
        }

        long toc = System.currentTimeMillis();
        status.syncTime += (toc - tic);
        status.totTime += (toc - tic);

        if (i != (data.z_size * data.y_size * data.x_size)) {
            int finalI = i;
            int finalCount = count;
            Bukkit.getScheduler().runTaskLater(RegionalPathfinder.getInstance(),
                    () -> _load(finalI, finalCount, data, status, tmp), tick_skip);
            return;
        }

        if (count != 0) {
            data.status = LoadData.Status.LOADED;
            status.setProduct(new Location[]{data.lowerCorner, data.upperCorner});
            Logger.info("Loaded region: " + data.region.getName());
            Logger.fine("server got halted for: " + status.syncTime + " ms");
            Logger.fine("total compute time: " + status.totTime + " ms");
        } else {
            data.status = null;
            Logger.info("Failed loading region: " + data.region.getName());
            Logger.fine("server got halted for: " + status.syncTime + " ms");
            Logger.fine("total compute time: " + status.totTime + " ms");
        }
        tmp.release();
    }

    //extracting the reachable locations and adding them to the graph
    @Override
    public void evaluate(LoadData data, StatusImpl<Location> status) {
        new Thread(() -> {
            boolean locked = false;
            long tic = System.currentTimeMillis();
            long toc;
            try {
                if (data.status.getValue() < LoadData.Status.LOADED.getValue())
                    throw new LoaderException("Region not loaded", data.region);
                status.setStatus(1);
                data.region.lock.lockInterruptibly();
                locked=true;
                status.setStatus(2);
                data.status = LoadData.Status.EVALUATING;
                Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(),
                        () -> Logger.info("Started evaluating region: " + data.region.getName()));
                preEvaluate(data);

                //create visit queue
                Queue<Node> queue = new LinkedList<>(data.graph.vertexSet());

                Semaphore tmp = new Semaphore(0);

                Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(),
                        () -> _evaluate(data, queue, status, tmp));

                tmp.acquire();

                StrongConnectivityAlgorithm<Node, Edge> scAlg =
                        new KosarajuStrongConnectivityInspector<>(data.graph);
                List<Graph<Node, Edge>> stronglyConnectedSubgraphs =
                        scAlg.getStronglyConnectedComponents();

                Node samplePoint = data.getNode(data.samplePoint);

                Graph<Node, Edge> scs = stronglyConnectedSubgraphs.stream().filter(g -> g.containsVertex(samplePoint)).findFirst().orElse(null);


                toc = System.currentTimeMillis();
                if (scs == null) {
                    data.status = LoadData.Status.LOADED;
                    status.totTime = (toc - tic);
                    status.setStatus(3);
                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () -> {
                        Logger.info("Failed evaluating region: " + data.region.getName());
                        Logger.fine("server got halted for: " + status.syncTime + " ms");
                        Logger.fine("total compute time: " + status.totTime + " ms");
                    });
                    return;
                }

                data.reachableGraph = scs;
                data.shortestPath = new JohnsonShortestPaths<>(data.getReachableGraph());

                status.totTime = (toc - tic);
                status.setProduct(data.samplePoint);
                data.status = LoadData.Status.EVALUATED;
                data.region.lock.unlock();
                status.setStatus(3);
                Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () -> {
                    Logger.info("Evalauted region: " + data.region.getName());
                    Logger.fine("server got halted for: " + status.syncTime + " ms");
                    Logger.fine("total compute time: " + status.totTime + " ms");
                });
            } catch (Exception ex) {
                status.ex = ex;
                if(locked)
                    data.region.lock.unlock();
                status.setStatus(4);
            }
        }).start();
    }

    private void _evaluate(LoadData data, Queue<Node> queue, StatusImpl status, Semaphore sem) {
        long tic = System.currentTimeMillis();
        long max_time = RegionalPathfinder.getInstance().getConfig().getInt("async-max-halt-time");
        long tick_skip = RegionalPathfinder.getInstance().getConfig().getInt("async-tick-delay");
        while (!queue.isEmpty() && (System.currentTimeMillis() < (tic + (max_time * 0.80)))) {
            Node act = queue.poll();

            int y = ((act.getI() / data.x_size) / data.z_size) % data.y_size;
            int z = (act.getI() / data.x_size) % data.z_size;
            int x = act.getI() % data.x_size;

            Vector[] movements = data.region.getEntity().getAllowedMovements();
            //iterate for all the possible movements
            for (Vector movement : movements) {
                int dx = movement.getBlockX();
                int dz = movement.getBlockZ();
                int dy = movement.getBlockY();

                int dest_x = (x + dx);
                int dest_y = (y + dy);
                int dest_z = (z + dz);

                if (dest_x >= 0 && dest_x < data.x_size &&
                        dest_y >= 0 && dest_y <= data.y_size &&
                        dest_z >= 0 && dest_z <= data.z_size) {
                    int dest_id = dest_x + dest_z * data.x_size + dest_y * data.x_size * data.z_size;
                    Node dest = data.nodesMap.get(dest_id);

                    //if dest is valid
                    if (dest != null &&
                            data.region.getEntity().extraMovementChecks(act.getLoc(), dest.getLoc())) {
                            Edge edge = data.graph.addEdge(act, dest);
                            data.graph.setEdgeWeight(edge,data.region.getEntity().movementCost(act.getLoc(), dest.getLoc()));
                    }
                }

            }
        }

        long toc = System.currentTimeMillis();
        status.syncTime += (toc - tic);
        status.totTime += (toc - tic);

        if (!queue.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(RegionalPathfinder.getInstance(),
                    () -> _evaluate(data, queue, status, sem), tick_skip);
            return;
        }

        sem.release();

    }

    @Override
    public void validate(LoadData data, StatusImpl status) {
        new Thread(()-> {
            boolean locked = false;
            long tic = System.currentTimeMillis();
            long toc;
            try {
                if (data.status.getValue() < LoadData.Status.EVALUATED.getValue())
                    throw new LoaderException("Region is not evaluated", data.region);

                status.setStatus(1);
                data.region.lock.lockInterruptibly();
                locked=true;
                status.setStatus(2);

                data.status = LoadData.Status.VALIDATING;

                Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(),
                        () -> Logger.info("Started validating region: " + data.region.getName()));

                Queue<Node> nodeQueue = new LinkedList<>(data.getReachableGraph().vertexSet());

                Semaphore tmp = new Semaphore(0);
                Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(),
                        ()->_validate(nodeQueue,data,status,tmp));

                tmp.acquire();
                toc = System.currentTimeMillis();
                status.totTime = (toc - tic);
                data.region.lock.unlock();
                status.setStatus(3);

                Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(),()-> {
                    Logger.info(((data.status == LoadData.Status.VALIDATED) ? "Validated" : "Failded validating") + " region: " + data.region.getName());
                    Logger.fine("server got halted for: " + status.syncTime + " ms");
                    Logger.fine("total compute time: " + status.totTime + " ms");
                });
            } catch (Exception ex) {
                status.ex = ex;
                if(locked)
                    data.region.lock.unlock();
                status.setStatus(4);
            }
        }).start();
    }

    private void _validate(Queue<Node> queue,LoadData data,StatusImpl status,Semaphore sem) {
        int i;
        long tic = System.currentTimeMillis();
        long max_time = RegionalPathfinder.getInstance().getConfig().getInt("async-max-halt-time");
        long tick_skip = RegionalPathfinder.getInstance().getConfig().getInt("async-tick-delay");

        while (!queue.isEmpty() && System.currentTimeMillis()<(tic+(max_time*0.9))){
            Node curr = queue.poll();
            assert curr != null;
            if(!data.region.getEntity().isValidLocation(curr.getLoc())){
                data.status = LoadData.Status.EVALUATED;
                sem.release();
                return;
            }
        }

        long toc = System.currentTimeMillis();

        status.totTime+=(toc-tic);
        status.syncTime+=(toc-tic);

        if(queue.isEmpty()) {
            data.status = LoadData.Status.VALIDATED;
            sem.release();
        }else{
            Bukkit.getScheduler().runTaskLater(RegionalPathfinder.getInstance(),
                    ()->_validate(queue, data, status, sem),tick_skip);
        }
    }

    private void preLoad(LoadData data) {
        try {
            Math.multiplyExact(Math.multiplyExact(data.x_size, data.y_size), data.z_size);
        } catch (ArithmeticException ex) {
            throw new LoaderException("Region is too big", data.region);
        }

        data.nodesMap.clear();
        data.graph = GraphTypeBuilder.<Node, Edge>directed().edgeClass(Edge.class).weighted(true).buildGraph();
    }

    private void preEvaluate(LoadData data) {
        Graph<Node,Edge> newGraph = GraphTypeBuilder.<Node, Edge>directed().edgeClass(Edge.class).weighted(true).buildGraph();
        data.graph.vertexSet().forEach(newGraph::addVertex);
        data.graph = newGraph;
        data.reachableGraph = null;
        data.shortestPath = null;
    }


}
