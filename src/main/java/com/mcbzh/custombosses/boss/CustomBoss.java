package com.mcbzh.custombosses.boss;

import com.mcbzh.custombosses.model.ModelData;
import com.mcbzh.custombosses.model.ModelInstance;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.util.Vector;

public class CustomBoss implements Boss {

    private final LivingEntity core;
    private final Interaction interaction;
    private final ModelInstance model;

    public CustomBoss(ModelData data, Location location) {

        // 1. Spawn Core (Invisible Zombie)
        this.core = (LivingEntity) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        this.core.setInvisible(true);
        this.core.setSilent(true);
        // TODO: Disable AI or set specific AI goals later
        if (core instanceof Zombie) {
            // ((Zombie) core).setShouldBurnInDay(false); // Deprecated/Removed in newer
            // versions
            ((Zombie) core).setAdult();
        }

        // 2. Spawn Interaction (Hitbox)
        this.interaction = (Interaction) location.getWorld().spawnEntity(location, EntityType.INTERACTION);
        Vector size = data.getHitboxSize();
        this.interaction.setInteractionWidth((float) size.getX());
        this.interaction.setInteractionHeight((float) size.getY());

        // 3. Create Model
        this.model = new ModelInstance(data, location);
    }

    @Override
    public void tick() {
        if (!isValid())
            return;

        // Sync Interaction to Core
        interaction.teleport(core.getLocation());

        // Update Model
        model.tick();
        // TODO: Teleport model to core (handled inside ModelInstance usually, or here)
    }

    @Override
    public void despawn() {
        if (core != null)
            core.remove();
        if (interaction != null)
            interaction.remove();
        if (model != null)
            model.despawn();
    }

    @Override
    public boolean isValid() {
        return core != null && core.isValid() && interaction != null && interaction.isValid();
    }

    @Override
    public LivingEntity getCore() {
        return core;
    }

    @Override
    public void damage(double amount) {
        core.damage(amount);
        model.hurt();
    }
}
