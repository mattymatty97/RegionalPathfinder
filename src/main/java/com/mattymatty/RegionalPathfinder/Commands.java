package com.mattymatty.RegionalPathfinder;

import com.mattymatty.RegionalPathfinder.api.Status;
import com.mattymatty.RegionalPathfinder.api.region.BaseRegion;
import com.mattymatty.RegionalPathfinder.api.region.ExtendedRegion;
import com.mattymatty.RegionalPathfinder.api.region.Region;
import com.mattymatty.RegionalPathfinder.api.region.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("Duplicates")
public class Commands implements CommandExecutor {

    private final RegionalPathfinder plugin;
    private Location pos1;
    private Location pos2;
    private List<Thread> particles = new LinkedList<>();

    Commands(RegionalPathfinder plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
        } else {
            Player player = (Player) sender;
            if (args.length >= 2) {
                if (args[0].equals("create")) {
                    if (args.length == 3) {
                        if (args[1].equalsIgnoreCase("base")) {
                            plugin.createRegion(args[2], RegionType.BASE);
                            sender.sendMessage("Region " + args[2] + " created");
                        } else if (args[1].equalsIgnoreCase("extended")) {
                            plugin.createRegion(args[2], RegionType.EXTENDED);
                            sender.sendMessage("Region " + args[2] + " created");
                        }
                    } else {
                        return false;
                    }
                } else {
                    Region region = plugin.getRegion(args[0]);
                    if (region != null) {
                        if (region instanceof BaseRegion) {
                            BaseRegion baseRegion = region.asBaseRegion();
                            switch (args[1].toLowerCase()) {
                                case "delete":
                                    plugin.removeRegion(region);
                                    return true;

                                case "set": {
                                    if (args.length == 3) {
                                        switch (args[2].toLowerCase()) {
                                            case "corners":
                                                if (pos1 != null && pos2 != null) {
                                                    baseRegion.setCorners(pos1, pos2);
                                                    Status<Location[]> status = baseRegion.load();
                                                    if (status != null) {
                                                        status.setOnSyncDone((product) -> sender.sendMessage("Successfully added corners and loaded region" +
                                                                "\ntook: " + status.getTimeTotal() + " ms" +
                                                                "\nand halted server for: " + status.getTimeSync() + " ms")
                                                        ).setOnSyncException((ex) -> sender.sendMessage("Exception loading corners" +
                                                                "\ntook: " + status.getTimeTotal() + " ms" +
                                                                "\nand halted server for: " + status.getTimeSync() + " ms"));

                                                        AtomicLong next_tic = new AtomicLong(0);

                                                        status.setOnSyncProgress((progress) -> {
                                                            if (System.currentTimeMillis() > next_tic.get()) {
                                                                sender.sendMessage("Loading: " + String.format("%.2f", progress * 100) + "%");
                                                                next_tic.set(System.currentTimeMillis() + 100);
                                                            }
                                                        });

                                                    } else {
                                                        sender.sendMessage("Error loading corners");
                                                    }
                                                } else {
                                                    sender.sendMessage("Error set the positions first (pos1, pos2)");
                                                }
                                                return true;
                                            case "samplepoint":
                                                baseRegion.setSamplePoint(((Player) sender).getLocation());
                                                Status<Location> status = baseRegion.evaluate();
                                                if (status != null) {
                                                    status.setOnSyncDone((product) -> sender.sendMessage("Successfully set sample point and evaluated region" +
                                                            "\ntook: " + status.getTimeTotal() + " ms" +
                                                            "\nand halted server for: " + status.getTimeSync() + " ms")
                                                    ).setOnSyncException((ex) -> sender.sendMessage("Exception setting sample point" +
                                                            "\ntook: " + status.getTimeTotal() + " ms" +
                                                            "\nand halted server for: " + status.getTimeSync() + " ms"));

                                                    AtomicLong next_tic = new AtomicLong(0);

                                                    status.setOnSyncProgress((progress) -> {
                                                        if (System.currentTimeMillis() > next_tic.get()) {
                                                            sender.sendMessage("Evaluating: " + String.format("%.2f", progress * 100) + "%");
                                                            next_tic.set(System.currentTimeMillis() + 100);
                                                        }
                                                    });
                                                } else {
                                                    sender.sendMessage("Error setting sample point");
                                                }
                                                return true;
                                            default:
                                                return false;
                                        }
                                    }
                                    return false;
                                }
                                case "validate": {
                                    Status<Boolean> stat = region.validate();
                                    stat.setOnSyncDone((product) -> {
                                        Bukkit.getScheduler().runTask(plugin, () -> {
                                            if (product) {
                                                sender.sendMessage("Successfully validated the region" +
                                                        "\ntook: " + stat.getTimeTotal() + " ms" +
                                                        "\nand halted server for: " + stat.getTimeSync() + " ms");
                                            } else {
                                                sender.sendMessage("Error Validating Region" +
                                                        "\ntook: " + stat.getTimeTotal() + " ms" +
                                                        "\nand halted server for: " + stat.getTimeSync() + " ms");
                                            }
                                        });
                                    });
                                    stat.setOnSyncException((ex) -> sender.sendMessage("Exception setting sample point" +
                                            "\ntook: " + stat.getTimeTotal() + " ms" +
                                            "\nand halted server for: " + stat.getTimeSync() + " ms")
                                    );

                                    AtomicLong next_tic = new AtomicLong(0);

                                    stat.setOnSyncProgress((progress) -> {
                                        if (System.currentTimeMillis() > next_tic.get()) {
                                            sender.sendMessage("Validating: " + String.format("%.2f", progress * 100) + "%");
                                            next_tic.set(System.currentTimeMillis() + 100);
                                        }
                                    });
                                    return true;
                                }
                                case "validloc":
                                case "validlocations": {
                                    List<Location> locations = region.getValidLocations();
                                    if (locations.isEmpty()) {
                                        sender.sendMessage("Error no valid locations");
                                    } else {
                                        showParticles(locations);
                                        sender.sendMessage("Shown particles onto valid Locations, remove with /regionalpathfinder particle");
                                    }
                                    return true;
                                }
                                case "reachableloc":
                                case "reachablelocations": {
                                    List<Location> locations = region.getReachableLocations();
                                    if (locations.isEmpty()) {
                                        sender.sendMessage("Error no reachable locations");
                                    } else {
                                        showParticles(locations);
                                        sender.sendMessage("Shown particles onto reachable Locations, remove with /regionalpathfinder particle");
                                    }
                                    return true;
                                }
                                case "path": {
                                    Status<Region.Path> status = region.getPath(pos1, pos2);
                                    status.setOnSyncDone((product) -> {
                                        if (product == null) {
                                            sender.sendMessage("Error no path found" +
                                                    "\ntook: " + status.getTimeTotal() + " ms" +
                                                    "\nand halted server for: " + status.getTimeSync() + " ms");
                                        } else {
                                            showParticles(product.getPath(), true);
                                            sender.sendMessage("Shown particles onto path, remove with /regionalpathfinder particle" +
                                                    "\ntook: " + status.getTimeTotal() + " ms" +
                                                    "\nand halted server for: " + status.getTimeSync() + " ms");
                                        }
                                    }).setOnSyncException((ex) ->
                                            sender.sendMessage("Exception while pathfinding" +
                                                    "\ntook: " + status.getTimeTotal() + " ms" +
                                                    "\nand halted server for: " + status.getTimeSync() + " ms")
                                    ).setOnSyncProgress((progress) ->
                                            sender.sendMessage("Started Pathfinding")
                                    );
                                }
                            }
                        } else {
                            ExtendedRegion extendedRegion = region.asExtendedRegion();
                            switch (args[1].toLowerCase()) {
                                case "delete":
                                    plugin.removeRegion(region);
                                    return true;
                                case "path": {
                                    Status<Region.Path> status = region.getPath(pos1, pos2);
                                    status.setOnSyncDone((product) -> {
                                        if (product == null) {
                                            sender.sendMessage("Error no path found" +
                                                    "\ntook: " + status.getTimeTotal() + " ms" +
                                                    "\nand halted server for: " + status.getTimeSync() + " ms");
                                        } else {
                                            showParticles(product.getPath(), true);
                                            sender.sendMessage("Shown particles onto path, remove with /regionalpathfinder particle" +
                                                    "\ntook: " + status.getTimeTotal() + " ms" +
                                                    "\nand halted server for: " + status.getTimeSync() + " ms");
                                        }
                                    }).setOnSyncException((ex) ->
                                            sender.sendMessage("Exception while pathfinding" +
                                                    "\ntook: " + status.getTimeTotal() + " ms" +
                                                    "\nand halted server for: " + status.getTimeSync() + " ms")
                                    ).setOnSyncProgress((progress) ->
                                            sender.sendMessage("Started Pathfinding")
                                    );
                                    return true;
                                }
                                case "validloc":
                                case "validlocations": {
                                    List<Location> locations = region.getValidLocations();
                                    if (locations.isEmpty()) {
                                        sender.sendMessage("Error no valid locations");
                                    } else {
                                        showParticles(locations);
                                        sender.sendMessage("Shown particles onto valid Locations, remove with /regionalpathfinder particle");
                                    }
                                    return true;
                                }
                                case "reachableloc":
                                case "reachablelocations": {
                                    List<Location> locations = region.getReachableLocations();
                                    if (locations.isEmpty()) {
                                        sender.sendMessage("Error no reachable locations");
                                    } else {
                                        showParticles(locations);
                                        sender.sendMessage("Shown particles onto reachable Locations, remove with /regionalpathfinder particle");
                                    }
                                    return true;
                                }
                                case "validate": {
                                    Status<Boolean> stat = region.validate();
                                    stat.setOnSyncDone((product) -> {
                                        Bukkit.getScheduler().runTask(plugin, () -> {
                                            if (product) {
                                                sender.sendMessage("Successfully validated the region" +
                                                        "\ntook: " + stat.getTimeTotal() + " ms" +
                                                        "\nand halted server for: " + stat.getTimeSync() + " ms");
                                            } else {
                                                sender.sendMessage("Error Validating Region" +
                                                        "\ntook: " + stat.getTimeTotal() + " ms" +
                                                        "\nand halted server for: " + stat.getTimeSync() + " ms");
                                            }
                                        });
                                    });
                                    stat.setOnSyncException((ex) -> sender.sendMessage("Exception setting sample point" +
                                            "\ntook: " + stat.getTimeTotal() + " ms" +
                                            "\nand halted server for: " + stat.getTimeSync() + " ms")
                                    );

                                    AtomicLong next_tic = new AtomicLong(0);

                                    stat.setOnSyncProgress((progress) -> {
                                        if (System.currentTimeMillis() > next_tic.get()) {
                                            sender.sendMessage("Validating: " + String.format("%.2f", progress * 100) + "%");
                                            next_tic.set(System.currentTimeMillis() + 100);
                                        }
                                    });
                                    return true;
                                }
                                case "add": {
                                    if (args.length < 3)
                                        return false;
                                    Region toAdd = RegionalPathfinder.getInstance().getRegion(args[2]);
                                    if (toAdd == null) {
                                        sender.sendMessage("Region not found");
                                        return true;
                                    }
                                    Status<Region[]> status = extendedRegion.addRegion(toAdd);
                                    if (status == null) {
                                        sender.sendMessage("Region already added");
                                        return true;
                                    } else {
                                        status.setOnSyncProgress((progress) -> {
                                            if (progress == 0f)
                                                sender.sendMessage("Started adding Region");
                                            else
                                                sender.sendMessage("Adding: " + String.format("%.2f", progress * 100) + "%");
                                        }).setOnSyncException((ex) ->
                                                sender.sendMessage("Exception adding Region" +
                                                        "\ntook: " + status.getTimeTotal() + " ms" +
                                                        "\nand halted server for: " + status.getTimeSync() + " ms")
                                        ).setOnSyncDone((regions -> sender.sendMessage("Successfully added region")));
                                    }
                                    return true;
                                }
                                case "remove": {
                                    if (args.length < 3)
                                        return false;
                                    Region toRemove = RegionalPathfinder.getInstance().getRegion(args[2]);
                                    if (toRemove == null) {
                                        sender.sendMessage("Region not found");
                                        return true;
                                    }
                                    Status<Region[]> status = extendedRegion.removeRegion(toRemove);

                                    status.setOnSyncProgress((progress) -> {
                                        if (progress == 0f)
                                            sender.sendMessage("Started removing Region");
                                        else
                                            sender.sendMessage("Removing: " + String.format("%.2f", progress * 100) + "%");
                                    }).setOnSyncException((ex) ->
                                            sender.sendMessage("Exception removing Region" +
                                                    "\ntook: " + status.getTimeTotal() + " ms" +
                                                    "\nand halted server for: " + status.getTimeSync() + " ms")
                                    ).setOnSyncDone((regions -> sender.sendMessage("Successfully removed region")));

                                    return true;
                                }
                                case "intersection":
                                case "intersections": {
                                    List<Location> locations = extendedRegion.getIntersections();
                                    if (locations.isEmpty()) {
                                        sender.sendMessage("Error no intersections");
                                    } else {
                                        showParticles(locations, Particle.BARRIER);
                                        sender.sendMessage("Shown particles onto Intersections, remove with /regionalpathfinder particle");
                                    }
                                    return true;
                                }
                            }
                        }
                        return false;
                    } else return false;
                }
                return true;
            } else if (args.length == 1) {
                switch (args[0].toLowerCase()) {
                    case "pos1": {
                        pos1 = player.getLocation();
                        sender.sendMessage("Pos1 set");
                        return true;
                    }
                    case "pos2": {
                        pos2 = player.getLocation();
                        sender.sendMessage("Pos2 set");
                        return true;
                    }
                    case "particle": {
                        particles.forEach(Thread::interrupt);
                        sender.sendMessage("Particles cleared");
                        return true;
                    }
                    case "list": {
                        sender.sendMessage("Existing regions:\n" + Arrays.stream(plugin.getRegions()).map(Region::getName).reduce("", (s1, s2) -> s1 + "\n" + s2));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void showParticles(Iterable<Location> locations) {
        showParticles(locations, false);
    }

    private void showParticles(Iterable<Location> locations, boolean isPath) {
        showParticles(locations, isPath, Particle.VILLAGER_HAPPY);
    }

    private void showParticles(Iterable<Location> locations, Particle type) {
        showParticles(locations, false, type);
    }

    private void showParticles(Iterable<Location> locations, boolean isPath, Particle type) {
        Thread particle = new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    long tic = System.currentTimeMillis();
                    int i = 0;
                    for (Location loc : locations) {
                        i++;
                        Location act = cloneLoc(loc);
                        if (!isPath)
                            plugin.getServer().getScheduler().runTask(plugin, () -> Objects.requireNonNull(act.getWorld()).spawnParticle(type, act, 7));
                        else
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> Objects.requireNonNull(act.getWorld()).spawnParticle(type, act, 7), i * 3);
                    }
                    long toc = System.currentTimeMillis();
                    Thread.sleep(((isPath) ? (5000) : (500)) - (toc - tic));
                }
            } catch (InterruptedException ignored) {
            }
        });
        particle.start();
        particles.add(particle);
    }


    private Location cloneLoc(Location loc) {
        return new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());
    }

}
