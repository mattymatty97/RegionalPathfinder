package com.mattymatty.RegionalPathfinder.core.loader;

import com.github.quickhull3d.QuickHull3D;
import com.mattymatty.RegionalPathfinder.Logger;
import com.mattymatty.RegionalPathfinder.core.StatusImpl;
import com.mattymatty.RegionalPathfinder.core.graph.Edge;
import com.mattymatty.RegionalPathfinder.core.graph.Node;
import com.mattymatty.RegionalPathfinder.exeptions.LoaderException;
import com.mattymatty.RegionalPathfinder.exeptions.RegionException;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class SynchronousLoader implements Loader {

    //getting the valid locations inside the region
    @Override
    public void load(LoadData data, StatusImpl<Location[]> status) {
        long tic = System.currentTimeMillis();
        try {
            status.setStatus(1);
            data.status = LoadData.Status.LOADING;
            Logger.info("Started loading region: " + data.region.getName());
            preLoad(data);
            int count = 0;
            for (int i = 0; (i < data.z_size * data.y_size * data.x_size); i++) {
                int y = ((i / data.x_size) / data.z_size) % data.y_size;
                int z = (i / data.x_size) % data.z_size;
                int x = i % data.x_size;
                Location actual = cloneLoc(data.lowerCorner).add(x, y, z);
                if(!actual.getChunk().isLoaded())
                    throw new LoaderException("Chunk not loaded",data.region);
                //test if the point is a valid point
                if (data.region.getEntity().isValidLocation(actual)) {
                    count++;
                    Node node = new Node(actual, i);
                    data.graph.addVertex(node);
                    data.nodesMap.put(i, node);
                }
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
            long toc = System.currentTimeMillis();
            status.percentage = 1f;
            status.totTime = (toc - tic);
            status.setStatus(3);
        } catch (Exception ex) {
            Logger.info("Failed loading region: " + data.region.getName());
            Logger.fine("server got halted for: " + status.syncTime + " ms");
            Logger.fine("total compute time: " + status.totTime + " ms");
            status.ex = ex;
            status.setStatus(4);
        }
    }

    //extracting the reachable locations and adding them to the graph
    @Override
    public void evaluate(LoadData data, StatusImpl<Location> status) {
        long tic = System.currentTimeMillis();
        long toc;
        try {
            if (data.status.getValue() < LoadData.Status.LOADED.getValue())
                throw new LoaderException("Region not loaded", data.region);
            status.setStatus(1);
            status.setStatus(2);
            if (data.samplePoint.getWorld() != data.upperCorner.getWorld()) {
                throw new RegionException("samplepoint is not in the world", data.region);
            }
            if (!data.region.getValidLocations().contains(data.samplePoint)) {
                throw new RegionException("samplepoint is not in the region", data.region);
            }
            data.status = LoadData.Status.EVALUATING;
            Logger.info("Started evaluating region: " + data.region.getName());
            preEvaluate(data);

            //create visit queue
            Queue<Node> queue = new LinkedList<>(data.graph.vertexSet());

            while (!queue.isEmpty()) {
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
                            Edge edge = data.graph.addEdge(act, dest).setSource(act).setDest(dest);
                            data.graph.setEdgeWeight(edge, data.region.getEntity().movementCost(act.getLoc(), dest.getLoc()));
                        }
                    }

                }
            }

            StrongConnectivityAlgorithm<Node, Edge> scAlg =
                    new KosarajuStrongConnectivityInspector<>(data.graph);
            List<Graph<Node, Edge>> stronglyConnectedSubgraphs =
                    scAlg.getStronglyConnectedComponents();

            status.percentage = 0.95f;
            status.setStatus(2);

            Node samplePoint = data.getNode(data.samplePoint);

            Graph<Node, Edge> scs = stronglyConnectedSubgraphs.stream().filter(g -> g.containsVertex(samplePoint)).findFirst().orElse(null);


            toc = System.currentTimeMillis();
            status.percentage = 1f;
            if (scs == null) {
                data.status = LoadData.Status.LOADED;
                status.totTime = (toc - tic);
                status.setStatus(3);
                    Logger.info("Failed evaluating region: " + data.region.getName());
                    Logger.fine("server got halted for: " + status.syncTime + " ms");
                    Logger.fine("total compute time: " + status.totTime + " ms");
                return;
            }

            data.reachableGraph = scs;
            data.shortestPath = new DijkstraShortestPath<>(data.getReachableGraph());
            QuickHull3D hull = new QuickHull3D();
            hull.build(scs.vertexSet().stream().map(Node::getLoc).flatMapToDouble((l) -> DoubleStream.of(l.getX(), l.getY(), l.getZ())).toArray());

            data.boundary = Arrays.stream(hull.getVertices()).map((p) -> new Location(data.lowerCorner.getWorld(), p.x, p.y, p.z)).collect(Collectors.toList());

            status.totTime = (toc - tic);
            status.setProduct(data.samplePoint);
            data.status = LoadData.Status.EVALUATED;
            status.setStatus(3);
            Logger.info("Evalauted region: " + data.region.getName());
            Logger.fine("server got halted for: " + status.syncTime + " ms");
            Logger.fine("total compute time: " + status.totTime + " ms");
        } catch (Exception ex) {
            Logger.info("Failed evaluating region: " + data.region.getName());
            Logger.fine("server got halted for: " + status.syncTime + " ms");
            Logger.fine("total compute time: " + status.totTime + " ms");
            status.ex = ex;
            status.setStatus(4);
        }
    }

    @Override
    public void validate(LoadData data, StatusImpl<Boolean> status) {
        status.setProduct(false);
        long tic = System.currentTimeMillis();
        long toc;
        try {
            if (data.status.getValue() < LoadData.Status.EVALUATED.getValue())
                throw new LoaderException("Region is not evaluated", data.region);

            status.setStatus(1);
            status.setStatus(2);

            data.status = LoadData.Status.VALIDATING;

            Logger.info("Started validating region: " + data.region.getName());

            if (data.getReachableGraph().vertexSet().parallelStream()
                    .anyMatch(n -> !data.region.getEntity().isValidLocation(n.getLoc()))
                    ||
                    data.getReachableGraph().edgeSet().parallelStream()
                            .anyMatch(e -> !data.region.getEntity().extraMovementChecks(e.getSource().getLoc(), e.getDest().getLoc()))) {
                data.status = LoadData.Status.EVALUATED;
            }

            if (data.status == LoadData.Status.VALIDATING) {
                data.status = LoadData.Status.VALIDATED;
                status.setProduct(true);
            }
            toc = System.currentTimeMillis();
            status.totTime = (toc - tic);
            status.percentage = 1f;
            status.setStatus(3);

            Logger.info(((data.status == LoadData.Status.VALIDATED) ? "Validated" : "Failded validating") + " region: " + data.region.getName());
            Logger.fine("server got halted for: " + status.syncTime + " ms");
            Logger.fine("total compute time: " + status.totTime + " ms");
        } catch (Exception ex) {
            Logger.info("Failded validating region: " + data.region.getName());
            Logger.fine("server got halted for: " + status.syncTime + " ms");
            Logger.fine("total compute time: " + status.totTime + " ms");
            status.ex = ex;
            status.setStatus(4);
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
