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
        File folder = getDataFolder();
        if(!folder.exists())
            folder.mkdir();
        folder = new File(folder.getAbsolutePath()+"/libs");
        if(!folder.exists())
            folder.mkdir();
        File lib = new File(folder.getAbsolutePath()+"/libc-graph.so");
        if(!lib.exists()) {
            try {
                lib.createNewFile();
                InputStream stream = this.getResource("libc-graph.so");
                FileOutputStream fo = new FileOutputStream(lib);
                int readBytes;
                byte[] buffer = new byte[4096];
                while ((readBytes = stream.read(buffer)) > 0) {
                    fo.write(buffer, 0, readBytes);
                }
            }catch (IOException ignored){}
        }
        try {
            System.load(RegionalPathfinder.getInstance().getDataFolder().getAbsolutePath() + "/libs/libc-graph.so");
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Objects.requireNonNull(this.getCommand("regionalpathfinder")).setExecutor(new Commands(this));
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

    public RegionalPathfinder() {
        instance = this;
    }

    public RegionalPathfinder(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
        instance = this;
    }
}
