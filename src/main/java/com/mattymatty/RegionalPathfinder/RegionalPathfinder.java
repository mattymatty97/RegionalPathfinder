package com.mattymatty.RegionalPathfinder;

import com.mattymatty.RegionalPathfinder.api.region.Region;
import com.mattymatty.RegionalPathfinder.api.region.RegionType;
import com.mattymatty.RegionalPathfinder.core.StatusImpl;
import com.mattymatty.RegionalPathfinder.core.graph.Node;
import com.mattymatty.RegionalPathfinder.core.loader.AsynchronousLoader;
import com.mattymatty.RegionalPathfinder.core.loader.Loader;
import com.mattymatty.RegionalPathfinder.core.region.BaseRegionImpl;
import com.mattymatty.RegionalPathfinder.core.region.ExtendedRegionImpl;
import com.mattymatty.RegionalPathfinder.core.region.RegionImpl;
import com.mattymatty.RegionalPathfinder.exeptions.InvalidNameException;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public class RegionalPathfinder extends JavaPlugin {

    private static RegionalPathfinder instance;

    private Map<String, Region> regionMap = new HashMap<>();

    public Executor executor = Executors.newCachedThreadPool();

    public static RegionalPathfinder getInstance() {
        return instance;
    }

    public final Map<Location, Node> nodeMap = new HashMap<>();

    @Override
    public void onLoad() {
        super.onLoad();
        instance = this;
        saveDefaultConfig();
        new File(getDataFolder().getAbsolutePath() + "/Cache").mkdir();
        readCache();
    }

    public Set<Thread> runningThreads = new HashSet<>();
    public Set<BukkitTask> runningTasks = new HashSet<>();

    @Override
    public void onDisable() {
        super.onDisable();

        runningThreads.forEach(Thread::interrupt);
        runningTasks.forEach(BukkitTask::cancel);

    }

    @Override
    public void onEnable() {
        super.onEnable();
        Objects.requireNonNull(this.getCommand("regionalpathfinder")).setExecutor(new Commands(this));
        Objects.requireNonNull(this.getCommand("regionalpathfinder")).setTabCompleter(new TabComplete());
        Loader loader = new AsynchronousLoader();
        String max_threads = getConfig().getString("max-parallel-threads");
        if (max_threads == null || max_threads.equalsIgnoreCase("default"))
            executor = Executors.newWorkStealingPool();
        else
            executor = Executors.newWorkStealingPool(getConfig().getInt("max-parallel-threads"));

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

    private void readCache() {
        try {
            File cacheFolder = new File(getDataFolder().getAbsolutePath() + "/Cache");
            File baseFolder = new File(cacheFolder.getAbsolutePath() + "/Base");
            File extendedFolder = new File(cacheFolder.getAbsolutePath() + "/Extended");
            baseFolder.mkdir();
            extendedFolder.mkdir();
            for (File file :
                    baseFolder.listFiles()) {
                JSONObject json = new JSONObject(new String(Files.readAllBytes(file.toPath())));

                Region base = createRegion(file.getName().replace(".json", ""), RegionType.BASE);
                ((BaseRegionImpl) base).fromJson(json);
            }
            for (File file :
                    extendedFolder.listFiles()) {
                JSONObject json = new JSONObject(new String(Files.readAllBytes(file.toPath())));

                Region extended = createRegion(file.getName().replace(".json", ""), RegionType.EXTENDED);
                ((ExtendedRegionImpl) extended).fromJson(json);
            }
        } catch (Exception ignored) {
        }
    }

    public void saveCache() {
        File cacheFolder = new File(getDataFolder().getAbsolutePath() + "/Cache");
        File baseFolder = new File(cacheFolder.getAbsolutePath() + "/Base");
        File extendedFolder = new File(cacheFolder.getAbsolutePath() + "/Extended");
        cacheFolder.mkdir();
        baseFolder.mkdir();
        extendedFolder.mkdir();
        for (Region region :
                getRegions()) {
            try {
                ((RegionImpl) region).toJson(baseFolder, extendedFolder);
            } catch (IOException ignored) {
            }
        }
    }

    public void clearCache() throws IOException {
        File cache = new File(getDataFolder() + "/Cache");
        Files.walkFileTree(cache.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(
                    Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
        cache.delete();
    }

}
