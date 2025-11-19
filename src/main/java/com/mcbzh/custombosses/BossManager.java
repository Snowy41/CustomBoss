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
                boss.despawn(); // Ensure cleanup
                return true;
            }
            boss.tick();
            return false;
        });
    }

    public Boss spawnBoss(ModelData model, Location location) {
        CustomBoss boss = new CustomBoss(model, location);
        activeBosses.put(boss.getCore().getUniqueId(), boss);
        return boss;
    }

    public void despawnAll() {
        activeBosses.values().forEach(Boss::despawn);
        activeBosses.clear();
    }

    public void removeAllBosses() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        despawnAll();
    }
}
