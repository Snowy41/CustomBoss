package com.mcbzh.custombosses.manager;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.boss.CustomBoss;
import com.mcbzh.custombosses.model.ModelData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BossManager {

    private final CustomBossesPlugin plugin;
    private final Map<UUID, CustomBoss> bosses;
    private BukkitTask tickTask;

    public BossManager(CustomBossesPlugin plugin) {
        this.plugin = plugin;
        this.bosses = new HashMap<>();
        startTicking();
    }

    private void startTicking() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        bosses.values().removeIf(boss -> {
            if (!boss.isValid()) {
                boss.despawn();
                return true;
            }
            boss.tick();
            return false;
        });
    }

    public CustomBoss spawnBoss(ModelData model, Location location) {
        CustomBoss boss = new CustomBoss(model, location);
        bosses.put(boss.getUUID(), boss);
        plugin.getLogger().info("Spawned boss: " + model.getId());
        return boss;
    }

    public CustomBoss getBoss(UUID uuid) {
        return bosses.get(uuid);
    }

    public void removeBoss(CustomBoss boss) {
        boss.despawn();
        bosses.remove(boss.getUUID());
    }

    public void removeAll() {
        bosses.values().forEach(CustomBoss::despawn);
        bosses.clear();
    }

    public Collection<CustomBoss> getAllBosses() {
        return bosses.values();
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        removeAll();
    }
}