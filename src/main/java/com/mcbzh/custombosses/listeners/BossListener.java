package com.mcbzh.custombosses.listeners;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.boss.CustomBoss;
import com.mcbzh.custombosses.manager.BossManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class BossListener implements Listener {

    private final BossManager bossManager;

    public BossListener(CustomBossesPlugin plugin) {
        this.bossManager = plugin.getBossManager();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        for (CustomBoss boss : bossManager.getAllBosses()) {
            if (boss.getCoreEntity().equals(entity)) {
                event.getDrops().clear();
                event.setDroppedExp(0);
                bossManager.removeBoss(boss);
                break;
            }
        }
    }
}