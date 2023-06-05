package org.worldeater.worldeater;

import org.bukkit.GameMode;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Iterator;

public class PlayerState {
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
}
