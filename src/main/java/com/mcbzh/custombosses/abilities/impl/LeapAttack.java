package com.mcbzh.custombosses.abilities.impl;

import com.mcbzh.custombosses.abilities.AbilityScheduler;
import com.mcbzh.custombosses.abilities.BossAbility;
import com.mcbzh.custombosses.boss.CustomBoss;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Collection; /**
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
        // FIXED: Use world.getNearbyEntities instead of location.getNearbyEntities
        Collection<Entity> nearby = world.getNearbyEntities(landLoc, radius, radius, radius);

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
