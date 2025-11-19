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

        // Spawn instance at player location
        Location spawnLoc = player.getLocation().clone();
        currentInstance = new ModelInstance(currentModel, spawnLoc);
        currentInstance.spawn();

        active = true;

        // Give tools
        player.getInventory().clear();
        player.getInventory().setItem(0, createTool(Material.EMERALD, "§aSpawn Part", "spawn"));
        player.getInventory().setItem(1, createTool(Material.SPECTRAL_ARROW, "§eSelect Part", "select"));
        player.getInventory().setItem(2, createTool(Material.STICK, "§6Move Tool", "move"));
        player.getInventory().setItem(3, createTool(Material.BLAZE_ROD, "§6Rotate Tool", "rotate"));
        player.getInventory().setItem(4, createTool(Material.SLIME_BALL, "§6Scale Tool", "scale"));
        player.getInventory().setItem(5, createTool(Material.LEAD, "§dParent Tool", "parent"));
        player.getInventory().setItem(6, createTool(Material.MAGMA_CREAM, "§bMaterial Tool", "material"));
        player.getInventory().setItem(7, createTool(Material.TNT, "§cDelete Part", "delete"));
        player.getInventory().setItem(8, createTool(Material.BARRIER, "§cExit & Save", "exit"));

        player.sendMessage("§e=== Editing Model: " + modelId + " ===");
        player.sendMessage("§7Parts: " + currentModel.getParts().size());
        player.sendMessage("§7Use hotbar tools to edit");
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

        player.getInventory().clear();
        active = false;
        currentModel = null;
    }

    public void handleToolUse(String tool, boolean rightClick) {
        if (!active || currentInstance == null) return;

        switch (tool) {
            case "spawn" -> handleSpawn();
            case "select" -> handleSelect();
            case "move" -> handleTransform("position", rightClick ? 0.1 : -0.1);
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

    private void handleSpawn() {
        String id = "part_" + System.currentTimeMillis();

        // Spawn 3 blocks in front of player
        Location eyeLoc = player.getEyeLocation();
        Location spawnLoc = eyeLoc.clone().add(eyeLoc.getDirection().multiply(3.0));
        Vector offset = spawnLoc.toVector().subtract(currentInstance.getRootLocation().toVector());

        ModelData.PartData newPart = new ModelData.PartData(id);
        newPart.position = offset;
        currentModel.addPart(newPart);

        // Respawn to show new part
        Location root = currentInstance.getRootLocation();
        currentInstance.despawn();
        currentInstance.spawn();
        currentInstance.setRootLocation(root);
        currentInstance.update();

        player.sendMessage("§aSpawned: " + id);
    }

    private void handleSelect() {
        // Find BlockDisplay player is looking at
        BlockDisplay closest = null;
        double closestDist = Double.MAX_VALUE;

        for (ModelInstance.Part part : currentInstance.getParts().values()) {
            BlockDisplay entity = part.getEntity();
            if (entity == null) continue;

            double dist = player.getEyeLocation().distance(entity.getLocation());
            if (dist > 10) continue;

            Vector toEntity = entity.getLocation().toVector()
                    .subtract(player.getEyeLocation().toVector())
                    .normalize();
            double dot = toEntity.dot(player.getEyeLocation().getDirection());

            if (dot > 0.9 && dist < closestDist) {
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
                    .filter(p -> p.getEntity().equals(finalClosest))
                    .findFirst()
                    .map(ModelInstance.Part::getId)
                    .orElse("unknown");

            player.sendMessage("§aSelected: " + partId);
        } else {
            selectedPart = null;
            player.sendMessage("§7Deselected");
        }
    }

    private void handleTransform(String type, double delta) {
        if (selectedPart == null || !selectedPart.isValid()) {
            player.sendMessage("§cNo part selected!");
            return;
        }

        final BlockDisplay finalSelected = selectedPart; // Make effectively final
        ModelInstance.Part part = currentInstance.getParts().values().stream()
                .filter(p -> p.getEntity().equals(finalSelected))
                .findFirst()
                .orElse(null);

        if (part == null) return;

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
        if (selectedPart == null) {
            player.sendMessage("§cNo part selected!");
            return;
        }

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null || !offhand.getType().isBlock()) {
            player.sendMessage("§cHold a block in offhand!");
            return;
        }

        final BlockDisplay finalSelected = selectedPart; // Make effectively final
        ModelInstance.Part part = currentInstance.getParts().values().stream()
                .filter(p -> p.getEntity().equals(finalSelected))
                .findFirst()
                .orElse(null);

        if (part != null) {
            part.getData().material = offhand.getType();
            selectedPart.setBlock(offhand.getType().createBlockData());
            player.sendMessage("§aMaterial changed to " + offhand.getType());
        }
    }

    private void handleDelete() {
        if (selectedPart == null) {
            player.sendMessage("§cNo part selected!");
            return;
        }

        final BlockDisplay finalSelected = selectedPart; // Make effectively final
        ModelInstance.Part part = currentInstance.getParts().values().stream()
                .filter(p -> p.getEntity().equals(finalSelected))
                .findFirst()
                .orElse(null);

        if (part != null) {
            String id = part.getId();
            currentModel.removePart(id);

            Location root = currentInstance.getRootLocation();
            currentInstance.despawn();
            currentInstance.spawn();
            currentInstance.setRootLocation(root);
            currentInstance.update();

            selectedPart = null;
            player.sendMessage("§cDeleted: " + id);
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
