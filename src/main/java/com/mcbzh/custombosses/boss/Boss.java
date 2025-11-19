package com.mcbzh.custombosses.boss;

import org.bukkit.entity.LivingEntity;

public interface Boss {
    void tick();

    void despawn();

    boolean isValid();

    LivingEntity getCore();

    void damage(double amount);
}
