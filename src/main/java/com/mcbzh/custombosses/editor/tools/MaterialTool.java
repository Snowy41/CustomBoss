package com.mcbzh.custombosses.editor.tools;

import com.mcbzh.custombosses.editor.EditorSession;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;

public class MaterialTool extends EditorTool {

    public MaterialTool(EditorSession session) {
        super(session);
    }

    @Override
    public ItemStack getIcon() {
        ItemStack item = new ItemStack(Material.MAGMA_CREAM);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§dMaterial Tool");
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onUse(Player player, Action action) {
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand == null || !offhand.getType().isBlock()) {
                player.sendMessage("§cHold a block in your offhand to apply it!");
                return;
            }

            RayTraceResult result = player.getWorld().rayTraceEntities(
                    player.getEyeLocation(),
                    player.getEyeLocation().getDirection(),
                    10.0,
                    e -> e instanceof BlockDisplay);

            if (result != null && result.getHitEntity() != null) {
                BlockDisplay bd = (BlockDisplay) result.getHitEntity();
                bd.setBlock(offhand.getType().createBlockData());
                player.sendMessage("§aApplied material: " + offhand.getType());
                // TODO: Update ModelPartData in session
            }
        }
    }
}
