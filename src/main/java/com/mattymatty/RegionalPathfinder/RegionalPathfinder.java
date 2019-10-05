package com.mattymatty.RegionalPathfinder;

import com.mattymatty.RegionalPathfinder.api.region.Region;
import com.mattymatty.RegionalPathfinder.api.region.RegionType;
import com.mattymatty.RegionalPathfinder.core.StatusImpl;
import com.mattymatty.RegionalPathfinder.core.loader.AsynchronousLoader;
import com.mattymatty.RegionalPathfinder.core.loader.Loader;
import com.mattymatty.RegionalPathfinder.core.region.BaseRegionImpl;
import com.mattymatty.RegionalPathfinder.core.region.ExtendedRegionImpl;
import com.mattymatty.RegionalPathfinder.core.region.RegionImpl;
import com.mattymatty.RegionalPathfinder.exeptions.InvalidNameException;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public class RegionalPathfinder extends JavaPlugin {

    private static RegionalPathfinder instance;

    private Map<String, Region> regionMap = new HashMap<>();

    public static RegionalPathfinder getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        instance = this;
        saveDefaultConfig();
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
        Loader loader = new AsynchronousLoader();
        int log_level = getConfig().getInt("logging-level");
        switch (log_level) {
            case 1:
                getLogger().setLevel(Level.FINER);
                break;
            case -1:
                getLogger().setLevel(Level.WARNING);
                break;
            default:
                break;
        }
        getServer().getScheduler().runTaskLater(this,
                ()-> {
                    Logger.warning("Changed to Asynchronous Loader");
                    BaseRegionImpl.loader = loader;
                    ExtendedRegionImpl.sync = false;
                    StatusImpl.sync = false;
                },1);
    }

    public Region createRegion(String name, RegionType type) {
        if (name == null || name.isEmpty() || !name.matches("\\S*") ||
                name.equalsIgnoreCase("create") || name.equalsIgnoreCase("pos1") || name.equalsIgnoreCase("pos2") || name.equalsIgnoreCase("particle") || name.equalsIgnoreCase("list"))
            throw new InvalidNameException("name: " + ((name == null || name.isEmpty()) ? "null" : name) + " Not Valid as Region name");
        if (regionMap.containsKey(name))
            throw new InvalidNameException("name: " + name + " already exist as Region");
        Region region = RegionImpl.createRegion(name, type);
        regionMap.put(name, region);
        Logger.fine("Created " + ((type == RegionType.BASE) ? "base" : "extended") + " region: " + name);
        return region;
    }

    public Region[] getRegions() {
        return regionMap.values().stream().sorted(Comparator.comparing(Region::getLevel)).toArray(Region[]::new);
    }

    public Region getRegion(String name) {
        return regionMap.get(name);
    }

    public void removeRegion(Region region) {
        regionMap.remove(region.getName());
        ((RegionImpl) region).delete();
    }

}
