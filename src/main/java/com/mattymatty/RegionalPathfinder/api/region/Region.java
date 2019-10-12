package com.mattymatty.RegionalPathfinder.api.region;

import com.mattymatty.RegionalPathfinder.api.Status;
import com.mattymatty.RegionalPathfinder.api.entity.Entity;
import com.mattymatty.RegionalPathfinder.core.StatusImpl;
import com.mattymatty.RegionalPathfinder.core.region.RegionImpl;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Set;

/*
    THIS CLASS IS THE PUBLIC INTERFACE THAT USERS SHOULD USE
 */

public interface Region {

    int getID();

    String getName();

    //from lvl 1 ( block driven pathFinding ) to higher levels
    int getLevel();

    //the world this region is into
    World getWorld();

    //the corners of this region ( 6 if lvl 1, more if higher lvl but always multiples of 6 )
    Location[] getCorners();

    Location getMinCorner();

    Location getMaxCorner();

    //a getter for all the locations where the entity can stand
    Set<Location> getValidLocations();

    //a getter for all the locations where the entity can stand near a point

    /**
     * //  NOT YET IMPLEMENTED
     **/
    Set<Location> getValidLocations(Location center, int range);

    //a getter for all the locations where the entity can walk to
    Set<Location> getReachableLocations();

    //a getter for all the locations where the entity can stand near a point

    /**
     * //  NOT YET IMPLEMENTED
     **/
    Set<Location> getReachableLocations(Location center, int range);

    Set<Location> getReachableLocations(Location center, int x_range, int y_range, int z_range);


    //a check for intersection locations
    default Status<Set<Location>> getIntersection(Region region) {
        StatusImpl<Set<Location>> result = new StatusImpl<>();
        if (StatusImpl.sync) {
            result.setProduct(((RegionImpl) this)._getIntersection(region));
            result.setStatus(3);
        } else {
            result.setStatus(1);
            new Thread(() -> {
                result.setStatus(2);
                result.setProduct(((RegionImpl) this)._getIntersection(region));
                result.setStatus(3);
            }).start();
        }
        return result;
    }

    //tester if a location is inside the definition zone (lowerCorner,highCorner)
    boolean isInRegion(Location location);

    //tester if a location is where the entity can stand
    boolean isValidLocation(Location location);


    //tester if a location is where the entity can walk to
    boolean isReachableLocation(Location location);

    //setter methods for entity
    Entity setEntity(Entity entity);


    //getter method for entity
    Entity getEntity();

    //if this region is ready to be used
    boolean isValid();

    //validation command for the current region
    Status<Boolean> validate();

    //pathfind command for the current region
    Status<Path> getPath(Location start, Location end);


    default BaseRegion asBaseRegion(){
        return (this instanceof BaseRegion)?(BaseRegion)this:null;
    }

    default ExtendedRegion asExtendedRegion(){
        return (this instanceof ExtendedRegion)?(ExtendedRegion)this:null;
    }

    class Path {
        private final List<Location> path;
        private final double weight;

        public List<Location> getPath() {
            return path;
        }

        public double getWeight() {
            return weight;
        }

        public Path(List<Location> path, double weight) {
            this.path = path;
            this.weight = weight;
        }
    }

}
