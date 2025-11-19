package com.mcbzh.custombosses.model;

import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ModelPart {

    private final ModelPartData data;
    private final ModelInstance model;
    private BlockDisplay display;

    private final Matrix4f cachedGlobalMatrix = new Matrix4f();

    public ModelPart(ModelPartData data, ModelInstance model) {
        this.data = data;
        this.model = model;
    }

    public void spawn(Location origin) {
        if (display != null)
            return;
        display = (BlockDisplay) origin.getWorld().spawnEntity(origin, EntityType.BLOCK_DISPLAY);
        display.setBlock(data.getMaterial().createBlockData());
        updateTransform(origin);
    }

    public void despawn() {
        if (display != null) {
            display.remove();
            display = null;
        }
    }

    public void updateTransform(Location rootLocation) {
        if (display == null)
            return;

        // 1. Calculate Global Matrix
        Matrix4f globalMatrix = new Matrix4f();

        // Start with identity (Root)
        globalMatrix.identity();

        // If we have a parent, get its global matrix first
        ModelPart parent = model.getPart(data.getParentId());
        if (parent != null) {
            parent.getGlobalMatrix(globalMatrix);
        }

        // 2. Apply Local Transform to the matrix
        // Matrix multiplication order: Parent * Local

        // Local Translation
        globalMatrix.translate((float) data.getOffset().getX(), (float) data.getOffset().getY(),
                (float) data.getOffset().getZ());

        // Local Rotation
        globalMatrix.rotateXYZ(
                (float) Math.toRadians(data.getRotation().getX()),
                (float) Math.toRadians(data.getRotation().getY()),
                (float) Math.toRadians(data.getRotation().getZ()));

        // Local Scale
        globalMatrix.scale((float) data.getScale().getX(), (float) data.getScale().getY(),
                (float) data.getScale().getZ());

        // Cache this matrix for children to use
        this.cachedGlobalMatrix.set(globalMatrix);

        // 3. Apply to Entity
        // We keep the entity at the Root Location
        display.teleport(rootLocation);

        // Extract components for Transformation
        Vector3f translation = globalMatrix.getTranslation(new Vector3f());
        Quaternionf leftRotation = globalMatrix.getUnnormalizedRotation(new Quaternionf());
        Vector3f scale = globalMatrix.getScale(new Vector3f());

        Transformation t = new Transformation(
                translation,
                leftRotation,
                scale,
                new Quaternionf());

        display.setTransformation(t);
    }

    public void getGlobalMatrix(Matrix4f dest) {
        dest.set(cachedGlobalMatrix);
    }

    public String getId() {
        return data.getId();
    }

    public BlockDisplay getEntity() {
        return display;
    }

    public ModelPartData getData() {
        return data;
    }

    public Vector3f getGlobalPosition() {
        return cachedGlobalMatrix.getTranslation(new Vector3f());
    }
}
