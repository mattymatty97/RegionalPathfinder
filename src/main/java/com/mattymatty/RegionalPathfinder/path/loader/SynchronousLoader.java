package com.mattymatty.RegionalPathfinder.path.loader;

import com.mattymatty.RegionalPathfinder.graph.Graph;
import com.mattymatty.RegionalPathfinder.path.BlockNode;
import com.mattymatty.RegionalPathfinder.path.MovementCost;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.util.Vector;

import java.util.*;

public class SynchronousLoader implements Loader {

    //getting the valid locations inside the region
    @Override
    public void load(LoadData data) {
        data.status = LoadData.Status.LOADING;
        int count=0;
        //visit all the points inside the region
        for (int i = 0; i < data.z_size * data.y_size * data.x_size; i++) {
            int y = i / data.y_size;
            int z = (i % data.y_size) / data.z_size;
            int x = ((i % data.y_size) % data.z_size) / data.x_size;
            Location actual = data.lowerCorner.clone().add(x, y, z);
            //test if the point is a valid point
            if (!data.entity.isValidLocation(actual))
                data.map[x][y][z] = 0;
            else
                count++;
        }
        if(count!=0)
            data.status = LoadData.Status.LOADED;
        else
            data.status = null;
    }

    //extracting the reachable locations and adding them to the graph
    @Override
    public void evaluate(LoadData data) {
        if(data.status.getValue() < LoadData.Status.LOADED.getValue())
            return;
        data.status = LoadData.Status.EVALUATING;
        try {
            //create the queue for in deept research
            Queue<BlockNode> findQueue = new LinkedList<>();
            //get the coordinates of the sampleBlock
            int x = data.samplePoint.getBlockX() - data.lowerCorner.getBlockX();
            int y = data.samplePoint.getBlockY() - data.lowerCorner.getBlockY();
            int z = data.samplePoint.getBlockZ() - data.lowerCorner.getBlockZ();
            int id = x + z * data.x_size + y * data.x_size * data.z_size;
            if(data.map[x][y][z] == 0){
                data.status = LoadData.Status.LOADED;
                return;
            }
            data.map[x][y][z] = 2;
            //add the point to the graph
            BlockNode node = new BlockNode(data.graph, id, data.samplePoint);
            data.graph.addNode(node);
            data.nodesMap.put(node.getLocation(),node);

            //add the block to the queue
            findQueue.add(node);

            int count=0;
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
                        //if the movement is indie region
                        if (next_x >= 0 && next_x < data.x_size
                                && next_y >= 0 && next_y < data.y_size
                                && next_z >= 0 && next_z < data.z_size
                        )
                            //if dest is valid
                            if (data.map[next_x][next_y][next_z] == 1) {
                                count++;
                                data.map[next_x][next_y][next_z] = 2;
                                int next_id = next_x + next_z * data.x_size + next_y * data.x_size * data.z_size;

                                Location next_loc = data.lowerCorner.clone().add(next_x,next_y,next_z);
                                BlockNode next_node = new BlockNode(data.graph,next_id,next_loc);

                                findQueue.add(next_node);

                                data.graph.addNode(next_node);
                                data.nodesMap.put(next_node.getLocation(),next_node);
                                Graph.Edge edge1 = new Graph.Edge(node,next_node,cost(data.cost,node.getLocation(),next_loc));
                                Graph.Edge edge2 = new Graph.Edge(next_node,node,cost(data.cost,next_loc,node.getLocation()));
                                data.graph.addEdge(edge1);
                                data.graph.addEdge(edge2);
                                break;
                            }
                    }
                }

            }

            if(count==0){
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
        if(data.status.getValue() < LoadData.Status.EVALUATED.getValue())
        data.status = LoadData.Status.VALIDATING;
        int i;
        for (i = 0; i < data.z_size * data.y_size * data.x_size; i++) {
            int y = i / data.y_size;
            int z = (i % data.y_size) / data.z_size;
            int x = ((i % data.y_size) % data.z_size) / data.x_size;
            if(data.map[x][y][z] == 2){
                Location actual = data.lowerCorner.clone().add(x, y, z);
                if(!data.entity.isValidLocation(actual))
                    break;
            }
        }

        if(i < data.z_size * data.y_size * data.x_size){
            data.status = LoadData.Status.EVALUATED;
            return;
        }

        data.status = LoadData.Status.VALIDATED;
    }

    private double cost(MovementCost cost, Location source, Location dest){
        double result = 0;

        int dx = dest.getBlockX() - source.getBlockX();
        int dy = dest.getBlockY() - source.getBlockY();
        int dz = dest.getBlockZ() - source.getBlockZ();

        if(Math.abs(dy)==0){
            result+= cost.getDefaultMovement();
        }else{
            if(isStairMovement(source,dest)){
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

    private boolean isStairMovement(Location start, Location end){
        int dx = end.getBlockX() - start.getBlockX();
        int dy = end.getBlockY() - start.getBlockY();
        int dz = end.getBlockZ() - start.getBlockZ();
        Block block;
        if(dy==1){
            block = end.getBlock();
            if (block.getBlockData() instanceof Slab)
                return true;
            if (block.getBlockData() instanceof Stairs){
                Stairs stair = (Stairs)block.getBlockData();
                if(stair.getHalf() == Bisected.Half.TOP)
                    return false;
                Vector direction = stair.getFacing().getOppositeFace().getDirection();
                if(stair.getShape() == Stairs.Shape.STRAIGHT)
                    return direction.equals(new Vector(dx, 0, dz));
                if(stair.getShape() == Stairs.Shape.OUTER_LEFT)
                    return direction.equals(new Vector(dx, 0, dz)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(-90)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(-45));
                if(stair.getShape() == Stairs.Shape.OUTER_RIGHT)
                    return direction.equals(new Vector(dx, 0, dz)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(90)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(45));
                if(stair.getShape() == Stairs.Shape.INNER_LEFT)
                    return  direction.equals(new Vector(dx, 0, dz).rotateAroundY(-45));
                if(stair.getShape() == Stairs.Shape.INNER_RIGHT)
                    return  direction.equals(new Vector(dx, 0, dz).rotateAroundY(45));
            }
        }else if(dy==-1){
            block = start.getBlock();
            if (block.getBlockData() instanceof Slab)
                return true;
            if (block.getBlockData() instanceof Stairs){
                Stairs stair = (Stairs)block.getBlockData();
                if(stair.getHalf() == Bisected.Half.TOP)
                    return false;
                Vector direction = stair.getFacing().getDirection();
                if(stair.getShape() == Stairs.Shape.STRAIGHT)
                    return direction.equals(new Vector(dx, 0, dz));
                if(stair.getShape() == Stairs.Shape.OUTER_LEFT)
                    return direction.equals(new Vector(dx, 0, dz)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(-90)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(-45));
                if(stair.getShape() == Stairs.Shape.OUTER_RIGHT)
                    return direction.equals(new Vector(dx, 0, dz)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(90)) ||
                            direction.equals(new Vector(dx, 0, dz).rotateAroundY(45));
                if(stair.getShape() == Stairs.Shape.INNER_LEFT)
                    return  direction.equals(new Vector(dx, 0, dz).rotateAroundY(-45));
                if(stair.getShape() == Stairs.Shape.INNER_RIGHT)
                    return  direction.equals(new Vector(dx, 0, dz).rotateAroundY(45));
            }
        }
        return false;
    }
}
