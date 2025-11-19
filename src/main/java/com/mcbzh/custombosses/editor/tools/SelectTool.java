package com.mcbzh.custombosses.editor.tools;

import com.mcbzh.custombosses.editor.EditorSession;
import com.mcbzh.custombosses.model.ModelInstance;
import com.mcbzh.custombosses.model.ModelPart;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;

public class SelectTool extends EditorTool {

    public SelectTool(EditorSession session) {
        super(session);
    }

    @Override
    public ItemStack getIcon() {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eSelect Part");
        meta.setLore(java.util.List.of(
                "§7Right-Click: Select part you're looking at",
                "§7Selected parts glow yellow"
        ));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onUse(Player player, Action action) {
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ModelInstance instance = session.getActiveInstance();
        if (instance == null) {
            player.sendMessage("§cNo active model instance!");
            return;
        }

        // Use Bukkit's built-in raytrace to find BlockDisplay entities
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                10.0, // 10 block range
                0.5,  // entity bounding box expansion
                entity -> entity instanceof BlockDisplay
        );

        if (result != null && result.getHitEntity() != null) {
            Entity hitEntity = result.getHitEntity();

            // Find which part this entity belongs to
            ModelPart foundPart = null;
            for (ModelPart part : instance.getParts().values()) {
                if (part.getEntity() != null && part.getEntity().equals(hitEntity)) {
                    foundPart = part;
                    break;
                }
            }

            if (foundPart != null) {
                session.setSelectedPart(hitEntity);
                player.sendMessage("§aSelected: §f" + foundPart.getId());
                player.sendMessage("§7Material: §f" + foundPart.getData().getMaterial().name());
                player.sendMessage("§7Parent: §f" + (foundPart.getData().getParentId() != null ?
                        foundPart.getData().getParentId() : "None (Root)"));
            } else {
                player.sendMessage("§cCouldn't identify this entity as a model part!");
            }
        } else {
            // Deselect
            session.setSelectedPart(null);
            player.sendMessage("§7Deselected (no part found in view)");
        }
    }

    @Override
    public void onTick() {
        // Show which part is currently selected
        Entity selected = session.getSelectedPart();
        if (selected != null && selected.isValid()) {
            // Keep it glowing (already set in setSelectedPart, but ensure it stays)
            if (!selected.isGlowing()) {
                selected.setGlowing(true);
            }
        }
    }
}