package com.mcbzh.custombosses.model;

import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public class ModelInstance {

    private final ModelData data;
    private Location rootLocation;
    private final Map<String, Part> parts;

    public ModelInstance(ModelData data, Location location) {
        this.data = data;
        this.rootLocation = location.clone();
        this.parts = new LinkedHashMap<>();
    }

    public void spawn() {
        // Spawn in hierarchy order
        List<ModelData.PartData> ordered = getHierarchyOrder();

        for (ModelData.PartData partData : ordered) {
            Part part = new Part(partData);
            part.spawn(rootLocation);
            parts.put(partData.id, part);
        }

        update();
    }

    public void despawn() {
        parts.values().forEach(Part::despawn);
        parts.clear();
    }

    public void update() {
        List<ModelData.PartData> ordered = getHierarchyOrder();
        for (ModelData.PartData partData : ordered) {
            Part part = parts.get(partData.id);
            if (part != null) {
                part.update(rootLocation, parts);
            }
        }
    }

    public void setRootLocation(Location location) {
        this.rootLocation = location.clone();
    }

    public Location getRootLocation() {
        return rootLocation.clone();
    }

    public Map<String, Part> getParts() {
        return parts;
    }

    public ModelData getData() {
        return data;
    }

    private List<ModelData.PartData> getHierarchyOrder() {
        List<ModelData.PartData> ordered = new ArrayList<>();
        Set<String> added = new HashSet<>();
        Queue<ModelData.PartData> queue = new LinkedList<>(data.getParts());

        while (!queue.isEmpty()) {
            ModelData.PartData part = queue.poll();

            if (part.parentId == null || added.contains(part.parentId)) {
                ordered.add(part);
                added.add(part.id);
            } else {
                queue.offer(part);
            }
        }

        return ordered;
    }

    // Inner class representing a spawned part
    public static class Part {
        private final ModelData.PartData data;
        private BlockDisplay entity;
        private final Matrix4f globalMatrix;

        public Part(ModelData.PartData data) {
            this.data = data;
            this.globalMatrix = new Matrix4f();
        }

        public void spawn(Location root) {
            entity = (BlockDisplay) root.getWorld().spawnEntity(root, EntityType.BLOCK_DISPLAY);
            entity.setBlock(data.material.createBlockData());
        }

        public void despawn() {
            if (entity != null) {
                entity.remove();
                entity = null;
            }
        }

        public void update(Location root, Map<String, Part> allParts) {
            if (entity == null) return;

            // Calculate global matrix
            globalMatrix.identity();

            // Apply parent transform
            if (data.parentId != null) {
                Part parent = allParts.get(data.parentId);
                if (parent != null) {
                    globalMatrix.set(parent.globalMatrix);
                }
            }

            // Apply local transform
            globalMatrix.translate((float) data.position.getX(),
                    (float) data.position.getY(),
                    (float) data.position.getZ());

            globalMatrix.rotateXYZ(
                    (float) Math.toRadians(data.rotation.getX()),
                    (float) Math.toRadians(data.rotation.getY()),
                    (float) Math.toRadians(data.rotation.getZ())
            );

            globalMatrix.scale((float) data.scale.getX(),
                    (float) data.scale.getY(),
                    (float) data.scale.getZ());

            // Apply to entity
            entity.teleport(root);

            Vector3f translation = globalMatrix.getTranslation(new Vector3f());
            Quaternionf rotation = globalMatrix.getUnnormalizedRotation(new Quaternionf());
            Vector3f scale = globalMatrix.getScale(new Vector3f());

            entity.setTransformation(new Transformation(
                    translation, rotation, scale, new Quaternionf()
            ));
        }

        public BlockDisplay getEntity() {
            return entity;
        }

        public ModelData.PartData getData() {
            return data;
        }

        public String getId() {
            return data.id;
        }
    }
}
