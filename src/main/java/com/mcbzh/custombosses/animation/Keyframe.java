package com.mcbzh.custombosses.animation;

import org.bukkit.util.Vector;

public class Keyframe {
    private final float time; // Time in seconds or ticks
    private final Vector offset;
    private final Vector rotation;
    private final Vector scale;

    public Keyframe(float time, Vector offset, Vector rotation, Vector scale) {
        this.time = time;
        this.offset = offset;
        this.rotation = rotation;
        this.scale = scale;
    }

    public float getTime() {
        return time;
    }

    public Vector getOffset() {
        return offset;
    }

    public Vector getRotation() {
        return rotation;
    }

    public Vector getScale() {
        return scale;
    }
}
