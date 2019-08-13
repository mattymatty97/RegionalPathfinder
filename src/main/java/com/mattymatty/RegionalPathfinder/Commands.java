package com.mattymatty.RegionalPathfinder;

import com.mattymatty.RegionalPathfinder.api.region.BaseRegion;
import com.mattymatty.RegionalPathfinder.api.region.Region;
import com.mattymatty.RegionalPathfinder.api.region.RegionType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
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

    public Commands(RegionalPathfinder plugin) {
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
                                                if (((BaseRegion) region).setCorners(pos1, pos2) != null) {
                                                    sender.sendMessage("Successfully added corners and loaded region");
                                                } else {
                                                    sender.sendMessage("Error loading corners");
                                                }
                                            } else {
                                                sender.sendMessage("Error set the positions first (pos1, pos2)");
                                            }
                                            return true;
                                        case "samplepoint":
                                            if (((BaseRegion) region).setSamplePoint(player.getLocation()) != null) {
                                                sender.sendMessage("Successfully set SamplePoint and evaluated region");
                                            } else {
                                                sender.sendMessage("Error adding SamplePoint");
                                            }
                                            return true;
                                        default:
                                            return false;
                                    }
                                }
                                return false;
                            }
                            case "validate": {
                                region.validate();
                                if (region.isValid()) {
                                    sender.sendMessage("Successfully validated the region");
                                } else {
                                    sender.sendMessage("Error Validating Region");
                                }
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
                                region.getAsyncPath(pos1,pos2,(path)->{
                                    if (path == null) {
                                        sender.sendMessage("Error no path found");
                                    } else {
                                        showParticles(path);
                                        sender.sendMessage("Shown particles onto path, remove with /regionalpathfinder particle");
                                    }
                                });
                            }
                        }
                        return true;


                    }else return false;
                }
                return true;
            }else if(args.length == 1) {
                if (args[0].equalsIgnoreCase("pos1")) {
                    pos1 = player.getLocation();
                    sender.sendMessage("Pos1 set");
                    return true;
                } else if (args[0].equalsIgnoreCase("pos2")) {
                    pos2 = player.getLocation();
                    sender.sendMessage("Pos2 set");
                    return true;
                } else if(args[0].equalsIgnoreCase("particle")){
                    particles.forEach(Thread::interrupt);
                    sender.sendMessage("Particles cleared");
                    return true;
                } else if(args[0].equalsIgnoreCase("list")){
                    sender.sendMessage("Existing regions:\n" + Arrays.stream(plugin.getRegions()).map(Region::getName).reduce("",(s1,s2)->s1 + "\n" + s2));
                    return true;
                }
            }
        }
        return false;
    }

    private void showParticles(Iterable<Location> locations){
        for (Location loc : locations) {
            Location act = cloneLoc(loc);
            Thread particle = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> Objects.requireNonNull(act.getWorld()).spawnParticle(Particle.VILLAGER_HAPPY, act, 7));
                        Thread.sleep(500);
                    }
                } catch (InterruptedException ignored) { }
            });
            particle.start();
            particles.add(particle);
        }
    }


    private Location cloneLoc(Location loc){
        return new Location(loc.getWorld(),loc.getX(),loc.getY(),loc.getZ());
    }

}
