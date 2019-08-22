package com.mattymatty.RegionalPathfinder;

import com.mattymatty.RegionalPathfinder.api.Status;
import com.mattymatty.RegionalPathfinder.api.region.BaseRegion;
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

public class Commands implements CommandExecutor {

    private Location pos1;
    private Location pos2;
    private final RegionalPathfinder plugin;
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
            if(args.length >= 2){
                if(args[0].equals("create")){
                    if(args.length==3 && args[1].equalsIgnoreCase("base")) {
                        plugin.createRegion(args[2], RegionType.BASE);
                        sender.sendMessage("Region " + args[2] + " created");
                    }
                    else
                        return false;
                }else{
                    Region region = plugin.getRegion(args[0]);
                    if(region!=null){
                        switch (args[1].toLowerCase()){
                            case "delete":
                                plugin.removeRegion(region);
                                break;

                            case "set": {
                                if (args.length == 3) {
                                    switch (args[2].toLowerCase()) {
                                        case "corners":
                                            if (pos1 != null && pos2 != null) {
                                                Status<Location[]> status = ((BaseRegion) region).setCorners(pos1, pos2);
                                                if (status != null) {
                                                    status.addListener((stat)->{
                                                        if(stat.isDone())
                                                            plugin.getServer().getScheduler().runTask(plugin,
                                                                    ()->sender.sendMessage("Successfully added corners and loaded region" +
                                                                            "\ntook: "+stat.getTimeTotal() +" ms" +
                                                                            "\nand halted server for: " +stat.getTimeSync()+" ms"));
                                                    }).addListener((stat)->{
                                                        if(stat.hasException())
                                                            plugin.getServer().getScheduler().runTask(plugin,
                                                                    ()->sender.sendMessage("Exception loading corners" +
                                                                            "\ntook: "+stat.getTimeTotal() +" ms" +
                                                                            "\nand halted server for: " +stat.getTimeSync()+" ms"));
                                                    });

                                                } else {
                                                    sender.sendMessage("Error loading corners");
                                                }
                                            } else {
                                                sender.sendMessage("Error set the positions first (pos1, pos2)");
                                            }
                                            return true;
                                        case "samplepoint":
                                            Status<Location> status = ((BaseRegion) region).setSamplePoint(((Player) sender).getLocation());
                                            if (status != null) {
                                                status.addListener((stat)->{
                                                    if(stat.isDone())
                                                        plugin.getServer().getScheduler().runTask(plugin,
                                                                ()->sender.sendMessage("Successfully set sample point and evaluated region" +
                                                                        "\ntook: "+stat.getTimeTotal() +" ms" +
                                                                        "\nand halted server for: " +stat.getTimeSync()+" ms"));
                                                }).addListener((stat)->{
                                                    if(stat.hasException())
                                                        plugin.getServer().getScheduler().runTask(plugin,
                                                                ()->sender.sendMessage("Exception setting sample point" +
                                                                        "\ntook: "+stat.getTimeTotal() +" ms" +
                                                                        "\nand halted server for: " +stat.getTimeSync()+" ms"));
                                                });
                                            }else {
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
                                Status stat = region.validate();
                                stat.addListener((s)->{
                                    if(stat.isDone())
                                        Bukkit.getScheduler().runTask(plugin,()-> {
                                            if (region.isValid()) {
                                                sender.sendMessage("Successfully validated the region" +
                                                        "\ntook: " + stat.getTimeTotal() + " ms" +
                                                        "\nand halted server for: " + stat.getTimeSync() + " ms");
                                            } else {
                                                sender.sendMessage("Error Validating Region"+
                                                        "\ntook: "+stat.getTimeTotal() +" ms" +
                                                        "\nand halted server for: " +stat.getTimeSync()+" ms");
                                            }
                                        });
                                });
                                stat.addListener((s)->{
                                    if(stat.hasException())
                                        Bukkit.getScheduler().runTask(plugin,()->
                                                sender.sendMessage("Exception setting sample point" +
                                                        "\ntook: "+stat.getTimeTotal() +" ms" +
                                                        "\nand halted server for: " +stat.getTimeSync()+" ms"));
                                });
                                break;
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
                                region.getPath(pos1,pos2).addListener((status)->{
                                    if(status.isDone()) {
                                        if (status.getProduct() == null) {
                                            plugin.getServer().getScheduler().runTask(plugin,()->sender.sendMessage("Error no path found"+
                                                    "\ntook: "+status.getTimeTotal() +" ms" +
                                                    "\nand halted server for: " +status.getTimeSync()+" ms"));
                                        } else {
                                            plugin.getServer().getScheduler().runTask(plugin,()->{
                                                showParticles(status.getProduct(), true);
                                                sender.sendMessage("Shown particles onto path, remove with /regionalpathfinder particle"+
                                                        "\ntook: "+status.getTimeTotal() +" ms" +
                                                        "\nand halted server for: " +status.getTimeSync()+" ms");
                                            });
                                        }
                                    }
                                }).addListener((status)->{
                                    if(status.hasException())
                                        plugin.getServer().getScheduler().runTask(plugin,()->sender.sendMessage("Exception while pathfinding"+
                                                "\ntook: "+status.getTimeTotal() +" ms" +
                                                "\nand halted server for: " +status.getTimeSync()+" ms"));
                                });
                            }
                        }
                        return false;
                    }else return false;
                }
                return true;
            }else if(args.length == 1) {
                switch (args[0].toLowerCase()) {
                    case "pos1": {
                        pos1 = player.getLocation();
                        sender.sendMessage("Pos1 set");
                        return true;
                    }
                    case "pos2" : {
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
                    case "async": {
                        plugin.setAsyncLoading();
                        sender.sendMessage("Successfully set asynchronous loading");
                        return true;
                    }
                    case "sync": {
                        plugin.setSyncLoading();
                        sender.sendMessage("Successfully set synchronous loading");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void showParticles(Iterable<Location> locations){ showParticles(locations,false);}

    private void showParticles(Iterable<Location> locations,boolean isPath){
        Thread particle = new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    long tic = System.currentTimeMillis();
                    int i=0;
                    for (Location loc : locations) {
                        i++;
                        Location act = cloneLoc(loc);
                        if(!isPath)
                            plugin.getServer().getScheduler().runTask(plugin, () -> Objects.requireNonNull(act.getWorld()).spawnParticle(Particle.VILLAGER_HAPPY, act, 7));
                        else
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> Objects.requireNonNull(act.getWorld()).spawnParticle(Particle.VILLAGER_HAPPY, act, 7),i*3);
                    }
                    long toc = System.currentTimeMillis();
                    Thread.sleep(((isPath)?(5000):(500))-(toc-tic));
                }
            } catch (InterruptedException ignored) {
            }
        });
        particle.start();
        particles.add(particle);
    }


    private Location cloneLoc(Location loc){
        return new Location(loc.getWorld(),loc.getX(),loc.getY(),loc.getZ());
    }

}
