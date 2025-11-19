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

public class ParentTool extends EditorTool {

    private Entity firstSelection = null;

    public ParentTool(EditorSession session) {
        super(session);
    }

    @Override
    public ItemStack getIcon() {
        ItemStack item = new ItemStack(Material.LEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Parent Tool");
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onUse(Player player, Action action) {
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            RayTraceResult result = player.getWorld().rayTraceEntities(
                    player.getEyeLocation(),
                    player.getEyeLocation().getDirection(),
                    10.0,
                    e -> e instanceof BlockDisplay);

            if (result != null && result.getHitEntity() != null) {
                Entity hit = result.getHitEntity();

                if (firstSelection == null) {
                    firstSelection = hit;
                    player.sendMessage("§eSelected CHILD. Now click PARENT.");
                    hit.setGlowing(true);
                } else {
                    if (hit == firstSelection) {
                        player.sendMessage("§cCannot parent to itself!");
                        return;
                    }
                    player.sendMessage("§aLinked " + firstSelection.getUniqueId() + " to " + hit.getUniqueId());
                    firstSelection.setGlowing(false);
                    firstSelection = null;

                    // TODO: Update ModelPartData parentId
                }
            } else {
                if (firstSelection != null) {
                    player.sendMessage("§cCancelled parenting.");
                    firstSelection.setGlowing(false);
                    firstSelection = null;
                }
            }
        }
    }
}
