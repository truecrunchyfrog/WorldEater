package org.worldeater.worldeater;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class Events implements Listener {
    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent e) {
        try {
            PlayerState.restoreState(e.getPlayer()); // Checks if saved player state exists, then restores player to that state.
        } catch(IllegalArgumentException err) {
            err.printStackTrace();
        }
    }
}
