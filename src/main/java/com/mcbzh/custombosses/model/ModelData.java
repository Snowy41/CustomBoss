package com.mcbzh.custombosses.model;

import org.bukkit.Material;
import org.bukkit.util.Vector;
import java.util.ArrayList;
import java.util.List;

public class ModelData {
    private String id;
    private List<PartData> parts;
    private Vector hitboxSize;

    public ModelData(String id) {
        this.id = id;
        this.parts = new ArrayList<>();
        this.hitboxSize = new Vector(1.0, 2.0, 1.0);
    }

    public String getId() { return id; }
    public List<PartData> getParts() { return parts; }
    public Vector getHitboxSize() { return hitboxSize; }
    public void setHitboxSize(Vector size) { this.hitboxSize = size; }

    public void addPart(PartData part) {
        parts.add(part);
    }

    public void removePart(String partId) {
        parts.removeIf(p -> p.id.equals(partId));
    }

    public PartData getPart(String partId) {
        return parts.stream()
                .filter(p -> p.id.equals(partId))
                .findFirst()
                .orElse(null);
    }

    public static class PartData {
        public String id;
        public String parentId;
        public Material material;
        public Vector position;
        public Vector rotation;
        public Vector scale;

        public PartData(String id) {
            this.id = id;
            this.material = Material.WHITE_CONCRETE;
            this.position = new Vector(0, 0, 0);
            this.rotation = new Vector(0, 0, 0);
            this.scale = new Vector(1, 1, 1);
        }
    }
}