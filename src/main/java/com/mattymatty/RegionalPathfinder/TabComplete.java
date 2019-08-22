package com.mattymatty.RegionalPathfinder;

import com.mattymatty.RegionalPathfinder.api.region.Region;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TabComplete implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        try {
            if (args.length == 1) {
                List<String> ret = new LinkedList<>(Arrays.asList("pos1", "pos2", "create", "particle", "list"));
                ret.addAll(
                        Arrays.stream(RegionalPathfinder.getInstance().getRegions()).map(Region::getName).collect(Collectors.toList())
                );
                return ret.stream().filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
            }
            switch (args[0].toLowerCase()) {
                case "create":
                    if (args.length == 2)
                        return Stream.of("base"/*,"extended"*/).filter(s -> s.startsWith(args[1])).collect(Collectors.toList());
                    else
                        return null;

                case "pos1":
                case "pos2":
                case "particle":
                case "list":
                    return null;

                default:
                    Region region = RegionalPathfinder.getInstance().getRegion(args[0]);
                    if (region != null)
                        if (region.getLevel() == 1)
                            if (args.length == 2)
                                return Stream.of("set", "validate", "path", "validLoc", "reachableLoc").filter(s -> s.startsWith(args[1])).collect(Collectors.toList());
                            else if (args[1].equalsIgnoreCase("set"))
                                if (args.length == 3)
                                    return Stream.of("corners", "samplepoint").filter(s -> s.startsWith(args[2])).collect(Collectors.toList());
                                else
                                    return null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
