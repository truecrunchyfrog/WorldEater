package org.worldeater.worldeater;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.worldeater.worldeater.commands.EatWorld.EatWorld;

import java.io.File;
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

    public static void sendWorldBroadcast(World world, String text) {
        for(Player eachPlayer : world.getPlayers())
            sendMessage(eachPlayer, text);
    }

    public static void sendWorldBroadcast(World world, String text, String command) {
        for(Player eachPlayer : world.getPlayers())
            sendMessage(eachPlayer, text, command);
    }

    public static void sendMessage(CommandSender commandSender, String text) {
        commandSender.sendMessage(getFancyText(text));
    }

    public static void sendMessage(Player player, String text) {
        player.sendMessage(getFancyText(text));
    }

    public static void sendMessage(Player player, String text, String runCommand) {
        TextComponent textComponent = new TextComponent(getFancyText(text));
        textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + runCommand));
        player.spigot().sendMessage(textComponent);
    }

    public static ItemStack getCookedItem(ItemStack inputItem) {
        FurnaceRecipe recipe = Bukkit.getServer().getRecipesFor(inputItem).stream()
                .filter(FurnaceRecipe.class::isInstance)
                .map(FurnaceRecipe.class::cast)
                .findFirst()
                .orElse(null);

        return recipe != null ? recipe.getResult().clone() : null;
    }

    public File getPluginDirectory() {
        File dir = getDataFolder();

        if(!dir.mkdir() && (!dir.exists() || !dir.isDirectory()))
            throw new Error("Cannot create plugin directory.");

        return dir;
    }
}