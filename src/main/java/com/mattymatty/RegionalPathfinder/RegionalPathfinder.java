package com.mattymatty.RegionalPathfinder;

import com.mattymatty.RegionalPathfinder.core.loader.SynchronousLoader;
import com.mattymatty.RegionalPathfinder.core.region.RegionImpl;
import com.mattymatty.RegionalPathfinder.api.region.Region;
import com.mattymatty.RegionalPathfinder.api.region.RegionType;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"WeakerAccess","UnusedReturnValue"})
public class RegionalPathfinder extends JavaPlugin {

    private static RegionalPathfinder instance;

    private Class actLoader = SynchronousLoader.class;

    private Map<String, Region> regionMap = new HashMap<>();

    @Override
    public void onLoad() {
        super.onLoad();
        instance = this;
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Objects.requireNonNull(this.getCommand("regionalpathfinder")).setExecutor(new Commands(this));
        Objects.requireNonNull(this.getCommand("regionalpathfinder")).setTabCompleter(new TabComplete());
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
        ((RegionImpl)region).delete();
    }

    public static RegionalPathfinder getInstance(){
        return instance;
    }


}
