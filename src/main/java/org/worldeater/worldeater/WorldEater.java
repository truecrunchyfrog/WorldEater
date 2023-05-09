package org.worldeater.worldeater;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.worldeater.worldeater.commands.EatWorld.EatWorld;

import java.util.Objects;

public final class WorldEater extends JavaPlugin {

    private static WorldEater plugin;
    public static String messagePrefix = "§2§lWORLD§6§lEATER §8§l>> §7";

    @Override
    public void onEnable() {
        plugin = this;

        Objects.requireNonNull(getCommand("eatworld")).setExecutor(new EatWorld());

        getServer().getPluginManager().registerEvents(new Events(), this);

        getLogger().info("WorldEater plugin has been initialized.");
    }

    @Override
    public void onDisable() {
        getLogger().info("WorldEater plugin has been stopped.");
    }

    public static WorldEater getPlugin() {
        return plugin;
    }

    private static String getFancyText(String text) {
        return messagePrefix + text;
    }

    public static void sendBroadcast(String text) {
        plugin.getServer().broadcastMessage(getFancyText(text));
    }

    public static void sendMessage(CommandSender commandSender, String text) {
        commandSender.sendMessage(getFancyText(text));
    }

    public static void sendMessage(Player player, String text) {
        player.sendMessage(getFancyText(text));
    }
}