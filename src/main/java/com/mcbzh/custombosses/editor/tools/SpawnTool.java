package com.mcbzh.custombosses.editor.tools;

import com.mcbzh.custombosses.editor.EditorSession;
import com.mcbzh.custombosses.model.ModelPartData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

public class SpawnTool extends EditorTool {

    public SpawnTool(EditorSession session) {
        super(session);
    }

    @Override
    public ItemStack getIcon() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aSpawn Part");
        meta.setLore(java.util.List.of(
                "§7Right-Click Block: Spawn at block",
                "§7Right-Click Air: Spawn at your feet"
        ));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onUse(Player player, Action action) {
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }

        if (session.getActiveModel() == null) {
            player.sendMessage("§cNo active model! Use /cb editor to select or create a model first.");
            return;
        }

        String newPartId = "part_" + System.currentTimeMillis();
        Vector offset;

        if (action == Action.RIGHT_CLICK_BLOCK) {
            // Spawn at clicked block location
            Block targetBlock = player.getTargetBlockExact(10);
            if (targetBlock != null) {
                Location targetLoc = targetBlock.getLocation().add(0.5, 1.0, 0.5); // Center of block, 1 block up
                Location rootLoc = session.getActiveInstance().getRootLocation();

                // Calculate offset from root (simple subtraction, no rotation)
                offset = targetLoc.toVector().subtract(rootLoc.toVector());

                player.sendMessage("§7Spawning at block location");
            } else {
                offset = new Vector(0, 1, 0);
                player.sendMessage("§7No block targeted, spawning at default position");
            }
        } else {
            // Spawn at default position (in front of root)
            offset = new Vector(0, 1, 2);
            player.sendMessage("§7Spawning at default position");
        }

        // Create new part with white concrete
        ModelPartData newPart = new ModelPartData(
                newPartId,
                Material.WHITE_CONCRETE,
                offset,
                new Vector(0, 0, 0),
                new Vector(1, 1, 1));

        // Add to model data
        session.getActiveModel().addPart(newPart);

        // Respawn instance to show new part
        if (session.getActiveInstance() != null) {
            Location rootLoc = session.getActiveInstance().getRootLocation();
            session.getActiveInstance().despawn();
            session.getActiveInstance().spawn();
        }

        player.sendMessage("§aSpawned new part: §f" + newPartId);
        player.sendMessage("§7Offset from root: §f" + String.format("%.2f, %.2f, %.2f",
                offset.getX(), offset.getY(), offset.getZ()));
    }
}