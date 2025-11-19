package com.mcbzh.custombosses.editor.tools;

import com.mcbzh.custombosses.editor.EditorSession;
import com.mcbzh.custombosses.model.ModelPart;
import com.mcbzh.custombosses.model.ModelPartData;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

public class TransformTool extends EditorTool {

    private final TransformMode mode;
    private Axis axis = Axis.Y;

    public enum TransformMode {
        MOVE, ROTATE, SCALE
    }

    public enum Axis {
        X, Y, Z, ALL
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
        meta.setDisplayName("§6" + mode.name() + " Tool §7[" + axis.name() + "]");
        meta.setLore(java.util.List.of(
                "§7Left-Click: Decrease",
                "§7Right-Click: Increase",
                "§7Shift+Left: Change axis",
                "§7Shift+Right: Enter exact value"
        ));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onUse(Player player, Action action) {
        Entity selected = session.getSelectedPart();
        if (selected == null) {
            player.sendMessage("§cNo part selected! Use the Select tool first.");
            return;
        }

        // Cycle axis with Shift + Left Click
        if (player.isSneaking() && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            axis = Axis.values()[(axis.ordinal() + 1) % Axis.values().length];
            player.sendMessage("§eAxis: §f" + axis.name());

            // Update item display
            player.getInventory().setItem(
                    player.getInventory().getHeldItemSlot(),
                    getIcon()
            );
            return;
        }

        // Precision mode with Shift + Right Click
        if (player.isSneaking() && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            player.sendMessage("§eEnter value for " + mode.name() + " " + axis.name() + ":");
            player.sendMessage("§7Type a number in chat (e.g. '1.5' or '45')");

            session.setWaitingForChatInput(true, (input) -> {
                try {
                    float value = Float.parseFloat(input);
                    applyValue(selected, value, player);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number: " + input);
                }
            });
            return;
        }

        // Normal increment/decrement
        float delta = (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) ? 1.0f : -1.0f;

        // Adjust delta based on mode
        delta = switch (mode) {
            case MOVE -> delta * 0.1f;      // 0.1 blocks per click
            case ROTATE -> delta * 15.0f;   // 15 degrees per click
            case SCALE -> delta * 0.1f;     // 0.1 scale per click
        };

        applyDelta(selected, delta, player);
    }

    private void applyValue(Entity selected, float value, Player player) {
        ModelPart part = getPart(selected);
        if (part == null) return;

        ModelPartData data = part.getData();
        Vector oldOffset = data.getOffset().clone();
        Vector oldRotation = data.getRotation().clone();
        Vector oldScale = data.getScale().clone();

        switch (mode) {
            case MOVE -> {
                switch (axis) {
                    case X -> data.getOffset().setX(value);
                    case Y -> data.getOffset().setY(value);
                    case Z -> data.getOffset().setZ(value);
                    case ALL -> data.setOffset(new Vector(value, value, value));
                }
                player.sendMessage(String.format("§aSet position %s to %.2f", axis.name(), value));
            }
            case ROTATE -> {
                switch (axis) {
                    case X -> data.getRotation().setX(value);
                    case Y -> data.getRotation().setY(value);
                    case Z -> data.getRotation().setZ(value);
                    case ALL -> data.setRotation(new Vector(value, value, value));
                }
                player.sendMessage(String.format("§aSet rotation %s to %.2f°", axis.name(), value));
            }
            case SCALE -> {
                // Prevent zero or negative scale
                if (value <= 0.01f) value = 0.01f;

                switch (axis) {
                    case X -> data.getScale().setX(value);
                    case Y -> data.getScale().setY(value);
                    case Z -> data.getScale().setZ(value);
                    case ALL -> data.setScale(new Vector(value, value, value));
                }
                player.sendMessage(String.format("§aSet scale %s to %.2f", axis.name(), value));
            }
        }

        recordAndUpdate(data, oldOffset, oldRotation, oldScale);
    }

    private void applyDelta(Entity selected, float delta, Player player) {
        ModelPart part = getPart(selected);
        if (part == null) return;

        ModelPartData data = part.getData();
        Vector oldOffset = data.getOffset().clone();
        Vector oldRotation = data.getRotation().clone();
        Vector oldScale = data.getScale().clone();

        switch (mode) {
            case MOVE -> {
                switch (axis) {
                    case X -> data.getOffset().setX(data.getOffset().getX() + delta);
                    case Y -> data.getOffset().setY(data.getOffset().getY() + delta);
                    case Z -> data.getOffset().setZ(data.getOffset().getZ() + delta);
                }
                player.sendMessage(String.format("§7%s: §f%.2f, %.2f, %.2f",
                        mode.name(),
                        data.getOffset().getX(),
                        data.getOffset().getY(),
                        data.getOffset().getZ()));
            }
            case ROTATE -> {
                switch (axis) {
                    case X -> data.getRotation().setX(data.getRotation().getX() + delta);
                    case Y -> data.getRotation().setY(data.getRotation().getY() + delta);
                    case Z -> data.getRotation().setZ(data.getRotation().getZ() + delta);
                }
                player.sendMessage(String.format("§7%s: §f%.1f°, %.1f°, %.1f°",
                        mode.name(),
                        data.getRotation().getX(),
                        data.getRotation().getY(),
                        data.getRotation().getZ()));
            }
            case SCALE -> {
                switch (axis) {
                    case X -> data.getScale().setX(Math.max(0.01, data.getScale().getX() + delta));
                    case Y -> data.getScale().setY(Math.max(0.01, data.getScale().getY() + delta));
                    case Z -> data.getScale().setZ(Math.max(0.01, data.getScale().getZ() + delta));
                    case ALL -> {
                        double newScale = Math.max(0.01, data.getScale().getX() + delta);
                        data.setScale(new Vector(newScale, newScale, newScale));
                    }
                }
                player.sendMessage(String.format("§7%s: §f%.2f, %.2f, %.2f",
                        mode.name(),
                        data.getScale().getX(),
                        data.getScale().getY(),
                        data.getScale().getZ()));
            }
        }

        recordAndUpdate(data, oldOffset, oldRotation, oldScale);
    }

    private void recordAndUpdate(ModelPartData data, Vector oldOffset, Vector oldRotation, Vector oldScale) {
        session.recordAction(new com.mcbzh.custombosses.editor.history.TransformAction(
                data, oldOffset, oldRotation, oldScale,
                data.getOffset(), data.getRotation(), data.getScale()));

        if (session.getActiveInstance() != null) {
            session.getActiveInstance().update();
        }
    }

    private ModelPart getPart(Entity entity) {
        if (session.getActiveInstance() != null) {
            for (ModelPart p : session.getActiveInstance().getParts().values()) {
                if (p.getEntity() != null && p.getEntity().equals(entity)) {
                    return p;
                }
            }
        }
        return null;
    }

    @Override
    public void onTick() {
        Entity selected = session.getSelectedPart();
        if (selected != null && selected.isValid()) {
            ModelPart part = getPart(selected);
            if (part != null) {
                // Show gizmo at part location
                session.getGizmoManager().showGizmo(
                        selected.getLocation(),
                        new org.joml.Quaternionf()
                );
            }
        } else {
            session.getGizmoManager().hideGizmo();
        }
    }
}