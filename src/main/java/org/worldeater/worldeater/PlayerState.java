package org.worldeater.worldeater;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PlayerState {
    private final UUID playerUUID;
    private final PlayerInventory inventory;
    private final double health;
    private final int foodLevel;
    private final float experience;
    private final Location location;
    private final GameMode gameMode;
    private final boolean allowFlight;
    private final boolean invisible;
    private final boolean invulnerable;

    private static File getPlayerStateDir() {
        File playerStateDir = new File(WorldEater.getPlugin().getDataFolder(), "playerStates");

        if(!playerStateDir.mkdir() && (!playerStateDir.exists() || !playerStateDir.isDirectory())) {
            return null;
        }

        return playerStateDir;
    }
    private static File getPlayerStateFile(UUID playerId) {
        return new File(getPlayerStateDir(), playerId.toString() + ".yml");
    }
    private static void deletePlayerStateFile(UUID playerId) {
        if(!getPlayerStateFile(playerId).delete())
            throw new Error("Cannot delete UUID " + playerId + " state file.");
    }

    @SuppressWarnings("unchecked")
    public static void restoreState(Player player) {
        if(!player.isOnline()) return;

        File stateFile = getPlayerStateFile(player.getUniqueId());
        if(!stateFile.exists()) return;

        prepareDefault(player);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(stateFile);

        player.getInventory().setContents(((List<ItemStack>) Objects.requireNonNull(config.get("inventory.contents"))).toArray(new ItemStack[0]));
        player.getInventory().setArmorContents(((List<ItemStack>) Objects.requireNonNull(config.get("inventory.armor"))).toArray(new ItemStack[0]));
        player.getInventory().setItemInOffHand((ItemStack) Objects.requireNonNull(config.get("inventory.off_hand")));

        player.setHealth(config.getDouble("health"));
        player.setFoodLevel(config.getInt("food_level"));
        player.setExp(((float) config.getDouble("experience")));
        player.setGameMode(GameMode.valueOf(config.getString("game_mode")));

        player.setAllowFlight(config.getBoolean("allow_flight"));
        player.setInvisible(config.getBoolean("invisible"));
        player.setInvulnerable(config.getBoolean("invulnerable"));

        deletePlayerStateFile(player.getUniqueId());
    }

    public static void prepareDefault(Player player) {
        player.setInvulnerable(false);
        player.setInvisible(false);

        player.setAllowFlight(false);
        player.setFlying(false);

        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setExp(0);
        player.setFireTicks(0);
        player.setHealthScaled(false);

        player.getInventory().clear();

        for(PotionEffect potionEffect : player.getActivePotionEffects())
            player.removePotionEffect(potionEffect.getType());
    }

    public PlayerState(Player player) {
        playerUUID = player.getUniqueId();
        inventory = player.getInventory();
        health = player.getHealth();
        foodLevel = player.getFoodLevel();
        experience = player.getExp();
        location = player.getLocation();
        gameMode = player.getGameMode();
        allowFlight = player.getAllowFlight();
        invisible = player.isInvisible();
        invulnerable = player.isInvulnerable();
    }

    private YamlConfiguration saveToConfig() {
        YamlConfiguration config = new YamlConfiguration();

        config.set("uuid", playerUUID.toString());
        config.set("inventory.contents", inventory.getContents());
        config.set("inventory.armor", inventory.getArmorContents());
        config.set("inventory.off_hand", inventory.getItemInOffHand());
        config.set("health", health);
        config.set("food_level", foodLevel);
        config.set("experience", experience);
        config.set("location", location);
        config.set("game_mode", gameMode.name());
        config.set("allow_flight", allowFlight);
        config.set("invisible", invisible);
        config.set("invulnerable", invulnerable);

        return config;
    }

    public void saveState() throws IOException {
        saveToConfig().save(getPlayerStateFile(playerUUID));
    }
}
