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

public class DeleteTool extends EditorTool {

    public DeleteTool(EditorSession session) {
        super(session);
    }

    @Override
    public ItemStack getIcon() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cDelete Part");
        meta.setLore(java.util.List.of(
                "§7Right-Click: Delete selected part",
                "§7WARNING: Cannot be undone!"
        ));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onUse(Player player, Action action) {
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Entity selected = session.getSelectedPart();
        if (selected == null) {
            player.sendMessage("§cNo part selected! Use the Select tool first.");
            return;
        }

        // Find the part data
        ModelPart partToDelete = null;
        for (ModelPart part : session.getActiveInstance().getParts().values()) {
            if (part.getEntity() != null && part.getEntity().equals(selected)) {
                partToDelete = part;
                break;
            }
        }

        if (partToDelete == null) {
            player.sendMessage("§cCouldn't find part data!");
            return;
        }

        String partId = partToDelete.getId();

        // Check if any parts are parented to this one
        boolean hasChildren = session.getActiveModel().getParts().stream()
                .anyMatch(p -> partId.equals(p.getParentId()));

        if (hasChildren) {
            player.sendMessage("§cCannot delete! Other parts are parented to this one.");
            player.sendMessage("§7Unparent children first or delete them.");
            return;
        }

        // Remove from model data
        session.getActiveModel().getParts().removeIf(p -> p.getId().equals(partId));

        // Respawn instance
        session.getActiveInstance().despawn();
        session.getActiveInstance().spawn();

        session.setSelectedPart(null);
        player.sendMessage("§cDeleted part: §f" + partId);
    }
}