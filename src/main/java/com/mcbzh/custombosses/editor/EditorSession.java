package com.mcbzh.custombosses.editor;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.model.ModelData;
import com.mcbzh.custombosses.model.ModelInstance;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

    // Root marker for model center
    private BlockDisplay rootMarker;
    private Location fixedRootLocation;

    // Advanced gizmo system
    private final GizmoManager gizmoManager;
    private final DebugVisualizer debugVisualizer;

    // Crosshair movement mode
    private boolean crosshairMoveMode = false;
    private double crosshairDistance = 3.0; // Distance from player eyes

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
        // Open main editor menu
        EditorGUI.openMainMenu(player, this);
    }

    public void editModel(String modelId) {
        // Load or create model
        currentModel = plugin.getModelStorage().get(modelId);
        if (currentModel == null) {
            currentModel = new ModelData(modelId);
            plugin.getModelStorage().save(currentModel);
        }

        // Set fixed root location at player's current position
        fixedRootLocation = player.getLocation().clone();

        // Spawn visual root marker
        spawnRootMarker();

        // Spawn instance at fixed root
        currentInstance = new ModelInstance(currentModel, fixedRootLocation);
        currentInstance.spawn();

        active = true;

        // Give tools
        player.getInventory().clear();
        player.getInventory().setItem(0, createTool(Material.EMERALD, "§aSpawn Part", "spawn"));
        player.getInventory().setItem(1, createTool(Material.SPECTRAL_ARROW, "§eSelect Part", "select"));
        player.getInventory().setItem(2, createTool(Material.STICK, "§6Move Tool §7(Right-click: Toggle Crosshair)", "move"));
        player.getInventory().setItem(3, createTool(Material.BLAZE_ROD, "§6Rotate Tool", "rotate"));
        player.getInventory().setItem(4, createTool(Material.SLIME_BALL, "§6Scale Tool", "scale"));
        player.getInventory().setItem(5, createTool(Material.LEAD, "§dParent Tool", "parent"));
        player.getInventory().setItem(6, createTool(Material.MAGMA_CREAM, "§bMaterial Tool", "material"));
        player.getInventory().setItem(7, createTool(Material.TNT, "§cDelete Part", "delete"));
        player.getInventory().setItem(8, createTool(Material.BARRIER, "§cExit & Save", "exit"));

        player.sendMessage("§e=== Editing Model: " + modelId + " ===");
        player.sendMessage("§7Parts: " + currentModel.getParts().size());
        player.sendMessage("§7Root at: " + String.format("%.1f, %.1f, %.1f",
                fixedRootLocation.getX(), fixedRootLocation.getY(), fixedRootLocation.getZ()));
        player.sendMessage("§7Use hotbar tools to edit");
        player.sendMessage("§e§lTips:");
        player.sendMessage("§7- Right-click Move Tool to enable crosshair mode");
        player.sendMessage("§7- Scroll while holding Scale/Rotate for fine control");
        player.sendMessage("§7- Hold block in offhand + Material Tool to change color");
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

        // Clean up gizmos and debug displays
        gizmoManager.hideGizmo();
        debugVisualizer.clearAll();

        player.getInventory().clear();
        active = false;
        currentModel = null;
        fixedRootLocation = null;
    }

    public void handleToolUse(String tool, boolean rightClick) {
        if (!active || currentInstance == null) return;

        switch (tool) {
            case "spawn" -> handleSpawn();
            case "select" -> handleSelect();
            case "move" -> {
                if (rightClick) {
                    toggleCrosshairMode();
                } else {
                    handleTransform("position", -0.1);
                }
            }
            case "rotate" -> handleTransform("rotation", rightClick ? 15 : -15);
            case "scale" -> handleTransform("scale", rightClick ? 0.1 : -0.1);
            case "parent" -> handleParent();
            case "material" -> handleMaterial();
            case "delete" -> handleDelete();
            case "exit" -> {
                exit();
                openHub();
            }
        }
    }

    public void handleScrollTransform(String type, double delta) {
        if (!active || currentInstance == null) return;
        handleTransform(type, delta);
    }

    public void adjustCrosshairDistance(double delta) {
        crosshairDistance = Math.max(0.5, Math.min(10.0, crosshairDistance + delta));
        player.sendMessage("§7Crosshair distance: §e" + String.format("%.1f", crosshairDistance) + " blocks");
    }

    private void toggleCrosshairMode() {
        crosshairMoveMode = !crosshairMoveMode;
        if (crosshairMoveMode) {
            if (selectedPart == null) {
                player.sendMessage("§cSelect a part first!");
                crosshairMoveMode = false;
                return;
            }
            player.sendMessage("§a§lCrosshair Move Mode: ON");
            player.sendMessage("§7Part will follow your crosshair");
            player.sendMessage("§7Scroll to adjust distance (current: " + String.format("%.1f", crosshairDistance) + ")");
            player.sendMessage("§7Right-click again to lock position");
        } else {
            player.sendMessage("§c§lCrosshair Move Mode: OFF");
            player.sendMessage("§7Position locked");
        }
    }

    private void handleSpawn() {
        if (fixedRootLocation == null || currentModel == null || currentInstance == null) {
            player.sendMessage("§cEditor not properly initialized!");
            return;
        }

        String id = "part_" + System.currentTimeMillis();

        // Spawn at player's current crosshair location (3 blocks ahead)
        Location eyeLoc = player.getEyeLocation();
        Location targetLoc = eyeLoc.clone().add(eyeLoc.getDirection().multiply(3.0));

        // Calculate offset relative to FIXED root location
        Vector offset = targetLoc.toVector().subtract(fixedRootLocation.toVector());

        ModelData.PartData newPart = new ModelData.PartData(id);
        newPart.position = offset;
        currentModel.addPart(newPart);

        // Respawn instance at the SAME fixed root
        currentInstance.despawn();
        currentInstance = new ModelInstance(currentModel, fixedRootLocation);
        currentInstance.spawn();

        player.sendMessage("§aSpawned: " + id + " at offset " +
                String.format("§7(%.2f, %.2f, %.2f)", offset.getX(), offset.getY(), offset.getZ()));
    }

    private void handleSelect() {
        if (currentInstance == null || currentInstance.getParts().isEmpty()) {
            player.sendMessage("§7No parts to select");
            return;
        }

        // Find BlockDisplay player is looking at
        BlockDisplay closest = null;
        double closestDist = Double.MAX_VALUE;

        Location eyeLoc = player.getEyeLocation();
        Vector lookDir = eyeLoc.getDirection().normalize();

        for (ModelInstance.Part part : currentInstance.getParts().values()) {
            BlockDisplay entity = part.getEntity();
            if (entity == null || !entity.isValid()) continue;

            Location partLoc = entity.getLocation();
            double dist = eyeLoc.distance(partLoc);

            // Skip if too far
            if (dist > 10) continue;

            // Calculate direction to entity
            Vector toEntity = partLoc.toVector()
                    .subtract(eyeLoc.toVector())
                    .normalize();

            // Check if player is looking at it (more lenient threshold)
            double dot = toEntity.dot(lookDir);

            if (dot > 0.7 && dist < closestDist) {
                closest = entity;
                closestDist = dist;
            }
        }

        // Deselect old
        if (selectedPart != null && selectedPart.isValid()) {
            selectedPart.setGlowing(false);
        }

        if (closest != null) {
            selectedPart = closest;
            selectedPart.setGlowing(true);

            final BlockDisplay finalClosest = closest; // Make effectively final for lambda
            String partId = currentInstance.getParts().values().stream()
                    .filter(p -> p.getEntity() != null && p.getEntity().equals(finalClosest))
                    .findFirst()
                    .map(ModelInstance.Part::getId)
                    .orElse("unknown");

            player.sendMessage("§aSelected: " + partId);
        } else {
            selectedPart = null;
            player.sendMessage("§7Deselected (no part in view)");
        }
    }

    private void handleTransform(String type, double delta) {
        if (selectedPart == null || !selectedPart.isValid()) {
            player.sendMessage("§cNo part selected!");
            return;
        }

        if (currentInstance == null) {
            player.sendMessage("§cInstance not available!");
            return;
        }

        final BlockDisplay finalSelected = selectedPart; // Make effectively final
        ModelInstance.Part part = currentInstance.getParts().values().stream()
                .filter(p -> p.getEntity() != null && p.getEntity().equals(finalSelected))
                .findFirst()
                .orElse(null);

        if (part == null) {
            player.sendMessage("§cPart not found in instance!");
            return;
        }

        ModelData.PartData data = part.getData();
        Vector oldPos = data.position.clone();
        Vector oldRot = data.rotation.clone();
        Vector oldScale = data.scale.clone();

        if (player.isSneaking()) {
            // Shift = Z axis
            switch (type) {
                case "position" -> data.position.setZ(data.position.getZ() + delta);
                case "rotation" -> data.rotation.setZ(data.rotation.getZ() + delta);
                case "scale" -> data.scale.setZ(Math.max(0.01, data.scale.getZ() + delta));
            }
        } else {
            // Normal = Y axis
            switch (type) {
                case "position" -> data.position.setY(data.position.getY() + delta);
                case "rotation" -> data.rotation.setY(data.rotation.getY() + delta);
                case "scale" -> data.scale.setY(Math.max(0.01, data.scale.getY() + delta));
            }
        }

        currentInstance.update();
        recordAction(new TransformAction(data, oldPos, oldRot, oldScale));

        player.sendMessage(String.format("§7%s: %.2f, %.2f, %.2f",
                type, data.position.getX(), data.position.getY(), data.position.getZ()));
    }

    private void handleParent() {
        player.sendMessage("§eParent tool not yet implemented");
    }

    private void handleMaterial() {
        if (selectedPart == null || !selectedPart.isValid()) {
            player.sendMessage("§cNo part selected!");
            return;
        }

        if (currentInstance == null) {
            player.sendMessage("§cInstance not available!");
            return;
        }

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null || offhand.getType() == Material.AIR || !offhand.getType().isBlock()) {
            player.sendMessage("§cHold a block in offhand!");
            return;
        }

        final BlockDisplay finalSelected = selectedPart; // Make effectively final
        ModelInstance.Part part = currentInstance.getParts().values().stream()
                .filter(p -> p.getEntity() != null && p.getEntity().equals(finalSelected))
                .findFirst()
                .orElse(null);

        if (part != null) {
            part.getData().material = offhand.getType();
            selectedPart.setBlock(offhand.getType().createBlockData());
            player.sendMessage("§aMaterial changed to " + offhand.getType());
        } else {
            player.sendMessage("§cPart not found!");
        }
    }

    private void handleDelete() {
        if (selectedPart == null || !selectedPart.isValid()) {
            player.sendMessage("§cNo part selected!");
            return;
        }

        if (currentInstance == null || fixedRootLocation == null) {
            player.sendMessage("§cInstance not available!");
            return;
        }

        final BlockDisplay finalSelected = selectedPart; // Make effectively final
        ModelInstance.Part part = currentInstance.getParts().values().stream()
                .filter(p -> p.getEntity() != null && p.getEntity().equals(finalSelected))
                .findFirst()
                .orElse(null);

        if (part != null) {
            String id = part.getId();
            currentModel.removePart(id);

            // Respawn at fixed root
            currentInstance.despawn();
            currentInstance = new ModelInstance(currentModel, fixedRootLocation);
            currentInstance.spawn();

            selectedPart = null;
            player.sendMessage("§cDeleted: " + id);
        } else {
            player.sendMessage("§cPart not found!");
        }
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            EditorAction action = undoStack.pop();
            action.undo();
            redoStack.push(action);
            currentInstance.update();
            player.sendMessage("§eUndid action");
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            EditorAction action = redoStack.pop();
            action.redo();
            undoStack.push(action);
            currentInstance.update();
            player.sendMessage("§eRedid action");
        }
    }

    private void recordAction(EditorAction action) {
        undoStack.push(action);
        redoStack.clear();
    }

    public void tick() {
        // Keep selected part glowing
        if (selectedPart != null && selectedPart.isValid() && !selectedPart.isGlowing()) {
            selectedPart.setGlowing(true);
        }

        // Crosshair movement mode
        if (crosshairMoveMode && selectedPart != null && selectedPart.isValid() && currentInstance != null && fixedRootLocation != null) {
            updateCrosshairMovement();
        }

        // Keep root marker visible and at correct location
        if (active && fixedRootLocation != null) {
            if (rootMarker == null || !rootMarker.isValid()) {
                spawnRootMarker();
            }
        }

        // Update gizmo for selected part
        if (selectedPart != null && selectedPart.isValid() && currentInstance != null) {
            final BlockDisplay finalSelected = selectedPart;
            ModelInstance.Part part = currentInstance.getParts().values().stream()
                    .filter(p -> p.getEntity() != null && p.getEntity().equals(finalSelected))
                    .findFirst()
                    .orElse(null);

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
        final BlockDisplay finalSelected = selectedPart;
        ModelInstance.Part part = currentInstance.getParts().values().stream()
                .filter(p -> p.getEntity() != null && p.getEntity().equals(finalSelected))
                .findFirst()
                .orElse(null);

        if (part == null) return;

        // Calculate target position from player's crosshair
        Location eyeLoc = player.getEyeLocation();
        Location targetLoc = eyeLoc.clone().add(eyeLoc.getDirection().multiply(crosshairDistance));

        // Convert to offset from root
        Vector newOffset = targetLoc.toVector().subtract(fixedRootLocation.toVector());

        // Update part position
        ModelData.PartData data = part.getData();
        data.position = newOffset;

        // Update visual
        currentInstance.update();
    }

    private void spawnRootMarker() {
        if (fixedRootLocation == null) return;

        // Remove old marker if exists
        if (rootMarker != null && rootMarker.isValid()) {
            rootMarker.remove();
        }

        // Spawn glowing beacon glass as root marker
        rootMarker = (BlockDisplay) fixedRootLocation.getWorld()
                .spawnEntity(fixedRootLocation, org.bukkit.entity.EntityType.BLOCK_DISPLAY);
        rootMarker.setBlock(Material.BEACON.createBlockData());
        rootMarker.setGlowing(true);
        rootMarker.setGlowColorOverride(org.bukkit.Color.YELLOW);

        // Make it small and centered
        org.bukkit.util.Transformation transform = new org.bukkit.util.Transformation(
                new org.joml.Vector3f(-0.25f, 0f, -0.25f), // Offset to center
                new org.joml.Quaternionf(),
                new org.joml.Vector3f(0.5f, 0.5f, 0.5f), // Half size
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

    // Editor Action interface
    public interface EditorAction {
        void undo();
        void redo();
    }

    // Transform Action
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