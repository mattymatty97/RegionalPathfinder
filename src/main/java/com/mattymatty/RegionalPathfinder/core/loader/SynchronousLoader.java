package com.mattymatty.RegionalPathfinder.core.loader;

import com.mattymatty.RegionalPathfinder.core.graph.Graph;
import com.mattymatty.RegionalPathfinder.core.graph.BlockNode;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.lang.invoke.LambdaConversionException;
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
            if (!data.region.getEntity().isValidLocation(actual))
                data.map[x][y][z] = 0;
            else
                count++;
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
            return;
        data.status = LoadData.Status.EVALUATING;
        preEvaluate(data);
        try {
            //create the queue for in deept research
            Queue<BlockNode> findQueue = new LinkedList<>();
            //get the coordinates of the sampleBlock
            int x = data.samplePoint.getBlockX() - data.lowerCorner.getBlockX();
            int y = data.samplePoint.getBlockY() - data.lowerCorner.getBlockY();
            int z = data.samplePoint.getBlockZ() - data.lowerCorner.getBlockZ();
            int id = x + z * data.x_size + y * data.x_size * data.z_size;
            if (data.map[x][y][z] == 0) {
                data.status = LoadData.Status.LOADED;
                return;
            }
            data.map[x][y][z] = 2;
            //add the point to the graph
            BlockNode node = new BlockNode(data.region.graph, id, data.region.graph.getAndIncrementNodeID(), data.samplePoint);
            data.region.graph.addNode(node);
            data.nodesMap.put(data.samplePoint,node);

            //add the block to the queue
            findQueue.add(node);

            int count = 0;
            //till there are no more reachable points
            while (!findQueue.isEmpty()) {
                node = findQueue.poll();

                id = node.getI();

                y = ((id / data.x_size) / data.z_size) % data.y_size;
                z = (id / data.x_size) % data.z_size;
                x = id % data.x_size;

                Vector[] movements = data.region.getEntity().getAllowedMovements();

                //iterate for all the possible movements
                for (int i=0;i<movements.length;i++) {
                    Vector movement = movements[i];
                    int dx = movement.getBlockX();
                    int dz = movement.getBlockZ();
                    int dy = movement.getBlockY();

                    int next_x = (x + dx);
                    int next_y = (y + dy);
                    int next_z = (z + dz);
                    //if the movement is inside region
                    if (next_x >= 0 && next_x < data.x_size
                            && next_y >= 0 && next_y < data.y_size
                            && next_z >= 0 && next_z < data.z_size
                    ) {

                        Location next_loc = cloneLoc(data.lowerCorner).add(next_x, next_y, next_z);
                        //if dest is valid
                        if (data.map[next_x][next_y][next_z] == 1
                                &&
                                data.region.getEntity().extraMovementChecks(node.getLocation(), next_loc)) {
                            count++;
                            data.map[next_x][next_y][next_z] = 2;
                            int next_id = next_x + next_z * data.x_size + next_y * data.x_size * data.z_size;
                            BlockNode next_node = new BlockNode(data.region.graph, next_id, data.region.graph.getAndIncrementNodeID(), next_loc);

                            findQueue.add(next_node);

                            data.region.graph.addNode(next_node);
                            data.nodesMap.put(next_loc,next_node);
                            Graph.Edge edge1 = new Graph.Edge(node, next_node, data.region.getEntity().movementCost(node.getLocation(), next_loc));
                            Graph.Edge edge2 = new Graph.Edge(next_node, node, data.region.getEntity().movementCost(next_loc, node.getLocation()));
                            data.region.graph.addEdge(edge1);
                            data.region.graph.addEdge(edge2);
                        }
                    }
                }


            }

            if (count == 0) {
                data.status = LoadData.Status.LOADED;
                return;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        data.status = LoadData.Status.EVALUATED;
    }

    @Override
    public void validate(LoadData data) {
        if (data.status.getValue() < LoadData.Status.EVALUATED.getValue())
            data.status = LoadData.Status.VALIDATING;
        int i;
        for (i = 0; i < data.z_size * data.y_size * data.x_size; i++) {
            int y = ((i / data.x_size) / data.z_size) % data.y_size;
            int z = (i / data.x_size) % data.z_size;
            int x = i % data.x_size;
            if (data.map[x][y][z] == 2) {
                Location actual = cloneLoc(data.lowerCorner).add(x, y, z);
                if (!data.region.getEntity().isValidLocation(actual))
                    break;
            }
        }

        if (i < data.z_size * data.y_size * data.x_size) {
            data.status = LoadData.Status.EVALUATED;
            return;
        }

        data.status = LoadData.Status.VALIDATED;
    }

    private void preLoad(LoadData data){
        for (int i = 0; i < data.z_size * data.y_size * data.x_size; i++) {
            int y = ((i / data.x_size) / data.z_size) % data.y_size;
            int z = (i / data.x_size) % data.z_size;
            int x = i % data.x_size;
            data.map[x][y][z] = 1;
        }
    }

    private void preEvaluate(LoadData data){
        data.nodesMap.clear();
        data.region.graph = new Graph();
        for (int i = 0; i < data.z_size * data.y_size * data.x_size; i++) {
            int y = ((i / data.x_size) / data.z_size) % data.y_size;
            int z = (i / data.x_size) % data.z_size;
            int x = i % data.x_size;
            if(data.map[x][y][z] > 1)
                data.map[x][y][z] = 1;
        }
    }


}
