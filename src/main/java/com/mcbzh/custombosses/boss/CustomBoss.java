package com.mcbzh.custombosses.boss;

import com.mcbzh.custombosses.animation.SmoothTransformSystem;
import com.mcbzh.custombosses.model.ModelData;
import com.mcbzh.custombosses.model.ModelInstance;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * CustomBoss with smooth interpolation for fluid 60fps animations
 * Unlike editor mode, live bosses use smooth interpolation for natural movement
 */
public class CustomBoss {

    private final UUID uuid;
    private final ModelData modelData;
    private final ModelInstance modelInstance;
    private final LivingEntity coreEntity;
    private final Interaction hitbox;

    // Movement tracking for adaptive interpolation
    private Location lastLocation;
    private int ticksSinceLastMove = 0;

    public CustomBoss(ModelData data, Location location) {
        this.uuid = UUID.randomUUID();
        this.modelData = data;
        this.lastLocation = location.clone();

        // Spawn core (invisible zombie for AI)
        this.coreEntity = (LivingEntity) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        coreEntity.setInvisible(true);
        coreEntity.setSilent(true);
        coreEntity.setAI(true);

        if (coreEntity instanceof Zombie zombie) {
            zombie.setAdult();
        }

        // Set health
        coreEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100.0);
        coreEntity.setHealth(100.0);

        // Add slowness to reduce jitter
        coreEntity.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false
        ));

        // Spawn hitbox
        this.hitbox = (Interaction) location.getWorld().spawnEntity(location, EntityType.INTERACTION);
        hitbox.setInteractionWidth((float) data.getHitboxSize().getX());
        hitbox.setInteractionHeight((float) data.getHitboxSize().getY());
        hitbox.setResponsive(true);

        // Spawn visual model
        this.modelInstance = new ModelInstance(data, location);

        // *** CRITICAL: Live bosses use SMOOTH mode (not editor mode) ***
        modelInstance.setEditorMode(false);
        modelInstance.setInterpolationMode(SmoothTransformSystem.InterpolationMode.NORMAL);

        modelInstance.spawn();
    }

    public void tick() {
        if (!isValid()) return;

        Location coreLoc = coreEntity.getLocation();

        // Sync hitbox to core (instant, no interpolation needed)
        hitbox.teleport(coreLoc);

        // Check if boss moved
        boolean hasMoved = !coreLoc.getWorld().equals(lastLocation.getWorld()) ||
                coreLoc.distanceSquared(lastLocation) > 0.001;

        if (hasMoved) {
            // Adaptive interpolation based on movement speed
            double distance = coreLoc.distance(lastLocation);

            if (distance > 2.0) {
                // Large teleport - use instant update
                modelInstance.updateInstant();
            } else if (distance > 0.5) {
                // Fast movement - use faster interpolation
                modelInstance.setInterpolationMode(SmoothTransformSystem.InterpolationMode.FAST);
                modelInstance.setRootLocation(coreLoc);
                modelInstance.update();
            } else {
                // Normal movement - use smooth interpolation
                modelInstance.setInterpolationMode(SmoothTransformSystem.InterpolationMode.NORMAL);
                modelInstance.setRootLocation(coreLoc);
                modelInstance.update();
            }

            lastLocation = coreLoc.clone();
            ticksSinceLastMove = 0;
        } else {
            ticksSinceLastMove++;

            // Even when not moving, update occasionally for animations
            if (ticksSinceLastMove % 5 == 0) {
                modelInstance.update();
            }
        }
    }

    /**
     * Apply animation to boss
     * Uses batch update system for synchronized part movement
     */
    public void applyAnimation(String animationId) {
        // This would be called by your AnimationSystem
        // The batch update ensures all parts move together smoothly

        SmoothTransformSystem.BatchTransformUpdate batch = modelInstance.beginBatchUpdate();

        // Example: Apply animation transforms to parts
        // batch.updatePart("arm_left", newPos, newRot, newScale);
        // batch.updatePart("arm_right", newPos, newRot, newScale);

        batch.apply(); // All parts update together with smooth interpolation
    }

    public void damage(double amount) {
        if (coreEntity != null && coreEntity.isValid()) {
            coreEntity.damage(amount);
        }
    }

    public void despawn() {
        if (coreEntity != null) coreEntity.remove();
        if (hitbox != null) hitbox.remove();
        if (modelInstance != null) modelInstance.despawn();
    }

    public boolean isValid() {
        return coreEntity != null && coreEntity.isValid() &&
                hitbox != null && hitbox.isValid();
    }

    public UUID getUUID() { return uuid; }
    public LivingEntity getCoreEntity() { return coreEntity; }
    public Interaction getHitbox() { return hitbox; }
    public ModelInstance getModelInstance() { return modelInstance; }
    public ModelData getModelData() { return modelData; }
}