package com.mattymatty.RegionalPathfinder.core.loader;

import com.mattymatty.RegionalPathfinder.Logger;
import com.mattymatty.RegionalPathfinder.api.Status;
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

import java.util.*;

public class SynchronousLoader implements Loader<Location> {

    //getting the valid locations inside the region
    @Override
    public void load(LoadData data, StatusImpl<Location[]> status) {
        long tic = System.currentTimeMillis();
        try {
            status.setStatus(2);
            if (!data.region.lock.tryLock())
                throw new AsyncExecption("Async operation still running on this region", data.region);
            data.status = LoadData.Status.LOADING;
            Logger.info("Started loading region: " + data.region.getName());
            preLoad(data);
            int count = 0;
            //visit all the points inside the region
            for (int i = 0; i < data.z_size * data.y_size * data.x_size; i++) {
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
            if (count != 0) {
                data.status = LoadData.Status.LOADED;
                status.syncTime = status.totTime = (toc-tic);
                status.setProduct(new Location[]{data.lowerCorner, data.upperCorner});
                status.setStatus(3);
                Logger.info("Loaded region: " + data.region.getName());
                Logger.fine("elapsed: " + (toc - tic) + " ms");
            } else {
                data.status = null;
                status.syncTime = status.totTime = (toc-tic);
                status.setStatus(3);
                Logger.info("Failed loading region: " + data.region.getName());
                Logger.fine("elapsed: " + (toc - tic) + " ms");
            }
        }catch (Exception ex){
            status.ex = ex;
            status.setStatus(4);
        }
        data.region.lock.unlock();
    }

    //extracting the reachable locations and adding them to the graph
    @Override
    public void evaluate(LoadData data, StatusImpl<Location> status) {
        long tic = System.currentTimeMillis();
        long toc;
        try {
            if (data.status.getValue() < LoadData.Status.LOADED.getValue())
                throw new LoaderException("Region not loaded", data.region);
            status.setStatus(2);
            if (!data.region.lock.tryLock())
                throw new AsyncExecption("Async operation still running on this region", data.region);
            data.status = LoadData.Status.EVALUATING;
            Logger.info("Started evaluating region: " + data.region.getName());
            preEvaluate(data);
            try {

                //create visit queue
                Queue<Node> queue = new LinkedList<>(data.graph.vertexSet());

                while (!queue.isEmpty()) {
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

                StrongConnectivityAlgorithm<Node, Edge> scAlg =
                        new KosarajuStrongConnectivityInspector<>(data.graph);
                List<Graph<Node, Edge>> stronglyConnectedSubgraphs =
                        scAlg.getStronglyConnectedComponents();

                Node samplePoint = data.getNode(data.samplePoint);

                Graph<Node, Edge> scs = stronglyConnectedSubgraphs.stream().filter(g -> g.containsVertex(samplePoint)).findFirst().orElse(null);


                toc = System.currentTimeMillis();
                if (scs == null) {
                    data.status = LoadData.Status.LOADED;
                    status.syncTime = status.totTime = (toc-tic);
                    status.setStatus(3);
                    Logger.info("Failed evaluating region: " + data.region.getName());
                    Logger.fine("elapsed: " + (toc - tic) + " ms");
                    return;
                }

                data.reachableGraph = scs;
                data.shortestPath = new JohnsonShortestPaths<>(data.getReachableGraph());

            } catch (Exception ex) {
                toc = System.currentTimeMillis();
                status.syncTime = status.totTime = (toc-tic);
                throw new LoaderException(ex, data.region);
            }

            status.syncTime = status.totTime = (toc-tic);
            status.setProduct(data.samplePoint);
            status.setStatus(3);
            Logger.info("Evalauted region: " + data.region.getName());
            Logger.fine("elapsed: " + (toc - tic) + " ms");

            data.status = LoadData.Status.EVALUATED;

            data.region.lock.unlock();
        }catch (Exception ex){
            status.ex = ex;
            status.setStatus(4);
        }
    }

    @Override
    public void validate(LoadData data, StatusImpl status) {
        long tic = System.currentTimeMillis();
        try {
            if (data.status.getValue() < LoadData.Status.EVALUATED.getValue())
                throw new LoaderException("Region is not evaluated", data.region);

            status.setStatus(2);
            if (!data.region.lock.tryLock())
                throw new AsyncExecption("Async operation still running on this region", data.region);

            data.status = LoadData.Status.VALIDATING;

            Logger.info("Started validating region: " + data.region.getName());

            if (data.reachableGraph.vertexSet().parallelStream().anyMatch(n -> !data.region.getEntity().isValidLocation(n.getLoc())))
                data.status = LoadData.Status.EVALUATED;
            else
                data.status = LoadData.Status.VALIDATED;


            long toc = System.currentTimeMillis();
            status.syncTime = status.totTime = (toc-tic);
            status.setStatus(3);

            Logger.info(((data.status == LoadData.Status.VALIDATED) ? "Validated" : "Failded validating") + " region: " + data.region.getName());
            Logger.info("elapsed: " + (toc - tic) + " ms");
        }catch (Exception ex){
            status.ex = ex;
            status.setStatus(4);
        }
        data.region.lock.unlock();
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
        data.graph.removeAllEdges(data.graph.edgeSet());
    }


}
