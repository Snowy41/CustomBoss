package com.mcbzh.custombosses.abilities;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.boss.CustomBoss;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Ability Manager - handles ability execution and cooldowns for a boss
 */
public class AbilityManager {

    private final CustomBoss boss;
    private final List<BossAbility> abilities;
    private final Random random;
    private BukkitTask abilityCheckTask;

    public AbilityManager(CustomBoss boss) {
        this.boss = boss;
        this.abilities = new ArrayList<>();
        this.random = new Random();
    }

    /**
     * Register an ability for this boss
     */
    public void registerAbility(BossAbility ability) {
        abilities.add(ability);
    }

    /**
     * Start automatically checking for ability usage
     * @param checkInterval How often to check (in ticks)
     */
    public void startAbilityLoop(int checkInterval) {
        if (abilityCheckTask != null) {
            abilityCheckTask.cancel();
        }

        abilityCheckTask = Bukkit.getScheduler().runTaskTimer(
                CustomBossesPlugin.getInstance(),
                this::checkAbilities,
                checkInterval,
                checkInterval
        );
    }

    /**
     * Check if boss should use an ability
     */
    private void checkAbilities() {
        if (!boss.isValid()) {
            stop();
            return;
        }

        // Find target
        LivingEntity target = boss.getTarget();
        if (target == null || !target.isValid()) return;

        // Get usable abilities
        List<BossAbility> usable = new ArrayList<>();
        for (BossAbility ability : abilities) {
            if (ability.canUse(boss, target)) {
                usable.add(ability);
            }
        }

        // Use random ability if any available
        if (!usable.isEmpty()) {
            BossAbility chosen = usable.get(random.nextInt(usable.size()));
            chosen.use(boss, target);
        }
    }

    /**
     * Force use an ability by ID
     */
    public boolean useAbility(String abilityId, LivingEntity target) {
        for (BossAbility ability : abilities) {
            if (ability.getId().equals(abilityId)) {
                if (ability.canUse(boss, target)) {
                    ability.use(boss, target);
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    /**
     * Get an ability by ID
     */
    public BossAbility getAbility(String id) {
        for (BossAbility ability : abilities) {
            if (ability.getId().equals(id)) {
                return ability;
            }
        }
        return null;
    }

    /**
     * Stop ability checking
     */
    public void stop() {
        if (abilityCheckTask != null) {
            abilityCheckTask.cancel();
            abilityCheckTask = null;
        }
    }

    /**
     * Get all registered abilities
     */
    public List<BossAbility> getAbilities() {
        return new ArrayList<>(abilities);
    }

    /**
     * Remove an ability
     */
    public boolean removeAbility(String id) {
        return abilities.removeIf(a -> a.getId().equals(id));
    }

    /**
     * Clear all abilities
     */
    public void clearAbilities() {
        abilities.clear();
    }
}