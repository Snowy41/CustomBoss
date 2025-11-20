package com.mcbzh.custombosses.animation;

import com.mcbzh.custombosses.model.ModelInstance;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * Smooth interpolation system for BlockDisplay entities
 * Uses BlockDisplay's built-in interpolation for 60fps smooth movement
 *
 * CRITICAL: This system is DISABLED in editor mode to allow instant feedback
 * when moving parts. Only used for live boss animations.
 */
public class SmoothTransformSystem {

    private static final int DEFAULT_INTERPOLATION_TICKS = 2; // ~100ms at 20 TPS
    private static final int EDITOR_INTERPOLATION_TICKS = 0; // Instant in editor

    /**
     * Smoothly update a part's transformation
     * Uses BlockDisplay's native interpolation for butter-smooth movement
     *
     * @param part The part to update
     * @param targetTransform The desired transformation
     * @param durationTicks How many ticks to interpolate over (0 = instant)
     */
    public static void applySmooth(ModelInstance.Part part,
                                   Transformation targetTransform,
                                   int durationTicks) {
        if (part.getEntity() == null || !part.getEntity().isValid()) return;

        // Set interpolation duration BEFORE changing transformation
        // This tells the client to smoothly transition
        part.getEntity().setInterpolationDuration(durationTicks);
        part.getEntity().setInterpolationDelay(0); // Start immediately

        // Now set the target transformation
        part.getEntity().setTransformation(targetTransform);
    }

    /**
     * Update an entire model instance with smooth interpolation
     *
     * @param instance The model to update
     * @param durationTicks Interpolation duration (0 for instant, useful in editor)
     */
    public static void updateModelSmooth(ModelInstance instance, int durationTicks) {
        // First pass: calculate all transformations
        Map<String, Transformation> transforms = new HashMap<>();
        List<ModelInstance.Part> ordered = instance.getHierarchyOrderedParts();

        for (ModelInstance.Part part : ordered) {
            Transformation transform = calculateGlobalTransform(part, instance);
            transforms.put(part.getId(), transform);
        }

        // Second pass: apply all at once for synchronized movement
        for (ModelInstance.Part part : ordered) {
            Transformation transform = transforms.get(part.getId());
            if (transform != null) {
                applySmooth(part, transform, durationTicks);
            }
        }
    }

    /**
     * Calculate global transformation for a part (includes parent hierarchy)
     */
    private static Transformation calculateGlobalTransform(ModelInstance.Part part, ModelInstance instance) {
        org.joml.Matrix4f matrix = new org.joml.Matrix4f();

        // Build parent chain
        List<ModelInstance.Part> chain = new ArrayList<>();
        ModelInstance.Part current = part;
        while (current != null) {
            chain.add(0, current); // Add to front
            String parentId = current.getData().parentId;
            current = parentId != null ? instance.getParts().get(parentId) : null;
        }

        // Apply transforms in order
        for (ModelInstance.Part p : chain) {
            org.bukkit.util.Vector pos = p.getData().position;
            org.bukkit.util.Vector rot = p.getData().rotation;
            org.bukkit.util.Vector scale = p.getData().scale;

            matrix.translate(
                    (float) pos.getX(),
                    (float) pos.getY(),
                    (float) pos.getZ()
            );

            matrix.rotateXYZ(
                    (float) Math.toRadians(rot.getX()),
                    (float) Math.toRadians(rot.getY()),
                    (float) Math.toRadians(rot.getZ())
            );

            matrix.scale(
                    (float) scale.getX(),
                    (float) scale.getY(),
                    (float) scale.getZ()
            );
        }

        // Extract components
        Vector3f translation = matrix.getTranslation(new Vector3f());
        Quaternionf rotation = matrix.getUnnormalizedRotation(new Quaternionf());
        Vector3f scale = matrix.getScale(new Vector3f());

        return new Transformation(translation, rotation, scale, new Quaternionf());
    }

    /**
     * Interpolation mode for different contexts
     */
    public enum InterpolationMode {
        INSTANT(0),           // Editor mode - no delay
        FAST(1),              // Quick animations
        NORMAL(2),            // Default for bosses
        SMOOTH(3),            // Very smooth, slightly delayed
        VERY_SMOOTH(5);       // Maximum smoothness

        public final int ticks;

        InterpolationMode(int ticks) {
            this.ticks = ticks;
        }
    }

    /**
     * Adaptive interpolation based on distance moved
     * Larger movements get longer interpolation for smoothness
     */
    public static int calculateAdaptiveDuration(Vector3f oldPos, Vector3f newPos,
                                                InterpolationMode baseMode) {
        float distance = oldPos.distance(newPos);

        if (distance < 0.01f) return 0; // Too small to notice
        if (distance < 0.1f) return baseMode.ticks;
        if (distance < 0.5f) return baseMode.ticks + 1;
        return baseMode.ticks + 2; // Cap at +2 ticks even for large movements
    }

    /**
     * Batch update system for animations
     * Applies all part transforms at the same time for synchronized movement
     */
    public static class BatchTransformUpdate {
        private final ModelInstance instance;
        private final Map<String, PartTransform> pendingUpdates = new HashMap<>();
        private int interpolationTicks = DEFAULT_INTERPOLATION_TICKS;

        public BatchTransformUpdate(ModelInstance instance) {
            this.instance = instance;
        }

        public void setInterpolation(int ticks) {
            this.interpolationTicks = ticks;
        }

        public void setInterpolation(InterpolationMode mode) {
            this.interpolationTicks = mode.ticks;
        }

        /**
         * Queue a part transformation
         */
        public void updatePart(String partId, Vector position, Vector rotation, Vector scale) {
            pendingUpdates.put(partId, new PartTransform(position, rotation, scale));
        }

        /**
         * Apply all queued transforms at once
         */
        public void apply() {
            if (pendingUpdates.isEmpty()) return;

            // Update data
            for (Map.Entry<String, PartTransform> entry : pendingUpdates.entrySet()) {
                ModelInstance.Part part = instance.getParts().get(entry.getKey());
                if (part == null) continue;

                PartTransform transform = entry.getValue();
                part.getData().position = transform.position.clone();
                part.getData().rotation = transform.rotation.clone();
                part.getData().scale = transform.scale.clone();
            }

            // Apply visually with smooth interpolation
            updateModelSmooth(instance, interpolationTicks);

            pendingUpdates.clear();
        }

        /**
         * Apply immediately without clearing (for previews)
         */
        public void preview() {
            for (Map.Entry<String, PartTransform> entry : pendingUpdates.entrySet()) {
                ModelInstance.Part part = instance.getParts().get(entry.getKey());
                if (part == null) continue;

                PartTransform transform = entry.getValue();
                part.getData().position = transform.position.clone();
                part.getData().rotation = transform.rotation.clone();
                part.getData().scale = transform.scale.clone();
            }

            updateModelSmooth(instance, interpolationTicks);
        }

        private static class PartTransform {
            Vector position, rotation, scale;

            PartTransform(Vector pos, Vector rot, Vector scale) {
                this.position = pos;
                this.rotation = rot;
                this.scale = scale;
            }
        }
    }

    /**
     * Easing functions for custom interpolation curves
     * (BlockDisplay uses linear by default, but we can calculate custom curves)
     */
    public static class Easing {
        public static float linear(float t) {
            return t;
        }

        public static float easeInQuad(float t) {
            return t * t;
        }

        public static float easeOutQuad(float t) {
            return t * (2 - t);
        }

        public static float easeInOutQuad(float t) {
            return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
        }

        public static float easeInCubic(float t) {
            return t * t * t;
        }

        public static float easeOutCubic(float t) {
            float f = t - 1;
            return f * f * f + 1;
        }

        public static float easeInOutCubic(float t) {
            return t < 0.5f ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;
        }

        public static float easeInElastic(float t) {
            if (t == 0 || t == 1) return t;
            return (float) (-Math.pow(2, 10 * (t - 1)) * Math.sin((t - 1.1) * 5 * Math.PI));
        }

        public static float easeOutElastic(float t) {
            if (t == 0 || t == 1) return t;
            return (float) (Math.pow(2, -10 * t) * Math.sin((t - 0.1) * 5 * Math.PI) + 1);
        }

        public static float easeInBounce(float t) {
            return 1 - easeOutBounce(1 - t);
        }

        public static float easeOutBounce(float t) {
            if (t < 1 / 2.75f) {
                return 7.5625f * t * t;
            } else if (t < 2 / 2.75f) {
                t -= 1.5f / 2.75f;
                return 7.5625f * t * t + 0.75f;
            } else if (t < 2.5f / 2.75f) {
                t -= 2.25f / 2.75f;
                return 7.5625f * t * t + 0.9375f;
            } else {
                t -= 2.625f / 2.75f;
                return 7.5625f * t * t + 0.984375f;
            }
        }
    }

    /**
     * Spring physics for natural movement
     * Useful for things like head bobbing, swaying
     */
    public static class SpringMotion {
        private float position = 0;
        private float velocity = 0;
        private float target = 0;

        // Spring properties
        private float stiffness = 100.0f;  // How strong the spring is
        private float damping = 10.0f;     // How much it resists motion
        private float mass = 1.0f;

        public SpringMotion() {}

        public SpringMotion(float stiffness, float damping) {
            this.stiffness = stiffness;
            this.damping = damping;
        }

        public void setTarget(float target) {
            this.target = target;
        }

        public void update(float deltaTime) {
            // F = -kx - bv (Hooke's law with damping)
            float force = -stiffness * (position - target) - damping * velocity;
            float acceleration = force / mass;

            velocity += acceleration * deltaTime;
            position += velocity * deltaTime;
        }

        public float getPosition() {
            return position;
        }

        public void reset(float position) {
            this.position = position;
            this.velocity = 0;
        }

        public boolean isAtRest() {
            return Math.abs(velocity) < 0.001f && Math.abs(position - target) < 0.001f;
        }
    }
}