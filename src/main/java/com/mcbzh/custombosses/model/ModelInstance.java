package com.mcbzh.custombosses.model;

import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class ModelInstance {
    private final ModelData data;
    private Location rootLocation;
    private final Map<String, ModelPart> parts = new HashMap<>();
    private int hurtTicks = 0;

    public ModelInstance(ModelData data, Location location) {
        this.data = data;
        this.rootLocation = location.clone();
    }

    public Location getRootLocation() {
        return rootLocation;
    }

    public void updateRootLocation(Location newLocation) {
        this.rootLocation = newLocation.clone();
    }

    public void spawn() {
        // Build parts in hierarchy order (parents first)
        List<ModelPartData> orderedParts = buildHierarchyOrder();

        for (ModelPartData partData : orderedParts) {
            ModelPart part = new ModelPart(partData, this);
            parts.put(partData.getId(), part);
            part.spawn(rootLocation);
        }
        update();
    }

    /**
     * Orders parts so parents are created before children
     */
    private List<ModelPartData> buildHierarchyOrder() {
        List<ModelPartData> ordered = new ArrayList<>();
        List<ModelPartData> remaining = new ArrayList<>(data.getParts());

        while (!remaining.isEmpty()) {
            boolean addedAny = false;

            for (int i = remaining.size() - 1; i >= 0; i--) {
                ModelPartData part = remaining.get(i);

                // Add if no parent or parent already added
                if (part.getParentId() == null ||
                        ordered.stream().anyMatch(p -> p.getId().equals(part.getParentId()))) {
                    ordered.add(part);
                    remaining.remove(i);
                    addedAny = true;
                }
            }

            // Prevent infinite loop if hierarchy is broken
            if (!addedAny && !remaining.isEmpty()) {
                System.err.println("[CustomBosses] Warning: Broken hierarchy detected in model " + data.getId());
                ordered.addAll(remaining);
                break;
            }
        }

        return ordered;
    }

    public void update() {
        // Update all parts in hierarchy order
        List<ModelPartData> orderedParts = buildHierarchyOrder();

        for (ModelPartData partData : orderedParts) {
            ModelPart part = parts.get(partData.getId());
            if (part != null) {
                part.updateTransform(rootLocation);
            }
        }
    }

    public void tick() {
        if (hurtTicks > 0) {
            hurtTicks--;
        }
        update();
    }

    public void despawn() {
        for (ModelPart part : parts.values()) {
            part.despawn();
        }
        parts.clear();
    }

    public void hurt() {
        hurtTicks = 10; // Red flash for 10 ticks
        // Make all parts glow red temporarily
        for (ModelPart part : parts.values()) {
            if (part.getEntity() != null) {
                part.getEntity().setGlowing(true);
                // Schedule to remove glow
                org.bukkit.Bukkit.getScheduler().runTaskLater(
                        com.mcbzh.custombosses.CustomBossesPlugin.getInstance(),
                        () -> {
                            if (part.getEntity() != null) {
                                part.getEntity().setGlowing(false);
                            }
                        }, 10L);
            }
        }
    }

    public ModelPart getPart(String id) {
        return parts.get(id);
    }

    public Map<String, ModelPart> getParts() {
        return parts;
    }

    public ModelData getData() {
        return data;
    }
}