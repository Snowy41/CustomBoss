package com.mcbzh.custombosses.editor;

import com.mcbzh.custombosses.boss.CustomBoss;
import com.mcbzh.custombosses.model.ModelInstance;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * Debug visualization system for models and bosses
 * Shows hitboxes, part hierarchy, transforms, and performance stats
 */
public class DebugVisualizer {

    private final Map<UUID, List<Display>> debugDisplays = new HashMap<>();
    private boolean showHitboxes = true;
    private boolean showPartNames = true;
    private boolean showHierarchy = true;
    private boolean showTransforms = false;
    private boolean showPerformance = false;

    /**
     * Visualize a boss for debugging
     */
    public void visualizeBoss(CustomBoss boss, Player viewer) {
        UUID id = boss.getUUID();
        clearDebugDisplays(id);

        List<Display> displays = new ArrayList<>();

        if (showHitboxes) {
            displays.addAll(createHitboxDisplay(boss));
        }

        if (showPartNames || showHierarchy) {
            displays.addAll(createPartLabels(boss.getModelInstance(), viewer));
        }

        if (showTransforms) {
            displays.addAll(createTransformGizmos(boss.getModelInstance()));
        }

        debugDisplays.put(id, displays);
    }

    /**
     * Visualize a model instance in editor
     */
    public void visualizeModelInstance(ModelInstance instance, Player viewer) {
        UUID id = UUID.randomUUID();
        clearDebugDisplays(id);

        List<Display> displays = new ArrayList<>();

        if (showPartNames) {
            displays.addAll(createPartLabels(instance, viewer));
        }

        if (showHierarchy) {
            displays.addAll(createHierarchyLines(instance));
        }

        if (showTransforms) {
            displays.addAll(createTransformGizmos(instance));
        }

        debugDisplays.put(id, displays);
    }

    /**
     * Show hitbox boundaries
     */
    private List<Display> createHitboxDisplay(CustomBoss boss) {
        List<Display> displays = new ArrayList<>();
        Location loc = boss.getHitbox().getLocation();

        float width = boss.getHitbox().getInteractionWidth();
        float height = boss.getHitbox().getInteractionHeight();

        // Create wireframe box
        displays.addAll(createWireframeBox(
                loc,
                new Vector3f(width, height, width),
                Material.RED_STAINED_GLASS,
                Color.RED
        ));

        // Add label
        TextDisplay label = createLabel(
                loc.clone().add(0, height + 0.5, 0),
                String.format("§cHitbox\n§7%.1fx%.1fx%.1f", width, height, width),
                Color.RED
        );
        displays.add(label);

        return displays;
    }

    /**
     * Show labels for each part
     */
    private List<Display> createPartLabels(ModelInstance instance, Player viewer) {
        List<Display> displays = new ArrayList<>();

        for (ModelInstance.Part part : instance.getParts().values()) {
            if (part.getEntity() == null || !part.getEntity().isValid()) continue;

            Location loc = part.getEntity().getLocation();

            // Calculate distance to viewer
            double distance = viewer.getLocation().distance(loc);
            if (distance > 10) continue; // Too far to read

            // Build label text
            StringBuilder text = new StringBuilder();
            text.append("§e").append(part.getId());

            if (showTransforms) {
                Vector pos = part.getData().position;
                Vector rot = part.getData().rotation;
                Vector scale = part.getData().scale;

                text.append("\n§7Pos: §f").append(String.format("%.1f,%.1f,%.1f",
                        pos.getX(), pos.getY(), pos.getZ()));
                text.append("\n§7Rot: §f").append(String.format("%.0f°,%.0f°,%.0f°",
                        rot.getX(), rot.getY(), rot.getZ()));
                text.append("\n§7Scale: §f").append(String.format("%.2f,%.2f,%.2f",
                        scale.getX(), scale.getY(), scale.getZ()));
            }

            TextDisplay label = createLabel(
                    loc.clone().add(0, 0.5, 0),
                    text.toString(),
                    Color.YELLOW
            );
            displays.add(label);
        }

        return displays;
    }

    /**
     * Show parent-child hierarchy with lines
     */
    private List<Display> createHierarchyLines(ModelInstance instance) {
        List<Display> displays = new ArrayList<>();

        for (ModelInstance.Part part : instance.getParts().values()) {
            if (part.getData().parentId == null) continue;
            if (part.getEntity() == null) continue;

            // Find parent
            ModelInstance.Part parent = instance.getParts().get(part.getData().parentId);
            if (parent == null || parent.getEntity() == null) continue;

            // Draw line from child to parent
            Location childLoc = part.getEntity().getLocation();
            Location parentLoc = parent.getEntity().getLocation();

            displays.add(createLine(childLoc, parentLoc, Material.CYAN_STAINED_GLASS));
        }

        return displays;
    }

    /**
     * Show transform axes for each part
     */
    private List<Display> createTransformGizmos(ModelInstance instance) {
        List<Display> displays = new ArrayList<>();

        for (ModelInstance.Part part : instance.getParts().values()) {
            if (part.getEntity() == null) continue;

            Location loc = part.getEntity().getLocation();
            Vector rot = part.getData().rotation;

            // Create small XYZ axes at part location
            Quaternionf rotation = new Quaternionf()
                    .rotateXYZ(
                            (float) Math.toRadians(rot.getX()),
                            (float) Math.toRadians(rot.getY()),
                            (float) Math.toRadians(rot.getZ())
                    );

            displays.addAll(createMiniAxes(loc, rotation, 0.5f));
        }

        return displays;
    }

    /**
     * Create a wireframe box
     */
    private List<Display> createWireframeBox(Location center, Vector3f size,
                                             Material material, Color glowColor) {
        List<Display> displays = new ArrayList<>();
        float thickness = 0.02f;

        // 12 edges of a box
        Vector3f[][] edges = {
                // Bottom face
                {new Vector3f(0, 0, 0), new Vector3f(size.x, 0, 0)},
                {new Vector3f(0, 0, size.z), new Vector3f(size.x, 0, size.z)},
                {new Vector3f(0, 0, 0), new Vector3f(0, 0, size.z)},
                {new Vector3f(size.x, 0, 0), new Vector3f(size.x, 0, size.z)},
                // Top face
                {new Vector3f(0, size.y, 0), new Vector3f(size.x, size.y, 0)},
                {new Vector3f(0, size.y, size.z), new Vector3f(size.x, size.y, size.z)},
                {new Vector3f(0, size.y, 0), new Vector3f(0, size.y, size.z)},
                {new Vector3f(size.x, size.y, 0), new Vector3f(size.x, size.y, size.z)},
                // Vertical edges
                {new Vector3f(0, 0, 0), new Vector3f(0, size.y, 0)},
                {new Vector3f(size.x, 0, 0), new Vector3f(size.x, size.y, 0)},
                {new Vector3f(0, 0, size.z), new Vector3f(0, size.y, size.z)},
                {new Vector3f(size.x, 0, size.z), new Vector3f(size.x, size.y, size.z)}
        };

        for (Vector3f[] edge : edges) {
            Location start = center.clone().add(edge[0].x, edge[0].y, edge[0].z);
            Location end = center.clone().add(edge[1].x, edge[1].y, edge[1].z);
            displays.add(createLine(start, end, material));
        }

        return displays;
    }

    /**
     * Create a line between two points
     */
    private BlockDisplay createLine(Location start, Location end, Material material) {
        Vector3f diff = new Vector3f(
                (float) (end.getX() - start.getX()),
                (float) (end.getY() - start.getY()),
                (float) (end.getZ() - start.getZ())
        );

        float length = diff.length();
        Location mid = start.clone().add(
                diff.x / 2, diff.y / 2, diff.z / 2
        );

        BlockDisplay display = (BlockDisplay) start.getWorld()
                .spawnEntity(mid, EntityType.BLOCK_DISPLAY);
        display.setBlock(material.createBlockData());
        display.setGlowing(true);
        display.setBrightness(new Display.Brightness(15, 15));

        // Calculate rotation to point toward end
        Vector3f direction = diff.normalize();
        Quaternionf rotation = new Quaternionf(); // TODO: Calculate proper rotation

        Transformation t = new Transformation(
                new Vector3f(0, 0, 0),
                rotation,
                new Vector3f(0.02f, 0.02f, length),
                new Quaternionf()
        );

        display.setTransformation(t);
        return display;
    }

    /**
     * Create mini XYZ axes
     */
    private List<Display> createMiniAxes(Location center, Quaternionf rotation, float size) {
        List<Display> displays = new ArrayList<>();
        float thickness = 0.03f;

        // X (Red), Y (Green), Z (Blue)
        displays.add(createAxisLine(center, rotation, new Vector3f(size, 0, 0),
                thickness, Material.RED_CONCRETE));
        displays.add(createAxisLine(center, rotation, new Vector3f(0, size, 0),
                thickness, Material.LIME_CONCRETE));
        displays.add(createAxisLine(center, rotation, new Vector3f(0, 0, size),
                thickness, Material.LIGHT_BLUE_CONCRETE));

        return displays;
    }

    private BlockDisplay createAxisLine(Location center, Quaternionf baseRotation,
                                        Vector3f direction, float thickness, Material material) {
        BlockDisplay display = (BlockDisplay) center.getWorld()
                .spawnEntity(center, EntityType.BLOCK_DISPLAY);
        display.setBlock(material.createBlockData());
        display.setGlowing(true);
        display.setBrightness(new Display.Brightness(15, 15));

        float length = direction.length();
        Vector3f scale = new Vector3f(thickness, thickness, thickness);

        if (direction.x > 0) scale.x = length;
        else if (direction.y > 0) scale.y = length;
        else if (direction.z > 0) scale.z = length;

        Transformation t = new Transformation(
                direction.mul(0.5f),
                baseRotation,
                scale,
                new Quaternionf()
        );

        display.setTransformation(t);
        return display;
    }

    private TextDisplay createLabel(Location location, String text, Color backgroundColor) {
        TextDisplay display = (TextDisplay) location.getWorld()
                .spawnEntity(location, EntityType.TEXT_DISPLAY);
        display.setText(text);
        display.setBackgroundColor(Color.fromARGB(150,
                backgroundColor.getRed(),
                backgroundColor.getGreen(),
                backgroundColor.getBlue()));
        display.setBillboard(Display.Billboard.CENTER);
        display.setSeeThrough(false);
        display.setBrightness(new Display.Brightness(15, 15));
        display.setAlignment(TextDisplay.TextAlignment.CENTER);

        return display;
    }

    public void clearDebugDisplays(UUID id) {
        List<Display> displays = debugDisplays.remove(id);
        if (displays != null) {
            displays.forEach(d -> {
                if (d != null && d.isValid()) d.remove();
            });
        }
    }

    public void clearAll() {
        debugDisplays.keySet().forEach(this::clearDebugDisplays);
    }

    // Toggles
    public void setShowHitboxes(boolean show) { this.showHitboxes = show; }
    public void setShowPartNames(boolean show) { this.showPartNames = show; }
    public void setShowHierarchy(boolean show) { this.showHierarchy = show; }
    public void setShowTransforms(boolean show) { this.showTransforms = show; }
    public void setShowPerformance(boolean show) { this.showPerformance = show; }
}