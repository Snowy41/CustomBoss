package com.mcbzh.custombosses.boss;

import com.mcbzh.custombosses.model.ModelData;
import com.mcbzh.custombosses.model.ModelInstance;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class CustomBoss implements Boss {

    private final LivingEntity core;
    private final Interaction interaction;
    private final ModelInstance model;
    private final ModelData modelData;

    public CustomBoss(ModelData data, Location location) {
        this.modelData = data;

        // 1. Spawn Core (Invisible Zombie)
        this.core = (LivingEntity) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        this.core.setInvisible(true);
        this.core.setSilent(true);
        this.core.setAI(true); // Enable AI for pathfinding

        if (core instanceof Zombie zombie) {
            zombie.setAdult();
            zombie.setInvulnerable(true);
        }

        // Set custom health if specified
        if (core.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            core.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100.0);
            core.setHealth(100.0);
        }

        // Add slowness to make it less jittery
        core.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));

        // 2. Spawn Interaction (Hitbox)
        this.interaction = (Interaction) location.getWorld().spawnEntity(location, EntityType.INTERACTION);
        Vector size = data.getHitboxSize();
        this.interaction.setInteractionWidth((float) size.getX());
        this.interaction.setInteractionHeight((float) size.getY());
        this.interaction.setResponsive(true);

        // 3. Create Model
        this.model = new ModelInstance(data, location);
        this.model.spawn();
    }

    @Override
    public void tick() {
        if (!isValid()) {
            return;
        }

        // Sync Interaction to Core
        Location coreLoc = core.getLocation();
        interaction.teleport(coreLoc);

        // Update Model to follow core
        model.tick();

        // Teleport model root to core location
        for (var part : model.getParts().values()) {
            // Parts are updated via ModelInstance.update() which uses rootLocation
            // We need to update the root location
        }

        // Update model root location
        Location newRoot = core.getLocation();
        model.updateRootLocation(newRoot);
    }

    @Override
    public void despawn() {
        if (core != null && core.isValid()) {
            core.remove();
        }
        if (interaction != null && interaction.isValid()) {
            interaction.remove();
        }
        if (model != null) {
            model.despawn();
        }
    }

    @Override
    public boolean isValid() {
        return core != null && core.isValid() &&
                interaction != null && interaction.isValid();
    }

    @Override
    public LivingEntity getCore() {
        return core;
    }

    public Interaction getInteraction() {
        return interaction;
    }

    @Override
    public void damage(double amount) {
        if (core != null && core.isValid()) {
            core.damage(amount);
            model.hurt();
        }
    }

    public ModelInstance getModel() {
        return model;
    }
}