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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Consumer;

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
        if (!active || currentInstance == null) return;

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
                        handleTransformClick();
                    }
                }
            }
            case "rotate" -> {
                if (rightClick) {
                    cycleAxis();
                } else {
                    currentTransformMode = TransformMode.ROTATE;
                    handleTransformClick();
                }
            }
            case "scale" -> {
                if (rightClick) {
                    cycleAxis();
                } else {
                    currentTransformMode = TransformMode.SCALE;
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

        currentInstance.update();
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

        currentInstance.update();
        refreshSelection();
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
        if (fixedRootLocation == null || currentModel == null || currentInstance == null) return;

        String id = "part_" + System.currentTimeMillis();

        Location eyeLoc = player.getEyeLocation();
        Location targetLoc = eyeLoc.clone().add(eyeLoc.getDirection().multiply(3.0));
        Vector offset = targetLoc.toVector().subtract(fixedRootLocation.toVector());

        ModelData.PartData newPart = new ModelData.PartData(id);
        newPart.position = offset;
        currentModel.addPart(newPart);

        currentInstance.despawn();
        currentInstance = new ModelInstance(currentModel, fixedRootLocation);
        currentInstance.spawn();

        player.sendMessage("§aSpawned: " + id);
    }

    private void handleSelect() {
        if (currentInstance == null || currentInstance.getParts().isEmpty()) {
            player.sendMessage("§7No parts to select");
            return;
        }

        // Raycast to find BlockDisplay
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        BlockDisplay closest = null;
        String closestId = null;
        double closestDist = Double.MAX_VALUE;

        for (Map.Entry<String, ModelInstance.Part> entry : currentInstance.getParts().entrySet()) {
            BlockDisplay entity = entry.getValue().getEntity();
            if (entity == null || !entity.isValid()) continue;

            Location partLoc = entity.getLocation();
            double dist = eyeLoc.distance(partLoc);

            if (dist > 10) continue;

            Vector toEntity = partLoc.toVector().subtract(eyeLoc.toVector()).normalize();
            double dot = toEntity.dot(direction);

            if (dot > 0.85 && dist < closestDist) {
                closest = entity;
                closestId = entry.getKey();
                closestDist = dist;
            }
        }

        // Deselect old
        if (selectedPart != null && selectedPart.isValid()) {
            selectedPart.setGlowing(false);
        }

        if (closest != null) {
            selectedPart = closest;
            selectedPartId = closestId;
            selectedPart.setGlowing(true);
            player.sendMessage("§aSelected: " + closestId);
        } else {
            selectedPart = null;
            selectedPartId = null;
            player.sendMessage("§7Deselected");
        }
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
        currentInstance.despawn();
        currentInstance = new ModelInstance(currentModel, fixedRootLocation);
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
        if (selectedPart != null && selectedPart.isValid() && !selectedPart.isGlowing()) {
            selectedPart.setGlowing(true);
        }

        // Crosshair movement
        if (crosshairMoveMode && selectedPartId != null && currentInstance != null && fixedRootLocation != null) {
            updateCrosshairMovement();
        }

        // Root marker
        if (active && fixedRootLocation != null) {
            if (rootMarker == null || !rootMarker.isValid()) {
                spawnRootMarker();
            }
        }

        // Gizmo update
        if (selectedPart != null && selectedPart.isValid() && currentInstance != null && selectedPartId != null) {
            ModelInstance.Part part = currentInstance.getParts().get(selectedPartId);
            if (part != null) {
                Vector rot = part.getData().rotation;
                org.joml.Quaternionf rotation = new org.joml.Quaternionf()
                        .rotateXYZ(
                                (float) Math.toRadians(rot.getX()),
                                (float) Math.toRadians(rot.getY()),
                                (float) Math.toRadians(rot.getZ())
                        );
                gizmoManager.showGizmo(selectedPart.getLocation(), rotation);
            }
        }
    }

    private void updateCrosshairMovement() {
        ModelInstance.Part part = currentInstance.getParts().get(selectedPartId);
        if (part == null) return;

        Location eyeLoc = player.getEyeLocation();
        Location targetLoc = eyeLoc.clone().add(eyeLoc.getDirection().multiply(crosshairDistance));
        Vector newOffset = targetLoc.toVector().subtract(fixedRootLocation.toVector());

        part.getData().position = newOffset;
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
}