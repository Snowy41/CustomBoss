package com.mcbzh.custombosses.model;

import org.bukkit.util.Vector;
import java.util.List;
import java.util.ArrayList;

public class ModelData {
    private String id;
    private List<ModelPartData> parts = new ArrayList<>();
    private Vector hitboxSize = new Vector(1, 2, 1); // Default 1x2x1

    public ModelData(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public List<ModelPartData> getParts() {
        return parts;
    }

    public void addPart(ModelPartData part) {
        parts.add(part);
    }

    public Vector getHitboxSize() {
        return hitboxSize;
    }

    public void setHitboxSize(Vector size) {
        this.hitboxSize = size;
    }
}
