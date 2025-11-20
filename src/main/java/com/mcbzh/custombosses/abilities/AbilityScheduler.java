package com.mcbzh.custombosses.abilities;

import com.mcbzh.custombosses.CustomBossesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask; /**
 * Helper class for scheduled ability effects
 */
public class AbilityScheduler {

    /**
     * Schedule an effect after a delay
     */
    public static void scheduleEffect(Runnable effect, int delayTicks) {
        Bukkit.getScheduler().runTaskLater(
                CustomBossesPlugin.getInstance(),
                effect,
                delayTicks
        );
    }

    /**
     * Schedule repeating effect
     */
    public static BukkitTask scheduleRepeating(Runnable effect, int intervalTicks, int totalTicks) {
        final int[] ticksRemaining = {totalTicks};

        return Bukkit.getScheduler().runTaskTimer(
                CustomBossesPlugin.getInstance(),
                () -> {
                    if (ticksRemaining[0] <= 0) {
                        return;
                    }
                    effect.run();
                    ticksRemaining[0] -= intervalTicks;
                },
                0L,
                intervalTicks
        );
    }
}
