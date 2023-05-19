package org.worldeater.worldeater;

import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerState {
    private final UUID playerUUID;
    private final PlayerInventory inventory;
    private final double health;
    private final int foodLevel;
    private final int experienceLevel;
    private final float experienceProgress;
    private final Location location;
    private final GameMode gameMode;
    private final boolean allowFlight;
    private final boolean invisible;
    private final boolean invulnerable;
    private final String[] advancementCriteria;

    private static File getPlayerStateDir() {
        File playerStateDir = new File(WorldEater.getPlugin().getPluginDirectory(), "PlayerStates");

        if(!playerStateDir.mkdir() && (!playerStateDir.exists() || !playerStateDir.isDirectory()))
            throw new Error("Cannot create PlayerStates directory.");

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

        prepareNormal(player, true);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(stateFile);

        player.getInventory().setContents(((List<ItemStack>) Objects.requireNonNull(config.get("inventory.contents"))).toArray(new ItemStack[0]));
        player.getInventory().setArmorContents(((List<ItemStack>) Objects.requireNonNull(config.get("inventory.armor"))).toArray(new ItemStack[0]));
        player.getInventory().setItemInOffHand((ItemStack) Objects.requireNonNull(config.get("inventory.off_hand")));

        player.setHealth(config.getDouble("health"));
        player.setFoodLevel(config.getInt("food_level"));
        player.setLevel(config.getInt("experience_level"));
        player.setExp(((float) config.getDouble("experience_progress")));
        player.setGameMode(GameMode.valueOf(config.getString("game_mode")));

        player.setAllowFlight(config.getBoolean("allow_flight"));
        player.setInvisible(config.getBoolean("invisible"));
        player.setInvulnerable(config.getBoolean("invulnerable"));

        List<String> restoreCriteriaList = config.getStringList("advancement_criteria");

        Iterator<Advancement> advancementIterator = WorldEater.getPlugin().getServer().advancementIterator();

        boolean oldAnnouncementValue = Boolean.TRUE.equals(player.getWorld().getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS));
        player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);

        while(advancementIterator.hasNext()) {
            AdvancementProgress progress = player.getAdvancementProgress(advancementIterator.next());
            for(String criteria : progress.getRemainingCriteria())
                if(restoreCriteriaList.contains(criteria))
                    progress.awardCriteria(criteria);
        }

        if(oldAnnouncementValue)
            player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);

        deletePlayerStateFile(player.getUniqueId());
    }

    private static void prepareDefault(Player player) {
        if(player.isDead())
            player.spigot().respawn();

        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setFoodLevel(20);

        player.setLevel(0);
        player.setExp(0);

        player.setFireTicks(0);
        player.setHealthScaled(false);

        player.getInventory().clear();

        for(PotionEffect potionEffect : player.getActivePotionEffects())
            player.removePotionEffect(potionEffect.getType());

        Iterator<Advancement> advancementIterator = WorldEater.getPlugin().getServer().advancementIterator();

        while(advancementIterator.hasNext()) {
            AdvancementProgress progress = player.getAdvancementProgress(advancementIterator.next());
            for(String criteria : progress.getAwardedCriteria())
                progress.revokeCriteria(criteria); // Remove all advancement criteria, resetting the advancement state.
        }

        Scoreboard scoreboard = player.getScoreboard();
        Team seekerTeam = scoreboard.getTeam("seekers");
        Team hiderTeam = scoreboard.getTeam("hiders");

        if(seekerTeam != null && seekerTeam.hasEntry(player.getName()))
            seekerTeam.removeEntry(player.getName());

        if(hiderTeam != null && hiderTeam.hasEntry(player.getName()))
            hiderTeam.removeEntry(player.getName());
    }

    public static void prepareNormal(Player player, boolean inherit) {
        if(inherit)
            prepareDefault(player);

        player.setInvulnerable(false);
        player.setInvisible(false);

        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFallDistance(0);
    }

    public static void prepareIdle(Player player, boolean inherit) {
        if(inherit)
            prepareDefault(player);

        player.setInvulnerable(true);
        player.setInvisible(true);

        player.setAllowFlight(true);
        player.setFlying(true);
    }

    public PlayerState(Player player) {
        playerUUID = player.getUniqueId();
        inventory = player.getInventory();
        health = player.getHealth();
        foodLevel = player.getFoodLevel();
        experienceLevel = player.getLevel();
        experienceProgress = player.getExp();
        location = player.getLocation();
        gameMode = player.getGameMode();
        allowFlight = player.getAllowFlight();
        invisible = player.isInvisible();
        invulnerable = player.isInvulnerable();

        List<String> criteriaListTemp = new ArrayList<>();
        Iterator<Advancement> advancementIterator = WorldEater.getPlugin().getServer().advancementIterator();

        while(advancementIterator.hasNext())
            criteriaListTemp.addAll(
                    player.getAdvancementProgress( // Advancement progression for player.
                            advancementIterator.next() // This advancement.
                    ).getAwardedCriteria() // Get requirements for the advancement.
            );

        advancementCriteria = criteriaListTemp.toArray(new String[] {});
    }

    private YamlConfiguration saveToConfig() {
        YamlConfiguration config = new YamlConfiguration();

        config.set("uuid", playerUUID.toString());
        config.set("inventory.contents", inventory.getContents());
        config.set("inventory.armor", inventory.getArmorContents());
        config.set("inventory.off_hand", inventory.getItemInOffHand());
        config.set("health", health);
        config.set("food_level", foodLevel);
        config.set("experience_level", experienceLevel);
        config.set("experience_progress", experienceProgress);
        config.set("location", location);
        config.set("game_mode", gameMode.name());
        config.set("allow_flight", allowFlight);
        config.set("invisible", invisible);
        config.set("invulnerable", invulnerable);
        config.set("advancement_criteria", advancementCriteria);

        return config;
    }

    public void saveState() throws IOException {
        saveToConfig().save(getPlayerStateFile(playerUUID));
    }
}
