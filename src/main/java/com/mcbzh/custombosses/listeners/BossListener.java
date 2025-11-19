package com.mcbzh.custombosses.listeners;

import com.mcbzh.custombosses.BossManager;
import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.boss.Boss;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Handles boss damage and death events
 */
public class BossListener implements Listener {

    private final BossManager bossManager;

    public BossListener(CustomBossesPlugin plugin) {
        this.bossManager = plugin.getBossManager();
    }

    @EventHandler
    public void onInteractionDamage(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();

        if (entity instanceof Interaction) {
            // Find the boss that owns this interaction
            Boss boss = bossManager.getBossByInteraction(entity.getUniqueId());
            if (boss != null) {
                event.setCancelled(true);
                boss.damage(1.0); // Default damage, can be modified
                event.getPlayer().sendMessage("§cHit boss!");
            }
        }
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Boss boss = bossManager.getBossByCore(entity.getUniqueId());

        if (boss != null) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            // TODO: Handle custom loot drops
            bossManager.removeBoss(boss);
        }
    }

    @EventHandler
    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            Boss boss = bossManager.getBossByCore(event.getEntity().getUniqueId());
            if (boss != null) {
                // Damage is handled through the core entity naturally
                // Just trigger hurt animation
                boss.damage(event.getDamage());
            }
        }
    }
}