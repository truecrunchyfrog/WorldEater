package org.worldeater.worldeater.commands.EatWorld;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.worldeater.worldeater.PlayerState;
import org.worldeater.worldeater.WorldEater;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import static org.worldeater.worldeater.WorldEater.getCookedItem;

public final class Events implements Listener {
    private final Game game;
    private final ArrayList<ItemStack> ghostHeadItems;

    public Events(Game gameInstance) {
        game = gameInstance;
        ghostHeadItems = new ArrayList<>();
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent e) {
        if(e.getFrom() != e.getTo() && e.getTo() != null && game.status == Game.GameStatus.RUNNING && e.getPlayer().getWorld() == game.world) {
            if(game.frozenPlayers.contains(e.getPlayer())) {
                e.setCancelled(true); // Prevent frozen player from moving.
            } else if(e.getTo().getY() < game.world.getMinHeight() - 20 && game.players.contains(e.getPlayer())) {
                e.getPlayer().setHealth(0);
            } else if(game.spectators.contains(e.getPlayer()) && Objects.requireNonNull(e.getTo()).getY() < game.world.getMinHeight() - 20) {
                e.setCancelled(true);
                e.getPlayer().setFlying(true);
                e.getPlayer().setVelocity(new Vector(0, 15, 0));
                WorldEater.sendMessage(e.getPlayer(), "§cWoah there!");
            } else if(Math.abs(e.getTo().getX()) > 64 || Math.abs(e.getTo().getZ()) > 64) {
                e.setCancelled(true);
                Location teleportTo = e.getTo();

                teleportTo.setX(teleportTo.getX() > 64 ? 64 : -64);
                teleportTo.setZ(teleportTo.getZ() > 64 ? 64 : -64);

                e.getPlayer().teleport(teleportTo);
                WorldEater.sendMessage(e.getPlayer(), "§cYou are too far from spawn!");
            }
        }
    }

    @EventHandler
    private void onPlayerTeleport(PlayerTeleportEvent e) {
        if(e.getFrom() != e.getTo() && game.frozenPlayers.contains(e.getPlayer()))
            e.setCancelled(true); // Prevent frozen player from teleporting.
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent e) {
        if((game.players.contains(e.getEntity()) || game.spectators.contains(e.getEntity())) && game.status != Game.GameStatus.AWAITING_START) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(WorldEater.getPlugin(), () -> {
                Player player = e.getEntity();
                Location realSpawn = player.getBedSpawnLocation();
                player.setBedSpawnLocation(player.getLocation());
                player.spigot().respawn();
                player.setBedSpawnLocation(realSpawn);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.setLastDeathLocation(null);
                    }
                }.runTaskLater(WorldEater.getPlugin(), 0);

                if(game.spectators.contains(player))
                    return;

                if(!game.hiders.contains(player)) {
                    e.setKeepInventory(true);
                    e.getDrops().clear();

                    game.seekerRespawn(player);

                    ItemStack item = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta skullMeta = (SkullMeta) item.getItemMeta();

                    assert skullMeta != null;

                    skullMeta.setOwningPlayer(player);
                    skullMeta.setDisplayName("§c§lGift of the Ghosts §8[ §eRight-click for invisibility §8]");
                    skullMeta.setLore(Collections.singletonList("§eRight-click to become invisible for 20 seconds."));

                    item.setItemMeta(skullMeta);

                    ghostHeadItems.add(item);
                    e.getDrops().add(item);
                } else {
                    PlayerState.prepareNormal(player, false);

                    if(game.hiders.size() == 1) {
                        game.stop(false); // Hiders are dead, lost.
                    } else {
                        game.hiders.remove(player);
                        game.players.remove(player);
                        game.playerJoin(player, true); // Make dead player spectator.
                    }
                }
            }, 2);
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent e) {
        if(game.players.contains(e.getPlayer()) || game.spectators.contains(e.getPlayer()))
            game.playerLeave(e.getPlayer());
    }

    @EventHandler
    private void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        if((game.players.contains(e.getPlayer()) || game.spectators.contains(e.getPlayer())) && !e.getPlayer().getWorld().equals(game.world))
            e.getPlayer().teleport(game.getSpawnLocation());
    }

    @EventHandler
    private void onBlockBreak(BlockBreakEvent e) {
        if(game.players.contains(e.getPlayer()) && (game.status == Game.GameStatus.AWAITING_START || game.frozenPlayers.contains(e.getPlayer())))
            e.setCancelled(true);
    }
    
    @EventHandler
    private void onInventoryClick(InventoryClickEvent e) {
        if(e.getWhoClicked() instanceof Player && game.frozenPlayers.contains((Player) e.getWhoClicked()))
            e.setCancelled(true);
    }

    @EventHandler
    private void onPlayerDropItem(PlayerDropItemEvent e) {
        if(game.frozenPlayers.contains(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler
    private void onPortalCreateEvent(PortalCreateEvent e) {
        if(e.getWorld() == game.world) {
            Location averageBlock = new Location(game.world, 0, 0, 0);

            for(BlockState eachBlock : e.getBlocks()) {
                averageBlock.add(eachBlock.getLocation());
                eachBlock.getBlock().breakNaturally();
            }

            averageBlock.multiply(1f / e.getBlocks().size());

            game.world.strikeLightning(averageBlock);

            e.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerBedEnter(PlayerBedEnterEvent e) {
        if(game.players.contains(e.getPlayer())) {
            e.getPlayer().setFireTicks(20);
            e.getPlayer().playSound(e.getPlayer(), Sound.ENTITY_PIGLIN_JEALOUS, 1, 0.5f);
            e.setCancelled(true);
        }
    }

    @EventHandler
    private void onBlockPlace(BlockPlaceEvent e) {
        if(game.frozenPlayers.contains(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler
    private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if(e.getDamager() instanceof Player && game.frozenPlayers.contains((Player) e.getDamager()))
            e.setCancelled(true);
    }

    @EventHandler
    private void onEntityDamage(EntityDamageEvent e) {
        if(e.getEntity().getWorld() == game.world && game.status != Game.GameStatus.RUNNING)
            e.setCancelled(true);
    }

    @EventHandler
    private void onEntityPickupItem(EntityPickupItemEvent e) {
        if(e.getEntity().getWorld() == game.world && (game.status != Game.GameStatus.RUNNING || game.frozenPlayers.contains((Player) e.getEntity())))
            e.setCancelled(true);
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent e) {
        if((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && game.players.contains(e.getPlayer())) {
            if(ghostHeadItems.contains(e.getItem())) {
                e.setCancelled(true);

                if(!game.hiders.contains(e.getPlayer()) && !game.debug) {
                    WorldEater.sendMessage(e.getPlayer(), "§cOnly hiders may use this!");
                    return;
                }

                Objects.requireNonNull(e.getItem()).setAmount(0);

                Player player = e.getPlayer();
                player.playSound(player, Sound.ENTITY_CAT_HISS, 1, 0.5f);
                player.sendTitle("§aInvisible", "§3You are now §ninvisible§3.", 10, 20 * 3, 10);
                WorldEater.sendMessage(player, "§eYou are now invisible for §c20§e seconds.");

                player.setInvisible(true);
                game.bukkitTasks.add(new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.setInvisible(false);
                        player.sendTitle("§c§lWAH!", "§eYou are no longer invisible.", 10, 20 * 3, 10);
                        WorldEater.sendMessage(player, "§eYou are visible again.");
                    }
                }.runTaskLater(WorldEater.getPlugin(), 20 * 20));
            }
        }
    }
}
