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
            world.spawnParticle(Particle.CRIT,
                    x, center.getY(), z,
                    20, 0.5, 0.1, 0.5, 0.1,
                    Material.STONE.createBlockData());
        }

        // Sound
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE,
                SoundCategory.HOSTILE, 2.0f, 0.5f);
        world.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK,
                SoundCategory.HOSTILE, 1.5f, 0.8f);

        // Damage nearby entities
        Collection<Entity> nearby = center.getWorld()
                .getNearbyEntities(center, radius, radius, radius);

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

/**
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

            // Check for nearby entities
            Collection<Entity> nearby = projectile.getLocation()
                    .getNearbyEntities(1.5, 1.5, 1.5);

            for (Entity entity : nearby) {
                if (entity instanceof LivingEntity living && entity != projectile) {
                    // Hit!
                    living.damage(damage);

                    // Impact effect
                    World world = projectile.getWorld();
                    world.spawnParticle(Particle.BUBBLE,
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

/**
 * LEAP ATTACK - Boss leaps toward target
 */
public class LeapAttack extends BossAbility {

    private final double damage;
    private final double force;

    public LeapAttack() {
        this(12.0, 1.5);
    }

    public LeapAttack(double damage, double force) {
        super("leap", "Leap Attack", 80); // 4 second cooldown
        this.damage = damage;
        this.force = force;
        setAnimationState("ABILITY_2");
    }

    @Override
    protected boolean checkConditions(CustomBoss boss, LivingEntity target) {
        double distance = boss.getCoreEntity().getLocation().distance(target.getLocation());
        return distance >= 5.0 && distance <= 15.0 && boss.getCoreEntity().isOnGround();
    }

    @Override
    protected void execute(CustomBoss boss, LivingEntity target) {
        Location start = boss.getCoreEntity().getLocation();
        Location targetLoc = target.getLocation();

        // Calculate leap vector
        Vector direction = targetLoc.toVector().subtract(start.toVector()).normalize();
        direction.setY(0.5); // Upward arc
        direction.multiply(force);

        // Apply velocity
        boss.getCoreEntity().setVelocity(direction);

        // Sound
        start.getWorld().playSound(start, Sound.ENTITY_ENDER_DRAGON_FLAP,
                SoundCategory.HOSTILE, 1.0f, 0.8f);

        // Trail particles during leap
        AbilityScheduler.scheduleRepeating(() -> {
            if (!boss.isValid()) return;
            Location loc = boss.getCoreEntity().getLocation();
            loc.getWorld().spawnParticle(Particle.CLOUD, loc, 5, 0.3, 0.3, 0.3, 0);
        }, 2, 40);

        // Check for landing
        checkLanding(boss, start);
    }

    private void checkLanding(CustomBoss boss, Location startLoc) {
        AbilityScheduler.scheduleRepeating(() -> {
            if (!boss.isValid()) return;

            LivingEntity entity = boss.getCoreEntity();
            if (entity.isOnGround() && entity.getVelocity().lengthSquared() < 0.1) {
                // Landed!
                performLandingEffect(boss.getCoreEntity().getLocation());
            }
        }, 5, 60);
    }

    private void performLandingEffect(Location landLoc) {
        World world = landLoc.getWorld();

        // Impact
        world.spawnParticle(Particle.EXPLOSION, landLoc, 1);
        world.playSound(landLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

        // Damage nearby
        double radius = 4.0;
        Collection<Entity> nearby = landLoc.getNearbyEntities(radius, radius, radius);

        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity living) {
                double distance = entity.getLocation().distance(landLoc);
                double damageMultiplier = 1.0 - (distance / radius);
                living.damage(damage * damageMultiplier);

                // Knockback
                Vector direction = entity.getLocation().subtract(landLoc).toVector().normalize();
                living.setVelocity(direction.multiply(1.0));
            }
        }
    }
}

