package com.mcbzh.custombosses;

import com.mcbzh.custombosses.boss.Boss;
import com.mcbzh.custombosses.boss.CustomBoss;
import com.mcbzh.custombosses.model.ModelData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossManager {

    private final CustomBossesPlugin plugin;
    private final Map<UUID, Boss> activeBosses = new ConcurrentHashMap<>();
    private final Map<UUID, Boss> interactionToBoss = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public BossManager(CustomBossesPlugin plugin) {
        this.plugin = plugin;
        startTickTask();
    }

    private void startTickTask() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void tick() {
        activeBosses.values().removeIf(boss -> {
            if (!boss.isValid()) {
                removeBoss(boss);
                return true;
            }
            boss.tick();
            return false;
        });
    }

    public Boss spawnBoss(ModelData model, Location location) {
        if (model == null) {
            plugin.getLogger().warning("Attempted to spawn boss with null model!");
            return null;
        }

        CustomBoss boss = new CustomBoss(model, location);
        activeBosses.put(boss.getCore().getUniqueId(), boss);
        interactionToBoss.put(boss.getInteraction().getUniqueId(), boss);

        plugin.getLogger().info("Spawned boss: " + model.getId() + " at " + location);
        return boss;
    }

    public Boss getBossByCore(UUID coreUUID) {
        return activeBosses.get(coreUUID);
    }

    public Boss getBossByInteraction(UUID interactionUUID) {
        return interactionToBoss.get(interactionUUID);
    }

    public void removeBoss(Boss boss) {
        boss.despawn();
        activeBosses.remove(boss.getCore().getUniqueId());
        if (boss instanceof CustomBoss) {
            interactionToBoss.remove(((CustomBoss) boss).getInteraction().getUniqueId());
        }
    }

    public void despawnAll() {
        activeBosses.values().forEach(Boss::despawn);
        activeBosses.clear();
        interactionToBoss.clear();
    }

    public void removeAllBosses() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        despawnAll();
    }

    public Map<UUID, Boss> getActiveBosses() {
        return activeBosses;
    }
}