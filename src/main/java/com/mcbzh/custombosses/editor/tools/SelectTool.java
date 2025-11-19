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

public class SelectTool extends EditorTool {

    public SelectTool(EditorSession session) {
        super(session);
    }

    @Override
    public ItemStack getIcon() {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eSelect Part");
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onUse(Player player, Action action) {
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            // Custom Raycast for BlockDisplays
            // Iterate all parts, check AABB intersection
            com.mcbzh.custombosses.model.ModelInstance instance = session.getActiveInstance();
            if (instance == null)
                return;

            org.bukkit.Location eye = player.getEyeLocation();
            org.bukkit.util.Vector origin = eye.toVector();
            org.bukkit.util.Vector direction = eye.getDirection();

            com.mcbzh.custombosses.model.ModelPart closestPart = null;
            double closestDist = Double.MAX_VALUE;

            for (com.mcbzh.custombosses.model.ModelPart part : instance.getParts().values()) {
                org.joml.Vector3f pos = part.getGlobalPosition();
                // Simple sphere check for now (radius 0.75 to cover block)
                // Or AABB check if we want precision. Sphere is faster and usually "good
                // enough" for clicking blocks.
                // Let's use a slightly generous sphere.
                org.bukkit.util.Vector partCenter = new org.bukkit.util.Vector(pos.x, pos.y, pos.z);

                // Check if ray intersects sphere
                // Vector from ray origin to sphere center
                org.bukkit.util.Vector toSphere = partCenter.clone().subtract(origin);
                double projection = toSphere.dot(direction);

                // If projection is negative, sphere is behind
                if (projection < 0)
                    continue;

                // Closest point on ray to sphere center
                org.bukkit.util.Vector closestPoint = origin.clone().add(direction.clone().multiply(projection));
                double distToCenter = closestPoint.distance(partCenter);

                if (distToCenter < 0.8) { // 0.8 radius
                    double distFromPlayer = partCenter.distance(origin);
                    if (distFromPlayer < closestDist) {
                        closestDist = distFromPlayer;
                        closestPart = part;
                    }
                }
            }

            if (closestPart != null) {
                player.sendMessage("§eSelected: " + closestPart.getId());
                session.setSelectedPart(closestPart.getEntity());
            } else {
                player.sendMessage("§cNo part selected.");
                session.setSelectedPart(null);
            }
        }
    }
}
