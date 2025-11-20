package com.mcbzh.custombosses.boss;

import com.mcbzh.custombosses.abilities.AbilityManager;
import com.mcbzh.custombosses.abilities.BossAbility;
import com.mcbzh.custombosses.animation.AnimationStateMachine;
import com.mcbzh.custombosses.animation.AnimationSystem;
import com.mcbzh.custombosses.animation.SmoothTransformSystem;
import com.mcbzh.custombosses.model.ModelData;
import com.mcbzh.custombosses.model.ModelInstance;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * CustomBoss - NOW WITH PHASE 1 IMPROVEMENTS:
 * - Fixed hierarchical transformations
 * - Animation state machine
 * - Ability system
 */
public class CustomBoss {

    private final UUID uuid;
    private final ModelData modelData;
    private final ModelInstance modelInstance;
    private final LivingEntity coreEntity;
    private final Interaction hitbox;

    // PHASE 1 ADDITIONS
    private final AnimationStateMachine stateMachine;
    private final AbilityManager abilityManager;
    private int ticksLived = 0;

    // Movement tracking for adaptive interpolation
    private Location lastLocation;
    private int ticksSinceLastMove = 0;

    // Target tracking
    private LivingEntity currentTarget;
    private int ticksSinceTargetCheck = 0;

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
        modelInstance.setEditorMode(false);
        modelInstance.setInterpolationMode(SmoothTransformSystem.InterpolationMode.NORMAL);
        modelInstance.spawn();

        // PHASE 1: Initialize animation state machine
        this.stateMachine = new AnimationStateMachine(modelInstance);
        setupDefaultAnimations();

        // PHASE 1: Initialize ability manager
        this.abilityManager = new AbilityManager(this);
        abilityManager.startAbilityLoop(20); // Check every second
    }

    /**
     * Setup default animation states
     * Override this or load from config for custom animations
     */
    private void setupDefaultAnimations() {
        // Create placeholder animations
        // In production, these would be loaded from JSON files

        AnimationSystem.Animation idleAnim = new AnimationSystem.Animation("idle", modelData.getId());
        idleAnim.loop = true;
        idleAnim.duration = 40; // 2 seconds
        // Add keyframes here

        AnimationSystem.Animation walkAnim = new AnimationSystem.Animation("walk", modelData.getId());
        walkAnim.loop = true;
        walkAnim.duration = 20; // 1 second

        AnimationSystem.Animation attackAnim = new AnimationSystem.Animation("attack", modelData.getId());
        attackAnim.loop = false;
        attackAnim.duration = 30; // 1.5 seconds

        // Register animations
        stateMachine.registerAnimation(AnimationStateMachine.AnimationState.IDLE, idleAnim);
        stateMachine.registerAnimation(AnimationStateMachine.AnimationState.WALK, walkAnim);
        stateMachine.registerAnimation(AnimationStateMachine.AnimationState.ATTACK, attackAnim);

        // Set transition times
        stateMachine.setTransitionTime(
                AnimationStateMachine.AnimationState.IDLE,
                AnimationStateMachine.AnimationState.WALK,
                0.2f
        );
        stateMachine.setTransitionTime(
                AnimationStateMachine.AnimationState.WALK,
                AnimationStateMachine.AnimationState.ATTACK,
                0.1f
        );
    }

    /**
     * Register an ability for this boss
     */
    public void registerAbility(BossAbility ability) {
        abilityManager.registerAbility(ability);
    }

    /**
     * Main tick method - now with animation state management
     */
    public void tick() {
        if (!isValid()) return;

        ticksLived++;
        Location coreLoc = coreEntity.getLocation();

        // Sync hitbox to core (instant, no interpolation needed)
        hitbox.teleport(coreLoc);

        // Update target
        updateTarget();

        // PHASE 1: Update animation state machine
        if (stateMachine != null) {
            stateMachine.autoUpdate(coreEntity);
            stateMachine.tick();
        }

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
     * Find and track nearest player as target
     */
    private void updateTarget() {
        ticksSinceTargetCheck++;

        // Check for new target every 20 ticks
        if (ticksSinceTargetCheck < 20) return;
        ticksSinceTargetCheck = 0;

        // Find nearest player within range
        List<Entity> nearby = coreEntity.getNearbyEntities(32, 32, 32);
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : nearby) {
            if (!(entity instanceof Player player)) continue;
            if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) continue;
            if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;

            double dist = player.getLocation().distance(coreEntity.getLocation());
            if (dist < nearestDist) {
                nearest = player;
                nearestDist = dist;
            }
        }

        if (nearest != null) {
            currentTarget = nearest;

            // Make zombie target this player
            if (coreEntity instanceof Mob mob) {
                mob.setTarget(nearest);
            }
        }
    }

    /**
     * Trigger a specific ability by ID
     */
    public boolean useAbility(String abilityId) {
        if (currentTarget == null) return false;
        return abilityManager.useAbility(abilityId, currentTarget);
    }

    /**
     * Play a specific animation state
     */
    public void playAnimation(AnimationStateMachine.AnimationState state) {
        if (stateMachine != null) {
            stateMachine.transitionTo(state);
        }
    }

    public void damage(double amount) {
        if (coreEntity != null && coreEntity.isValid()) {
            coreEntity.damage(amount);

            // Trigger hurt animation
            if (stateMachine != null) {
                stateMachine.playOneShot(AnimationStateMachine.AnimationState.HURT, null);
            }
        }
    }

    public void despawn() {
        if (abilityManager != null) {
            abilityManager.stop();
        }
        if (coreEntity != null) coreEntity.remove();
        if (hitbox != null) hitbox.remove();
        if (modelInstance != null) modelInstance.despawn();
    }

    public boolean isValid() {
        return coreEntity != null && coreEntity.isValid() &&
                hitbox != null && hitbox.isValid();
    }

    // Getters
    public UUID getUUID() { return uuid; }
    public LivingEntity getCoreEntity() { return coreEntity; }
    public Interaction getHitbox() { return hitbox; }
    public ModelInstance getModelInstance() { return modelInstance; }
    public ModelData getModelData() { return modelData; }
    public AnimationStateMachine getStateMachine() { return stateMachine; }
    public AbilityManager getAbilityManager() { return abilityManager; }
    public int getTicksLived() { return ticksLived; }
    public LivingEntity getTarget() { return currentTarget; }
}