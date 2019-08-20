package com.mattymatty.RegionalPathfinder.core.loader;

import com.mattymatty.RegionalPathfinder.core.graph.Edge;
import com.mattymatty.RegionalPathfinder.core.graph.Node;
import com.mattymatty.RegionalPathfinder.exeptions.LoaderException;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.util.*;

public class SynchronousLoader implements Loader {

    //getting the valid locations inside the region
    @Override
    public void load(LoadData data) {
        data.status = LoadData.Status.LOADING;
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
        if (count != 0)
            data.status = LoadData.Status.LOADED;
        else
            data.status = null;
    }

    //extracting the reachable locations and adding them to the graph
    @Override
    public void evaluate(LoadData data) {
        if (data.status.getValue() < LoadData.Status.LOADED.getValue())
            throw new LoaderException("Region not loaded", data.region);
        data.status = LoadData.Status.EVALUATING;
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

                    int dest_id = dest_x + dest_z * data.x_size + dest_y * data.x_size * data.z_size;
                    Node dest = data.nodesMap.get(dest_id);

                    //if dest is valid
                    if (dest!=null &&
                            data.region.getEntity().extraMovementChecks(act.getLoc(), dest.getLoc())) {
                        data.graph.addEdge(act, dest).setWeight(data.region.getEntity().movementCost(act.getLoc(), dest.getLoc()));
                    }
                }
            }

            StrongConnectivityAlgorithm<Node, Edge> scAlg =
                    new KosarajuStrongConnectivityInspector<>(data.graph);
            List<Graph<Node, Edge>> stronglyConnectedSubgraphs =
                    scAlg.getStronglyConnectedComponents();

            Node samplePoint = data.getNode(data.samplePoint);

            Graph<Node, Edge> scs = stronglyConnectedSubgraphs.stream().filter(g -> g.containsVertex(samplePoint)).findFirst().orElse(null);

            if (scs == null) {
                data.status = LoadData.Status.LOADED;
                return;
            }

            data.reachableGraph = scs;
            data.shortestPath = new DijkstraShortestPath<>(data.getReachableGraph());

        } catch (Exception ex) {
            throw new LoaderException(ex, data.region);
        }

        data.status = LoadData.Status.EVALUATED;
    }

    @Override
    public void validate(LoadData data) {
        if (data.status.getValue() < LoadData.Status.EVALUATED.getValue())
            throw new LoaderException("Region is not evaluated", data.region);

        data.status = LoadData.Status.VALIDATING;

        if(data.reachableGraph.vertexSet().parallelStream().anyMatch(n->!data.region.getEntity().isValidLocation(n.getLoc())))
            data.status = LoadData.Status.EVALUATED;
        else
            data.status = LoadData.Status.VALIDATED;
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
