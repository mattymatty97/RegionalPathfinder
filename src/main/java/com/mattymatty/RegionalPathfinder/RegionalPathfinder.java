package com.mattymatty.RegionalPathfinder;

import com.mattymatty.RegionalPathfinder.path.InternalRegion;
import com.mattymatty.RegionalPathfinder.path.Region;
import com.mattymatty.RegionalPathfinder.path.RegionType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class RegionalPathfinder extends JavaPlugin {

    private static Map<String, Region> regionMap = new HashMap<>();

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    public static Region createRegion(String name, RegionType type){
        Region region = Region.createRegion(name,type);
        regionMap.put(name,region);
        return region;
    }

    public static Region getRegion(String name){
        return regionMap.get(name);
    }

    public static void removeRegion(Region region){
        regionMap.remove(region.getName());
        ((InternalRegion)region).delete();
    }

}
