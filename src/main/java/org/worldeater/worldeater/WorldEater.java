package org.worldeater.worldeater;

import org.bukkit.plugin.java.JavaPlugin;
import org.worldeater.worldeater.commands.EatWorld;

public final class WorldEater extends JavaPlugin {

    private static WorldEater plugin;
    public static String messagePrefix = "§8:: §2World§6Eater §8:: §7";

    @Override
    public void onEnable() {
        plugin = this;

        getCommand("eatworld").setExecutor(new EatWorld());

        getLogger().info("WorldEater plugin has been initialized.");
    }

    @Override
    public void onDisable() {
        getLogger().info("WorldEater plugin has been stopped.");
    }

    public static WorldEater getPlugin() {
        return plugin;
    }

    public static void sendBroadcast(String s) {
        plugin.getServer().broadcastMessage(messagePrefix + s);
    }
}