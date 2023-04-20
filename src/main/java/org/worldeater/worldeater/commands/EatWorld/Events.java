package org.worldeater.worldeater.commands.EatWorld;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
        if(game.frozenPlayers != null && game.frozenPlayers.contains(e.getPlayer()) && e.getFrom() != e.getTo())
            e.setCancelled(true); // Prevent frozen player from moving.
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent e) {
        if(game.players.contains(e.getEntity())) {
            final Player player = e.getEntity();
            Location realSpawn = player.getBedSpawnLocation();
            player.setBedSpawnLocation(new Location(game.world, 8, game.world.getHighestBlockYAt(8, 8) + 2, 8));
            Bukkit.getScheduler().scheduleSyncDelayedTask(WorldEater.getPlugin(), () -> {
                player.spigot().respawn();
                player.setBedSpawnLocation(realSpawn);

                if(!player.equals(game.hider)) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
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
                            player.setInvisible(false);

                            game.frozenPlayers.remove(player);
                        }
                    }.runTaskLater(WorldEater.getPlugin(), 20 * 10));
                }
            }, 2);

            if(player.equals(game.hider)) {
                game.stop(false); // Hider died and lost.
            }
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent e) {
        if(game.players.contains(e.getPlayer())) {
            Player playerQuit = e.getPlayer();
            if(game.status == Game.GameStatus.AWAITING_START) {
                game.players.remove(playerQuit);
                WorldEater.sendBroadcast("§cA player in queue left.");
            } else if(game.status == Game.GameStatus.RUNNING) {
                game.players.remove(playerQuit);
                game.sendGameMessage("§c" + playerQuit.getName() + " quit the game!");

                if(playerQuit.equals(game.hider)) {
                    game.sendGameMessage("§cThe hider has left the game, so the game is over.");
                    game.stop(false);
                } else if(game.players.size() == 1) { // Only 1 player still in game.
                    game.sendGameMessage("§cThere is no seeker remaining, so the game is over.");
                    game.stop(true);
                }
            }
        }
    }

    @EventHandler
    private void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        if(game.players != null && game.players.contains(e.getPlayer()) && !e.getPlayer().getWorld().equals(game.world))
            e.getPlayer().teleport(new Location(game.world, 8, game.world.getHighestBlockYAt(8, 8) + 2, 8));
    }
}
