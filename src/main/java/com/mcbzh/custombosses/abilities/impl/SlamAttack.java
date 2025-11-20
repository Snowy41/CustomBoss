package com.mcbzh.custombosses.abilities.impl;

import com.mcbzh.custombosses.abilities.AbilityScheduler;
import com.mcbzh.custombosses.abilities.BossAbility;
import com.mcbzh.custombosses.boss.CustomBoss;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;

/**
 * SLAM ATTACK - Boss slams ground causing AOE damage and knockback
 */
public class SlamAttack extends BossAbility {

    private final double radius;
    private final double damage;
    private final double knockbackStrength;

    public SlamAttack() {
        this(8.0, 15.0, 2.0);
    }

    public SlamAttack(double radius, double damage, double knockbackStrength) {
        super("slam", "Ground Slam", 100); // 5 second cooldown
        this.radius = radius;
        this.damage = damage;
        this.knockbackStrength = knockbackStrength;
        setAnimationState("ATTACK");
    }

    @Override
    protected boolean checkConditions(CustomBoss boss, LivingEntity target) {
        // Only use if target is close
        double distance = boss.getCoreEntity().getLocation().distance(target.getLocation());
        return distance <= radius * 1.5;
    }

    @Override
    protected void execute(CustomBoss boss, LivingEntity target) {
        Location center = boss.getCoreEntity().getLocation();

        // Wind-up effect (particles rising)
        for (int i = 0; i < 20; i++) {
            AbilityScheduler.scheduleEffect(() -> {
                spawnWindupParticles(center);
            }, i);
        }

        // Impact after 1 second (20 ticks)
        AbilityScheduler.scheduleEffect(() -> {
            performSlam(center);
        }, 20);
    }

    private void spawnWindupParticles(Location center) {
        World world = center.getWorld();
        for (int i = 0; i < 5; i++) {
            double angle = Math.random() * Math.PI * 2;
            double r = Math.random() * radius;
            double x = center.getX() + r * Math.cos(angle);
            double z = center.getZ() + r * Math.sin(angle);

            world.spawnParticle(Particle.SMOKE,
                    x, center.getY(), z,
                    1, 0, 0.5, 0, 0.05);
        }
    }

    private void performSlam(Location center) {
        World world = center.getWorld();

        // Visual effect - explosion particle
        world.spawnParticle(Particle.EXPLOSION, center, 3);

        // Dust ring effect
        for (int i = 0; i < 30; i++) {
            double angle = (Math.PI * 2 * i) / 30;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            world.spawnParticle(Particle.BLOCK,
                    x, center.getY(), z,
                    20, 0.5, 0.1, 0.5, 0.1,
                    Material.STONE.createBlockData());
        }

        // Sound
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE,
                SoundCategory.HOSTILE, 2.0f, 0.5f);
        world.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK,
                SoundCategory.HOSTILE, 1.5f, 0.8f);

        // FIXED: Use world.getNearbyEntities instead of center.getNearbyEntities
        Collection<Entity> nearby = world.getNearbyEntities(center, radius, radius, radius);

        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity instanceof Player && ((Player) entity).isOp()) continue; // Don't hurt admins in testing

            double distance = entity.getLocation().distance(center);
            if (distance > radius) continue;

            // Falloff damage based on distance
            double damageMultiplier = 1.0 - (distance / radius);
            living.damage(damage * damageMultiplier);

            // Knockback
            Vector direction = entity.getLocation().subtract(center).toVector().normalize();
            direction.setY(0.5); // Add upward component
            living.setVelocity(direction.multiply(knockbackStrength * damageMultiplier));

            // Brief slowness
            living.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, 40, 1, false, true
            ));
        }
    }
}

