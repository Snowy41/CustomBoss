package com.mcbzh.custombosses.editor.tools;

import com.mcbzh.custombosses.editor.EditorSession;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TransformTool extends EditorTool {

    private final TransformMode mode;

    public enum TransformMode {
        MOVE, ROTATE, SCALE
    }

    public TransformTool(EditorSession session, TransformMode mode) {
        super(session);
        this.mode = mode;
    }

    @Override
    public ItemStack getIcon() {
        Material mat = switch (mode) {
            case MOVE -> Material.STICK;
            case ROTATE -> Material.BLAZE_ROD;
            case SCALE -> Material.SLIME_BALL;
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6" + mode.name() + " Tool");
        item.setItemMeta(meta);
        return item;
    }

    private Axis axis = Axis.Y;

    public enum Axis {
        X, Y, Z, ALL
    }

    @Override
    public void onUse(Player player, Action action) {
        Entity selected = session.getSelectedPart();
        if (selected == null) {
            player.sendMessage("§cSelect a part first!");
            return;
        }

        // Cycle Axis with Left Click Air (if not sneaking) or maybe Swap Hands?
        // Let's use Swap Hands event in EditorManager? No, simpler: Left Click Air
        // cycles axis, Right Click applies +Delta, Shift+Right applies -Delta?
        // Current: Left = -Delta, Right = +Delta.
        // Let's keep that.
        // How to cycle axis? Maybe Drop Item?
        // Or just use a command / chat?
        // Let's use Shift + Left Click to cycle axis.

        if (player.isSneaking() && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            axis = Axis.values()[(axis.ordinal() + 1) % Axis.values().length];
            player.sendMessage("§eSelected Axis: " + axis.name());
            return;
        }

        // Precision Mode via Chat (Shift + Right Click)
        if (player.isSneaking() && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            player.sendMessage(
                    "§eEnter a value in chat to set " + mode.name() + " on " + axis.name() + " (or 'cancel'):");
            session.setWaitingForChatInput(true, (input) -> {
                try {
                    float value = Float.parseFloat(input);
                    applyValue(selected, value);
                    player.sendMessage("§aSet " + mode.name() + " " + axis.name() + " to " + value);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number.");
                }
            });
            return;
        }

        float delta = 0.1f;
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            delta = -0.1f;
        }

        // Apply Delta
        applyDelta(selected, delta);
    }

    private void applyValue(Entity selected, float value) {
        com.mcbzh.custombosses.model.ModelPart part = getPart(selected);
        if (part == null)
            return;
        com.mcbzh.custombosses.model.ModelPartData data = part.getData();

        // Capture old state
        org.bukkit.util.Vector oldOffset = data.getOffset().clone();
        org.bukkit.util.Vector oldRotation = data.getRotation().clone();
        org.bukkit.util.Vector oldScale = data.getScale().clone();

        switch (mode) {
            case MOVE:
                if (axis == Axis.X)
                    data.getOffset().setX(value);
                if (axis == Axis.Y)
                    data.getOffset().setY(value);
                if (axis == Axis.Z)
                    data.getOffset().setZ(value);
                break;
            case ROTATE:
                if (axis == Axis.X)
                    data.getRotation().setX(value);
                if (axis == Axis.Y)
                    data.getRotation().setY(value);
                if (axis == Axis.Z)
                    data.getRotation().setZ(value);
                break;
            case SCALE:
                if (axis == Axis.ALL)
                    data.setScale(new org.bukkit.util.Vector(value, value, value));
                else {
                    if (axis == Axis.X)
                        data.getScale().setX(value);
                    if (axis == Axis.Y)
                        data.getScale().setY(value);
                    if (axis == Axis.Z)
                        data.getScale().setZ(value);
                }
                break;
        }

        recordAndUpdate(data, oldOffset, oldRotation, oldScale);
    }

    private void applyDelta(Entity selected, float delta) {
        com.mcbzh.custombosses.model.ModelPart part = getPart(selected);
        if (part == null)
            return;
        com.mcbzh.custombosses.model.ModelPartData data = part.getData();

        // Capture old state
        org.bukkit.util.Vector oldOffset = data.getOffset().clone();
        org.bukkit.util.Vector oldRotation = data.getRotation().clone();
        org.bukkit.util.Vector oldScale = data.getScale().clone();

        switch (mode) {
            case MOVE:
                if (axis == Axis.X)
                    data.setOffset(data.getOffset().add(new org.bukkit.util.Vector(delta, 0, 0)));
                if (axis == Axis.Y)
                    data.setOffset(data.getOffset().add(new org.bukkit.util.Vector(0, delta, 0)));
                if (axis == Axis.Z)
                    data.setOffset(data.getOffset().add(new org.bukkit.util.Vector(0, 0, delta)));
                break;
            case ROTATE:
                float rotDelta = delta * 15; // 15 degrees per click
                if (axis == Axis.X)
                    data.setRotation(data.getRotation().add(new org.bukkit.util.Vector(rotDelta, 0, 0)));
                if (axis == Axis.Y)
                    data.setRotation(data.getRotation().add(new org.bukkit.util.Vector(0, rotDelta, 0)));
                if (axis == Axis.Z)
                    data.setRotation(data.getRotation().add(new org.bukkit.util.Vector(0, 0, rotDelta)));
                break;
            case SCALE:
                if (axis == Axis.ALL)
                    data.setScale(data.getScale().add(new org.bukkit.util.Vector(delta, delta, delta)));
                else {
                    if (axis == Axis.X)
                        data.getScale().setX(data.getScale().getX() + delta);
                    if (axis == Axis.Y)
                        data.getScale().setY(data.getScale().getY() + delta);
                    if (axis == Axis.Z)
                        data.getScale().setZ(data.getScale().getZ() + delta);
                }
                break;
        }

        recordAndUpdate(data, oldOffset, oldRotation, oldScale);
    }

    private void recordAndUpdate(com.mcbzh.custombosses.model.ModelPartData data, org.bukkit.util.Vector oldOffset,
            org.bukkit.util.Vector oldRotation, org.bukkit.util.Vector oldScale) {
        session.recordAction(new com.mcbzh.custombosses.editor.history.TransformAction(
                data, oldOffset, oldRotation, oldScale,
                data.getOffset(), data.getRotation(), data.getScale()));
        session.getActiveInstance().update();
    }

    private com.mcbzh.custombosses.model.ModelPart getPart(Entity entity) {
        if (session.getActiveInstance() != null) {
            for (com.mcbzh.custombosses.model.ModelPart p : session.getActiveInstance().getParts().values()) {
                if (p.getEntity().equals(entity)) {
                    return p;
                }
            }
        }
        return null;
    }

    @Override
    public void onTick() {
        Entity selected = session.getSelectedPart();
        if (selected != null) {
            com.mcbzh.custombosses.model.ModelPart part = getPart(selected);
            if (part != null) {
                // We need global rotation for the gizmo
                // For now, let's just use the entity's location and a default rotation
                session.getGizmoManager().showGizmo(selected.getLocation(), new org.joml.Quaternionf());
            }
        } else {
            session.getGizmoManager().hideGizmo();
        }
    }
}
