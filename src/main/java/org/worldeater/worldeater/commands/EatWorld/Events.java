package org.worldeater.worldeater.commands.EatWorld;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.worldeater.worldeater.WorldEater;

public final class Events implements Listener {
    private final Game game;

    public Events(Game gameInstance) {
        game = gameInstance;
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent e) {
        if(game.frozenPlayers.contains(e.getPlayer()) && e.getFrom() != e.getTo())
            e.setCancelled(true); // Prevent frozen player from moving.
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent e) {
        if(game.players.contains(e.getEntity())) {
            final Player player = e.getEntity();
            Location realSpawn = player.getBedSpawnLocation();
            player.setBedSpawnLocation(game.getSpawnLocation());
            Bukkit.getScheduler().scheduleSyncDelayedTask(WorldEater.getPlugin(), () -> {
                player.spigot().respawn();
                player.setBedSpawnLocation(realSpawn);

                if(!game.hiders.contains(player)) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    player.setInvulnerable(true);
                    player.setInvisible(true);

                    game.frozenPlayers.add(player);

                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 10, 10, true, false));

                    for(int i = 10; i > 0; i--) {
                        int finalI = i;
                        game.bukkitTasks.add(new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.sendTitle("§c" + finalI, "§euntil you can continue...", 0, 20 * 2, 0);
                            }
                        }.runTaskLater(WorldEater.getPlugin(), 20L * (10 - i)));
                    }

                    game.bukkitTasks.add(new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.setAllowFlight(false);
                            player.setFlying(false);
                            player.setInvulnerable(false);
                            player.setInvisible(false);

                            game.frozenPlayers.remove(player);
                        }
                    }.runTaskLater(WorldEater.getPlugin(), 20 * 10));
                }
            }, 2);

            if(game.hiders.contains(player))
                game.stop(false); // Hider died and lost.
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent e) {
        if(game.players.contains(e.getPlayer()) || game.spectators.contains(e.getPlayer()))
            game.playerLeave(e.getPlayer());
    }

    @EventHandler
    private void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        if(game.players.contains(e.getPlayer()) && !e.getPlayer().getWorld().equals(game.world))
            e.getPlayer().teleport(game.getSpawnLocation());
    }

    @EventHandler
    private void onBlockBreak(BlockBreakEvent e) {
        if(game.players.contains(e.getPlayer()) && game.status == Game.GameStatus.AWAITING_START)
            e.setCancelled(true);
    }
}
