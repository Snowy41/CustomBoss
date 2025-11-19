package com.mcbzh.custombosses.editor;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.editor.tools.EditorTool;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditorManager implements Listener {

    private final Map<UUID, EditorSession> sessions = new HashMap<>();
    private final CustomBossesPlugin plugin;

    public EditorManager(CustomBossesPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Tick task for visual guides
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public EditorSession getSession(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), uuid -> new EditorSession(uuid));
    }

    public void removeSession(Player player) {
        EditorSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            session.exitEditor();
        }
    }

    public void cleanupAll() {
        for (EditorSession session : sessions.values()) {
            Player player = session.getPlayer();
            if (player != null && player.isOnline()) {
                session.exitEditor();
            }
        }
        sessions.clear();
    }

    private void tick() {
        sessions.values().removeIf(session -> {
            Player player = session.getPlayer();
            if (player == null || !player.isOnline()) {
                session.exitEditor();
                return true;
            }
            session.tick();
            return false;
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeSession(event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();
        EditorSession session = sessions.get(player.getUniqueId());

        if (session != null && session.isInEditor()) {
            int slot = player.getInventory().getHeldItemSlot();
            EditorTool tool = session.getTool(slot);

            if (tool != null) {
                event.setCancelled(true);
                tool.onUse(player, event.getAction());
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        EditorSession session = sessions.get(player.getUniqueId());

        if (session != null && session.isWaitingForChatInput()) {
            event.setCancelled(true);

            // Handle on main thread
            String message = event.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> {
                session.handleChatInput(message);
            });
        }
    }
}