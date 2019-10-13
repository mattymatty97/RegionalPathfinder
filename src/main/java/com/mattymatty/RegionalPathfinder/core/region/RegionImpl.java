package com.mattymatty.RegionalPathfinder.core.region;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mattymatty.RegionalPathfinder.api.region.Region;
import com.mattymatty.RegionalPathfinder.api.region.RegionType;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/*
    THIS ARE THE METHODS THAT EVERY REGION SHOULD HAVE
 */
public interface RegionImpl extends Region {

    AtomicInteger nextID = new AtomicInteger(1);

    //static creator
    static Region createRegion(String name, RegionType type) {
        if (type == RegionType.BASE)
            return new BaseRegionImpl(name);
        if (type == RegionType.EXTENDED)
            return new ExtendedRegionImpl(name);
        return null;
    }

    Map<Region, Cache<Region, Set<Location>>> intersectionCacheMap = new HashMap<>();

    default Set<Location> _getIntersection(Region region) {
        Cache<Region, Set<Location>> cache = intersectionCacheMap.computeIfAbsent(this, (k) -> CacheBuilder.newBuilder().softValues()
                .maximumSize(15).build());
        Set<Location> intersection = cache.getIfPresent(region);
        if (intersection != null)
            return intersection;

        Location min_1 = region.getMinCorner();
        Location max_1 = region.getMaxCorner();
        if (min_1 == null || max_1 == null)
            return new HashSet<>();
        Location center_1 = new Location(getWorld(), (min_1.getBlockX() + max_1.getBlockX()) / 2.0, (min_1.getBlockY() + max_1.getBlockY()) / 2.0, (min_1.getBlockZ() + max_1.getBlockZ()) / 2.0);
        int x_range_1 = (int) Math.ceil((max_1.getBlockX() - min_1.getBlockX()) / 2.0) + 1;
        int y_range_1 = (int) Math.ceil((max_1.getBlockY() - min_1.getBlockY()) / 2.0) + 1;
        int z_range_1 = (int) Math.ceil((max_1.getBlockZ() - min_1.getBlockZ()) / 2.0) + 1;
        int medium_range_1 = (x_range_1 + y_range_1 + z_range_1) / 3;
        Location min_2 = this.getMinCorner();
        Location max_2 = this.getMaxCorner();
        if (min_2 == null || max_2 == null)
            return new HashSet<>();
        Location center_2 = new Location(getWorld(), (min_2.getBlockX() + max_2.getBlockX()) / 2.0, (min_2.getBlockY() + max_2.getBlockY()) / 2.0, (min_2.getBlockZ() + max_2.getBlockZ()) / 2.0);
        int x_range_2 = (int) Math.ceil((max_2.getBlockX() - min_2.getBlockX()) / 2.0) + 1;
        int y_range_2 = (int) Math.ceil((max_2.getBlockY() - min_2.getBlockY()) / 2.0) + 1;
        int z_range_2 = (int) Math.ceil((max_2.getBlockZ() - min_2.getBlockZ()) / 2.0) + 1;
        int medium_range_2 = (x_range_2 + y_range_2 + z_range_2) / 3;

        if (medium_range_1 > medium_range_2) {
            Set<Location> common = new HashSet<>(region.getReachableLocations(center_2, x_range_2, y_range_2, z_range_2));
            common.retainAll(this.getReachableLocations());
            cache.put(region, common);
            return common;
        } else {
            Set<Location> common = new HashSet<>(region.getReachableLocations());
            common.retainAll(this.getReachableLocations(center_1, x_range_1, y_range_1, z_range_1));
            cache.put(region, common);
            return common;
        }

    }

    Path _getPath(Location start, Location end);

    //a cancellation method
    void delete();

    void invalidate();

    void referencer(RegionImpl region);
}
