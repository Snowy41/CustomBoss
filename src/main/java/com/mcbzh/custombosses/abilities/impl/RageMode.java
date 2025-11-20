package com.mcbzh.custombosses.abilities.impl;

import com.mcbzh.custombosses.abilities.AbilityScheduler;
import com.mcbzh.custombosses.abilities.BossAbility;
import com.mcbzh.custombosses.boss.CustomBoss;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType; /**
 * RAGE MODE - Boss enters enraged state, increasing speed and damage
 */
public class RageMode extends BossAbility {

    private final int duration;
    private final double speedMultiplier;

    public RageMode() {
        this(200, 1.5); // 10 seconds, 50% faster
    }

    public RageMode(int durationTicks, double speedMultiplier) {
        super("rage", "Enrage", 400); // 20 second cooldown
        this.duration = durationTicks;
        this.speedMultiplier = speedMultiplier;
        setAnimationState("ABILITY_3");
    }

    @Override
    protected boolean checkConditions(CustomBoss boss, LivingEntity target) {
        // Only use when below 50% health
        return boss.getCoreEntity().getHealth() / boss.getCoreEntity().getMaxHealth() < 0.5;
    }

    @Override
    protected void execute(CustomBoss boss, LivingEntity target) {
        LivingEntity entity = boss.getCoreEntity();
        Location loc = entity.getLocation();

        // Visual effect
        loc.getWorld().spawnParticle(Particle.LAVA, loc, 50, 1, 1, 1, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL,
                SoundCategory.HOSTILE, 2.0f, 0.5f);

        // Apply buffs
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED, duration, 1, false, true
        ));
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.STRENGTH, duration, 0, false, true
        ));
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE, duration, 0, false, true
        ));

        // Change model appearance (make parts red)
        if (boss.getModelInstance() != null) {
            boss.getModelInstance().getParts().values().forEach(part -> {
                if (part.getEntity() != null) {
                    part.getEntity().setGlowing(true);
                    part.getEntity().setGlowColorOverride(Color.RED);
                }
            });

            // Restore after duration
            AbilityScheduler.scheduleEffect(() -> {
                boss.getModelInstance().getParts().values().forEach(part -> {
                    if (part.getEntity() != null) {
                        part.getEntity().setGlowing(false);
                    }
                });
            }, duration);
        }

        // Aura particles during rage
        AbilityScheduler.scheduleRepeating(() -> {
            if (!boss.isValid()) return;
            Location auraLoc = entity.getLocation();
            auraLoc.getWorld().spawnParticle(Particle.FLAME,
                    auraLoc, 10, 0.5, 0.5, 0.5, 0.05);
        }, 5, duration);
    }
}
