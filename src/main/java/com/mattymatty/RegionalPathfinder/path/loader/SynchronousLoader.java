package com.mattymatty.RegionalPathfinder.path.loader;

import com.mattymatty.RegionalPathfinder.graph.Graph;
import com.mattymatty.RegionalPathfinder.path.BlockNode;
import com.mattymatty.RegionalPathfinder.path.MovementCost;
import org.bukkit.Location;
import org.bukkit.block.data.type.Slab;
import org.bukkit.material.Stairs;

import javax.crypto.spec.DESedeKeySpec;
import java.util.*;

public class SynchronousLoader implements Loader {

    //getting the valid locations inside the region
    @Override
    public void load(LoadData data) {
        data.status = LoadData.Status.LOADING;
        //visit all the points inside the region
        for (int i = 0; i < data.z_size * data.y_size * data.x_size; i++) {
            int y = i / data.y_size;
            int z = (i % data.y_size) / data.z_size;
            int x = ((i % data.y_size) % data.z_size) / data.x_size;
            Location actual = data.lowerCorner.clone().add(x, y, z);
            //test if the point is a valid point
            if (!data.entity.isValidLocation(actual))
                data.map[x][y][z] = 0;
        }
        data.status = LoadData.Status.LOADED;
    }

    //extracting the reachable locations and adding them to the graph
    @Override
    public void evaluate(LoadData data) {
        try {
            //create the queue for in deept research
            Queue<BlockNode> findQueue = new LinkedList<>();
            //get the coordinates of the sampleBlock
            int x = data.samplePoint.getBlockX() - data.lowerCorner.getBlockX();
            int y = data.samplePoint.getBlockY() - data.lowerCorner.getBlockY();
            int z = data.samplePoint.getBlockZ() - data.lowerCorner.getBlockZ();
            int id = x + z * data.x_size + y * data.x_size * data.z_size;
            data.map[x][y][z] = 2;
            //add the point to the graph
            BlockNode node = new BlockNode(data.graph, id, data.samplePoint);
            data.graph.addNode(node);

            //add the block to the queue
            findQueue.add(node);

            //till there are no more reachable points
            while (!findQueue.isEmpty()) {
                node = findQueue.poll();

                id = node.getN_id();

                y = id / data.y_size;
                z = (id % data.y_size) / data.z_size;
                x = ((id % data.y_size) % data.z_size) / data.x_size;

                //iterate for all the possible movements
                for (int i = 0; i < 3 * 3 * 3; i++) {
                    int dy = (id / 3) - 1;
                    int dz = (id % 3 / 3) - 1;
                    int dx = (id % 3 % 3 / 3) - 1;
                    int qt = data.entity.allowedMovement(dx, dy, dz);

                    //if this movement is allowed
                    for (int j = 1; j <= qt; j++) {
                        int next_x = (x + dx * j);
                        int next_y = (y + dy * j);
                        int next_z = (z + dz * j);
                        if (next_x >= 0 && next_x < data.x_size
                                && next_y >= 0 && next_y < data.y_size
                                && next_z >= 0 && next_z < data.z_size
                        )
                            if (data.map[next_x][next_y][next_z] == 1) {
                                data.map[next_x][next_y][next_z] = 2;
                                int next_id = next_x + next_z * data.x_size + next_y * data.x_size * data.z_size;

                                Location next_loc = data.lowerCorner.clone().add(next_x,next_y,next_z);
                                BlockNode next_node = new BlockNode(data.graph,next_id,next_loc);

                                findQueue.add(next_node);

                                data.graph.addNode(next_node);
                                Graph.Edge edge = new Graph.Edge(node,next_node,cost(data.cost,node.getLocation(),next_loc));
                            }
                    }
                }

            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void validate(LoadData data) {

    }

    private double cost(MovementCost cost, Location source, Location dest){
        double result = 0;

        int dx = dest.getBlockX() - source.getBlockX();
        int dy = dest.getBlockY() - source.getBlockY();
        int dz = dest.getBlockZ() - source.getBlockZ();

        if(Math.abs(dy)==0){
            result+= cost.getDefaultMovement();
        }else{
            if(dy==1 && (dest.getBlock().getType().getClass().isAssignableFrom(Stairs.class) || dest.getBlock().getType().getClass().isAssignableFrom(Slab.class))){
                result+= cost.getStair_slab();
            }else
                if(dy==-1 && (source.getBlock().getType().getClass().isAssignableFrom(Stairs.class) || source.getBlock().getType().getClass().isAssignableFrom(Slab.class))){
                result+= cost.getStair_slab();
            }else{
                result+=cost.getJump();
            }
        }

        if(Math.abs(dx)>0 && Math.abs(dz)>0)
            result+=cost.getDiagonalAddition();

        result+=cost.getBlockCost(dest.getBlock().getType());

        return result;
    }
}
