package com.mcbzh.custombosses.animation;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.model.ModelInstance;
import com.mcbzh.custombosses.model.ModelPart;
import org.bukkit.Bukkit;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimationManager {

    private final Map<ModelInstance, ActiveAnimation> activeAnimations = new HashMap<>();

    public AnimationManager(CustomBossesPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void playAnimation(ModelInstance model, AnimationData animation) {
        activeAnimations.put(model, new ActiveAnimation(animation));
    }

    public void stopAnimation(ModelInstance model) {
        activeAnimations.remove(model);
    }

    private void tick() {
        activeAnimations.entrySet().removeIf(entry -> {
            ModelInstance model = entry.getKey();
            ActiveAnimation active = entry.getValue();

            if (model.getParts().isEmpty())
                return true;

            active.time += 0.05f; // 1 tick = 0.05s
            if (active.time > active.data.getDuration()) {
                if (active.data.isLoop()) {
                    active.time %= active.data.getDuration();
                } else {
                    return true; // End animation
                }
            }

            applyAnimation(model, active);
            model.update();
            return false;
        });
    }

    private void applyAnimation(ModelInstance model, ActiveAnimation active) {
        for (Map.Entry<String, List<Keyframe>> track : active.data.getTracks().entrySet()) {
            String partId = track.getKey();
            List<Keyframe> frames = track.getValue();
            ModelPart part = model.getParts().get(partId);

            if (part != null) {
                Keyframe current = getKeyframeAt(frames, active.time);
                if (current != null) {
                    // Interpolation logic would go here
                    // For now, just snap to nearest previous keyframe
                    part.getData().setOffset(current.getOffset());
                    part.getData().setRotation(current.getRotation());
                    part.getData().setScale(current.getScale());
                }
            }
        }
    }

    private Keyframe getKeyframeAt(List<Keyframe> frames, float time) {
        Keyframe best = null;
        for (Keyframe k : frames) {
            if (k.getTime() <= time) {
                best = k;
            } else {
                break;
            }
        }
        return best;
    }

    private static class ActiveAnimation {
        final AnimationData data;
        float time = 0;

        ActiveAnimation(AnimationData data) {
            this.data = data;
        }
    }
}
