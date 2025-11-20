package com.mcbzh.custombosses.editor;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.model.ModelData;
import com.mcbzh.custombosses.model.ModelInstance;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Consumer;

/**
 * Updated EditorSession with smooth interpolation awareness
 * CRITICAL: Editor mode ALWAYS uses instant updates (no interpolation)
 * This ensures immediate visual feedback when moving parts
 */
public class EditorSession {

    private final Player player;
    private final CustomBossesPlugin plugin;

    private boolean active;
    private ModelData currentModel;
    private ModelInstance currentInstance;
    private BlockDisplay selectedPart;
    private String selectedPartId;

    // Root marker for model center
    private BlockDisplay rootMarker;
    private Location fixedRootLocation;

    // Advanced gizmo system
    private final GizmoManager gizmoManager;
    private final DebugVisualizer debugVisualizer;

    // Transform modes
    private enum TransformMode {
        NONE, MOVE, ROTATE, SCALE
    }
    private TransformMode currentTransformMode = TransformMode.NONE;
    private enum TransformAxis {
        X, Y, Z, ALL
    }
    private TransformAxis currentAxis = TransformAxis.ALL;

    // Crosshair movement mode
    private boolean crosshairMoveMode = false;
    private double crosshairDistance = 3.0;

    private boolean awaitingChatInput;
    private Consumer<String> chatCallback;

    private final Stack<EditorAction> undoStack;
    private final Stack<EditorAction> redoStack;

    public EditorSession(Player player, CustomBossesPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
        this.active = false;
        this.undoStack = new Stack<>();
        this.redoStack = new Stack<>();
        this.gizmoManager = new GizmoManager();
        this.debugVisualizer = new DebugVisualizer();
    }

    public void openHub() {
        EditorGUI.openMainMenu(player, this);
    }

    public void editModel(String modelId) {
        currentModel = plugin.getModelStorage().get(modelId);
        if (currentModel == null) {
            currentModel = new ModelData(modelId);
            plugin.getModelStorage().save(currentModel);
        }

        fixedRootLocation = player.getLocation().clone();
        spawnRootMarker();

        currentInstance = new ModelInstance(currentModel, fixedRootLocation);

        // *** CRITICAL: Enable editor mode for instant updates ***
        currentInstance.setEditorMode(true);

        currentInstance.spawn();

        active = true;

        // Give tools
        player.getInventory().clear();
        player.getInventory().setItem(0, createTool(Material.EMERALD, "§aSpawn Part", "spawn"));
        player.getInventory().setItem(1, createTool(Material.SPECTRAL_ARROW, "§eSelect Part", "select"));
        player.getInventory().setItem(2, createTool(Material.STICK, "§6Move Tool §7(Sneak: Toggle Axis)", "move"));
        player.getInventory().setItem(3, createTool(Material.BLAZE_ROD, "§6Rotate Tool §7(Sneak: Toggle Axis)", "rotate"));
        player.getInventory().setItem(4, createTool(Material.SLIME_BALL, "§6Scale Tool §7(Sneak: Toggle Axis)", "scale"));
        player.getInventory().setItem(5, createTool(Material.LEAD, "§dParent Tool", "parent"));
        player.getInventory().setItem(6, createTool(Material.MAGMA_CREAM, "§bMaterial Tool", "material"));
        player.getInventory().setItem(7, createTool(Material.TNT, "§cDelete Part", "delete"));
        player.getInventory().setItem(8, createTool(Material.BARRIER, "§cExit & Save", "exit"));

        player.sendMessage("§e=== Editing Model: " + modelId + " ===");
        player.sendMessage("§7Parts: " + currentModel.getParts().size());
        player.sendMessage("§7Root at: " + String.format("%.1f, %.1f, %.1f",
                fixedRootLocation.getX(), fixedRootLocation.getY(), fixedRootLocation.getZ()));
        player.sendMessage("§a§lEditor Mode: §fInstant updates enabled");
        player.sendMessage("§e§lTransform Controls:");
        player.sendMessage("§7- Left-Click: Apply transform");
        player.sendMessage("§7- Right-Click: Toggle axis (X/Y/Z/All)");
        player.sendMessage("§7- Scroll: Fine-tune value");
        player.sendMessage("§7- Sneak + Tool: Toggle crosshair mode");
    }

    public void exit() {
        if (currentModel != null) {
            plugin.getModelStorage().save(currentModel);
            player.sendMessage("§aSaved model: " + currentModel.getId());
        }

        if (currentInstance != null) {
            currentInstance.despawn();
            currentInstance = null;
        }

        if (selectedPart != null && selectedPart.isValid()) {
            selectedPart.setGlowing(false);
            selectedPart = null;
        }

        if (rootMarker != null && rootMarker.isValid()) {
            rootMarker.remove();
            rootMarker = null;
        }

        gizmoManager.hideGizmo();
        debugVisualizer.clearAll();

        player.getInventory().clear();
        active = false;
        currentModel = null;
        fixedRootLocation = null;
        selectedPartId = null;
        currentTransformMode = TransformMode.NONE;
    }

    public void handleToolUse(String tool, boolean rightClick) {
        if (!active || currentInstance == null) {
            player.sendMessage("§cEditor not active!");
            return;
        }

        player.sendMessage("§7Tool: " + tool + " | Right: " + rightClick); // Debug

        switch (tool) {
            case "spawn" -> handleSpawn();
            case "select" -> handleSelect();
            case "move" -> {
                if (rightClick) {
                    cycleAxis();
                } else {
                    if (player.isSneaking()) {
                        toggleCrosshairMode();
                    } else {
                        currentTransformMode = TransformMode.MOVE;
                        player.sendMessage("§6§lMove Mode Active");
                        handleTransformClick();
                    }
                }
            }
            case "rotate" -> {
                if (rightClick) {
                    cycleAxis();
                } else {
                    currentTransformMode = TransformMode.ROTATE;
                    player.sendMessage("§6§lRotate Mode Active");
                    handleTransformClick();
                }
            }
            case "scale" -> {
                if (rightClick) {
                    cycleAxis();
                } else {
                    currentTransformMode = TransformMode.SCALE;
                    player.sendMessage("§6§lScale Mode Active");
                    handleTransformClick();
                }
            }
            case "parent" -> handleParent();
            case "material" -> handleMaterial();
            case "delete" -> handleDelete();
            case "exit" -> {
                exit();
                openHub();
            }
            default -> player.sendMessage("§cUnknown tool: " + tool);
        }
    }

    private void cycleAxis() {
        currentAxis = switch (currentAxis) {
            case ALL -> TransformAxis.X;
            case X -> TransformAxis.Y;
            case Y -> TransformAxis.Z;
            case Z -> TransformAxis.ALL;
        };

        String color = switch (currentAxis) {
            case X -> "§c";
            case Y -> "§a";
            case Z -> "§9";
            case ALL -> "§e";
        };

        player.sendMessage(color + "Axis: " + currentAxis);
    }

    private void handleTransformClick() {
        if (selectedPartId == null) {
            player.sendMessage("§cNo part selected!");
            return;
        }

        ModelInstance.Part part = currentInstance.getParts().get(selectedPartId);
        if (part == null) return;

        ModelData.PartData data = part.getData();

        // Calculate movement based on player's view direction
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        double amount = 0.1; // Base movement amount

        Vector oldPos = data.position.clone();
        Vector oldRot = data.rotation.clone();
        Vector oldScale = data.scale.clone();

        switch (currentTransformMode) {
            case MOVE -> {
                // Move in the direction player is looking, constrained to axis
                Vector movement = direction.clone().multiply(amount);

                switch (currentAxis) {
                    case X -> data.position.setX(data.position.getX() + movement.getX());
                    case Y -> data.position.setY(data.position.getY() + amount); // Always up/down
                    case Z -> data.position.setZ(data.position.getZ() + movement.getZ());
                    case ALL -> {
                        // Project movement onto view plane
                        data.position.add(movement);
                    }
                }
            }
            case ROTATE -> {
                double rotAmount = 15.0;
                switch (currentAxis) {
                    case X -> data.rotation.setX(data.rotation.getX() + rotAmount);
                    case Y -> data.rotation.setY(data.rotation.getY() + rotAmount);
                    case Z -> data.rotation.setZ(data.rotation.getZ() + rotAmount);
                    case ALL -> data.rotation.setY(data.rotation.getY() + rotAmount); // Default to Y
                }
            }
            case SCALE -> {
                double scaleAmount = 0.1;
                switch (currentAxis) {
                    case X -> data.scale.setX(Math.max(0.01, data.scale.getX() + scaleAmount));
                    case Y -> data.scale.setY(Math.max(0.01, data.scale.getY() + scaleAmount));
                    case Z -> data.scale.setZ(Math.max(0.01, data.scale.getZ() + scaleAmount));
                    case ALL -> {
                        data.scale.setX(Math.max(0.01, data.scale.getX() + scaleAmount));
                        data.scale.setY(Math.max(0.01, data.scale.getY() + scaleAmount));
                        data.scale.setZ(Math.max(0.01, data.scale.getZ() + scaleAmount));
                    }
                }
            }
        }

        // Update with instant feedback (editor mode is already enabled)
        currentInstance.markDirty();
        currentInstance.update(); // Will use instant mode automatically

        updateGizmo();
        refreshSelection(); // Update selection to new position
        recordAction(new TransformAction(data, oldPos, oldRot, oldScale));
    }

    public void handleScrollTransform(double delta) {
        if (!active || currentInstance == null || selectedPartId == null) return;

        ModelInstance.Part part = currentInstance.getParts().get(selectedPartId);
        if (part == null) return;

        ModelData.PartData data = part.getData();
        Vector oldPos = data.position.clone();
        Vector oldRot = data.rotation.clone();
        Vector oldScale = data.scale.clone();

        // Adjust based on current mode
        double moveAmount = delta * 0.05;
        double rotateAmount = delta * 5.0;
        double scaleAmount = delta * 0.05;

        switch (currentTransformMode) {
            case MOVE -> {
                switch (currentAxis) {
                    case X -> data.position.setX(data.position.getX() + moveAmount);
                    case Y -> data.position.setY(data.position.getY() + moveAmount);
                    case Z -> data.position.setZ(data.position.getZ() + moveAmount);
                    case ALL -> data.position.setY(data.position.getY() + moveAmount);
                }
            }
            case ROTATE -> {
                switch (currentAxis) {
                    case X -> data.rotation.setX(data.rotation.getX() + rotateAmount);
                    case Y -> data.rotation.setY(data.rotation.getY() + rotateAmount);
                    case Z -> data.rotation.setZ(data.rotation.getZ() + rotateAmount);
                    case ALL -> data.rotation.setY(data.rotation.getY() + rotateAmount);
                }
            }
            case SCALE -> {
                switch (currentAxis) {
                    case X -> data.scale.setX(Math.max(0.01, data.scale.getX() + scaleAmount));
                    case Y -> data.scale.setY(Math.max(0.01, data.scale.getY() + scaleAmount));
                    case Z -> data.scale.setZ(Math.max(0.01, data.scale.getZ() + scaleAmount));
                    case ALL -> {
                        data.scale.setX(Math.max(0.01, data.scale.getX() + scaleAmount));
                        data.scale.setY(Math.max(0.01, data.scale.getY() + scaleAmount));
                        data.scale.setZ(Math.max(0.01, data.scale.getZ() + scaleAmount));
                    }
                }
            }
        }

        // Instant update in editor mode
        currentInstance.markDirty();
        currentInstance.update();
        refreshSelection();
        updateGizmo();
        recordAction(new TransformAction(data, oldPos, oldRot, oldScale));
    }

    public void adjustCrosshairDistance(double delta) {
        crosshairDistance = Math.max(0.5, Math.min(10.0, crosshairDistance + delta));
        player.sendMessage("§7Crosshair distance: §e" + String.format("%.1f", crosshairDistance));
    }

    private void toggleCrosshairMode() {
        crosshairMoveMode = !crosshairMoveMode;
        if (crosshairMoveMode) {
            if (selectedPartId == null) {
                player.sendMessage("§cSelect a part first!");
                crosshairMoveMode = false;
                return;
            }
            player.sendMessage("§a§lCrosshair Move Mode: ON");
            player.sendMessage("§7Part will follow your crosshair");
            player.sendMessage("§7Scroll to adjust distance");
            player.sendMessage("§7Sneak + Tool again to lock position");
        } else {
            player.sendMessage("§c§lCrosshair Move Mode: OFF");
        }
    }

    private void handleSpawn() {
        if (fixedRootLocation == null || currentModel == null || currentInstance == null) {
            player.sendMessage("§cEditor not properly initialized!");
            return;
        }

        String id = "part_" + System.currentTimeMillis();

        // Spawn part where player is looking
        Location eyeLoc = player.getEyeLocation();
        Location targetLoc = eyeLoc.clone().add(eyeLoc.getDirection().multiply(3.0));

        // Calculate offset from root (this is LOCAL position)
        Vector offset = targetLoc.toVector().subtract(fixedRootLocation.toVector());

        ModelData.PartData newPart = new ModelData.PartData(id);
        newPart.position = offset;
        newPart.material = Material.WHITE_CONCRETE; // Default material
        newPart.scale = new Vector(0.5, 0.5, 0.5); // Smaller default size
        currentModel.addPart(newPart);

        // Despawn and respawn to include new part
        currentInstance.despawn();
        currentInstance = new ModelInstance(currentModel, fixedRootLocation);
        currentInstance.setEditorMode(true);
        currentInstance.spawn();

        player.sendMessage("§aSpawned: " + id);
        player.sendMessage("§7Position: " + formatVector(offset));

        // Auto-select new part
        selectedPartId = id;
        ModelInstance.Part part = currentInstance.getParts().get(id);
        if (part != null && part.getEntity() != null) {
            selectedPart = part.getEntity();
            selectedPart.setGlowing(true);
            updateGizmo();
        }
    }

    private void handleSelect() {
        if (currentInstance == null || currentInstance.getParts().isEmpty()) {
            player.sendMessage("§7No parts to select");
            return;
        }

        // Deselect old part first
        if (selectedPart != null && selectedPart.isValid()) {
            selectedPart.setGlowing(false);
        }

        // Use improved raycast from ModelInstance
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        ModelInstance.Part hitPart = currentInstance.raycastPart(eyeLoc, direction, 10.0);

        if (hitPart != null && hitPart.getEntity() != null && hitPart.getEntity().isValid()) {
            selectedPart = hitPart.getEntity();
            selectedPartId = hitPart.getId();
            selectedPart.setGlowing(true);

            // Show detailed info
            ModelData.PartData data = hitPart.getData();
            player.sendMessage("§a§lSelected: " + selectedPartId);
            player.sendMessage("§7Position: §f" + formatVector(data.position));
            player.sendMessage("§7Rotation: §f" + formatVector(data.rotation));
            player.sendMessage("§7Scale: §f" + formatVector(data.scale));
            if (data.parentId != null) {
                player.sendMessage("§7Parent: §f" + data.parentId);
            }

            updateGizmo();
        } else {
            // Deselect
            selectedPart = null;
            selectedPartId = null;
            gizmoManager.hideGizmo();
            player.sendMessage("§7Deselected");
        }
    }

    private String formatVector(Vector v) {
        return String.format("%.2f, %.2f, %.2f", v.getX(), v.getY(), v.getZ());
    }

    private void refreshSelection() {
        if (selectedPartId != null && currentInstance != null) {
            ModelInstance.Part part = currentInstance.getParts().get(selectedPartId);
            if (part != null && part.getEntity() != null && part.getEntity().isValid()) {
                selectedPart = part.getEntity();
                selectedPart.setGlowing(true);
            }
        }
    }

    private void handleParent() {
        player.sendMessage("§eParent tool not yet implemented");
    }

    private void handleMaterial() {
        if (selectedPartId == null) {
            player.sendMessage("§cNo part selected!");
            return;
        }

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null || offhand.getType() == Material.AIR || !offhand.getType().isBlock()) {
            player.sendMessage("§cHold a block in offhand!");
            return;
        }

        ModelInstance.Part part = currentInstance.getParts().get(selectedPartId);
        if (part != null && selectedPart != null) {
            part.getData().material = offhand.getType();
            selectedPart.setBlock(offhand.getType().createBlockData());
            player.sendMessage("§aMaterial changed to " + offhand.getType());
        }
    }

    private void handleDelete() {
        if (selectedPartId == null) {
            player.sendMessage("§cNo part selected!");
            return;
        }

        currentModel.removePart(selectedPartId);

        // Respawn model
        currentInstance.despawn();
        currentInstance = new ModelInstance(currentModel, fixedRootLocation);
        currentInstance.setEditorMode(true); // Maintain editor mode
        currentInstance.spawn();

        selectedPart = null;
        selectedPartId = null;
        player.sendMessage("§cDeleted part");
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            EditorAction action = undoStack.pop();
            action.undo();
            redoStack.push(action);

            // Instant update
            currentInstance.markDirty();
            currentInstance.update();
            refreshSelection();
            player.sendMessage("§eUndid action");
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            EditorAction action = redoStack.pop();
            action.redo();
            undoStack.push(action);

            // Instant update
            currentInstance.markDirty();
            currentInstance.update();
            refreshSelection();
            player.sendMessage("§eRedid action");
        }
    }

    private void recordAction(EditorAction action) {
        undoStack.push(action);
        redoStack.clear();
    }


    public void tick() {
        // FIXED: Only refresh selection if it's actually deselected
        if (selectedPart != null && selectedPart.isValid()) {
            if (!selectedPart.isGlowing()) {
                selectedPart.setGlowing(true);
            }
        }

        // Crosshair movement with instant updates
        if (crosshairMoveMode && selectedPartId != null && currentInstance != null && fixedRootLocation != null) {
            updateCrosshairMovement();
        }

        // FIXED: Only spawn root marker once
        if (active && fixedRootLocation != null) {
            if (rootMarker == null || !rootMarker.isValid()) {
                spawnRootMarker();
            }
            // Don't recreate every tick!
        }

        // FIXED: Only update gizmo when selection changes or part moves
        // Remove the constant gizmo update from tick()
        // Instead, update gizmo only in:
        // - handleSelect() when selection changes
        // - handleTransformClick() after transform
        // - handleScrollTransform() after scroll
    }

    private void updateCrosshairMovement() {
        ModelInstance.Part part = currentInstance.getParts().get(selectedPartId);
        if (part == null) return;

        Location eyeLoc = player.getEyeLocation();
        Location targetLoc = eyeLoc.clone().add(eyeLoc.getDirection().multiply(crosshairDistance));
        Vector newOffset = targetLoc.toVector().subtract(fixedRootLocation.toVector());

        part.getData().position = newOffset;

        // Instant update for smooth crosshair following
        currentInstance.markDirty();
        currentInstance.update();
        refreshSelection();
    }

    private void spawnRootMarker() {
        if (fixedRootLocation == null) return;

        if (rootMarker != null && rootMarker.isValid()) {
            rootMarker.remove();
        }

        rootMarker = (BlockDisplay) fixedRootLocation.getWorld()
                .spawnEntity(fixedRootLocation, org.bukkit.entity.EntityType.BLOCK_DISPLAY);
        rootMarker.setBlock(Material.BEACON.createBlockData());
        rootMarker.setGlowing(true);
        rootMarker.setGlowColorOverride(org.bukkit.Color.YELLOW);

        org.bukkit.util.Transformation transform = new org.bukkit.util.Transformation(
                new org.joml.Vector3f(-0.25f, 0f, -0.25f),
                new org.joml.Quaternionf(),
                new org.joml.Vector3f(0.5f, 0.5f, 0.5f),
                new org.joml.Quaternionf()
        );
        rootMarker.setTransformation(transform);
    }

    public void requestChatInput(Consumer<String> callback) {
        this.awaitingChatInput = true;
        this.chatCallback = callback;
    }

    public boolean handleChatInput(String message) {
        if (!awaitingChatInput) return false;

        awaitingChatInput = false;
        if (chatCallback != null) {
            chatCallback.accept(message);
            chatCallback = null;
        }
        return true;
    }

    private ItemStack createTool(Material mat, String name, String id) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList("§8tool:" + id));
        item.setItemMeta(meta);
        return item;
    }

    public Player getPlayer() { return player; }
    public boolean isActive() { return active; }
    public GizmoManager getGizmoManager() { return gizmoManager; }
    public DebugVisualizer getDebugVisualizer() { return debugVisualizer; }
    public ModelInstance getCurrentInstance() { return currentInstance; }

    public interface EditorAction {
        void undo();
        void redo();
    }

    private static class TransformAction implements EditorAction {
        private final ModelData.PartData part;
        private final Vector oldPos, oldRot, oldScale;
        private final Vector newPos, newRot, newScale;

        public TransformAction(ModelData.PartData part, Vector oldPos, Vector oldRot, Vector oldScale) {
            this.part = part;
            this.oldPos = oldPos.clone();
            this.oldRot = oldRot.clone();
            this.oldScale = oldScale.clone();
            this.newPos = part.position.clone();
            this.newRot = part.rotation.clone();
            this.newScale = part.scale.clone();
        }

        @Override
        public void undo() {
            part.position = oldPos.clone();
            part.rotation = oldRot.clone();
            part.scale = oldScale.clone();
        }

        @Override
        public void redo() {
            part.position = newPos.clone();
            part.rotation = newRot.clone();
            part.scale = newScale.clone();
        }
    }


    private void updateGizmo() {
        if (selectedPart == null || !selectedPart.isValid()) {
            gizmoManager.hideGizmo();
            return;
        }

        if (currentInstance == null || selectedPartId == null) {
            gizmoManager.hideGizmo();
            return;
        }

        ModelInstance.Part part = currentInstance.getParts().get(selectedPartId);
        if (part == null) {
            gizmoManager.hideGizmo();
            return;
        }

        // Get the actual world location of the part
        Location partLoc = selectedPart.getLocation();

        // Get rotation for gizmo alignment
        Vector rot = part.getData().rotation;
        org.joml.Quaternionf rotation = new org.joml.Quaternionf()
                .rotateXYZ(
                        (float) Math.toRadians(rot.getX()),
                        (float) Math.toRadians(rot.getY()),
                        (float) Math.toRadians(rot.getZ())
                );

        gizmoManager.showGizmo(partLoc, rotation);

        // Debug message
        player.sendMessage("§8[Debug] Gizmo updated at: " +
                String.format("%.2f, %.2f, %.2f", partLoc.getX(), partLoc.getY(), partLoc.getZ()));
    }
}