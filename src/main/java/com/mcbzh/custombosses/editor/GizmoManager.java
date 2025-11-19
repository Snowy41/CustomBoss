package com.mcbzh.custombosses.editor;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Advanced Gizmo system inspired by professional 3D editors
 * Shows XYZ axes, grid, and selection indicators
 */
public class GizmoManager {

    private final List<Display> gizmos = new ArrayList<>();
    private Location currentCenter;
    private boolean gridEnabled = true;
    private boolean axesEnabled = true;
    private boolean labelsEnabled = true;

    public void showGizmo(Location center, Quaternionf rotation) {
        if (center.equals(currentCenter)) return; // No change

        hideGizmo();
        this.currentCenter = center.clone();

        if (axesEnabled) {
            spawnAxes(center, rotation);
        }

        if (gridEnabled) {
            spawnGrid(center);
        }

        if (labelsEnabled) {
            spawnLabels(center);
        }
    }

    /**
     * Spawn XYZ axis lines (Red/Green/Blue)
     * Much more visible than particles
     */
    private void spawnAxes(Location center, Quaternionf baseRotation) {
        float length = 2.0f;
        float thickness = 0.05f;

        // X Axis (Red) - Horizontal right
        spawnAxisLine(center, baseRotation, new Vector3f(1, 0, 0),
                Material.RED_CONCRETE, length, thickness, Color.RED);

        // Y Axis (Green) - Vertical up
        spawnAxisLine(center, baseRotation, new Vector3f(0, 1, 0),
                Material.LIME_CONCRETE, length, thickness, Color.LIME);

        // Z Axis (Blue) - Horizontal forward
        spawnAxisLine(center, baseRotation, new Vector3f(0, 0, 1),
                Material.LIGHT_BLUE_CONCRETE, length, thickness, Color.BLUE);
    }

    private void spawnAxisLine(Location center, Quaternionf baseRotation,
                               Vector3f axis, Material material,
                               float length, float thickness, Color glowColor) {
        BlockDisplay display = (BlockDisplay) center.getWorld()
                .spawnEntity(center, EntityType.BLOCK_DISPLAY);
        display.setBlock(material.createBlockData());
        display.setGlowing(true);
        display.setGlowColorOverride(glowColor);
        display.setBrightness(new Display.Brightness(15, 15)); // Full bright

        // Create elongated box along the axis
        Vector3f scale = new Vector3f(thickness, thickness, thickness);
        Vector3f translation = new Vector3f(0, 0, 0);

        // Scale and offset based on axis direction
        if (axis.x > 0.5) {
            scale.x = length;
            translation.x = length / 2; // Offset to start at center
        } else if (axis.y > 0.5) {
            scale.y = length;
            translation.y = length / 2;
        } else if (axis.z > 0.5) {
            scale.z = length;
            translation.z = length / 2;
        }

        // Apply base rotation
        Matrix4f matrix = new Matrix4f();
        matrix.rotate(baseRotation);
        matrix.translate(translation);

        Transformation t = new Transformation(
                matrix.getTranslation(new Vector3f()),
                matrix.getUnnormalizedRotation(new Quaternionf()),
                scale,
                new Quaternionf()
        );

        display.setTransformation(t);
        gizmos.add(display);
    }

    /**
     * Spawn a ground grid for reference
     * Helps with spatial awareness
     */
    private void spawnGrid(Location center) {
        int gridSize = 5;
        float cellSize = 1.0f;
        float thickness = 0.01f;

        Location gridBase = center.clone();
        gridBase.setY(Math.floor(center.getY())); // Snap to block

        // Horizontal lines (along X axis)
        for (int z = -gridSize; z <= gridSize; z++) {
            Location lineStart = gridBase.clone().add(0, 0, z * cellSize);
            spawnGridLine(lineStart, new Vector3f(gridSize * cellSize * 2, thickness, thickness),
                    Material.GRAY_CONCRETE, 0.3f);
        }

        // Horizontal lines (along Z axis)
        for (int x = -gridSize; x <= gridSize; x++) {
            Location lineStart = gridBase.clone().add(x * cellSize, 0, 0);
            spawnGridLine(lineStart, new Vector3f(thickness, thickness, gridSize * cellSize * 2),
                    Material.GRAY_CONCRETE, 0.3f);
        }
    }

    private void spawnGridLine(Location center, Vector3f scale, Material material, float alpha) {
        BlockDisplay display = (BlockDisplay) center.getWorld()
                .spawnEntity(center, EntityType.BLOCK_DISPLAY);
        display.setBlock(material.createBlockData());
        display.setGlowing(false);
        display.setBrightness(new Display.Brightness(5, 5)); // Dim

        // Offset to center the line
        Vector3f translation = new Vector3f(
                -scale.x / 2,
                0,
                -scale.z / 2
        );

        Transformation t = new Transformation(
                translation,
                new Quaternionf(),
                scale,
                new Quaternionf()
        );

        display.setTransformation(t);
        gizmos.add(display);
    }

    /**
     * Spawn text labels for axes
     * Makes it clear which axis is which
     */
    private void spawnLabels(Location center) {
        float labelDistance = 2.2f;

        spawnLabel(center.clone().add(labelDistance, 0, 0), "X", Color.RED);
        spawnLabel(center.clone().add(0, labelDistance, 0), "Y", Color.LIME);
        spawnLabel(center.clone().add(0, 0, labelDistance), "Z", Color.BLUE);
    }

    private void spawnLabel(Location location, String text, Color color) {
        TextDisplay display = (TextDisplay) location.getWorld()
                .spawnEntity(location, EntityType.TEXT_DISPLAY);
        display.setText("§l" + text);
        display.setBackgroundColor(Color.fromARGB(100, 0, 0, 0)); // Semi-transparent black
        display.setBillboard(Display.Billboard.CENTER);
        display.setSeeThrough(true);
        display.setBrightness(new Display.Brightness(15, 15));

        gizmos.add(display);
    }

    /**
     * Show a selection box around an entity
     */
    public void showSelectionBox(Location location, Vector3f size) {
        // Create wireframe box
        float lineThickness = 0.02f;
        Material mat = Material.YELLOW_CONCRETE;

        // Bottom edges
        spawnBoxEdge(location, new Vector3f(size.x, lineThickness, lineThickness),
                new Vector3f(0, 0, 0), mat);
        spawnBoxEdge(location, new Vector3f(lineThickness, lineThickness, size.z),
                new Vector3f(0, 0, 0), mat);
        spawnBoxEdge(location, new Vector3f(size.x, lineThickness, lineThickness),
                new Vector3f(0, 0, size.z), mat);
        spawnBoxEdge(location, new Vector3f(lineThickness, lineThickness, size.z),
                new Vector3f(size.x, 0, 0), mat);

        // Top edges
        spawnBoxEdge(location, new Vector3f(size.x, lineThickness, lineThickness),
                new Vector3f(0, size.y, 0), mat);
        spawnBoxEdge(location, new Vector3f(lineThickness, lineThickness, size.z),
                new Vector3f(0, size.y, 0), mat);
        spawnBoxEdge(location, new Vector3f(size.x, lineThickness, lineThickness),
                new Vector3f(0, size.y, size.z), mat);
        spawnBoxEdge(location, new Vector3f(lineThickness, lineThickness, size.z),
                new Vector3f(size.x, size.y, 0), mat);

        // Vertical edges
        spawnBoxEdge(location, new Vector3f(lineThickness, size.y, lineThickness),
                new Vector3f(0, 0, 0), mat);
        spawnBoxEdge(location, new Vector3f(lineThickness, size.y, lineThickness),
                new Vector3f(size.x, 0, 0), mat);
        spawnBoxEdge(location, new Vector3f(lineThickness, size.y, lineThickness),
                new Vector3f(0, 0, size.z), mat);
        spawnBoxEdge(location, new Vector3f(lineThickness, size.y, lineThickness),
                new Vector3f(size.x, 0, size.z), mat);
    }

    private void spawnBoxEdge(Location base, Vector3f scale, Vector3f offset, Material material) {
        Location loc = base.clone().add(offset.x, offset.y, offset.z);
        BlockDisplay display = (BlockDisplay) loc.getWorld()
                .spawnEntity(loc, EntityType.BLOCK_DISPLAY);
        display.setBlock(material.createBlockData());
        display.setGlowing(true);
        display.setGlowColorOverride(Color.YELLOW);
        display.setBrightness(new Display.Brightness(15, 15));

        Transformation t = new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                scale,
                new Quaternionf()
        );

        display.setTransformation(t);
        gizmos.add(display);
    }

    public void hideGizmo() {
        for (Display display : gizmos) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        gizmos.clear();
        currentCenter = null;
    }

    public void setGridEnabled(boolean enabled) {
        this.gridEnabled = enabled;
    }

    public void setAxesEnabled(boolean enabled) {
        this.axesEnabled = enabled;
    }

    public void setLabelsEnabled(boolean enabled) {
        this.labelsEnabled = enabled;
    }
}