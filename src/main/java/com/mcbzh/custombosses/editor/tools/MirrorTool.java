package com.mcbzh.custombosses.editor.tools;

import com.mcbzh.custombosses.editor.EditorSession;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MirrorTool extends EditorTool {

    public MirrorTool(EditorSession session) {
        super(session);
    }

    @Override
    public ItemStack getIcon() {
        ItemStack item = new ItemStack(Material.GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bMirror Tool");
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onUse(Player player, Action action) {
        if (session.getSelectedPart() == null) {
            player.sendMessage("§cSelect a part first!");
            return;
        }

        // Find part data
        com.mcbzh.custombosses.model.ModelPart part = null;
        if (session.getActiveInstance() != null) {
            for (com.mcbzh.custombosses.model.ModelPart p : session.getActiveInstance().getParts().values()) {
                if (p.getEntity().equals(session.getSelectedPart())) {
                    part = p;
                    break;
                }
            }
        }

        if (part == null)
            return;

        com.mcbzh.custombosses.model.ModelPartData originalData = part.getData();

        // Clone data
        com.mcbzh.custombosses.model.ModelPartData newData = new com.mcbzh.custombosses.model.ModelPartData(
                originalData.getId() + "_mirror",
                originalData.getParentId(),
                originalData.getMaterial());

        // Mirror X
        newData.setOffset(new org.bukkit.util.Vector(
                -originalData.getOffset().getX(),
                originalData.getOffset().getY(),
                originalData.getOffset().getZ()));

        // Mirror Rotation (Flip Y and Z usually for X mirror? Or just Y?
        // Simple mirror: Flip Position X. Rotation is tricky with Euler angles.
        // Let's just flip Position X for now.
        newData.setRotation(originalData.getRotation().clone());
        newData.setScale(originalData.getScale().clone());

        // Add to model
        session.getActiveModel().addPart(newData);

        // Respawn instance to show change
        session.getActiveInstance().despawn();
        session.getActiveInstance().spawn();

        player.sendMessage("§bMirrored part created: " + newData.getId());
    }
}
