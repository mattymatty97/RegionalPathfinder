package com.mattymatty.RegionalPathfinder;

import org.bukkit.Bukkit;

public class Logger {

    public static void severe(String msg){
        String actmsg = "["+RegionalPathfinder.getInstance().getName()+"] "+msg;
        Bukkit.getLogger().severe(actmsg);
    }

    public static void warning(String msg){
        String actmsg = "["+RegionalPathfinder.getInstance().getName()+"] "+msg;
        Bukkit.getLogger().warning(actmsg);
    }

    public static void config(String msg){
        String actmsg = "["+RegionalPathfinder.getInstance().getName()+"] "+msg;
        Bukkit.getLogger().config(actmsg);
    }

    public static void fine(String msg){
        String actmsg = "["+RegionalPathfinder.getInstance().getName()+"] "+msg;
        Bukkit.getLogger().fine(actmsg);
    }

    public static void finer(String msg){
        String actmsg = "["+RegionalPathfinder.getInstance().getName()+"] "+msg;
        Bukkit.getLogger().finer(actmsg);
    }

    public static void finest(String msg){
        String actmsg = "["+RegionalPathfinder.getInstance().getName()+"] "+msg;
        Bukkit.getLogger().finest(actmsg);
    }

    public static void info(String msg){
        String actmsg = "["+RegionalPathfinder.getInstance().getName()+"] "+msg;
        Bukkit.getLogger().info(actmsg);
    }

}
