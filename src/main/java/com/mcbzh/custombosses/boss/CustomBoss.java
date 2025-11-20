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

import java.util.List;
import java.util.UUID;

/**
 * CustomBoss - FIXED VERSION
 * - Model now follows zombie movement properly
 * - Hitbox syncs position AND rotation
 * - Ability manager starts after abilities registered
 */
public class CustomBoss {

    private final UUID uuid;
    private final ModelData modelData;
    private final ModelInstance modelInstance;
    private final LivingEntity coreEntity;
    private final Interaction hitbox;

    private final AnimationStateMachine stateMachine;
    private final AbilityManager abilityManager;
    private int ticksLived = 0;

    // Movement tracking - FIXED: Better threshold and proper update tracking
    private Location lastLocation;
    private int ticksSinceLastMove = 0;
    private static final double MOVEMENT_THRESHOLD = 0.01; // ~0.1 blocks

    // Target tracking
    private LivingEntity currentTarget;
    private int ticksSinceTargetCheck = 0;

    // Ability loop control
    private boolean abilitiesRegistered = false;

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

        // Add slowness to reduce jitter - REMOVED: This was preventing movement!
        // Instead, we'll rely on proper sync logic

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

        // Initialize animation state machine
        this.stateMachine = new AnimationStateMachine(modelInstance);
        setupDefaultAnimations();

        // Initialize ability manager (but DON'T start loop yet)
        this.abilityManager = new AbilityManager(this);
    }

    /**
     * Setup default animation states
     */
    private void setupDefaultAnimations() {
        AnimationSystem.Animation idleAnim = new AnimationSystem.Animation("idle", modelData.getId());
        idleAnim.loop = true;
        idleAnim.duration = 40;

        AnimationSystem.Animation walkAnim = new AnimationSystem.Animation("walk", modelData.getId());
        walkAnim.loop = true;
        walkAnim.duration = 20;

        AnimationSystem.Animation attackAnim = new AnimationSystem.Animation("attack", modelData.getId());
        attackAnim.loop = false;
        attackAnim.duration = 30;

        stateMachine.registerAnimation(AnimationStateMachine.AnimationState.IDLE, idleAnim);
        stateMachine.registerAnimation(AnimationStateMachine.AnimationState.WALK, walkAnim);
        stateMachine.registerAnimation(AnimationStateMachine.AnimationState.ATTACK, attackAnim);

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
     * Start ability system (call AFTER registering abilities)
     */
    public void startAbilities() {
        if (!abilitiesRegistered) {
            abilityManager.startAbilityLoop(20);
            abilitiesRegistered = true;
        }
    }

    /**
     * Main tick method - FIXED VERSION
     */
    public void tick() {
        if (!isValid()) return;

        ticksLived++;
        Location coreLoc = coreEntity.getLocation();

        // FIXED: Sync hitbox to core with BOTH position and rotation
        hitbox.teleport(coreLoc);

        // Update target
        updateTarget();

        // Update animation state machine
        if (stateMachine != null) {
            stateMachine.autoUpdate(coreEntity);
            stateMachine.tick();
        }

        // FIXED: Better movement detection and model sync
        boolean hasMoved = hasCoreMoved(coreLoc);

        if (hasMoved) {
            // Update model root location FIRST
            modelInstance.setRootLocation(coreLoc);

            // Then calculate distance to determine interpolation speed
            double distance = coreLoc.distance(lastLocation);

            if (distance > 5.0) {
                // Teleport - use instant update
                modelInstance.updateInstant();
            } else if (distance > 1.0) {
                // Fast movement
                modelInstance.setInterpolationMode(SmoothTransformSystem.InterpolationMode.FAST);
                modelInstance.update();
            } else {
                // Normal movement
                modelInstance.setInterpolationMode(SmoothTransformSystem.InterpolationMode.NORMAL);
                modelInstance.update();
            }

            lastLocation = coreLoc.clone();
            ticksSinceLastMove = 0;
        } else {
            ticksSinceLastMove++;

            // Still update occasionally for animations (every 5 ticks)
            if (ticksSinceLastMove % 5 == 0) {
                modelInstance.update();
            }
        }
    }

    /**
     * Check if core entity has moved significantly
     */
    private boolean hasCoreMoved(Location currentLoc) {
        if (!currentLoc.getWorld().equals(lastLocation.getWorld())) {
            return true; // World change
        }

        // Check distance squared for efficiency
        double distSq = currentLoc.distanceSquared(lastLocation);
        return distSq > MOVEMENT_THRESHOLD;
    }

    /**
     * Find and track nearest player as target
     */
    private void updateTarget() {
        ticksSinceTargetCheck++;

        if (ticksSinceTargetCheck < 20) return;
        ticksSinceTargetCheck = 0;

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