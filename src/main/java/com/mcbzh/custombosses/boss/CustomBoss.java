package com.mcbzh.custombosses.boss;

import com.mcbzh.custombosses.model.ModelData;
import com.mcbzh.custombosses.model.ModelInstance;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class CustomBoss {

    private final UUID uuid;
    private final ModelData modelData;
    private final ModelInstance modelInstance;
    private final LivingEntity coreEntity;
    private final Interaction hitbox;

    public CustomBoss(ModelData data, Location location) {
        this.uuid = UUID.randomUUID();
        this.modelData = data;

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
        modelInstance.spawn();
    }

    public void tick() {
        if (!isValid()) return;

        Location coreLoc = coreEntity.getLocation();

        // Sync hitbox to core
        hitbox.teleport(coreLoc);

        // Sync model to core
        modelInstance.setRootLocation(coreLoc);
        modelInstance.update();
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
}
