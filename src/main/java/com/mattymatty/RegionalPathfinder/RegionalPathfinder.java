package com.mattymatty.RegionalPathfinder;

import com.mattymatty.RegionalPathfinder.path.InternalRegion;
import com.mattymatty.RegionalPathfinder.path.Region;
import com.mattymatty.RegionalPathfinder.path.RegionType;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class RegionalPathfinder extends JavaPlugin {

    private static RegionalPathfinder instance;

    private Map<String, Region> regionMap = new HashMap<>();

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


    public Region createRegion(String name, RegionType type){
        Region region = Region.createRegion(name,type);
        regionMap.put(name,region);
        return region;
    }

    public Region[] getRegions(){
        return regionMap.values().stream().sorted(Comparator.comparing(Region::getLevel)).toArray(Region[]::new);
    }


    public Region getRegion(String name){
        return regionMap.get(name);
    }

    public void removeRegion(Region region){
        regionMap.remove(region.getName());
        ((InternalRegion)region).delete();
    }

    public static RegionalPathfinder getInstance(){
        return instance;
    }

    public RegionalPathfinder() {
        instance = this;
    }

    public RegionalPathfinder(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
        instance = this;
    }
}
