package org.worldeater.worldeater.commands.EatWorld;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.worldeater.worldeater.WorldEater;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TeamSelectionScreen implements InventoryHolder, Listener {
    private final Inventory inventory;
    protected final ArrayList<Player> seekers, hiders;

    protected TeamSelectionScreen() {
        inventory = Bukkit.createInventory(this, 18, "Choose a team to play in.");
        seekers = new ArrayList<>();
        hiders = new ArrayList<>();

        WorldEater.getPlugin().getServer().getPluginManager().registerEvents(this, WorldEater.getPlugin());
    }

    protected void update() {
        inventory.clear();

        for(int i = 0; i < 9; i++)
            inventory.setItem(i, getCustomItem("§c§lSEEKERS", Material.RED_STAINED_GLASS_PANE, Collections.singletonList("§ePlay as a seeker.")));

        for(int i = 9; i < 18; i++)
            inventory.setItem(i, getCustomItem("§a§lHIDERS", Material.LIME_STAINED_GLASS_PANE, Collections.singletonList("§ePlay as a hider.")));

        for(Player seeker : seekers)
            inventory.setItem(seekers.indexOf(seeker) + 1, getPlayerSkull(seeker, "§8[§cSeeker§8] §e" + seeker.getName()));

        for(Player hider : hiders)
            inventory.setItem(hiders.indexOf(hider) + 9 + 1, getPlayerSkull(hider, "§8[§aHider§8] §e" + hider.getName()));
    }

    public void stop() {
        inventory.clear();
        HandlerList.unregisterAll(this);

        for(Player seeker : seekers)
            seeker.closeInventory();

        for(Player hider : hiders)
            hider.closeInventory();

        seekers.clear();
        hiders.clear();
    }

    private ItemStack getCustomItem(String label, Material material, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(label);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getPlayerSkull(Player player, String label) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        assert skullMeta != null;
        skullMeta.setOwningPlayer(player);
        skullMeta.setDisplayName(label);
        item.setItemMeta(skullMeta);
        return item;
    }

    @SuppressWarnings("all")
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    private void onInventoryClick(InventoryClickEvent e) {
        if(e.getWhoClicked() instanceof Player) {
            Player player = (Player) e.getWhoClicked();

            if(seekers.contains(player) || hiders.contains(player)) {
                e.setCancelled(true);
                int slot = e.getSlot();

                if(slot < 9 && hiders.contains(player)) { // Clicked seeker slot: move to seeker.
                    hiders.remove(player);
                    seekers.add(player);
                    update();
                } else if(slot >= 9 && slot < 18 && seekers.contains(player)) { // Clicked hider slot: move to hider.
                    seekers.remove(player);
                    hiders.add(player);
                    update();
                } else return;
                player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 0.5f);
            }
        }
    }

    @EventHandler
    private void onInventoryClose(InventoryCloseEvent e) {
        if(e.getPlayer() instanceof Player) {
            Player player = (Player) e.getPlayer();
            if(seekers.contains(player) || hiders.contains(player))
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.openInventory(e.getInventory());
                    }
                }.runTaskLater(WorldEater.getPlugin(), 5);
        }
    }
}
