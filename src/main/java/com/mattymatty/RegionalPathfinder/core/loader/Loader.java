package com.mattymatty.RegionalPathfinder.core.loader;

import org.bukkit.Location;
import java.util.LinkedList;
import java.util.List;

public interface Loader {
    void load(LoadData data);

    void evaluate(LoadData data);

    void validate(LoadData data);

    default List<Location> getValid(LoadData data){
        List<Location> result = new LinkedList<>();

        if(data.getStatus().getValue() >= LoadData.Status.LOADED.getValue()){
            for (int i = 0; i < data.z_size * data.y_size * data.x_size; i++) {
                int y = (( i /data. x_size ) / data.z_size ) % data.y_size;
                int z = ( i / data.x_size ) % data.z_size;
                int x = i % data.x_size;
                if(data.map[x][y][z]>0) {
                    Location actual = cloneLoc(data.lowerCorner).add(x, y, z);
                    result.add(actual);
                }
                //test if the point is a valid point
            }
        }

        return (result.isEmpty())?null:result;
    }

    default List<Location> getReachable(LoadData data){
        List<Location> result = new LinkedList<>();

        if(data.getStatus().getValue() >= LoadData.Status.LOADED.getValue()){
            for (int i = 0; i < data.z_size * data.y_size * data.x_size; i++) {
                int y = (( i /data. x_size ) / data.z_size ) % data.y_size;
                int z = ( i / data.x_size ) % data.z_size;
                int x = i % data.x_size;
                if(data.map[x][y][z]>1) {
                    Location actual = cloneLoc(data.lowerCorner).add(x, y, z);
                    result.add(actual);
                }
                //test if the point is a valid point
            }
        }

        return (result.isEmpty())?null:result;
    }

    default Location cloneLoc(Location loc){
        return new Location(loc.getWorld(),loc.getX(),loc.getY(),loc.getZ());
    }
}
