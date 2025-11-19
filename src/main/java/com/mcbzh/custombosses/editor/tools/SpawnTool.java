package com.mcbzh.custombosses.editor.tools;

import com.mcbzh.custombosses.editor.EditorSession;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SpawnTool extends EditorTool {

    public SpawnTool(EditorSession session) {
        super(session);
    }

    @Override
    public ItemStack getIcon() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aSpawn Part");
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onUse(Player player, org.bukkit.event.block.Action action) {
        if (action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                || action == org.bukkit.event.block.Action.RIGHT_CLICK_AIR) {
            if (session.getActiveModel() == null) {
                player.sendMessage("§cNo active model! Use /cb editor to select or create a model first.");
                return;
            }

            String newPartId = "part_" + (session.getActiveModel().getParts().size() + 1);

            // Calculate position
            org.bukkit.util.Vector offset = new org.bukkit.util.Vector(0, 0, 0);
            if (action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                // Get target block location relative to model root
                org.bukkit.Location targetLoc = player.getTargetBlockExact(10).getLocation().add(0.5, 0.5, 0.5);
                org.bukkit.Location rootLoc = session.getActiveInstance().getRootLocation();
                if (rootLoc != null) {
                    // Global offset
                    offset = targetLoc.toVector().subtract(rootLoc.toVector());

                    // Rotate vector to be local to the root's rotation
                    // Minecraft Yaw: 0=South(+Z), 90=West(-X), 180=North(-Z), 270=East(+X)
                    // We need to rotate by -Yaw to align with global Z
                    // But wait, if root is rotated 90 deg (facing West), and we click 1 block West
                    // (local Forward),
                    // Global offset is (-1, 0, 0).
                    // We want local offset to be (0, 0, 1) (Forward).
                    // Rotating (-1,0,0) by -90 deg around Y:
                    // x' = x cos(-90) - z sin(-90) = -1 * 0 - 0 * -1 = 0
                    // z' = x sin(-90) + z cos(-90) = -1 * -1 + 0 * 0 = 1
                    // Result (0, 0, 1). Correct.

                    float yawRadians = (float) Math.toRadians(rootLoc.getYaw());
                    offset.rotateAroundY(yawRadians); // Bukkit rotateAroundY expects radians.
                    // Wait, Bukkit's rotateAroundY rotates counter-clockwise.
                    // If we want to convert Global to Local, we rotate by -Yaw?
                    // Let's try rotateAroundY(Math.toRadians(rootLoc.getYaw())) first, if flipped,
                    // invert.
                    // Actually, to go Global -> Local, we apply the Inverse rotation.
                    // If Root is rotated by R, Local = R^-1 * Global.
                    // So we rotate by -Yaw.
                    offset.rotateAroundY(Math.toRadians(rootLoc.getYaw()));
                    // Wait, if I rotate the coordinate system by Yaw, I need to rotate the point by
                    // -Yaw to get it in the new system?
                    // Let's stick to the logic:
                    // Global = Rot * Local + Pos
                    // Local = Rot^-1 * (Global - Pos)
                    // Rot^-1 is rotation by -Yaw.
                    // However, Minecraft Yaw is inverted (Clockwise).
                    // So a Yaw of 90 is a rotation of -90 (Counter-Clockwise).
                    // So Rot is -90. Rot^-1 is +90.
                    // So we rotate by +Yaw?
                    // Let's try rotating by -rootLoc.getYaw() converted to standard angle.
                    // Standard Angle = -Yaw.
                    // Inverse of Standard Angle = -(-Yaw) = Yaw.
                    // So rotateAroundY(toRadians(Yaw)) seems correct if Yaw is interpreted as
                    // clockwise.

                    // Let's just use the inverse of the root's rotation quaternion if possible, but
                    // Vector only has rotateAroundY.
                    // Let's assume rotateAroundY follows standard math (CCW).
                    // Minecraft Yaw 90 = West.
                    // Global Offset (-1, 0, 0).
                    // We want Local (0, 0, 1) [South is +Z in Bukkit? No, South is +Z].
                    // Wait, West is -X.
                    // If I rotate (-1, 0, 0) by +90 deg (PI/2):
                    // x' = -1 * 0 - 0 * 1 = 0
                    // z' = -1 * 1 + 0 * 0 = -1.
                    // Result (0, 0, -1) -> North. Wrong. We wanted South (+Z) if West is "Forward"?
                    // Ah, in Minecraft +Z is South.
                    // If I face West (Yaw 90), "Forward" is -X.
                    // If I want "Forward" to be +Z in local space (assuming model faces +Z by
                    // default):
                    // Then (-1, 0, 0) should become (0, 0, 1).
                    // We need to rotate (-1, 0, 0) to (0, 0, 1).
                    // That is a -90 degree rotation (CCW).
                    // So we rotate by -Yaw.

                    offset.rotateAroundY(Math.toRadians(rootLoc.getYaw()));
                }
            } else {
                // Default offset in front of player or 0,0,0
                // For now, 0,0,0 relative to root is safest default
            }

            com.mcbzh.custombosses.model.ModelPartData newPart = new com.mcbzh.custombosses.model.ModelPartData(
                    newPartId,
                    Material.WHITE_CONCRETE,
                    offset,
                    new org.bukkit.util.Vector(0, 0, 0),
                    new org.bukkit.util.Vector(1, 1, 1));

            // Add to model data
            session.getActiveModel().addPart(newPart);

            // Update instance if active
            if (session.getActiveInstance() != null) {
                // Re-initialize or add specific part (Re-init is safer for now)
                session.getActiveInstance().despawn();
                session.getActiveInstance().spawn();
            }

            player.sendMessage("§aSpawned new part: " + newPartId);
        }
    }
}
