package com.mattymatty.RegionalPathfinder;

import com.mattymatty.RegionalPathfinder.api.region.Region;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TabComplete implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if(args.length == 0) {
            List<String> ret = Arrays.asList("pos1","pos2","create","particle","list");
            ret.addAll(
                    Arrays.stream(RegionalPathfinder.getInstance().getRegions()).map(Region::getName).collect(Collectors.toList())
            );
            return ret;
        }
        switch (args[0].toLowerCase()){
            case "create":
                return Arrays.asList("base"/*,"extended"*/);

            case "pos1":
            case "pos2":
            case "particle":
            case "list":
                return null;

            default:
                Region region = RegionalPathfinder.getInstance().getRegion(args[0]);
                if(region != null)
                    if(region.getLevel()==1)
                        if(args.length==1)
                            return Arrays.asList("set","validate","path","validLoc","reachableLoc");
                        else if(args[1].equalsIgnoreCase("set"))
                            return Arrays.asList("corners","samplepoint");
        }
        return null;
    }
}
