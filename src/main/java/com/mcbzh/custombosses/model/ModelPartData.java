package com.mcbzh.custombosses.model;

import org.bukkit.Material;
import org.bukkit.util.Vector;

public class ModelPartData {
    private String id;
    private String parentId;
    private Material material;

    // Relative transform (Local space)
    private Vector offset = new Vector(0, 0, 0);
    private Vector rotation = new Vector(0, 0, 0); // Euler angles in degrees
    private Vector scale = new Vector(1, 1, 1);

    public ModelPartData(String id, Material material) {
        this.id = id;
        this.material = material;
    }

    public ModelPartData(String id, String parentId, Material material) {
        this.id = id;
        this.parentId = parentId;
        this.material = material;
    }

    public ModelPartData(String id, Material material, Vector offset, Vector rotation, Vector scale) {
        this.id = id;
        this.material = material;
        this.offset = offset;
        this.rotation = rotation;
        this.scale = scale;
    }

    public String getId() {
        return id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Vector getOffset() {
        return offset;
    }

    public void setOffset(Vector offset) {
        this.offset = offset;
    }

    public Vector getRotation() {
        return rotation;
    }

    public void setRotation(Vector rotation) {
        this.rotation = rotation;
    }

    public Vector getScale() {
        return scale;
    }

    public void setScale(Vector scale) {
        this.scale = scale;
    }
}
