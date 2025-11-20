package com.mcbzh.custombosses.model;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.animation.SmoothTransformSystem;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * Enhanced ModelInstance with smooth interpolation support
 * Now uses SmoothTransformSystem for buttery 60fps animations
 */
public class ModelInstance {

    private final ModelData data;
    private Location rootLocation;
    private final Map<String, Part> parts;

    // Performance optimization
    private boolean dirty = true;
    private long lastUpdateTick = 0;

    // Display settings
    private boolean fullBrightness = true;
    private float viewRange = 64.0f;

    // Interpolation mode - CRITICAL for editor vs live boss
    private SmoothTransformSystem.InterpolationMode interpolationMode =
            SmoothTransformSystem.InterpolationMode.NORMAL;
    private boolean isInEditorMode = false; // When true, use INSTANT interpolation

    // Persistent keys
    private static NamespacedKey PART_ID_KEY;
    private static NamespacedKey MODEL_ID_KEY;

    static {
        CustomBossesPlugin plugin = CustomBossesPlugin.getInstance();
        if (plugin != null) {
            PART_ID_KEY = new NamespacedKey(plugin, "part_id");
            MODEL_ID_KEY = new NamespacedKey(plugin, "model_id");
        }
    }

    public ModelInstance(ModelData data, Location location) {
        this.data = data;
        this.rootLocation = location.clone();
        this.parts = new LinkedHashMap<>();
    }

    /**
     * Enable editor mode - disables interpolation for instant feedback
     */
    public void setEditorMode(boolean enabled) {
        this.isInEditorMode = enabled;
        if (enabled) {
            // Force instant updates in editor
            this.interpolationMode = SmoothTransformSystem.InterpolationMode.INSTANT;
        } else {
            // Restore smooth interpolation for live bosses
            this.interpolationMode = SmoothTransformSystem.InterpolationMode.NORMAL;
        }
    }

    public void spawn() {
        List<ModelData.PartData> ordered = getHierarchyOrder();

        for (ModelData.PartData partData : ordered) {
            Part part = new Part(partData);
            part.spawn(rootLocation);
            parts.put(partData.id, part);
        }

        markDirty();
        update();
    }

    public void despawn() {
        parts.values().forEach(Part::despawn);
        parts.clear();
    }

    /**
     * Mark that this instance needs updating
     */
    public void markDirty() {
        this.dirty = true;
    }

    /**
     * Update with smooth interpolation
     * Uses INSTANT mode in editor, SMOOTH mode for live bosses
     */
    public void update() {
        if (!dirty) return;

        // Check chunk loaded
        if (!isChunkLoaded(rootLocation)) return;

        // Use smooth transform system
        int interpolationTicks = isInEditorMode ? 0 : interpolationMode.ticks;
        SmoothTransformSystem.updateModelSmooth(this, interpolationTicks);

        dirty = false;
        lastUpdateTick = System.currentTimeMillis();
    }

    /**
     * Force instant update (useful for teleporting)
     */
    public void updateInstant() {
        boolean wasInEditor = isInEditorMode;
        setEditorMode(true);
        update();
        setEditorMode(wasInEditor);
    }

    /**
     * Batch update system for animations
     * Applies multiple part transforms at once for synchronized movement
     */
    public SmoothTransformSystem.BatchTransformUpdate beginBatchUpdate() {
        SmoothTransformSystem.BatchTransformUpdate batch =
                new SmoothTransformSystem.BatchTransformUpdate(this);

        if (isInEditorMode) {
            batch.setInterpolation(SmoothTransformSystem.InterpolationMode.INSTANT);
        } else {
            batch.setInterpolation(interpolationMode);
        }

        return batch;
    }

    public void setRootLocation(Location location) {
        if (!this.rootLocation.equals(location)) {
            this.rootLocation = location.clone();
            markDirty();
        }
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

    /**
     * Get parts in hierarchy order (parents before children)
     * Used by SmoothTransformSystem
     */
    public List<Part> getHierarchyOrderedParts() {
        List<ModelData.PartData> orderedData = getHierarchyOrder();
        List<Part> orderedParts = new ArrayList<>();

        for (ModelData.PartData data : orderedData) {
            Part part = parts.get(data.id);
            if (part != null) {
                orderedParts.add(part);
            }
        }

        return orderedParts;
    }

    /**
     * Set interpolation mode for animations
     */
    public void setInterpolationMode(SmoothTransformSystem.InterpolationMode mode) {
        if (isInEditorMode) return; // Don't override editor mode
        this.interpolationMode = mode;
    }

    /**
     * Set view range for all parts
     */
    public void setViewRange(float range) {
        this.viewRange = range;
        parts.values().forEach(part -> {
            if (part.entity != null) {
                part.entity.setViewRange(range);
            }
        });
    }

    /**
     * Toggle full brightness
     */
    public void setFullBrightness(boolean enabled) {
        this.fullBrightness = enabled;
        parts.values().forEach(part -> {
            if (part.entity != null) {
                if (enabled) {
                    part.entity.setBrightness(new Display.Brightness(15, 15));
                } else {
                    part.entity.setBrightness(new Display.Brightness(0, 0));
                }
            }
        });
    }

    /**
     * Raycast to select a part - much more accurate than distance checking
     */
    public Part raycastPart(Location eyeLocation, Vector direction, double maxDistance) {
        Part closestPart = null;
        double closestDistance = maxDistance;

        for (Part part : parts.values()) {
            if (part.entity == null || !part.entity.isValid()) continue;

            Location partLoc = part.entity.getLocation();

            // Create bounding box for part (approximate)
            Vector3f scale = part.data.scale.toVector3f();
            BoundingBox box = new BoundingBox(
                    partLoc.getX() - scale.x() / 2,
                    partLoc.getY() - scale.y() / 2,
                    partLoc.getZ() - scale.z() / 2,
                    partLoc.getX() + scale.x() / 2,
                    partLoc.getY() + scale.y() / 2,
                    partLoc.getZ() + scale.z() / 2
            );

            RayTraceResult result = box.rayTrace(eyeLocation.toVector(), direction, maxDistance);
            if (result != null) {
                double distance = eyeLocation.distance(result.getHitPosition().toLocation(eyeLocation.getWorld()));
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPart = part;
                }
            }
        }

        return closestPart;
    }

    /**
     * Get part by entity
     */
    public Part getPartByEntity(BlockDisplay entity) {
        if (entity == null) return null;

        // Try persistent data first
        if (PART_ID_KEY != null) {
            String partId = entity.getPersistentDataContainer().get(PART_ID_KEY, PersistentDataType.STRING);
            if (partId != null) {
                return parts.get(partId);
            }
        }

        // Fallback to iteration
        for (Part part : parts.values()) {
            if (part.entity != null && part.entity.equals(entity)) {
                return part;
            }
        }

        return null;
    }

    private List<ModelData.PartData> getHierarchyOrder() {
        List<ModelData.PartData> ordered = new ArrayList<>();
        Set<String> added = new HashSet<>();
        Queue<ModelData.PartData> queue = new LinkedList<>(data.getParts());

        int maxIterations = queue.size() * 2; // Prevent infinite loops
        int iterations = 0;

        while (!queue.isEmpty() && iterations < maxIterations) {
            ModelData.PartData part = queue.poll();
            iterations++;

            if (part.parentId == null || added.contains(part.parentId)) {
                ordered.add(part);
                added.add(part.id);
            } else {
                queue.offer(part);
            }
        }

        return ordered;
    }

    private boolean isChunkLoaded(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        return loc.getWorld().isChunkLoaded(chunkX, chunkZ);
    }

    /**
     * Enhanced Part class with smooth interpolation support
     */
    public class Part {
        private final ModelData.PartData data;
        private BlockDisplay entity;
        private final Matrix4f globalMatrix;

        // Visual state
        private boolean selected = false;
        private boolean hidden = false;

        public Part(ModelData.PartData data) {
            this.data = data;
            this.globalMatrix = new Matrix4f();
        }

        public void spawn(Location root) {
            if (!isChunkLoaded(root)) return;

            entity = (BlockDisplay) root.getWorld().spawnEntity(root, EntityType.BLOCK_DISPLAY);
            entity.setBlock(data.material.createBlockData());

            // Enhanced display properties
            // Start with 0 interpolation, will be set during updates
            entity.setInterpolationDuration(0);
            entity.setTeleportDuration(1);
            entity.setViewRange(viewRange);

            if (fullBrightness) {
                entity.setBrightness(new Display.Brightness(15, 15));
            }

            // Culling optimization
            entity.setDisplayWidth(2.0f);
            entity.setDisplayHeight(2.0f);

            // Remove shadow for floating models
            entity.setShadowRadius(0.0f);
            entity.setShadowStrength(0.0f);

            // Store metadata
            if (PART_ID_KEY != null && MODEL_ID_KEY != null) {
                entity.getPersistentDataContainer().set(PART_ID_KEY, PersistentDataType.STRING, data.id);
                entity.getPersistentDataContainer().set(MODEL_ID_KEY, PersistentDataType.STRING, ModelInstance.this.data.getId());
            }
        }

        public void despawn() {
            if (entity != null) {
                entity.remove();
                entity = null;
            }
        }

        public void setSelected(boolean selected) {
            if (this.selected == selected) return;
            this.selected = selected;

            if (entity != null && entity.isValid()) {
                entity.setGlowing(selected);
                if (selected) {
                    entity.setGlowColorOverride(Color.YELLOW);
                }
            }
        }

        public boolean isSelected() {
            return selected;
        }

        public void setHidden(boolean hidden) {
            this.hidden = hidden;
            if (entity != null && entity.isValid()) {
                // Note: Can't actually hide BlockDisplay, but can make transparent
                // Would need to store/restore material
            }
        }

        public boolean isHidden() {
            return hidden;
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

        public Matrix4f getGlobalMatrix() {
            return new Matrix4f(globalMatrix);
        }
    }
}