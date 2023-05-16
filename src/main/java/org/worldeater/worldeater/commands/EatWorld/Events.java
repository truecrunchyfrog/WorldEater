package org.worldeater.worldeater.commands.EatWorld;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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
    private final ArrayList<ItemStack> silencerItems;
    private final ArrayList<Player> silencedPlayers;
    private final PacketListener packetListener;

    public Events(Game gameInstance) {
        game = gameInstance;
        silencerItems = new ArrayList<>();
        silencedPlayers = new ArrayList<>();
        packetListener = new PacketAdapter(WorldEater.getPlugin(), PacketType.Play.Server.BLOCK_ACTION) {
            @Override
            public void onPacketSending(PacketEvent event) {
                WorldEater.getPlugin().getLogger().info("PACKET SENDING EVENT!!!" + event.getPacketType().name());
                if(/*event.getPlayer().getWorld() == game.world && */silencedPlayers.contains(event.getPlayer())) {
                    WorldEater.getPlugin().getLogger().info("CANCEL PACKET EVENT");
                    event.setCancelled(true);
                }
            }
        };

        WorldEater.protocolManager.addPacketListener(packetListener);
    }

    public void stopPacketListener() {
        ProtocolLibrary.getProtocolManager().removePacketListener(packetListener);
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

                if(game.spectators.contains(player))
                    return;

                if(!game.hiders.contains(player)) {
                    game.seekerRespawn(player);

                    ItemStack item = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta skullMeta = (SkullMeta) item.getItemMeta();

                    assert skullMeta != null;

                    skullMeta.setOwningPlayer(player);
                    skullMeta.setDisplayName("§c§lA Moment of Silence §8[ §eRight-click §8]");
                    skullMeta.setLore(Collections.singletonList("§eRight-click to have sounds you make be silenced for §c30s§e."));

                    item.setItemMeta(skullMeta);

                    silencerItems.add(item);
                    game.world.dropItemNaturally(Objects.requireNonNull(player.getLastDeathLocation()), item);
                } else {
                    PlayerState.prepareNormal(player, false);

                    if(game.hiders.size() == 1) {
                        game.stop(false); // Hiders are dead, lost.
                    } else {
                        game.hiders.remove(player);
                        game.players.remove(player);
                        game.playerJoin(player, true, false); // Make dead player spectator.
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
        if(Objects.requireNonNull(e.getEntity()).getWorld() == game.world)
            e.setCancelled(true);
    }

    @EventHandler
    private void onPlayerBedEnter(PlayerBedEnterEvent e) {
        if(game.players.contains(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().setFireTicks(20);
            e.getPlayer().playSound(e.getPlayer(), Sound.ENTITY_PIGLIN_JEALOUS, 1, 0.5f);
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
        if(e.getEntity().getWorld() == game.world) {
            if(game.status != Game.GameStatus.RUNNING || game.frozenPlayers.contains((Player) e.getEntity()))
                e.setCancelled(true);
            else if(game.autoCook && getCookedItem(e.getItem().getItemStack()) != null)
                e.getItem().setItemStack(Objects.requireNonNull(getCookedItem(e.getItem().getItemStack())));
        }
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent e) {
        if((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && game.players.contains(e.getPlayer())) {
            if(silencerItems.contains(e.getItem())) {
                if(!game.hiders.contains(e.getPlayer()) && !game.debug) {
                    WorldEater.sendMessage(e.getPlayer(), "§cOnly hiders may use this!");
                    return;
                }

                Objects.requireNonNull(e.getItem()).setAmount(0);

                Player player = e.getPlayer();
                player.playSound(player, Sound.ENTITY_CAT_HISS, 1, 0.5f);
                player.sendTitle("§aSssh...", "§3Your are now §nsilent§3.", 10, 20 * 3, 10);
                WorldEater.sendMessage(player, "§eSounds you make will be silenced for §c30§e seconds.");

                game.bukkitTasks.add(new BukkitRunnable() {
                    @Override
                    public void run() {
                        silencedPlayers.remove(player);
                        player.sendTitle("§c§lSssh!", "§eYou are no longer silent.", 10, 20 * 3, 10);
                        WorldEater.sendMessage(player, "§eYou now make sounds again.");
                    }
                }.runTaskLater(WorldEater.getPlugin(), 20 * 30));
            }
        }
    }
}
