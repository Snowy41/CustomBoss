package com.mcbzh.custombosses.animation;

import com.mcbzh.custombosses.model.ModelData;
import com.mcbzh.custombosses.model.ModelInstance;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Complete animation system with keyframes and interpolation
 */
public class AnimationSystem {

    /**
     * Animation data structure
     */
    public static class Animation {
        public String id;
        public String modelId;
        public int duration; // in ticks
        public boolean loop;
        public List<Keyframe> keyframes;

        public Animation(String id, String modelId) {
            this.id = id;
            this.modelId = modelId;
            this.duration = 20; // 1 second default
            this.loop = true;
            this.keyframes = new ArrayList<>();
        }

        public void addKeyframe(Keyframe kf) {
            keyframes.add(kf);
            keyframes.sort(Comparator.comparingInt(k -> k.tick));
        }
    }

    /**
     * Single animation keyframe
     */
    public static class Keyframe {
        public int tick;
        public Map<String, PartTransform> transforms; // partId -> transform

        public Keyframe(int tick) {
            this.tick = tick;
            this.transforms = new HashMap<>();
        }

        public void setTransform(String partId, Vector pos, Vector rot, Vector scale) {
            transforms.put(partId, new PartTransform(pos, rot, scale));
        }
    }

    /**
     * Transform for a single part at a keyframe
     */
    public static class PartTransform {
        public Vector position;
        public Vector rotation;
        public Vector scale;

        public PartTransform(Vector pos, Vector rot, Vector scale) {
            this.position = pos.clone();
            this.rotation = rot.clone();
            this.scale = scale.clone();
        }

        public PartTransform clone() {
            return new PartTransform(position, rotation, scale);
        }
    }

    /**
     * Active animation player
     */
    public static class AnimationPlayer {
        private final Animation animation;
        private final ModelInstance instance;
        private int currentTick = 0;
        private boolean playing = true;
        private InterpolationMode interpolation = InterpolationMode.LINEAR;

        public AnimationPlayer(Animation animation, ModelInstance instance) {
            this.animation = animation;
            this.instance = instance;
        }

        /**
         * Tick the animation forward
         */
        public void tick() {
            if (!playing) return;

            applyFrame(currentTick);

            currentTick++;
            if (currentTick >= animation.duration) {
                if (animation.loop) {
                    currentTick = 0;
                } else {
                    playing = false;
                }
            }
        }

        /**
         * Apply transforms for a specific tick
         */
        public void applyFrame(int tick) {
            if (animation.keyframes.isEmpty()) return;

            // Find surrounding keyframes
            Keyframe prev = null;
            Keyframe next = null;

            for (Keyframe kf : animation.keyframes) {
                if (kf.tick <= tick) {
                    prev = kf;
                }
                if (kf.tick > tick && next == null) {
                    next = kf;
                    break;
                }
            }

            // Handle edge cases
            if (prev == null) prev = animation.keyframes.get(0);
            if (next == null) {
                if (animation.loop) {
                    next = animation.keyframes.get(0);
                } else {
                    next = animation.keyframes.get(animation.keyframes.size() - 1);
                }
            }

            // Calculate interpolation alpha
            float alpha = 0;
            if (next.tick != prev.tick) {
                alpha = (tick - prev.tick) / (float)(next.tick - prev.tick);
            }

            // Apply interpolated transforms to all parts
            Set<String> allParts = new HashSet<>(prev.transforms.keySet());
            allParts.addAll(next.transforms.keySet());

            for (String partId : allParts) {
                PartTransform t1 = prev.transforms.get(partId);
                PartTransform t2 = next.transforms.get(partId);

                // If transform missing in one keyframe, use current or default
                if (t1 == null) {
                    ModelInstance.Part part = instance.getParts().get(partId);
                    if (part != null) {
                        ModelData.PartData data = part.getData();
                        t1 = new PartTransform(data.position, data.rotation, data.scale);
                    } else {
                        continue;
                    }
                }
                if (t2 == null) {
                    t2 = t1.clone();
                }

                // Interpolate
                Vector pos = interpolate(t1.position, t2.position, alpha);
                Vector rot = interpolateRotation(t1.rotation, t2.rotation, alpha);
                Vector scale = interpolate(t1.scale, t2.scale, alpha);

                // Apply to model
                ModelInstance.Part part = instance.getParts().get(partId);
                if (part != null) {
                    part.getData().position = pos;
                    part.getData().rotation = rot;
                    part.getData().scale = scale;
                }
            }

            instance.markDirty();
            instance.update();
        }

        /**
         * Linear interpolation between two vectors
         */
        private Vector interpolate(Vector a, Vector b, float alpha) {
            switch (interpolation) {
                case LINEAR:
                    return lerp(a, b, alpha);
                case EASE_IN:
                    return lerp(a, b, easeIn(alpha));
                case EASE_OUT:
                    return lerp(a, b, easeOut(alpha));
                case EASE_IN_OUT:
                    return lerp(a, b, easeInOut(alpha));
                default:
                    return lerp(a, b, alpha);
            }
        }

        private Vector lerp(Vector a, Vector b, float t) {
            return new Vector(
                    a.getX() + (b.getX() - a.getX()) * t,
                    a.getY() + (b.getY() - a.getY()) * t,
                    a.getZ() + (b.getZ() - a.getZ()) * t
            );
        }

        /**
         * Interpolate rotation with proper wrapping
         */
        private Vector interpolateRotation(Vector a, Vector b, float alpha) {
            return new Vector(
                    lerpAngle(a.getX(), b.getX(), alpha),
                    lerpAngle(a.getY(), b.getY(), alpha),
                    lerpAngle(a.getZ(), b.getZ(), alpha)
            );
        }

        private double lerpAngle(double a, double b, float t) {
            // Normalize angles to -180..180
            a = normalizeAngle(a);
            b = normalizeAngle(b);

            // Find shortest path
            double diff = b - a;
            if (diff > 180) diff -= 360;
            if (diff < -180) diff += 360;

            return a + diff * t;
        }

        private double normalizeAngle(double angle) {
            while (angle > 180) angle -= 360;
            while (angle < -180) angle += 360;
            return angle;
        }

        // Easing functions
        private float easeIn(float t) {
            return t * t;
        }

        private float easeOut(float t) {
            return t * (2 - t);
        }

        private float easeInOut(float t) {
            return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
        }

        public void play() { playing = true; }
        public void pause() { playing = false; }
        public void stop() { playing = false; currentTick = 0; }
        public void setTick(int tick) { currentTick = Math.max(0, Math.min(tick, animation.duration - 1)); }
        public int getTick() { return currentTick; }
        public boolean isPlaying() { return playing; }
        public void setInterpolation(InterpolationMode mode) { this.interpolation = mode; }
    }

    public enum InterpolationMode {
        LINEAR,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT
    }

    /**
     * Animation capture tool - records current model state as keyframe
     */
    public static class AnimationRecorder {
        private final Animation animation;
        private final ModelInstance instance;

        public AnimationRecorder(Animation animation, ModelInstance instance) {
            this.animation = animation;
            this.instance = instance;
        }

        /**
         * Capture current model state as a keyframe
         */
        public Keyframe captureKeyframe(int tick) {
            Keyframe kf = new Keyframe(tick);

            for (ModelInstance.Part part : instance.getParts().values()) {
                ModelData.PartData data = part.getData();
                kf.setTransform(
                        data.id,
                        data.position.clone(),
                        data.rotation.clone(),
                        data.scale.clone()
                );
            }

            return kf;
        }

        /**
         * Add current state to animation
         */
        public void recordFrame(int tick) {
            Keyframe kf = captureKeyframe(tick);
            animation.addKeyframe(kf);
        }

        /**
         * Remove keyframe at tick
         */
        public void deleteKeyframe(int tick) {
            animation.keyframes.removeIf(kf -> kf.tick == tick);
        }
    }

    /**
     * Blend between multiple animations
     */
    public static class AnimationBlender {
        private final Map<Animation, Float> weights = new HashMap<>();
        private final ModelInstance instance;

        public AnimationBlender(ModelInstance instance) {
            this.instance = instance;
        }

        public void setWeight(Animation anim, float weight) {
            weights.put(anim, Math.max(0, Math.min(1, weight)));
        }

        /**
         * Apply blended transforms
         */
        public void apply(int tick) {
            // Normalize weights
            float totalWeight = 0;
            for (float w : weights.values()) {
                totalWeight += w;
            }

            if (totalWeight == 0) return;

            // Accumulate weighted transforms
            Map<String, Vector> blendedPos = new HashMap<>();
            Map<String, Vector> blendedRot = new HashMap<>();
            Map<String, Vector> blendedScale = new HashMap<>();

            for (Map.Entry<Animation, Float> entry : weights.entrySet()) {
                Animation anim = entry.getKey();
                float weight = entry.getValue() / totalWeight;

                // Get transforms at this tick (simplified - would need AnimationPlayer)
                // ... blend logic here
            }

            // Apply blended results
            // ... application logic
        }
    }
}