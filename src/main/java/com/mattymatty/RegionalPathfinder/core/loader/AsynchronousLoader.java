package com.mattymatty.RegionalPathfinder.core.loader;

import com.mattymatty.RegionalPathfinder.Logger;
import com.mattymatty.RegionalPathfinder.RegionalPathfinder;
import com.mattymatty.RegionalPathfinder.core.StatusImpl;
import com.mattymatty.RegionalPathfinder.core.graph.Edge;
import com.mattymatty.RegionalPathfinder.core.graph.Node;
import com.mattymatty.RegionalPathfinder.exeptions.LoaderException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class AsynchronousLoader implements Loader {

    private BukkitTask looper;
    //getting the valid locations inside the region
    @Override
    public void load(LoadData data, StatusImpl<Location[]> status) {
        Bukkit.getScheduler().runTaskAsynchronously(RegionalPathfinder.getInstance(),() -> {
            boolean locked = false;
            long tic = System.currentTimeMillis();
            try {
                status.setStatus(1);
                data.region.lock.lockInterruptibly();
                locked = true;
                status.setStatus(2);
                data.status = LoadData.Status.LOADING;
                Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(),
                        () -> Logger.info("Started loading region: " + data.region.getName()));
                preLoad(data);
                Semaphore tmp = new Semaphore(0);
                status.percentage = 0.01f;
                status.setStatus(2);
                long tick_skip = RegionalPathfinder.getInstance().getConfig().getInt("async-tick-delay");

                AtomicInteger i = new AtomicInteger(0);
                AtomicInteger count = new AtomicInteger(0);

                looper =
                        Bukkit.getScheduler().runTaskTimer(RegionalPathfinder.getInstance(),
                                () -> {
                                    _load(i, count, data, status, tmp);
                                },0,tick_skip);

                tmp.acquire();
                looper.cancel();

                if(status.hasException()) {
                    data.region.lock.unlock();
                    return;
                }

                if (count.get() != 0) {
                    data.status = LoadData.Status.LOADED;
                    status.setProduct(new Location[]{data.lowerCorner, data.upperCorner});
                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(),
                            () -> {
                                Logger.info("Loaded region: " + data.region.getName());
                                Logger.fine("server got halted for: " + status.syncTime + " ms");
                                Logger.fine("total compute time: " + status.totTime + " ms");
                            });
                } else {
                    data.status = null;
                    Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(),
                            () -> {
                                Logger.info("Failed loading region: " + data.region.getName());
                                Logger.fine("server got halted for: " + status.syncTime + " ms");
                                Logger.fine("total compute time: " + status.totTime + " ms");
                            });
                }
                long toc = System.currentTimeMillis();
                status.percentage=1f;
                status.totTime = (toc - tic);
                data.region.lock.unlock();
                status.setStatus(3);
            } catch (Exception ex) {
                status.ex = ex;
                if (locked)
                    data.region.lock.unlock();
                status.setStatus(4);
            }
        });
    }

    private void _load(AtomicInteger start_i, AtomicInteger count, LoadData data, StatusImpl<Location[]> status, Semaphore tmp) {
        long tic = System.currentTimeMillis();
        long max_time = RegionalPathfinder.getInstance().getConfig().getInt("async-max-halt-time");
        //visit all the points inside the region
        try {
            int i = 0;
            for (i = start_i.get();
                 (i < data.z_size * data.y_size * data.x_size)
                         && (System.currentTimeMillis() < (tic + (max_time * 0.9)))
                    ; i++) {
                int y = ((i / data.x_size) / data.z_size) % data.y_size;
                int z = (i / data.x_size) % data.z_size;
                int x = i % data.x_size;
                Location actual = cloneLoc(data.lowerCorner).add(x, y, z);
                if(!actual.getChunk().isLoaded())
                    throw new LoaderException("Chunk not loaded",data.region);
                //test if the point is a valid point
                if (data.region.getEntity().isValidLocation(actual)) {
                    count.incrementAndGet();
                    Node node = new Node(actual, i);
                    data.graph.addVertex(node);
                    data.nodesMap.put(i, node);
                }
            }

            long toc = System.currentTimeMillis();
            status.syncTime += (toc - tic);
            status.totTime += (toc - tic);

            status.percentage = ((float) i / (data.z_size * data.y_size * data.x_size));
            status.setStatus(2);
            start_i.set(i);

            if (i >= (data.z_size * data.y_size * data.x_size)) {
                tmp.release();
                looper.cancel();
            }
        }catch (Exception ex){
            data.status = null;
            status.ex = ex;
            status.setStatus(4);
            tmp.release();
            looper.cancel();
        }
    }

    //extracting the reachable locations and adding them to the graph
    @Override
    public void evaluate(LoadData data, StatusImpl<Location> status) {
        Bukkit.getScheduler().runTaskAsynchronously(RegionalPathfinder.getInstance(),() -> {
            boolean locked = false;
            long tic = System.currentTimeMillis();
            long toc;
            try {
                if (data.status.getValue() < LoadData.Status.LOADED.getValue())
                    throw new LoaderException("Region not loaded", data.region);
                status.setStatus(1);
                data.region.lock.lockInterruptibly();
                locked = true;
                status.setStatus(2);
                data.status = LoadData.Status.EVALUATING;
                Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(),
                        () -> Logger.info("Started evaluating region: " + data.region.getName()));
                preEvaluate(data);

                //create visit queue
                Queue<Node> queue = new LinkedList<>(data.graph.vertexSet());

                Semaphore tmp = new Semaphore(0);

                status.percentage=0.01f;
                status.setStatus(2);

                long tick_skip = RegionalPathfinder.getInstance().getConfig().getInt("async-tick-delay");

                looper =
                    Bukkit.getScheduler().runTaskTimer(RegionalPathfinder.getInstance(),
                            () -> _evaluate(data, queue, status, tmp),0,tick_skip);

                tmp.acquire();
                looper.cancel();

                if(status.hasException()){
                    data.region.lock.unlock();
                    return;
                }

                StrongConnectivityAlgorithm<Node, Edge> scAlg =
                        new KosarajuStrongConnectivityInspector<>(data.graph);
                List<Graph<Node, Edge>> stronglyConnectedSubgraphs =
                        scAlg.getStronglyConnectedComponents();

                status.percentage=0.95f;
                status.setStatus(2);

                Node samplePoint = data.getNode(data.samplePoint);

                Graph<Node, Edge> scs = stronglyConnectedSubgraphs.stream().filter(g -> g.containsVertex(samplePoint)).findFirst().orElse(null);


                toc = System.currentTimeMillis();
                status.percentage=1f;
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
                data.shortestPath = new DijkstraShortestPath<>(data.getReachableGraph());

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
                if (locked)
                    data.region.lock.unlock();
                status.setStatus(4);
            }
        });
    }

    private void _evaluate(LoadData data, Queue<Node> queue, StatusImpl status, Semaphore sem) {
        long tic = System.currentTimeMillis();
        long max_time = RegionalPathfinder.getInstance().getConfig().getInt("async-max-halt-time");
        long tick_skip = RegionalPathfinder.getInstance().getConfig().getInt("async-tick-delay");
        try {
            while (!queue.isEmpty() && (System.currentTimeMillis() < (tic + (max_time * 0.80)))) {
                Node act = queue.poll();
                assert act != null;
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
                            dest_y >= 0 && dest_y < data.y_size &&
                            dest_z >= 0 && dest_z < data.z_size) {
                        int dest_id = dest_x + dest_z * data.x_size + dest_y * data.x_size * data.z_size;
                        Node dest = data.nodesMap.get(dest_id);

                        //if dest is valid
                        if (dest != null &&
                                data.region.getEntity().extraMovementChecks(act.getLoc(), dest.getLoc())) {
                            Edge edge = data.graph.addEdge(act, dest);
                            data.graph.setEdgeWeight(edge, data.region.getEntity().movementCost(act.getLoc(), dest.getLoc()));
                        }
                    }

                }
            }

            long toc = System.currentTimeMillis();
            status.syncTime += (toc - tic);
            status.totTime += (toc - tic);

            int tot = data.graph.vertexSet().size();

            status.percentage = ((tot - queue.size()) / (float) tot) * (0.90f);
            status.setStatus(2);

            if (queue.isEmpty()) {
                sem.release();
                looper.cancel();
            }
        }catch (Exception ex){
            data.status = LoadData.Status.LOADED;
            status.ex = ex;
            status.setStatus(4);
            sem.release();
            looper.cancel();
        }
    }

    @Override
    public void validate(LoadData data, StatusImpl<Boolean> status) {
        status.setProduct(false);
        Bukkit.getScheduler().runTaskAsynchronously(RegionalPathfinder.getInstance(),() -> {
            boolean locked = false;
            long tic = System.currentTimeMillis();
            long toc;
            try {
                if (data.status.getValue() < LoadData.Status.EVALUATED.getValue())
                    throw new LoaderException("Region is not evaluated", data.region);

                status.setStatus(1);
                data.region.lock.lockInterruptibly();
                locked = true;
                status.setStatus(2);

                data.status = LoadData.Status.VALIDATING;

                Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(),
                        () -> Logger.info("Started validating region: " + data.region.getName()));

                Queue<Node> nodeQueue = new LinkedList<>(data.getReachableGraph().vertexSet());

                status.percentage=0.01f;
                status.setStatus(2);
                Semaphore tmp = new Semaphore(0);

                long tick_skip = RegionalPathfinder.getInstance().getConfig().getInt("async-tick-delay");

                looper =
                    Bukkit.getScheduler().runTaskTimer(RegionalPathfinder.getInstance(),
                            () -> _validate(nodeQueue, data, status, tmp),0,tick_skip);

                tmp.acquire();
                looper.cancel();

                if(status.hasException()){
                    data.region.lock.unlock();
                    return;
                }

                if(data.status == LoadData.Status.VALIDATING) {
                    data.status = LoadData.Status.VALIDATED;
                    status.setProduct(true);
                }
                toc = System.currentTimeMillis();
                status.totTime = (toc - tic);
                status.percentage=1f;
                data.region.lock.unlock();
                status.setStatus(3);

                Bukkit.getScheduler().runTask(RegionalPathfinder.getInstance(), () -> {
                    Logger.info(((data.status == LoadData.Status.VALIDATED) ? "Validated" : "Failded validating") + " region: " + data.region.getName());
                    Logger.fine("server got halted for: " + status.syncTime + " ms");
                    Logger.fine("total compute time: " + status.totTime + " ms");
                });
            } catch (Exception ex) {
                status.ex = ex;
                if (locked)
                    data.region.lock.unlock();
                status.setStatus(4);
            }
        });
    }

    private void _validate(Queue<Node> queue, LoadData data, StatusImpl status, Semaphore sem) {
        int i;
        long tic = System.currentTimeMillis();
        long max_time = RegionalPathfinder.getInstance().getConfig().getInt("async-max-halt-time");
        long tick_skip = RegionalPathfinder.getInstance().getConfig().getInt("async-tick-delay");

        try {
            while (!queue.isEmpty() && System.currentTimeMillis() < (tic + (max_time * 0.9))) {
                Node curr = queue.poll();
                assert curr != null;
                if (!data.region.getEntity().isValidLocation(curr.getLoc())) {
                    data.status = LoadData.Status.EVALUATED;
                    sem.release();
                    return;
                }
            }

            long toc = System.currentTimeMillis();

            status.totTime += (toc - tic);
            status.syncTime += (toc - tic);

            int tot = data.reachableGraph.vertexSet().size();

            status.percentage = ((tot - queue.size()) / (float) tot);
            status.setStatus(2);

            if (queue.isEmpty()) {
                sem.release();
                looper.cancel();
            }
        }catch (Exception ex){
            data.status = LoadData.Status.EVALUATED;
            status.ex = ex;
            status.setStatus(4);
            sem.release();
            looper.cancel();
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
        Graph<Node, Edge> newGraph = GraphTypeBuilder.<Node, Edge>directed().edgeClass(Edge.class).weighted(true).buildGraph();
        data.graph.vertexSet().forEach(newGraph::addVertex);
        data.graph = newGraph;
        data.reachableGraph = null;
        data.shortestPath = null;
    }


}
