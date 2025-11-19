package com.mcbzh.custombosses.editor;

import com.mcbzh.custombosses.editor.tools.EditorTool;
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

    public EditorSession(UUID playerId) {
        this.playerId = playerId;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }

    public void enterModelEdit(String modelId) {
        // Load or create model
        this.activeModel = com.mcbzh.custombosses.CustomBossesPlugin.getInstance().getConfigManager().getModel(modelId);
        if (this.activeModel == null) {
            this.activeModel = new ModelData(modelId);
            // Register it?
        }

        // Spawn instance for editing
        this.activeInstance = new ModelInstance(activeModel, getPlayer().getLocation());
        this.activeInstance.spawn();

        // Give tools
        setTool(0, new com.mcbzh.custombosses.editor.tools.SpawnTool(this));
        setTool(1, new com.mcbzh.custombosses.editor.tools.SelectTool(this));
        setTool(2, new com.mcbzh.custombosses.editor.tools.TransformTool(this,
                com.mcbzh.custombosses.editor.tools.TransformTool.TransformMode.MOVE));
        setTool(3, new com.mcbzh.custombosses.editor.tools.TransformTool(this,
                com.mcbzh.custombosses.editor.tools.TransformTool.TransformMode.ROTATE));
        setTool(4, new com.mcbzh.custombosses.editor.tools.TransformTool(this,
                com.mcbzh.custombosses.editor.tools.TransformTool.TransformMode.SCALE));
        setTool(5, new com.mcbzh.custombosses.editor.tools.ParentTool(this));
        setTool(6, new com.mcbzh.custombosses.editor.tools.MaterialTool(this));
        setTool(7, new com.mcbzh.custombosses.editor.tools.DuplicateTool(this));
        setTool(8, new com.mcbzh.custombosses.editor.tools.ExitTool(this));

        getPlayer().sendMessage("§eEntered Model Edit Mode for: " + modelId);
    }

    public void openHub() {
        new com.mcbzh.custombosses.editor.gui.EditorHub(this).open();
    }

    public void exitEditor() {
        if (activeModel != null) {
            com.mcbzh.custombosses.CustomBossesPlugin.getInstance().getConfigManager().saveModel(activeModel);
            getPlayer().sendMessage("§aModel '" + activeModel.getId() + "' saved.");
        }

        if (activeInstance != null) {
            activeInstance.despawn();
            activeInstance = null;
        }
        activeModel = null;
        selectedPart = null;
        gizmoManager.hideGizmo();
        hotbar.clear();
        getPlayer().getInventory().clear();
    }

    public void setSelectedPart(Entity part) {
        if (this.selectedPart != null) {
            this.selectedPart.setGlowing(false);
        }
        this.selectedPart = part;
        if (this.selectedPart != null) {
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
        for (EditorTool tool : hotbar.values()) {
            tool.onTick();
        }
    }

    private boolean waitingForChatInput = false;
    private java.util.function.Consumer<String> chatInputCallback;

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

    private final com.mcbzh.custombosses.editor.history.UndoStack undoStack = new com.mcbzh.custombosses.editor.history.UndoStack();

    public void recordAction(com.mcbzh.custombosses.editor.history.EditorAction action) {
        undoStack.push(action);
    }

    public void undo() {
        undoStack.undo();
        if (activeInstance != null)
            activeInstance.update();
        getPlayer().sendMessage("§eUndid last action.");
    }

    public void redo() {
        undoStack.redo();
        if (activeInstance != null)
            activeInstance.update();
        getPlayer().sendMessage("§eRedid last action.");
    }

    public void openAnimationHub() {
        new com.mcbzh.custombosses.editor.gui.AnimationHub(this).open(getPlayer());
    }
}
