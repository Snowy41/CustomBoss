package com.mcbzh.custombosses.model;

import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;

public class ModelInstance {
    private final ModelData data;
    private final Location rootLocation;
    private final Map<String, ModelPart> parts = new HashMap<>();

    public ModelInstance(ModelData data, Location location) {
        this.data = data;
        this.rootLocation = location;
    }

    public Location getRootLocation() {
        return rootLocation;
    }

    public void spawn() {
        for (ModelPartData partData : data.getParts()) {
            ModelPart part = new ModelPart(partData, this);
            parts.put(partData.getId(), part);
            part.spawn(rootLocation);
        }
        update();
    }

    public void update() {
        // Update all parts
        // In a real bone system, we need to update parents first, then children.
        // For now, just update all.
        for (ModelPart part : parts.values()) {
            part.updateTransform(rootLocation);
        }
    }

    public void tick() {
        update();
    }

    public void despawn() {
        for (ModelPart part : parts.values()) {
            part.despawn();
        }
        parts.clear();
    }

    public void hurt() {
        // TODO: Flash red
    }

    public ModelPart getPart(String id) {
        return parts.get(id);
    }

    public Map<String, ModelPart> getParts() {
        return parts;
    }
}
