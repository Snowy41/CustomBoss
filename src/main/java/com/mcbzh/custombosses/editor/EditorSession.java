package com.mcbzh.custombosses.editor;

import com.mcbzh.custombosses.editor.tools.*;
import com.mcbzh.custombosses.model.ModelData;
import com.mcbzh.custombosses.model.ModelInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditorSession {

    private final UUID playerId;
    private final Map<Integer, EditorTool> hotbar = new HashMap<>();

    private ModelInstance activeInstance;
    private ModelData activeModel;
    private com.mcbzh.custombosses.animation.AnimationData activeAnimation;
    private Entity selectedPart;
    private final GizmoManager gizmoManager = new GizmoManager();
    private boolean inEditor = false;

    private boolean waitingForChatInput = false;
    private java.util.function.Consumer<String> chatInputCallback;

    private final com.mcbzh.custombosses.editor.history.UndoStack undoStack =
            new com.mcbzh.custombosses.editor.history.UndoStack();

    public EditorSession(UUID playerId) {
        this.playerId = playerId;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }

    public boolean isInEditor() {
        return inEditor;
    }

    public void enterModelEdit(String modelId) {
        Player player = getPlayer();
        if (player == null) return;

        // Load or create model
        this.activeModel = com.mcbzh.custombosses.CustomBossesPlugin.getInstance()
                .getConfigManager().getModel(modelId);

        if (this.activeModel == null) {
            this.activeModel = new ModelData(modelId);
            com.mcbzh.custombosses.CustomBossesPlugin.getInstance()
                    .getConfigManager().saveModel(this.activeModel);
        }

        // Spawn instance for editing
        this.activeInstance = new ModelInstance(activeModel, player.getLocation());
        this.activeInstance.spawn();

        // Setup tools
        setupEditorTools();

        inEditor = true;
        player.sendMessage("§e=== Entered Model Edit Mode ===");
        player.sendMessage("§eEditing: §f" + modelId);
        player.sendMessage("§7Use items in hotbar to edit the model");
        player.sendMessage("§7Use §f/cb undo§7 and §f/cb redo§7 to undo/redo actions");
    }

    private void setupEditorTools() {
        setTool(0, new SpawnTool(this));
        setTool(1, new SelectTool(this));
        setTool(2, new TransformTool(this, TransformTool.TransformMode.MOVE));
        setTool(3, new TransformTool(this, TransformTool.TransformMode.ROTATE));
        setTool(4, new TransformTool(this, TransformTool.TransformMode.SCALE));
        setTool(5, new ParentTool(this));
        setTool(6, new MaterialTool(this));
        setTool(7, new DeleteTool(this));
        setTool(8, new ExitTool(this));
    }

    public void openHub() {
        Player player = getPlayer();
        if (player != null) {
            new com.mcbzh.custombosses.editor.gui.EditorHub(this).open();
        }
    }

    public void exitEditor() {
        Player player = getPlayer();

        if (activeModel != null) {
            com.mcbzh.custombosses.CustomBossesPlugin.getInstance()
                    .getConfigManager().saveModel(activeModel);

            if (player != null) {
                player.sendMessage("§aModel '" + activeModel.getId() + "' saved.");
            }
        }

        if (activeInstance != null) {
            activeInstance.despawn();
            activeInstance = null;
        }

        activeModel = null;
        selectedPart = null;
        gizmoManager.hideGizmo();
        hotbar.clear();
        inEditor = false;

        if (player != null) {
            player.getInventory().clear();
        }
    }

    public void setSelectedPart(Entity part) {
        if (this.selectedPart != null && this.selectedPart.isValid()) {
            this.selectedPart.setGlowing(false);
        }
        this.selectedPart = part;
        if (this.selectedPart != null && this.selectedPart.isValid()) {
            this.selectedPart.setGlowing(true);
        }
    }

    public Entity getSelectedPart() {
        return selectedPart;
    }

    public ModelInstance getActiveInstance() {
        return activeInstance;
    }

    public ModelData getActiveModel() {
        return activeModel;
    }

    public GizmoManager getGizmoManager() {
        return gizmoManager;
    }

    public com.mcbzh.custombosses.animation.AnimationData getActiveAnimation() {
        return activeAnimation;
    }

    public void setActiveAnimation(com.mcbzh.custombosses.animation.AnimationData animation) {
        this.activeAnimation = animation;
    }

    public void setTool(int slot, EditorTool tool) {
        hotbar.put(slot, tool);
        Player p = getPlayer();
        if (p != null) {
            p.getInventory().setItem(slot, tool.getIcon());
        }
    }

    public EditorTool getTool(int slot) {
        return hotbar.get(slot);
    }

    public void tick() {
        if (!inEditor) return;

        for (EditorTool tool : hotbar.values()) {
            tool.onTick();
        }
    }

    public void setWaitingForChatInput(boolean waiting, java.util.function.Consumer<String> callback) {
        this.waitingForChatInput = waiting;
        this.chatInputCallback = callback;
    }

    public boolean isWaitingForChatInput() {
        return waitingForChatInput;
    }

    public void handleChatInput(String input) {
        if (waitingForChatInput && chatInputCallback != null) {
            chatInputCallback.accept(input);
            waitingForChatInput = false;
            chatInputCallback = null;
        }
    }

    public void recordAction(com.mcbzh.custombosses.editor.history.EditorAction action) {
        undoStack.push(action);
    }

    public void undo() {
        Player player = getPlayer();
        undoStack.undo();
        if (activeInstance != null) {
            activeInstance.update();
        }
        if (player != null) {
            player.sendMessage("§eUndid last action.");
        }
    }

    public void redo() {
        Player player = getPlayer();
        undoStack.redo();
        if (activeInstance != null) {
            activeInstance.update();
        }
        if (player != null) {
            player.sendMessage("§eRedid last action.");
        }
    }

    public void openAnimationHub() {
        Player player = getPlayer();
        if (player != null) {
            new com.mcbzh.custombosses.editor.gui.AnimationHub(this).open(player);
        }
    }
}