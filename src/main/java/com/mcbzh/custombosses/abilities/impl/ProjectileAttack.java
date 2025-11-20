package com.mcbzh.custombosses.abilities.impl;

import com.mcbzh.custombosses.abilities.AbilityScheduler;
import com.mcbzh.custombosses.abilities.BossAbility;
import com.mcbzh.custombosses.boss.CustomBoss;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Collection; /**
 * PROJECTILE ATTACK - Boss throws projectiles at target
 */
public class ProjectileAttack extends BossAbility {

    private final double damage;
    private final int projectileCount;
    private final double speed;

    public ProjectileAttack() {
        this(10.0, 3, 1.5);
    }

    public ProjectileAttack(double damage, int projectileCount, double speed) {
        super("projectile", "Rock Throw", 60); // 3 second cooldown
        this.damage = damage;
        this.projectileCount = projectileCount;
        this.speed = speed;
        setAnimationState("ABILITY_1");
    }

    @Override
    protected boolean checkConditions(CustomBoss boss, LivingEntity target) {
        double distance = boss.getCoreEntity().getLocation().distance(target.getLocation());
        return distance >= 5.0 && distance <= 30.0; // Medium range
    }

    @Override
    protected void execute(CustomBoss boss, LivingEntity target) {
        Location start = boss.getCoreEntity().getEyeLocation();

        // Fire projectiles in a spread
        for (int i = 0; i < projectileCount; i++) {
            final int index = i;
            AbilityScheduler.scheduleEffect(() -> {
                fireProjectile(start, target.getEyeLocation(), index);
            }, i * 5); // Stagger launches
        }
    }

    private void fireProjectile(Location start, Location target, int index) {
        World world = start.getWorld();

        // Calculate direction with slight spread
        Vector direction = target.toVector().subtract(start.toVector()).normalize();
        double spreadAngle = Math.toRadians(10);
        direction.rotateAroundY((index - projectileCount / 2.0) * spreadAngle);

        // Spawn projectile (falling block for visual)
        FallingBlock projectile = world.spawnFallingBlock(start, Material.STONE.createBlockData());
        projectile.setDropItem(false);
        projectile.setHurtEntities(true);
        projectile.setVelocity(direction.multiply(speed));
        projectile.setGravity(true);

        // Track projectile for hit detection
        trackProjectile(projectile);

        // Sound
        world.playSound(start, Sound.ENTITY_SNOWBALL_THROW, 1.0f, 0.5f);

        // Trail particles
        AbilityScheduler.scheduleRepeating(() -> {
            if (!projectile.isValid()) return;
            world.spawnParticle(Particle.SMOKE,
                    projectile.getLocation(), 2, 0.1, 0.1, 0.1, 0);
        }, 1, 100);
    }

    private void trackProjectile(FallingBlock projectile) {
        AbilityScheduler.scheduleRepeating(() -> {
            if (!projectile.isValid()) return;

            // FIXED: Use world.getNearbyEntities instead of location.getNearbyEntities
            World world = projectile.getWorld();
            Collection<Entity> nearby = world.getNearbyEntities(
                    projectile.getLocation(), 1.5, 1.5, 1.5);

            for (Entity entity : nearby) {
                if (entity instanceof LivingEntity living && entity != projectile) {
                    // Hit!
                    living.damage(damage);

                    // Impact effect
                    world.spawnParticle(Particle.BLOCK,
                            projectile.getLocation(), 20, 0.5, 0.5, 0.5, 0.1,
                            Material.STONE.createBlockData());
                    world.playSound(projectile.getLocation(),
                            Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);

                    projectile.remove();
                    return;
                }
            }
        }, 1, 100);
    }
}
