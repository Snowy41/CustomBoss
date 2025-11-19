package com.mcbzh.custombosses.editor;

import com.mcbzh.custombosses.CustomBossesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class EditorManager {

    private final CustomBossesPlugin plugin;
    private final Map<UUID, EditorSession> sessions;
    private BukkitTask tickTask;

    public EditorManager(CustomBossesPlugin plugin) {
        this.plugin = plugin;
        this.sessions = new HashMap<>();
        startTicking();
    }

    private void startTicking() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        sessions.values().forEach(EditorSession::tick);

        // Clean up invalid sessions
        sessions.entrySet().removeIf(entry -> {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                entry.getValue().exit();
                return true;
            }
            return false;
        });
    }

    public EditorSession getSession(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(),
                uuid -> new EditorSession(player, plugin));
    }

    public void removeSession(Player player) {
        EditorSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            session.exit();
        }
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        sessions.values().forEach(EditorSession::exit);
        sessions.clear();
    }
}
