package com.mcbzh.custombosses.editor;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class GizmoManager {

    private final List<BlockDisplay> gizmos = new ArrayList<>();

    public void showGizmo(Location center, Quaternionf rotation) {
        hideGizmo(); // Clear old ones

        // X Axis (Red)
        spawnAxis(center, rotation, new Vector3f(1, 0, 0), Material.RED_CONCRETE);
        // Y Axis (Green)
        spawnAxis(center, rotation, new Vector3f(0, 1, 0), Material.LIME_CONCRETE);
        // Z Axis (Blue)
        spawnAxis(center, rotation, new Vector3f(0, 0, 1), Material.BLUE_CONCRETE);
    }

    private void spawnAxis(Location center, Quaternionf baseRotation, Vector3f axis, Material material) {
        BlockDisplay display = (BlockDisplay) center.getWorld().spawnEntity(center, EntityType.BLOCK_DISPLAY);
        display.setBlock(material.createBlockData());
        display.setGlowColorOverride(Color.WHITE); // Optional
        display.setGlowing(true);

        // Transform to stretch along the axis
        // We want a thin line.
        // Default block is 1x1x1.
        // Scale: Length=1.0 (or more), Thickness=0.05

        Vector3f scale = new Vector3f(0.05f, 0.05f, 0.05f);
        // If axis is X (1,0,0), we want to scale X to 1.0
        if (axis.x > 0.5)
            scale.x = 1.0f;
        if (axis.y > 0.5)
            scale.y = 1.0f;
        if (axis.z > 0.5)
            scale.z = 1.0f;

        // Rotation: Apply the base rotation of the part
        Transformation t = new Transformation(
                new Vector3f(0, 0, 0), // Center
                baseRotation,
                scale,
                new Quaternionf());

        display.setTransformation(t);
        gizmos.add(display);
    }

    public void hideGizmo() {
        for (BlockDisplay display : gizmos) {
            display.remove();
        }
        gizmos.clear();
    }
}
