package com.mcbzh.custombosses.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimationData {
    private final String id;
    private final float duration;
    private final boolean loop;
    // Map<PartID, List<Keyframe>>
    private final Map<String, List<Keyframe>> tracks = new HashMap<>();

    public AnimationData(String id, float duration, boolean loop) {
        this.id = id;
        this.duration = duration;
        this.loop = loop;
    }

    public void addKeyframe(String partId, Keyframe keyframe) {
        tracks.computeIfAbsent(partId, k -> new ArrayList<>()).add(keyframe);
        // Sort by time
        tracks.get(partId).sort((a, b) -> Float.compare(a.getTime(), b.getTime()));
    }

    public List<Keyframe> getTrack(String partId) {
        return tracks.get(partId);
    }

    public String getId() {
        return id;
    }

    public float getDuration() {
        return duration;
    }

    public boolean isLoop() {
        return loop;
    }

    public Map<String, List<Keyframe>> getTracks() {
        return tracks;
    }
}
