package com.mcbzh.custombosses.abilities;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.animation.AnimationStateMachine;
import com.mcbzh.custombosses.boss.CustomBoss;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Boss Ability System - allows bosses to perform special attacks
 * Abilities are triggered by AI logic and play animations
 */
public abstract class BossAbility {

    protected final String id;
    protected final String name;
    protected int cooldownTicks;
    protected int lastUsedTick = -9999;
    protected String animationStateId;

    public BossAbility(String id, String name, int cooldownTicks) {
        this.id = id;
        this.name = name;
        this.cooldownTicks = cooldownTicks;
    }

    /**
     * Check if this ability can be used right now
     */
    public boolean canUse(CustomBoss boss, LivingEntity target) {
        // Check cooldown
        int currentTick = boss.getTicksLived();
        if (currentTick - lastUsedTick < cooldownTicks) {
            return false;
        }

        // Check custom conditions
        return checkConditions(boss, target);
    }

    /**
     * Custom conditions for this specific ability
     */
    protected abstract boolean checkConditions(CustomBoss boss, LivingEntity target);

    /**
     * Execute the ability
     */
    public void use(CustomBoss boss, LivingEntity target) {
        lastUsedTick = boss.getTicksLived();

        // Play animation if specified
        if (animationStateId != null && boss.getStateMachine() != null) {
            try {
                AnimationStateMachine.AnimationState state =
                        AnimationStateMachine.AnimationState.valueOf(animationStateId.toUpperCase());
                boss.getStateMachine().playOneShot(state, () -> {
                    onAnimationComplete(boss, target);
                });
            } catch (IllegalArgumentException e) {
                // Animation state doesn't exist, just execute immediately
                execute(boss, target);
            }
        } else {
            // No animation, execute immediately
            execute(boss, target);
        }
    }

    /**
     * The actual ability logic
     */
    protected abstract void execute(CustomBoss boss, LivingEntity target);

    /**
     * Called when animation completes
     */
    protected void onAnimationComplete(CustomBoss boss, LivingEntity target) {
        // Override if needed
    }

    /**
     * Get cooldown progress (0.0 = ready, 1.0 = just used)
     */
    public float getCooldownProgress(CustomBoss boss) {
        int currentTick = boss.getTicksLived();
        int elapsed = currentTick - lastUsedTick;
        if (elapsed >= cooldownTicks) return 0.0f;
        return 1.0f - ((float) elapsed / cooldownTicks);
    }

    /**
     * Get remaining cooldown in ticks
     */
    public int getRemainingCooldown(CustomBoss boss) {
        int currentTick = boss.getTicksLived();
        int remaining = cooldownTicks - (currentTick - lastUsedTick);
        return Math.max(0, remaining);
    }

    public void setAnimationState(String stateId) {
        this.animationStateId = stateId;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getCooldownTicks() { return cooldownTicks; }
}

